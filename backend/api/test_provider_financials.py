from datetime import date, datetime, timedelta, timezone as dt_timezone
from decimal import Decimal
from hashlib import sha256
from io import StringIO
from types import SimpleNamespace
import gzip
import json
import pytest
from django.core.exceptions import ValidationError
from django.core.management import call_command
from django.core.management.base import CommandError
from api.provider_financials import parse_capture, summary_values, chart_capture
from api.models import AnnualFinancialStatement, Company, FinancialMetrics
from api.pillars import build_pillars, comparable_previous
from api.comparison_views import comparison_rows

pytestmark = pytest.mark.django_db
TODAY = date(2026, 11, 24)


def capture(ticker='AAPL'):
    amounts = {
        'annualTotalAssets': (100, 200), 'annualCurrentAssets': (60, 90), 'annualCurrentLiabilities': (30, 45),
        'annualStockholdersEquity': (60, 80), 'annualNetIncome': (20, 30), 'annualTotalRevenue': (150, 210),
        'annualTotalLiabilitiesNetMinorityInterest': (40, 120), 'annualCashAndCashEquivalents': (10, 20),
        'annualOtherShortTermInvestments': (5, 10), 'annualAccountsReceivable': (7, 14), 'annualInventory': (5, 7),
        'annualCostOfRevenue': (70, 90), 'annualPretaxIncome': (25, 35), 'annualInterestExpense': (2, 3),
        'trailingMarketCap': (200, 300), 'trailingPeRatio': (10, 12),
    }
    entries = []
    for field, values in amounts.items():
        entries.append({'meta': {'symbol': [ticker], 'type': [field]}, field: [
            {'asOfDate': when, 'periodType': '12M' if field.startswith('annual') else 'TTM',
             'currencyCode': 'USD', 'reportedValue': {'raw': value}}
            for when,value in zip(('2023-09-30','2024-09-30'), values)]})
    return {'timeseries': {'error': None, 'result': entries}}


def evidence(raw, ticker='AAPL'):
    return {'sha256': sha256(raw).hexdigest(), 'url': 'https://query1.finance.yahoo.com/ws/fundamentals-timeseries/v1/finance/timeseries/'+ticker+'?type=annualTotalAssets',
            'retrieved_at': '2024-11-01T12:00:00+00:00'}


def parsed(doc=None):
    raw = json.dumps(doc or capture()).encode()
    records, values = parse_capture('AAPL', raw, evidence(raw), TODAY)
    return records, values, evidence(raw)


def test_annual_records_do_not_invent_filing_or_exact_fiscal_start():
    records, values, source = parsed()
    assert len(records) == 2 and records[-1]['components']['total_assets']['value'] == '200'
    assert records[-1]['period_start'] is None and records[-1]['filed'] is None and records[-1]['accession'] == ''
    assert records[-1]['provenance']['period_basis'] == 'provider_normalized'
    assert records[-1]['snapshot_sha256'] == source['sha256']


@pytest.mark.parametrize('damage', ['symbol','hash','quarterly','currency','nonfinite','boolean','duplicate'])
def test_incompatible_values_are_rejected_or_stay_absent(damage):
    doc = capture(); field = doc['timeseries']['result'][0]
    if damage == 'symbol': field['meta']['symbol'] = ['MSFT']
    elif damage == 'quarterly': field['annualTotalAssets'][-1]['periodType'] = '3M'
    elif damage == 'currency': field['annualTotalAssets'][-1]['currencyCode'] = 'EUR'
    elif damage == 'nonfinite': field['annualTotalAssets'][-1]['reportedValue']['raw'] = float('nan')
    elif damage == 'boolean': field['annualTotalAssets'][-1]['reportedValue']['raw'] = True
    elif damage == 'duplicate': field['annualTotalAssets'].append(dict(field['annualTotalAssets'][-1], reportedValue={'raw': 201}))
    raw = json.dumps(doc).encode(); proof = evidence(raw)
    if damage == 'hash': proof['sha256'] = '0'*64
    if damage in ('hash','symbol'):
        with pytest.raises(ValueError): parse_capture('AAPL', raw, proof, TODAY)
    else:
        records, _ = parse_capture('AAPL', raw, proof, TODAY)
        assert [record['fiscal_year'] for record in records] == [2023]


