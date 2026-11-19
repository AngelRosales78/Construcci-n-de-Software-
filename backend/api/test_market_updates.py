from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone as dt_timezone
from hashlib import sha256
from io import StringIO
import json
from threading import Barrier, Event
from uuid import uuid4
from urllib.error import HTTPError
import fakeredis
import pytest
from django.db import close_old_connections
from django.core.management import call_command
from django.utils import timezone
from rest_framework.test import APIClient
from .market_provider import PROVIDER, SourceError, download, parse_prices, source_url
from .market_updates import reserve_call
from .models import AdministrationEvent, Company, FinancialMetrics, MarketDataOperation, PriceHistory, ProviderDailyUsage, User

pytestmark = pytest.mark.django_db
BASE = '/api/v1/admin/companies/'
NOW = datetime(2026, 11, 19, 18, tzinfo=dt_timezone.utc)


def chart(ticker='MSFT'):
    dates = [int(datetime(2026,11,day,14,30,tzinfo=dt_timezone.utc).timestamp()) for day in (16,17,18,19)]
    return {'chart': {'error': None, 'result': [{
        'meta': {'symbol': ticker, 'instrumentType': 'EQUITY', 'currency': 'USD',
                 'exchangeTimezoneName': 'America/New_York', 'longName': 'Empresa de ensayo', 'exchangeName': 'NMS'},
        'timestamp': dates, 'indicators': {'adjclose': [{'adjclose': [100.25,101.5,102.75,9999]}],
                                          'quote': [{'volume': [10,20,30,40]}]}}]}}


@pytest.fixture
def market(monkeypatch, settings):
    settings.MARKET_DATA_MIN_INTERVAL = 0
    monkeypatch.setattr(timezone, 'now', lambda: NOW)
    redis = fakeredis.FakeRedis(decode_responses=True)
    monkeypatch.setattr('api.search_cache.get_redis_connection', lambda: redis)
    monkeypatch.setattr('api.redis_utils.get_redis_connection', lambda: redis)
    calls = []
    def download(ticker):
        calls.append(ticker)
        return json.dumps(chart(ticker)).encode()
    monkeypatch.setattr('api.market_updates.download', download)
    admin = User.objects.create_superuser(username='market_admin', email='admin@example.test', password='Prueba!2026')
    client = APIClient(); client.force_authenticate(admin)
    company = Company.objects.create(ticker='MSFT', name='Microsoft', sector='technology')
    return client, admin, company, calls


def refresh(client, company, key=None):
    return client.post(BASE+f'{company.pk}/refresh/', {'request_id': str(key or uuid4())}, format='json')


def verify(client, ticker='NEW', key=None, sector='technology'):
    return client.post(BASE+'verify/', {'request_id': str(key or uuid4()), 'ticker': ticker, 'sector': sector}, format='json')


def test_prices_are_adjusted_dated_and_exclude_current_session(market):
    data = parse_prices(json.dumps(chart()).encode(), 'MSFT')
    assert [row['date'].day for row in data['prices']] == [16,17,18]
    assert str(data['prices'][-1]['adjusted_close']) == '102.750000'
    assert data['source'] == source_url('MSFT')
    assert all(not row['is_demo'] for row in data['prices'])


@pytest.mark.parametrize('status,expected', [(404,400),(422,400),(429,429),(403,503),(500,503)])
def test_http_failures_are_converted_to_safe_errors(market,monkeypatch,status,expected):
    class Opener:
        def open(self,request,timeout):
            assert request.full_url == source_url('MSFT') and timeout == 12
            raise HTTPError(request.full_url,status,'remote details',{},None)
    monkeypatch.setattr('api.market_provider.build_opener',lambda _:Opener())
    with pytest.raises(SourceError) as failure: download('MSFT')
    assert failure.value.status == expected and 'remote details' not in str(failure.value)


@pytest.mark.parametrize('damage', ['symbol','currency','instrument','adjclose','duplicate','negative','volume','length','nan','name'])
def test_incompatible_responses_are_rejected_before_any_write(market, damage):
    data = chart(); row = data['chart']['result'][0]
    if damage == 'symbol': row['meta']['symbol'] = 'OTHER'
    elif damage == 'currency': row['meta']['currency'] = 'EUR'
    elif damage == 'instrument': row['meta']['instrumentType'] = 'ETF'
    elif damage == 'adjclose': row['indicators'].pop('adjclose')
    elif damage == 'duplicate': row['timestamp'][1] = row['timestamp'][0]
    elif damage == 'negative': row['indicators']['adjclose'][0]['adjclose'][0] = -1
    elif damage == 'volume': row['indicators']['quote'][0]['volume'][0] = True
    elif damage == 'length': row['timestamp'].pop()
    elif damage == 'nan': row['indicators']['adjclose'][0]['adjclose'][0] = float('nan')
    elif damage == 'name': row['meta']['longName'] = None
    with pytest.raises(SourceError): parse_prices(json.dumps(data).encode(), 'MSFT')
    assert not PriceHistory.objects.exists()


