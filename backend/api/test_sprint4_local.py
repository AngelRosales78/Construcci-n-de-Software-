"""Sprint 4 en el servidor local: planes asignados, comprobantes, IA local, catálogo y primer administrador."""
import io
import json
import os
import re
from datetime import timedelta
from unittest.mock import patch
import pytest
from django.core.cache import cache
from django.core.management import CommandError, call_command
from django.utils import timezone
from rest_framework.test import APIClient
from rest_framework_simplejwt.tokens import RefreshToken
from .billing import PERIOD_DAYS, purchase, settle
from .local_ai import IDS, optional_local_ai
from .models import AdministrationEvent, AIUsage, Company, Invoice, ProviderBudget, User, UserSubscription
from .serializers import CustomTokenObtainPairSerializer

pytestmark = pytest.mark.django_db
PASSWORD = 'Prueba-local-9381'
CARD = '4242424242424242'


def signed(user):
    client = APIClient()
    client.credentials(HTTP_AUTHORIZATION=f'Bearer {CustomTokenObtainPairSerializer.get_token(user).access_token}')
    return client


@pytest.fixture
def admin():
    return User.objects.create_user('jefe', 'jefe@example.test', PASSWORD, is_staff=True, is_superuser=True)


@pytest.fixture
def person():
    return User.objects.create_user('persona', 'persona@example.test', PASSWORD)


def test_assigned_pro_has_access_without_charge_renewal_or_expiry(admin, person):
    result = signed(admin).patch(f'/api/v1/admin/users/{person.pk}/', {'plan': 'pro'}, format='json')
    assert result.status_code == 200 and result.data['plan'] == 'pro'
    client = signed(person)
    access = client.get('/api/v1/user/subscription/').data
    assert access['plan'] == 'pro' and access['active'] and 'compare' in access['features']
    assert access['grant_origin'] == 'administrator' and access['period_end'] is None and access['price'] is None
    assert not Invoice.objects.exists()
    UserSubscription.objects.update(valid_until=timezone.now() - timedelta(days=400))
    assert settle(UserSubscription.objects.get()).plan == 'pro' and not Invoice.objects.exists()
    assert signed(admin).get('/api/v1/admin/users/?plan=pro').data['count'] == 1
    assert client.post('/api/v1/subscription/cancel/', {}, format='json').status_code == 400
    assert signed(admin).patch(f'/api/v1/admin/users/{person.pk}/', {'plan': 'free'}, format='json').data['plan'] == 'free'
    assert client.get('/api/v1/user/subscription/').data['plan'] == 'free'
    assert signed(admin).get('/api/v1/admin/users/?plan=pro').data['count'] == 0


def test_assigning_pro_keeps_a_paid_period(admin, person):
    purchase(person, 'compra-1', CARD)
    before = UserSubscription.objects.get().valid_until
    assert signed(admin).patch(f'/api/v1/admin/users/{person.pk}/', {'plan': 'pro'}, format='json').status_code == 200
    row = UserSubscription.objects.get()
    assert row.valid_until == before and row.auto_renew and not row.granted


def test_paid_plan_replaces_an_assignment_after_free(admin, person):
    signed(admin).patch(f'/api/v1/admin/users/{person.pk}/', {'plan': 'pro'}, format='json')
    signed(admin).patch(f'/api/v1/admin/users/{person.pk}/', {'plan': 'free'}, format='json')
    purchase(person, 'compra-2', CARD)
    row = UserSubscription.objects.get()
    assert row.plan == 'pro' and not row.granted and row.valid_until > timezone.now() + timedelta(days=PERIOD_DAYS - 1)


def test_deleted_account_keeps_an_anonymous_receipt(admin, person):
    purchase(person, 'compra-3', CARD)
    assert signed(person).delete('/api/v1/auth/account/', {'password': PASSWORD}, format='json').status_code == 204
    receipt = Invoice.objects.get()
    assert receipt.user_id is None and receipt.card_last4 == '' and receipt.card_brand == ''
    assert not User.objects.filter(username='persona').exists()
    month = timezone.localdate().strftime('%Y-%m')
    assert signed(admin).get(f'/api/v1/admin/monitoring/subscriptions/?month={month}').data['transactions'] == 1


