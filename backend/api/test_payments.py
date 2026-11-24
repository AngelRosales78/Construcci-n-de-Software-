import copy
import hashlib
import hmac
from decimal import Decimal
from uuid import uuid4
from unittest.mock import Mock
import pytest
from django.core.cache import cache
from django.utils import timezone
from rest_framework.test import APIClient
from api.models import User, PaymentOrder, AccountPlan, SubscriptionTransaction
from api import payments

pytestmark=pytest.mark.django_db
ROOT='/api/v1/user/subscription/'
HOOK='/api/v1/payments/mercadopago/webhook/'

@pytest.fixture
def setup(settings,monkeypatch):
    settings.PAYMENT_PROVIDER='mercadopago_test'
    settings.MERCADOPAGO_ACCESS_TOKEN='APP_USR-not-a-real-token'
    settings.MERCADOPAGO_WEBHOOK_SECRET='unit-test-signing-secret-only'
    settings.MERCADOPAGO_TEST_SELLER_ID='123456'
    settings.MERCADOPAGO_TEST_BUYER_EMAIL='test_user_buyer@testuser.com'
    settings.MERCADOPAGO_RETURN_URL='https://app.example.test/profile/subscription'
    settings.MERCADOPAGO_WEBHOOK_URL='https://api.example.test'+HOOK
    settings.SIMULATED_PRO_PRICE='9.90';settings.SIMULATED_PRO_CURRENCY='PEN';settings.SIMULATED_PRO_DAYS=30
    settings.EMAIL_BACKEND='django.core.mail.backends.locmem.EmailBackend'
    cache.clear()
    user=User.objects.create_user(username='checkout-user',email='owner@example.test')
    client=APIClient();client.force_authenticate(user)
    provider={'order':None,'seller':{'id':123456,'site_id':'MPE','tags':['test_user']}}
    calls=[]
    def fake(method,path,payload=None,key=None):
        calls.append((method,path,copy.deepcopy(payload),key))
        if path=='/users/me':return copy.deepcopy(provider['seller'])
        if method=='POST':
            assert key and payload['total_amount']=='9.90'
            assert 'status' not in payload and 'outcome' not in payload
            provider['order'] = provider['order'] or dict(
                id='ORDTST01PAYMENT',external_reference=payload['external_reference'],user_id='123456',currency='PEN',
                country_code='PER',total_amount=payload['total_amount'],total_paid_amount='0.00',status='created',
                status_detail='created',checkout_url='https://www.mercadopago.com.pe/checkout/v1/redirect?order_id=ORDTST01PAYMENT')
        return copy.deepcopy(provider['order'])
    monkeypatch.setattr(payments,'request_api',fake)
    return client,user,provider,calls

def start(client,key=None):
    return client.post(ROOT+'checkout/',{'request_id':str(key or uuid4())},format='json')

def approved(provider):
    provider['order'].update(status='processed',status_detail='accredited',total_paid_amount='9.90',
        transactions={'payments':[{'status':'processed','status_detail':'accredited','paid_amount':'9.90'}]})

def notify(client,settings,identifier='ORDTST01PAYMENT',signature=None,**body):
    stamp='1780000000000';rid='signed-request-1'
    manifest=f'id:{identifier};request-id:{rid};ts:{stamp};'
    digest=hmac.new(settings.MERCADOPAGO_WEBHOOK_SECRET.encode(),manifest.encode(),hashlib.sha256).hexdigest()
    payload=dict(type='order',live_mode=False,data={'id':identifier});payload.update(body)
    return client.post(HOOK+'?data.id='+identifier,payload,format='json',
        HTTP_X_SIGNATURE=signature or f'ts={stamp},v1={digest}',HTTP_X_REQUEST_ID=rid)