def test_refresh_is_idempotent_and_never_overwrites_balance_metrics(market):
    client, _, company, calls = market
    metrics = FinancialMetrics.objects.create(company=company, as_of='2024-12-31', source='SEC FY2024', per=10)
    key = uuid4()
    response = refresh(client, company, key)
    assert response.status_code == 200 and response.data['operation']['status'] == 'succeeded'
    assert response.data['operation']['result']['new_dates'] == 3
    assert PriceHistory.objects.filter(company=company).count() == 3
    assert refresh(client, company, key).data['operation']['id'] == response.data['operation']['id']
    assert len(calls) == 1 and ProviderDailyUsage.objects.get().calls == 1
    # Una segunda consulta real actualiza fechas existentes sin duplicarlas.
    assert refresh(client, company).data['operation']['result']['new_dates'] == 0
    assert PriceHistory.objects.count() == 3
    metrics.refresh_from_db(); assert metrics.per == 10 and str(metrics.as_of) == '2024-12-31'
    operation = MarketDataOperation.objects.get(pk=response.data['operation']['id'])
    assert sha256(bytes(operation.raw_response)).hexdigest() == operation.response_sha256
    listing = client.get(BASE).data['results'][0]
    assert listing['coverage']['price_as_of'].day == 18
    assert listing['last_update']['status'] == 'succeeded'
    assert 'raw_response' not in listing['last_update']


def test_demo_restart_import_cannot_overwrite_refreshed_prices(market,tmp_path):
    client,_,company,_=market
    assert refresh(client,company).status_code==200
    csv=tmp_path/'old-prices.csv'
    csv.write_text('ticker,date,adjusted_close,volume,currency\nMSFT,2026-11-16,50,10,USD\n',encoding='utf-8')
    call_command('import_market_data',prices=str(csv),source='Old demonstration snapshot',preserve_refreshed=True,stdout=StringIO())
    row=PriceHistory.objects.get(company=company,date='2026-11-16')
    assert str(row.adjusted_close)=='100.250000' and row.source==source_url('MSFT')


def test_source_failure_is_visible_and_retains_all_previous_data(market, monkeypatch):
    client, _, company, _ = market
    old = PriceHistory.objects.create(company=company, date='2026-11-17', adjusted_close=50, source='https://example.test/old')
    company.latest_price=old; company.save(update_fields=['latest_price'])
    def broken(ticker): raise SourceError('source_unavailable','La fuente no respondió.',503)
    monkeypatch.setattr('api.market_updates.download', broken)
    key = uuid4(); response = refresh(client,company,key)
    assert response.status_code == 503 and response.data['operation']['status'] == 'failed'
    old.refresh_from_db(); company.refresh_from_db()
    assert old.adjusted_close == 50 and company.latest_price_id == old.pk
    assert PriceHistory.objects.count() == 1
    assert refresh(client,company,key).status_code == 503
    assert ProviderDailyUsage.objects.get().calls == 1
    assert client.get(BASE).data['results'][0]['last_update']['error_code'] == 'source_unavailable'


def test_verify_does_not_create_until_confirmed_and_confirmation_is_idempotent(market):
    client, admin, _, calls = market
    ready = verify(client, 'new').data['operation']
    assert ready['status'] == 'ready' and ready['result']['name'] == 'Empresa de ensayo'
    assert not Company.objects.filter(ticker='NEW').exists()
    response = client.post(BASE, {'verification_id': ready['id']}, format='json')
    assert response.status_code == 201
    created = Company.objects.get(ticker='NEW')
    assert not created.is_index_member and not created.is_pilot and created.is_active
    assert created.identity_source_url == source_url('NEW') and created.sector_overridden
    assert created.prices.count() == 3 and not FinancialMetrics.objects.filter(company=created).exists()
    assert len(calls) == 1
    assert client.post(BASE, {'verification_id': ready['id']}, format='json').status_code == 200
    assert Company.objects.filter(ticker='NEW').count() == 1
    assert AdministrationEvent.objects.get(action='company_added').actor == admin
    summary = client.get('/api/v1/companies/NEW/summary/').data
    assert summary['is_index_member'] is False and summary['altman_z']['score'] is None
    assert summary['per'] is None and summary['quote']['currency'] == 'USD'


