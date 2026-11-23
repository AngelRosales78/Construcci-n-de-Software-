from datetime import datetime, timedelta, timezone as dt_timezone
from decimal import Decimal
from unittest.mock import patch
import uuid
import pytest
from django.contrib.auth import get_user_model
from django.utils import timezone
from rest_framework.test import APIClient
from .models import AccountPlan, AIUsage, AITariff, ProviderBudget, BudgetWindow, SystemErrorLog, SubscriptionTransaction
from .monitoring_services import record_ai, record_error, reserve_call, BudgetExceeded

pytestmark = pytest.mark.django_db
BASE='/api/v1/admin/monitoring/'


@pytest.fixture
def admin():
    user=get_user_model().objects.create_user(username='monitor',is_staff=True,is_superuser=True)
    client=APIClient();client.force_authenticate(user)
    return client


@pytest.mark.parametrize('path',['ai/','plans/','budgets/','subscriptions/','errors/'])
def test_monitoring_is_administrative_and_private(admin,path):
    assert APIClient().get(BASE+path).status_code==401
    user=get_user_model().objects.create_user(username='ordinary')
    client=APIClient();client.force_authenticate(user)
    assert client.get(BASE+path).status_code==403
    response=admin.get(BASE+path)
    assert response.status_code==200
    assert response['Cache-Control']=='private, no-store'


def test_ai_records_observed_tokens_and_preserves_rate_version(admin):
    started=timezone.now()
    rate=AITariff.objects.create(provider='local_ai',model='sp500-local-qwen3',version='v1',currency='USD',
        input_per_million=Decimal('2'),output_per_million=Decimal('4'),effective_at=started-timedelta(seconds=1))
    record_ai(started,.25,'success',{'tokens_evaluated':100,'tokens_predicted':50,'content':'secret prompt'})
    row=AIUsage.objects.get()
    assert row.input_tokens==100 and row.output_tokens==50 and row.duration_ms==250
    assert row.estimated_cost==Decimal('.0004') and row.tariff['version']=='v1'
    rate.input_per_million=100;rate.save()
    assert AIUsage.objects.get().estimated_cost==Decimal('.0004')
    result=admin.get(BASE+'ai/').json()
    assert result['calls']==1 and result['unpriced_calls']==0
    assert 'secret' not in str(result)


def test_missing_tokens_and_tariff_are_not_zero_or_fabricated(admin):
    record_ai(timezone.now(),.01,'error',{'tokens_evaluated':True,'tokens_predicted':-1})
    row=AIUsage.objects.get()
    assert row.input_tokens is None and row.output_tokens is None and row.estimated_cost is None
    result=admin.get(BASE+'ai/').json()
    assert result['unpriced_calls']==1 and result['estimated_costs']==[]
    assert result['breakdown'][0]['unreported_tokens']==1


def test_plan_counts_expiry_blocking_and_admin_role_independently(admin):
    users=[get_user_model().objects.create_user(username='person'+str(i),is_active=i!=3) for i in range(4)]
    for i,user in enumerate(users):
        AccountPlan.objects.create(user=user,plan='pro',expires_at=timezone.now()+timedelta(days=-1 if i==2 else 1))
    result=admin.get(BASE+'plans/').json()
    assert result['total']==5 and result['blocked']==1
    assert {r['plan']:r['count'] for r in result['results']}=={'free':3,'pro':2}


def test_budget_thresholds_block_before_call_and_reset_next_day(admin):
    budget=ProviderBudget.objects.create(provider='local_ai',daily_limit=100,reset_timezone='UTC')
    first=datetime(2026,1,2,12,tzinfo=dt_timezone.utc)
    with patch('api.monitoring_services.timezone.now',return_value=first):
        for _ in range(84): reserve_call('local_ai')
        assert not SystemErrorLog.objects.filter(code='budget_warning').exists()
        reserve_call('local_ai')
        assert SystemErrorLog.objects.filter(code='budget_warning').count()==1
        for _ in range(15): reserve_call('local_ai')
        with pytest.raises(BudgetExceeded): reserve_call('local_ai')
        assert BudgetWindow.objects.get().calls==100
    with patch('api.monitoring_services.timezone.now',return_value=first+timedelta(days=1)):
        reserve_call('local_ai')
    assert sorted(BudgetWindow.objects.values_list('calls',flat=True))==[1,100]


def test_budget_edit_is_versioned_and_invalid_zone_is_rejected(admin):
    admin.get(BASE+'budgets/')
    data={'version':0,'daily_limit':15,'reset_timezone':'UTC'}
    assert admin.patch(BASE+'budgets/local_ai/',data,format='json').status_code==200
    assert admin.patch(BASE+'budgets/local_ai/',data,format='json').status_code==409
    data.update(version=1,reset_timezone='Invalid/Zone')
    assert admin.patch(BASE+'budgets/local_ai/',data,format='json').status_code==400


def test_monthly_value_uses_original_currency_amount_and_overlap(admin):
    user=get_user_model().objects.create_user(username='subscriber')
    start=datetime(2026,10,1,tzinfo=timezone.get_current_timezone())
    SubscriptionTransaction.objects.create(user=user,request_id=uuid.uuid4(),outcome='approved',amount=30,currency='USD',
        duration_days=30,starts_at=start,ends_at=start+timedelta(days=30))
    AccountPlan.objects.create(user=user,plan='pro',active=True,auto_renew=False,cancelled_at=timezone.now())
    data=admin.get(BASE+'subscriptions/?month=2026-10').json()
    assert data['simulated'] is True and data['plans_included']==1
    assert data['results']==[{'currency':'USD','amount':'30.00'}]
    user.delete()
    assert SubscriptionTransaction.objects.get().user_id is None
    assert admin.get(BASE+'subscriptions/?month=2026-10').json()['results']==[{'currency':'USD','amount':'30.00'}]
    assert admin.get(BASE+'subscriptions/?month=bad').status_code==400


def test_errors_have_allowlisted_context_and_failure_does_not_recurse(admin):
    record_error('provider_unavailable','local_ai',http_status=503,token='secret',email='private@example.com',prompt='personal')
    row=SystemErrorLog.objects.get()
    assert row.context=={'http_status':503}
    data=admin.get(BASE+'errors/'+str(row.pk)+'/').json()
    assert 'secret' not in str(data) and 'private' not in str(data) and 'personal' not in str(data)
    with patch.object(SystemErrorLog.objects,'create',side_effect=RuntimeError('database unavailable')):
        record_error('unexpected_error','api')


def test_period_filters_reject_future_and_reverse_dates(admin):
    assert admin.get(BASE+'ai/?start=2026-02-01&end=2026-01-01').status_code==400
    assert admin.get(BASE+'ai/?end=2099-01-01').status_code==400