def test_blocked_account_loses_open_session_and_cannot_refresh(admin, person):
    client = signed(person)
    refresh = str(CustomTokenObtainPairSerializer.get_token(person))
    assert client.get('/api/v1/auth/profile/').status_code == 200
    body = {'is_active': False, 'reason': 'Prueba'}
    assert signed(admin).patch(f'/api/v1/admin/users/{person.pk}/', body, format='json').status_code == 200
    assert client.get('/api/v1/auth/profile/').status_code == 401
    assert APIClient().post('/api/v1/auth/refresh/', {'refresh': refresh}, format='json').status_code == 401
    assert signed(admin).patch(f'/api/v1/admin/users/{person.pk}/', {'is_active': True}, format='json').status_code == 200
    assert client.get('/api/v1/auth/profile/').status_code == 401


def test_sessions_opened_before_this_version_remain_valid(person):
    old = RefreshToken.for_user(person)
    assert 'sv' not in old.payload
    client = APIClient()
    client.credentials(HTTP_AUTHORIZATION=f'Bearer {old.access_token}')
    assert client.get('/api/v1/auth/profile/').status_code == 200


def test_grant_admin_command_requires_one_account_and_protects_the_last(person):
    with pytest.raises(CommandError):
        call_command('grant_admin', 'nadie')
    call_command('grant_admin', 'PERSONA@example.test')
    person.refresh_from_db()
    assert person.is_staff and person.is_superuser and person.session_version == 1
    assert AdministrationEvent.objects.get().action == 'role_set_locally'
    with pytest.raises(CommandError):
        call_command('grant_admin', 'persona', revoke=True)


def test_inactive_company_is_hidden_from_search_and_autocomplete(person):
    Company.objects.create(ticker='VIS', name='Visible Uno', sector='technology')
    Company.objects.create(ticker='OCU', name='Visible Dos', sector='technology', is_active=False)
    client = signed(person)
    assert [row['ticker'] for row in client.get('/api/v1/companies/search/?q=Visible').data['results']] == ['VIS']
    rows = [{'ticker': 'VIS', 'name': 'Visible Uno'}, {'ticker': 'OCU', 'name': 'Visible Dos'}]
    with patch('api.search_cache.autocomplete', return_value=rows):
        assert [r['ticker'] for r in client.get('/api/v1/companies/autocomplete/?q=vis').data['suggestions']] == ['VIS']
    assert client.get('/api/v1/companies/OCU/overview/').data['is_active'] is False


def ai_report():
    return {'ticker': 'TEST', 'fiscal_year': 2024, 'level': 'simple', 'generation_mode': 'rules', 'validation': {'status': 'passed'},
            'pillars': [{'pillar_id': key, 'text': 'Valor verificado: 2,00', 'metric_ids': list(ids)} for key, ids in IDS.items()]}


def ai_reply(request, **kwargs):
    topic = re.search(r'Tema: ([^.]+)', json.loads(request.data)['messages'][-1]['content']).group(1)
    text = f'El pilar de {topic} permite examinar los indicadores del balance con los datos disponibles y sus limitaciones.'
    return io.BytesIO(json.dumps({'done': True, 'done_reason': 'stop', 'prompt_eval_count': 11, 'eval_count': 7,
                                  'message': {'content': json.dumps({'explanation': text})}}).encode())


def test_local_ai_counts_real_calls_and_not_the_cache():
    cache.clear()
    with patch.dict(os.environ, {'LOCAL_AI_MODEL': 'modelo-prueba'}), patch('api.local_ai.urlopen', side_effect=ai_reply):
        assert optional_local_ai(ai_report())['generation_mode'] == 'ai'
        optional_local_ai(ai_report())
    rows = AIUsage.objects.all()
    assert rows.count() == 5 and {r.model for r in rows} == {'modelo-prueba'}
    assert {(r.input_tokens, r.output_tokens, r.outcome) for r in rows} == {(11, 7, 'success')}


def test_local_ai_stops_at_the_internal_budget_and_keeps_the_report():
    cache.clear()
    ProviderBudget.objects.create(provider='local_ai', daily_limit=2)
    with patch.dict(os.environ, {'LOCAL_AI_MODEL': 'modelo-prueba'}), patch('api.local_ai.urlopen', side_effect=ai_reply) as calls:
        report = optional_local_ai(ai_report())
    assert calls.call_count == 2 and report['generation_mode'] == 'rules' and 'presupuesto' in report['generation_notice']