def test_order_keeps_free_until_provider_and_signed_notification(setup,settings,django_capture_on_commit_callbacks):
    client,user,provider,calls=setup
    result=start(client)
    assert result.status_code==201 and result.data['payment']['test_mode']
    assert result.data['subscription']['effective_plan']=='free'
    order=PaymentOrder.objects.get()
    approved(provider)
    # A query string that claims approval and even a provider GET alone do not activate Pro.
    path=ROOT+'payment-orders/'+str(order.id)+'/'
    assert client.get(path+'?status=approved').data['subscription']['effective_plan']=='free'
    response=client.post(path,{},format='json')
    assert response.data['payment']['status']=='review'
    assert not SubscriptionTransaction.objects.exists()
    with django_capture_on_commit_callbacks(execute=True):
        assert notify(client,settings).status_code==200
    account=AccountPlan.objects.get(user=user)
    assert account.plan=='pro' and account.active and not account.auto_renew
    end=account.expires_at
    assert SubscriptionTransaction.objects.count()==1
    assert notify(client,settings).status_code==200
    assert client.post(path,{},format='json').data['subscription']['effective_plan']=='pro'
    account.refresh_from_db()
    assert account.expires_at==end and SubscriptionTransaction.objects.count()==1
    assert (end-timezone.now()).days in (29,30)

@pytest.mark.parametrize('payload',[{'outcome':'approved'},{'amount':'0.01'},{'currency':'USD'},{'status':'approved'},{'return_url':'https://evil.example'}])
def test_browser_cannot_choose_result_amount_or_return_url(setup,payload):
    client,user,provider,calls=setup
    result=client.post(ROOT+'checkout/',{'request_id':str(uuid4()),**payload},format='json')
    assert result.status_code==400 and not PaymentOrder.objects.exists() and not calls

def test_retries_reuse_order_and_new_attempt_is_blocked(setup):
    client,user,provider,calls=setup;key=uuid4()
    first=start(client,key);second=start(client,key)
    assert first.data['payment']['id']==second.data['payment']['id']
    assert len([c for c in calls if c[0]=='POST'])==1
    assert start(client).status_code==409
    assert PaymentOrder.objects.count()==1 and not SubscriptionTransaction.objects.exists()

def test_non_test_seller_is_rejected_before_order_creation(setup):
    client,user,provider,calls=setup
    provider['seller']['tags']=['normal']
    response=start(client)
    assert response.status_code==503 and not PaymentOrder.objects.exists()
    assert not any(method=='POST' for method,*_ in calls)

def test_not_configured_never_contacts_provider(setup,settings):
    client,user,provider,calls=setup;settings.MERCADOPAGO_ACCESS_TOKEN=''
    assert client.get('/api/v1/plans/').data['payment']['ready'] is False
    assert start(client).status_code==503
    assert not calls and not PaymentOrder.objects.exists()

@pytest.mark.parametrize('field,value',[('total_amount','0.01'),('currency','USD'),('external_reference','someone-else'),('user_id','99'),('country_code','ARG'),('live_mode',True)])
def test_mismatched_remote_data_never_activates_pro(setup,settings,field,value):
    client,user,provider,calls=setup;start(client);approved(provider)
    provider['order'][field]=value
    assert notify(client,settings).status_code==503
    assert not SubscriptionTransaction.objects.exists()
    assert AccountPlan.objects.get(user=user).plan=='free'

def test_invalid_signature_and_production_event_are_rejected(setup,settings):
    client,user,provider,calls=setup;start(client);approved(provider);before=len(calls)
    assert notify(client,settings,signature='ts=1,v1=bad').status_code==401
    assert len(calls)==before
    assert notify(client,settings,live_mode=True).status_code==400
    assert not SubscriptionTransaction.objects.exists()

def test_missing_full_payment_or_pending_status_never_grants_pro(setup,settings):
    client,user,provider,calls=setup;start(client)
    assert notify(client,settings).status_code==200 and not SubscriptionTransaction.objects.exists()
    approved(provider);provider['order']['transactions']['payments'][0]['paid_amount']='0.01'
    assert notify(client,settings).status_code==503 and not SubscriptionTransaction.objects.exists()

def test_foreign_account_cannot_read_or_refresh_order(setup):
    client,user,provider,calls=setup;response=start(client);before=len(calls)
    other=User.objects.create_user(username='other');client.force_authenticate(other)
    path=ROOT+'payment-orders/'+response.data['payment']['id']+'/'
    assert client.get(path).status_code==404 and client.post(path,{},format='json').status_code==404
    assert len(calls)==before

