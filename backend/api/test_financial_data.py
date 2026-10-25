from datetime import timedelta
from decimal import Decimal
from unittest.mock import MagicMock
import pytest
from django.contrib.auth import get_user_model
from django.core.management import call_command
from django.core.management.base import CommandError
from django.db import connection, transaction, IntegrityError
from django.urls import reverse
from django.utils import timezone
from rest_framework.test import APIClient
from api.models import Company, FinancialMetrics, PriceHistory
from api.views import financial_profile

pytestmark=pytest.mark.django_db
@pytest.fixture
def setup(monkeypatch):
    redis=MagicMock();redis.exists.return_value=0
    monkeypatch.setattr('api.redis_utils.get_redis_connection',lambda:redis)
    user=get_user_model().objects.create_user(username='metricas')
    client=APIClient();client.force_authenticate(user)
    company=Company.objects.create(ticker='DATA',name='Empresa de prueba',sector='technology',market_cap=100)
    return client,company

@pytest.mark.parametrize('zone,category,color',[('safe','strong','green'),('grey','moderate','orange'),('distress','weak','red'),('unknown','unknown','grey')])
def test_profile_categories(zone,category,color):
    result=financial_profile({'zone':zone})
    assert result['category']==category and result['color']==color

def test_summary_has_all_metrics_preserves_zero_and_joins_once(setup,django_assert_num_queries):
    client,company=setup
    FinancialMetrics.objects.create(company=company,as_of=timezone.localdate(),source='Fixture de prueba',is_demo=True,
        per=0,roe=-3,market_cap=0,beta=0,dividend_yield=0,total_assets=100,total_liabilities=100,
        working_capital=0,retained_earnings=0,ebit=0,equity_value=0,sales=200)
    quote=PriceHistory.objects.create(company=company,date=timezone.localdate(),adjusted_close=20,source='Fixture')
    company.latest_price=quote;company.save(update_fields=['latest_price'])
    with django_assert_num_queries(1):
        response=client.get(reverse('auth:company-summary',args=['DATA']))
    assert response.status_code==200
    data=response.json()
    assert data['per']==data['market_cap']==data['beta']==data['dividend_yield']==0
    assert data['roe']==-3 and data['quote']['adjusted_close']==20
    assert data['profile']['category']=='moderate'
    assert data['is_demo'] is True

def test_missing_metrics_are_null_not_invented(setup):
    data=setup[0].get(reverse('auth:company-summary',args=['DATA'])).json()
    assert data['beta'] is None and data['dividend_yield'] is None
    assert data['is_demo'] is True
    assert data['altman_z']['score'] is None

@pytest.mark.parametrize('period,days',[('1M',30),('6M',180),('1Y',365),('5Y',1200)])
def test_history_ranges_are_chronological_and_adjusted(setup,period,days):
    client,company=setup
    today=timezone.localdate()
    PriceHistory.objects.bulk_create([PriceHistory(company=company,date=today-timedelta(days=i),
        adjusted_close=Decimal('12.125'),source='Fixture',is_demo=True) for i in range(1200)])
    response=client.get(reverse('auth:company-history',args=['DATA']),{'range':period})
    assert response.status_code==200
    data=response.json();rows=data['history']
    assert data['price_type']=='adjusted_close' and data['is_demo'] is True
    assert rows==sorted(rows,key=lambda row:row['date'])
    assert rows[-1]['price']==12.125
    if period=='5Y':assert len(rows)==1200
    else:assert days-3<=len(rows)<=days+5

def test_history_is_repeatable_and_empty_is_explicit(setup):
    client,_=setup;url=reverse('auth:company-history',args=['DATA'])
    assert client.get(url).json()['history']==[]
    assert client.get(url).json()==client.get(url).json()
    assert client.get(url,{'range':'BAD'}).status_code==400
    assert client.get(reverse('auth:company-summary',args=['MISSING'])).status_code==404

def test_duplicate_price_rejected(setup):
    _,company=setup
    values=dict(company=company,date=timezone.localdate(),adjusted_close=1,source='Fixture')
    PriceHistory.objects.create(**values)
    with pytest.raises(IntegrityError),transaction.atomic():PriceHistory.objects.create(**values)

def test_csv_import_is_idempotent_and_updates_latest_quote(setup,tmp_path):
    _,company=setup;today=timezone.localdate().isoformat()
    path=tmp_path/'prices.csv';path.write_text(f'ticker,date,adjusted_close,volume\nDATA,{today},12.125,0\n',encoding='utf-8')
    call_command('import_market_data',prices=str(path),source='Fixture CSV',demo=True)
    call_command('import_market_data',prices=str(path),source='Fixture CSV',demo=True)
    assert PriceHistory.objects.count()==1
    company.refresh_from_db();assert company.latest_price.adjusted_close==Decimal('12.125')
    assert company.latest_price.is_demo is True

@pytest.mark.parametrize('invalid',['NaN','Infinity','0','-1','incorrecto'])
def test_invalid_csv_has_no_partial_writes(setup,tmp_path,invalid):
    today=timezone.localdate().isoformat();path=tmp_path/'prices.csv'
    path.write_text(f'ticker,date,adjusted_close\nDATA,{today},10\nDATA,2025-01-01,{invalid}\n',encoding='utf-8')
    with pytest.raises(CommandError):call_command('import_market_data',prices=str(path),source='Fixture',demo=True)
    assert not PriceHistory.objects.exists()

def test_metrics_import_requires_date_and_source(setup,tmp_path):
    _,company=setup;today=timezone.localdate().isoformat();path=tmp_path/'metrics.csv'
    path.write_text(f'ticker,as_of,per,roe,beta,dividend_yield,total_assets,total_liabilities,sales\nDATA,{today},0,-2,1.2,0,100,50,200\n',encoding='utf-8')
    call_command('import_market_data',metrics=str(path),source='Fixture de prueba',demo=True)
    data=FinancialMetrics.objects.get(company=company)
    assert data.per==0 and data.roe==-2 and data.working_capital is None

@pytest.mark.skipif(connection.vendor!='postgresql',reason='Verifica los índices PostgreSQL reales')
def test_postgresql_search_and_sector_indexes_exist():
    with connection.cursor() as cursor:
        cursor.execute("SELECT indexname,indexdef FROM pg_indexes WHERE tablename='companies'")
        indexes=dict(cursor.fetchall())
    assert 'USING gin' in indexes['company_search_gin']
    assert 'sector, market_cap DESC, ticker' in indexes['company_sector_cap_idx']
    assert any('(sector)' in definition for definition in indexes.values())


def test_summary_accepts_numeric_company_id(setup):
    client,company=setup
    response=client.get(reverse('auth:company-summary',args=[str(company.pk)]))
    assert response.status_code==200 and response.json()['ticker']=='DATA'