def test_future_values_and_missing_interest_are_not_substituted_with_zero():
    doc = capture()
    for entry in doc['timeseries']['result']:
        field = entry['meta']['type'][0]
        if field == 'annualInterestExpense': entry[field] = []
        entry[field].append({'asOfDate':'2027-09-30','periodType':'12M','currencyCode':'USD','reportedValue':{'raw':999}})
    records, _, _ = parsed(doc)
    assert [record['fiscal_year'] for record in records] == [2023,2024]
    assert records[-1]['components']['interest_expense']['value'] is None


def test_zero_is_preserved_but_nonpositive_equity_does_not_create_roe():
    doc = capture()
    for entry in doc['timeseries']['result']:
        field = entry['meta']['type'][0]
        if field in ('annualNetIncome','annualStockholdersEquity'): entry[field][-1]['reportedValue']['raw'] = 0
    records, values, source = parsed(doc)
    result = summary_values('AAPL',records,values,{},[],{},source,{}, {})
    assert records[-1]['components']['net_income']['value'] == '0'
    assert result['roe'] is None and result['equity_value'] is None


def test_roe_uses_average_equity_and_retains_independent_source_dates():
    records, values, source = parsed()
    result = summary_values('AAPL',records,values,{},[],{},source,{}, {})
    assert result['roe'] == Decimal('42.86')
    assert result['market_cap'] == 300 and result['per'] == 12
    assert result['field_sources']['roe']['as_of'] == '2024-09-30'
    assert result['reference_basis'] == 'provider_normalized'
    assert result['equity_value'] is None


def test_beta_uses_252_common_adjusted_returns_and_no_interpolation():
    records, values, source = parsed(); stock = {}; index = {}; p = q = Decimal(100)
    for i in range(253):
        when = date(2023,1,1)+timedelta(days=i)
        stock[when] = {'adjusted':p,'close':p}; index[when] = {'adjusted':q,'close':q}
        r = [Decimal('.01'),Decimal('-.005'),Decimal('.002')][i%3]
        p *= 1+2*r; q *= 1+r
    proof = {'url':'https://finance.yahoo.com/quote/%5EGSPC/history/','sha256':'a'*64}
    result = summary_values('AAPL',records,values,stock,[],index,source,{'sha256':'b'*64},proof)
    assert result['beta'] == Decimal('2.0000')
    index.pop(min(index))
    result = summary_values('AAPL',records,values,stock,[],index,source,{'sha256':'b'*64},proof)
    assert result['beta'] is None


def test_dividends_use_unadjusted_close_and_require_recorded_events():
    records, values, source = parsed(); when = date(2024,9,30)
    chart = {when-timedelta(days=400):{'adjusted':40,'close':80},when:{'adjusted':50,'close':100}}
    proof = {'sha256':'a'*64}
    result = summary_values('AAPL',records,values,chart,[(when-timedelta(days=10),Decimal(2))],{},source,proof,{})
    assert result['dividend_yield'] == Decimal('2.0000')
    assert summary_values('AAPL',records,values,chart,[],{},source,proof,{})['dividend_yield'] is None


def test_recent_split_withholds_dividend_yield_instead_of_guessing_share_basis():
    timestamp = lambda when: int(datetime(when.year,when.month,when.day,20,tzinfo=dt_timezone.utc).timestamp())
    raw = json.dumps({'chart':{'result':[{'meta':{'symbol':'AAPL','currency':'USD','exchangeTimezoneName':'America/New_York'},
        'timestamp':[timestamp(date(2023,1,1)),timestamp(date(2024,9,30))],
        'indicators':{'adjclose':[{'adjclose':[50,100]}],'quote':[{'close':[100,100]}]},
        'events':{'dividends':{'a':{'date':timestamp(date(2024,8,1)),'amount':2}},
                  'splits':{'a':{'date':timestamp(date(2024,9,1)),'numerator':2,'denominator':1}}}}]}}).encode()
    rows,dividends = chart_capture('AAPL',raw,TODAY)
    assert rows and not dividends