def test_verification_is_private_expires_and_cannot_be_rebound(market):
    client, _, _, calls = market
    key=uuid4(); ready=verify(client,key=key).data['operation']
    assert verify(client,key=key,sector='energy').status_code == 409
    assert len(calls)==1
    other=User.objects.create_superuser(username='other',email='other@example.test',password='Prueba!2026')
    client.force_authenticate(other)
    assert client.get('/api/v1/admin/market-operations/'+ready['id']+'/').status_code == 404
    assert client.post(BASE, {'verification_id':ready['id']},format='json').status_code==404
    operation=MarketDataOperation.objects.get(pk=ready['id'])
    client.force_authenticate(operation.actor)
    MarketDataOperation.objects.filter(pk=operation.pk).update(finished_at=NOW-timedelta(minutes=11))
    assert client.post(BASE,{'verification_id':ready['id']},format='json').status_code==409
    assert not Company.objects.filter(ticker='NEW').exists()


def test_duplicate_and_invalid_requests_do_not_consume_calls(market):
    client, _, company, calls = market
    assert verify(client,'MSFT').status_code==409
    assert verify(client,'https://a').status_code==400
    assert client.post(BASE,{'ticker':'NEW','sector':'technology'},format='json').status_code==400
    assert client.post(BASE+'verify/',{'ticker':'NEW','sector':'invalid','request_id':str(uuid4())},format='json').status_code==400
    assert not calls
    regular=User.objects.create_user(username='normal')
    client.force_authenticate(regular)
    assert refresh(client,company).status_code==403 and verify(client).status_code==403
    assert not ProviderDailyUsage.objects.exists()


@pytest.mark.parametrize('kind',['currency','older','invalid'])
def test_failed_validation_does_not_partially_replace_history(market,monkeypatch,kind):
    client,_,company,_=market
    old=PriceHistory.objects.create(company=company,date='2026-11-19' if kind=='older' else '2026-11-17',
        adjusted_close=50,currency='EUR' if kind=='currency' else 'USD',source='https://example.test/old')
    if kind=='invalid': monkeypatch.setattr('api.market_updates.download',lambda ticker:b'not json')
    response=refresh(client,company)
    assert response.status_code in (409,502)
    assert PriceHistory.objects.count()==1
    old.refresh_from_db(); assert old.adjusted_close==50


def test_role_revoked_during_http_request_cannot_write_prices(market,monkeypatch):
    client,admin,company,_=market
    def revoked(ticker):
        User.objects.filter(pk=admin.pk).update(is_staff=False,is_superuser=False)
        return json.dumps(chart(ticker)).encode()
    monkeypatch.setattr('api.market_updates.download',revoked)
    assert refresh(client,company).status_code==403
    assert not PriceHistory.objects.exists()
    assert MarketDataOperation.objects.get().status=='failed'


def test_internal_quota_and_new_utc_day_are_real_counters(market,settings):
    client,_,company,calls=market
    settings.MARKET_DATA_DAILY_LIMIT=1
    assert refresh(client,company).status_code==200
    response=refresh(client,company)
    assert response.status_code==429 and response.data['operation']['error_code']=='internal_quota'
    assert len(calls)==1
    # Un contador de ayer no consume la ventana actual.
    ProviderDailyUsage.objects.update(day=NOW.date()-timedelta(days=1))
    assert refresh(client,company).status_code==200
    assert ProviderDailyUsage.objects.count()==2


@pytest.mark.django_db(transaction=True)
def test_concurrent_refresh_reuses_operation_and_rejects_another_job(market,monkeypatch):
    _,admin,company,_=market
    entered,release=Event(),Event()
    def waiting(ticker):
        entered.set(); assert release.wait(10)
        return json.dumps(chart(ticker)).encode()
    monkeypatch.setattr('api.market_updates.download',waiting)
    key=uuid4()
    def first():
        close_old_connections()
        try:
            client=APIClient(); client.force_authenticate(User.objects.get(pk=admin.pk))
            return refresh(client,company,key).status_code
        finally: close_old_connections()
    with ThreadPoolExecutor(max_workers=1) as pool:
        future=pool.submit(first)
        try:
            assert entered.wait(10)
            client=APIClient(); client.force_authenticate(admin)
            assert refresh(client,company,key).status_code==202
            assert refresh(client,company).status_code==409
        finally: release.set()
        assert future.result(timeout=10)==200
    assert PriceHistory.objects.count()==3 and ProviderDailyUsage.objects.get().calls==1


@pytest.mark.django_db(transaction=True)
def test_concurrent_reservations_cannot_exceed_daily_budget(market):
    ProviderDailyUsage.objects.create(provider=PROVIDER,day=NOW.date(),limit=1)
    barrier=Barrier(2)
    def reserve():
        close_old_connections()
        try:
            barrier.wait(timeout=10)
            try: reserve_call(); return 200
            except SourceError as error: return error.status
        finally: close_old_connections()
    with ThreadPoolExecutor(max_workers=2) as pool:
        results=list(pool.map(lambda _:reserve(),range(2)))
    assert sorted(results)==[200,429]
    assert ProviderDailyUsage.objects.get().calls==1
