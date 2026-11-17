from datetime import timedelta
from unittest.mock import patch
from uuid import uuid4
from concurrent.futures import ThreadPoolExecutor
from threading import Barrier
import pytest
from django.core import mail
from django.db import close_old_connections
from django.utils import timezone
from django.test import override_settings
from rest_framework.test import APIClient
from .models import User, AccountPlan, SubscriptionTransaction
from .subscription_views import deliver_receipt

pytestmark = pytest.mark.django_db
ROOT = '/api/v1/user/subscription/'


@pytest.fixture
def account(settings):
    with override_settings(EMAIL_BACKEND='django.core.mail.backends.locmem.EmailBackend',
                           SIMULATED_PRO_PRICE='9.90', SIMULATED_PRO_DAYS=30):
        user = User.objects.create_user(username='subscription', email='demo@example.test')
        client = APIClient()
        client.force_authenticate(user)
        yield client, user


def checkout(client, outcome='approved', key=None):
    return client.post(ROOT + 'checkout/', {'request_id': str(key or uuid4()), 'outcome': outcome}, format='json')


def test_approval_uses_configured_tariff_preserves_role_and_sends_receipt(account, settings, django_capture_on_commit_callbacks):
    client, user = account
    settings.SIMULATED_PRO_PRICE = '12.50'
    settings.SIMULATED_PRO_DAYS = 40
    with django_capture_on_commit_callbacks(execute=True):
        response = checkout(client)
    assert response.status_code == 201
    data = response.data
    assert data['receipt']['amount'] == '12.50'
    assert data['subscription']['features'] == ['compare']
    record = SubscriptionTransaction.objects.get()
    assert record.ends_at - record.starts_at == timedelta(days=40)
    assert record.email_status == 'sent'
    assert len(mail.outbox) == 1 and str(record.id) in mail.outbox[0].body
    assert mail.outbox[0].alternatives and 'SIMULADO' in mail.outbox[0].body
    user.refresh_from_db()
    assert not user.is_staff and not user.is_superuser


def test_rejection_never_activates_or_sends_email(account, django_capture_on_commit_callbacks):
    client, user = account
    with django_capture_on_commit_callbacks(execute=True) as callbacks:
        response = checkout(client, 'declined')
    assert response.status_code == 201
    assert response.data['subscription']['effective_plan'] == 'free'
    assert response.data['receipt']['ends_at'] is None
    assert not callbacks
    assert AccountPlan.objects.get(user=user).plan == 'free'


def test_idempotency_conflicting_payload_and_active_plan(account):
    client, _ = account
    key = uuid4()
    first = checkout(client, key=key)
    second = checkout(client, key=key)
    assert second.status_code == 200
    assert first.data['receipt']['id'] == second.data['receipt']['id']
    assert checkout(client, 'declined', key).status_code == 409
    assert checkout(client).status_code == 409
    assert SubscriptionTransaction.objects.count() == 1


def test_cancellation_preserves_term_is_idempotent_and_expires(account):
    client, user = account
    key = uuid4()
    checkout(client, key=key)
    before = AccountPlan.objects.get(user=user)
    response = client.post(ROOT + 'cancel/', {}, format='json')
    assert response.data['features'] == ['compare']
    assert not response.data['auto_renew']
    assert client.post(ROOT + 'cancel/', {}, format='json').data['cancelled_at'] == response.data['cancelled_at']
    assert checkout(client, key=key).status_code == 200
    after = AccountPlan.objects.get(user=user)
    assert after.expires_at == before.expires_at and not after.auto_renew
    with patch('api.access_views.timezone.now', return_value=before.expires_at + timedelta(seconds=1)):
        assert client.get(ROOT).data['effective_plan'] == 'free'
        assert client.get('/api/v1/compare/?a=MSFT&b=META&fiscal_year=2024').status_code == 403
    assert client.post(ROOT + 'cancel/', {'expires_at': '2099-01-01'}, format='json').status_code == 400


def test_legacy_admin_assignment_can_cancel_and_preserves_role(account):
    client, user = account
    AccountPlan.objects.create(user=user, plan='pro')
    first = client.post(ROOT + 'cancel/', {}, format='json')
    assert first.status_code == 200
    assert first.data['features'] == ['compare'] and first.data['expires_at']
    assert client.post(ROOT + 'cancel/', {}, format='json').data['expires_at'] == first.data['expires_at']


def test_expired_account_can_start_new_period_without_rewriting_receipt(account, settings):
    client, user = account
    checkout(client)
    original = SubscriptionTransaction.objects.get()
    AccountPlan.objects.filter(user=user).update(expires_at=timezone.now()-timedelta(days=1))
    settings.SIMULATED_PRO_PRICE = '20.00'
    assert checkout(client).status_code == 201
    original.refresh_from_db()
    assert str(original.amount) == '9.90'
    assert SubscriptionTransaction.objects.count() == 2


def test_receipts_and_history_are_private(account):
    client, _ = account
    record = checkout(client).data['receipt']
    url = ROOT + 'receipts/' + record['id'] + '/'
    assert client.get(url).status_code == 200
    other = User.objects.create_user(username='other')
    client.force_authenticate(other)
    assert client.get(url).status_code == 404
    assert client.get(ROOT + 'history/').data['receipts'] == []
    assert APIClient().get(url).status_code == 401
    assert APIClient().post(ROOT + 'checkout/', {}).status_code == 401
    assert client.get(ROOT + 'history/')['Cache-Control'] == 'private, no-store'


@pytest.mark.parametrize('payload', [
    {'outcome': 'approved'}, {'request_id': 'bad', 'outcome': 'approved'},
    {'request_id': str(uuid4()), 'outcome': 'approved', 'card_number': '4242'},
    {'request_id': str(uuid4()), 'outcome': 'paid'},
])
def test_checkout_rejects_invalid_or_payment_data(account, payload):
    client, _ = account
    assert client.post(ROOT+'checkout/', payload, format='json').status_code == 400
    assert not SubscriptionTransaction.objects.exists()


def test_email_failure_keeps_activation_and_retry_does_not_resend(account):
    client, _ = account
    checkout(client)
    record = SubscriptionTransaction.objects.get()
    with patch('api.subscription_views.EmailMultiAlternatives.send', side_effect=OSError('transport')):
        deliver_receipt(record.id)
    record.refresh_from_db()
    assert record.email_status == 'pending'
    assert client.get(ROOT).data['features'] == ['compare']
    deliver_receipt(record.id)
    deliver_receipt(record.id)
    assert len(mail.outbox) == 1


def test_plan_catalog_uses_server_settings(account, settings):
    settings.SIMULATED_PRO_CURRENCY = 'USD'
    client, _ = account
    data = client.get('/api/v1/plans/').data
    assert data['simulated'] and data['currency'] == 'USD'
    assert len(data['features']) == 5
    assert all(row['pro'] for row in data['features'])
    assert sum(not row['free'] for row in data['features']) == 2


@pytest.mark.django_db(transaction=True)
def test_concurrent_checkout_is_a_single_operation(account):
    _, user = account
    barrier = Barrier(2)
    key = uuid4()

    def request():
        close_old_connections()
        try:
            client = APIClient()
            client.force_authenticate(User.objects.get(pk=user.pk))
            barrier.wait(timeout=10)
            response = checkout(client, key=key)
            return response.status_code, response.data['receipt']['id']
        finally:
            close_old_connections()

    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(lambda _: request(), range(2)))
    assert sorted(status for status, _ in results) == [200, 201]
    assert len({identifier for _, identifier in results}) == 1
    assert SubscriptionTransaction.objects.count() == 1