def test_provider_periods_support_own_history_but_not_cross_company_winners():
    company = Company.objects.create(ticker='AAPL',name='Apple',sector='technology')
    records, _, _ = parsed()
    statements = [AnnualFinancialStatement(company=company,**record) for record in records]
    for statement in statements: statement.full_clean(validate_unique=False,validate_constraints=False)
    assert comparable_previous(statements[1],statements[0]) == statements[0]
    data = build_pillars(company,statements[1],statements[0],2024,[2024,2023],statements={statement.fiscal_year:statement for statement in statements})
    assert data['period']['start'] is None and data['period_note']
    assert all(row['status']=='not_comparable' for row in comparison_rows(data,data))
    assert all('normalizadas' in row['reason'] for row in comparison_rows(data,data))
    statements[0].source_kind='sec'
    assert comparable_previous(statements[1],statements[0]) is None


def test_provider_validation_rejects_fabricated_sec_metadata():
    company = Company.objects.create(ticker='AAPL',name='Apple',sector='technology')
    records,_,_ = parsed()
    statement = AnnualFinancialStatement(company=company,**records[-1])
    statement.filed = date(2024,11,1)
    with pytest.raises(ValidationError): statement.full_clean(validate_unique=False,validate_constraints=False)


def bundle_files(tmp_path):
    raw=json.dumps(capture()).encode(); proof=evidence(raw)
    def chart(symbol):
        return json.dumps({'chart':{'result':[{'meta':{'symbol':symbol,'currency':'USD','exchangeTimezoneName':'America/New_York'},
            'timestamp':[int(datetime(2024,9,30,20,tzinfo=dt_timezone.utc).timestamp())],
            'indicators':{'adjclose':[{'adjclose':[100]}],'quote':[{'close':[100]}]}}]}})
    stock = chart('AAPL'); index = chart('^GSPC')
    prices=tmp_path/'prices.gz'; prices.write_bytes(gzip.compress(json.dumps({'version':1,'companies':[
        {'ticker':'AAPL','raw_response':stock,'sha256':sha256(stock.encode()).hexdigest(),'price_as_of':'2024-09-30'}]}).encode()))
    payload={'version':1,'price_snapshot_sha256':sha256(prices.read_bytes()).hexdigest(),
        'benchmark':{'raw_response':index,'evidence':{'url':'https://finance.yahoo.com/quote/%5EGSPC/history/','sha256':sha256(index.encode()).hexdigest()}},
        'companies':[{'ticker':'AAPL','raw_response':raw.decode(),'evidence':proof}]}
    path=tmp_path/'financials.gz'; path.write_bytes(gzip.compress(json.dumps(payload).encode()))
    return path,prices,payload


def test_import_preserves_official_rows_and_existing_verified_summary(tmp_path,monkeypatch):
    monkeypatch.setattr('api.management.commands.import_provider_financials.invalidate_catalog',lambda:None)
    company=Company.objects.create(ticker='AAPL',name='Apple',sector='technology')
    old=AnnualFinancialStatement.objects.create(company=company,fiscal_year=2024,period_start=date(2023,10,1),period_end=date(2024,9,30),
        source_url='https://data.sec.gov/api/xbrl/companyfacts/CIK0000320193.json',accession='0000320193-24-000001',filed=date(2024,11,1),snapshot_sha256='a'*64)
    FinancialMetrics.objects.create(company=company,as_of=date(2024,9,30),source='Fuente verificada',per=17)
    path,prices,_=bundle_files(tmp_path)
    call_command('import_provider_financials',str(path),prices=str(prices),stdout=StringIO())
    old.refresh_from_db(); company.refresh_from_db()
    assert old.source_kind=='sec' and old.snapshot_sha256=='a'*64
    assert company.financial_metrics.per==17 and AnnualFinancialStatement.objects.filter(company=company).count()==2


def test_damaged_financial_bundle_does_not_write_partial_data(tmp_path):
    Company.objects.create(ticker='AAPL',name='Apple',sector='technology')
    path,prices,payload=bundle_files(tmp_path)
    payload['companies'][0]['evidence']['sha256']='0'*64
    path.write_bytes(gzip.compress(json.dumps(payload).encode()))
    with pytest.raises(CommandError): call_command('import_provider_financials',str(path),prices=str(prices))
    assert not AnnualFinancialStatement.objects.exists() and not FinancialMetrics.objects.exists()