def test_refund_revokes_only_access_granted_by_this_order(setup,settings):
    client,user,provider,calls=setup;start(client);approved(provider);notify(client,settings)
    provider['order'].update(status='refunded',status_detail='refunded')
    assert notify(client,settings).status_code==200
    assert not AccountPlan.objects.get(user=user).active
    assert SubscriptionTransaction.objects.count()==1
    # An out-of-order approved event cannot restore refunded access.
    approved(provider);notify(client,settings)
    assert not AccountPlan.objects.get(user=user).active
    assert PaymentOrder.objects.get().status=='refunded'

def test_failure_and_cancelled_states_are_visible_without_receipt(setup,settings):
    client,user,provider,calls=setup;start(client)
    provider['order'].update(status='failed',status_detail='rejected_by_issuer')
    assert notify(client,settings).status_code==200
    assert PaymentOrder.objects.get().status=='declined' and not SubscriptionTransaction.objects.exists()

def test_network_failure_preserves_identical_retry_payload(setup,monkeypatch):
    client,user,provider,calls=setup;original=payments.request_api;failed=False
    def request(method,path,payload=None,key=None):
        nonlocal failed
        if method=='POST' and not failed:
            failed=True
            original(method,path,payload,key)
            raise payments.PaymentUnavailable()
        return original(method,path,payload,key)
    monkeypatch.setattr(payments,'request_api',request)
    key=uuid4();assert start(client,key).status_code==503
    assert start(client,key).status_code==201
    posts=[c for c in calls if c[0]=='POST']
    assert posts[0][2:]==posts[1][2:] and PaymentOrder.objects.count()==1

def test_signature_preserves_order_id_case(settings):
    settings.MERCADOPAGO_WEBHOOK_SECRET='signature-test-only'
    identifier='ORDTST01AbC';rid='abc-123';ts='123456789'
    digest=hmac.new(settings.MERCADOPAGO_WEBHOOK_SECRET.encode(),f'id:{identifier};request-id:{rid};ts:{ts};'.encode(),hashlib.sha256).hexdigest()
    assert payments.verify_signature(f'ts={ts},v1={digest}',rid,identifier)
    assert not payments.verify_signature(f'ts={ts},v1={digest}',rid,identifier.lower())

def test_return_ignores_claimed_approval_and_redirect_target(setup,settings):
    client,user,provider,calls=setup
    response=client.get('/api/v1/payments/mercadopago/return/?status=approved&return_url=https://evil.example')
    assert response.status_code==302
    assert response.url==settings.FRONTEND_URL.rstrip('/')+'/profile/subscription'
    assert not calls and not SubscriptionTransaction.objects.exists()

def test_refund_does_not_revoke_unrelated_admin_access(setup,settings):
    client,user,provider,calls=setup;start(client);approved(provider);notify(client,settings)
    account=AccountPlan.objects.get(user=user)
    account.source_transaction=None;account.expires_at=None;account.save()
    provider['order'].update(status='refunded',status_detail='refunded')
    notify(client,settings)
    account.refresh_from_db()
    assert account.active and account.expires_at is None

@pytest.mark.django_db(transaction=True)
def test_two_confirmations_grant_only_one_term(setup):
    from concurrent.futures import ThreadPoolExecutor
    from threading import Barrier
    from django.db import close_old_connections
    client,user,provider,calls=setup
    start(client);approved(provider)
    order=PaymentOrder.objects.get()
    order.notification_verified_at=timezone.now();order.save()
    barrier=Barrier(2)
    def confirm(_):
        close_old_connections()
        try:
            barrier.wait(timeout=10)
            result=payments.reconcile(order.id)
            return result.receipt_id
        finally:close_old_connections()
    with ThreadPoolExecutor(max_workers=2) as pool:
        receipts=list(pool.map(confirm,range(2)))
    assert receipts[0]==receipts[1]
    assert SubscriptionTransaction.objects.count()==1
    assert AccountPlan.objects.get(user=user).source_transaction_id==receipts[0]
