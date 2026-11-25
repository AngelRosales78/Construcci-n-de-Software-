import copy
import hashlib
import gzip
import json
from datetime import date, datetime
from decimal import Decimal
from types import SimpleNamespace
from pathlib import Path
from zoneinfo import ZoneInfo
import pytest
from api.sec_altman import prepare_reference, derive, IncompleteReference
from api.altman_reference import validate_reference
from api.views import calculate_altman_z


def fixture():
    accession = '0000000123-26-000001'
    tags = {'Assets': 1000, 'AssetsCurrent': 400, 'LiabilitiesCurrent': 200,
            'RetainedEarningsAccumulatedDeficit': -10, 'Liabilities': 600,
            'CommonStockSharesOutstanding': 100, 'RevenueFromContractWithCustomerExcludingAssessedTax': 1500,
            'OperatingIncomeLoss': 100}
    gaap = {}
    for tag, amount in tags.items():
        point = {'end': '2025-12-31', 'val': amount, 'accn': accession, 'form': '10-K', 'filed': '2026-02-01'}
        if tag in ('RevenueFromContractWithCustomerExcludingAssessedTax', 'OperatingIncomeLoss'):
            point['start'] = '2025-01-01'
        gaap[tag] = {'units': {'shares' if tag == 'CommonStockSharesOutstanding' else 'USD': [point]}}
    facts = {'cik': 123, 'facts': {'us-gaap': gaap}}
    source = {'url': 'https://data.sec.gov/api/xbrl/companyfacts/CIK0000000123.json',
              'sha256': hashlib.sha256(json.dumps(facts).encode()).hexdigest(), 'retrieved_at': '2026-11-24T12:00:00+00:00'}
    stamp = int(datetime(2025, 12, 31, 16, tzinfo=ZoneInfo('America/New_York')).timestamp())
    prices = json.dumps({'chart': {'result': [{'meta': {'symbol': 'TEST', 'currency': 'USD'},
              'timestamp': [stamp], 'indicators': {'quote': [{'close': [20]}]}}]}})
    entry = {'raw_response': prices, 'sha256': hashlib.sha256(prices.encode()).hexdigest()}
    identity = {'cik': '0000000123', 'sic': '2000', 'tickers': ['TEST']}
    return facts, identity, source, entry


def prepare(data=None):
    return prepare_reference('TEST', 123, *(data or fixture()), target_year=2025, today=date(2026, 11, 24))


def test_all_inputs_reproduce_one_annual_filing_and_actual_fiscal_end_shares():
    reference = prepare()
    values = validate_reference(reference, today=date(2026, 11, 24))
    assert values['working_capital'] == 200
    assert values['retained_earnings'] == -10
    assert values['equity_value'] == 2000
    assert reference['period_start'] == '2025-01-01'
    assert reference['price_date'] == '2025-12-31'
    score = calculate_altman_z(SimpleNamespace(altman_reference=reference))
    assert score['score'] == 4.06 and score['zone'] == 'safe'


@pytest.mark.parametrize('change', [
    lambda f: f['facts']['us-gaap'].pop('CommonStockSharesOutstanding'),
    lambda f: f['facts']['us-gaap']['CommonStockSharesOutstanding']['units']['shares'][0].update(end='2026-01-20'),
    lambda f: f['facts']['us-gaap']['Assets']['units']['USD'][0].update(accn='0000000123-25-000003'),
    lambda f: f['facts']['us-gaap']['OperatingIncomeLoss']['units']['USD'][0].update(start='2025-10-01'),
])
def test_missing_fiscal_shares_other_filings_and_quarterly_flows_are_not_substituted(change):
    data = fixture()
    change(data[0])
    with pytest.raises(IncompleteReference):
        prepare(data)


@pytest.mark.parametrize('identity', [
    {'cik': 123, 'sic': '6021', 'tickers': ['TEST']},
    {'cik': 123, 'sic': '6798', 'tickers': ['TEST']},
    {'cik': 123, 'sic': '', 'tickers': ['TEST']},
    {'cik': 123, 'sic': '2000', 'tickers': ['TEST', 'TEST.A']},
    {'cik': 123, 'sic': '2000', 'tickers': ['OTHER']},
])
def test_sector_and_share_class_cases_require_individual_review(identity):
    data = list(fixture()); data[1] = identity
    with pytest.raises(IncompleteReference):
        prepare(data)


def test_conflicting_duplicate_values_are_rejected_instead_of_choosing_one():
    data = fixture()
    rows = data[0]['facts']['us-gaap']['Assets']['units']['USD']
    rows.append({**rows[0], 'val': 999})
    with pytest.raises(IncompleteReference, match='ambiguo'):
        prepare(data)


def test_later_stock_split_requires_reconciliation():
    data = list(fixture())
    prices = json.loads(data[3]['raw_response'])
    prices['chart']['result'][0]['events'] = {'splits': {'a': {'date': 1769000000}}}
    data[3]['raw_response'] = json.dumps(prices)
    data[3]['sha256'] = hashlib.sha256(data[3]['raw_response'].encode()).hexdigest()
    with pytest.raises(IncompleteReference, match='Desdoblamiento'):
        prepare(data)


@pytest.mark.parametrize('mutation', [
    lambda r: r['sec_evidence']['facts']['shares']['point'].update(end='2026-01-23'),
    lambda r: r['sec_evidence']['facts']['assets']['point'].update(accn='0000000123-25-000001'),
    lambda r: r['sec_evidence']['facts']['retained']['point'].update(val=999),
    lambda r: r['sec_evidence']['facts']['revenue'].update(unit='EUR'),
    lambda r: r['sec_evidence'].update(sic=6021),
    lambda r: r.update(report_url='https://www.sec.gov/wrong-report'),
])
def test_altered_facts_cannot_be_published_or_fall_back_to_current_market_cap(mutation):
    reference = prepare()
    mutation(reference)
    assert calculate_altman_z(SimpleNamespace(altman_reference=reference, equity_value=99999))['score'] is None


def test_zero_operating_profit_and_negative_working_capital_are_not_missing_values():
    data = fixture()
    data[0]['facts']['us-gaap']['OperatingIncomeLoss']['units']['USD'][0]['val'] = 0
    data[0]['facts']['us-gaap']['AssetsCurrent']['units']['USD'][0]['val'] = 100
    amounts, _ = derive(prepare(data)['sec_evidence'], today=date(2026, 11, 24))
    assert amounts['ebit'] == Decimal(0) and amounts['working_capital'] == -100


def test_reported_current_and_noncurrent_liabilities_are_added_without_assuming_equity():
    data = fixture()
    facts = data[0]['facts']['us-gaap']
    point = facts.pop('Liabilities')['units']['USD'][0]
    facts['LiabilitiesNoncurrent'] = {'units': {'USD': [{**point, 'val': 400}]}}
    reference = prepare(data)
    assert reference['inputs']['total_liabilities'] == '600'
    assert reference['sec_evidence']['liabilities_basis'] == 'current_plus_noncurrent'


def test_fiscal_issued_less_treasury_shares_use_actual_shares_not_earnings_averages():
    data = fixture()
    facts = data[0]['facts']['us-gaap']
    point = facts.pop('CommonStockSharesOutstanding')['units']['shares'][0]
    facts['CommonStockSharesIssued'] = {'units': {'shares': [{**point, 'val': 150}]}}
    facts['TreasuryStockShares'] = {'units': {'shares': [{**point, 'val': 50}]}}
    reference = prepare(data)
    assert reference['shares_outstanding'] == '100'
    assert reference['inputs']['equity_value'] == '2000'


PACKAGE = json.loads(gzip.decompress((Path(__file__).resolve().parents[2] /
    'docs/datos/altman_sec_references.json.gz').read_bytes()))


@pytest.mark.parametrize('row', [row for row in PACKAGE['companies'] if row['reference'].get('sec_evidence')],
                         ids=lambda row: row['ticker'])
def test_every_packaged_sec_reference_reproduces_its_original_facts(row):
    reference = row['reference']
    values = validate_reference(reference)
    score = calculate_altman_z(SimpleNamespace(altman_reference=reference))
    assert score['score'] is not None and len(score['components']) == 5
    assert values['equity_value'] == Decimal(reference['unadjusted_close']) * Decimal(reference['shares_outstanding'])
    assert reference['sec_evidence']['ticker'] == row['ticker']


@pytest.mark.parametrize('ticker,assets,liabilities,retained,shares', [
    ('NVDA', 206803000000, 49510000000, 146973000000, 24304000000),
    ('TSLA', 137806000000, 54941000000, 39003000000, 3751000000),
    ('IBM', 151880000000, 119139000000, 155648000000, 936952129),
])
def test_selected_components_match_independently_reviewed_annual_report_tables(ticker,assets,liabilities,retained,shares):
    reference = next(row['reference'] for row in PACKAGE['companies'] if row['ticker'] == ticker)
    assert Decimal(reference['inputs']['total_assets']) == assets
    assert Decimal(reference['inputs']['total_liabilities']) == liabilities
    assert Decimal(reference['inputs']['retained_earnings']) == retained
    assert Decimal(reference['shares_outstanding']) == shares


@pytest.mark.django_db
def test_compressed_sec_batch_is_repeatable_and_preserves_existing_indicators():
    from io import StringIO
    from django.core.management import call_command
    from api.models import Company, FinancialMetrics
    root = Path(__file__).resolve().parents[2]
    for row in PACKAGE['companies']:
        company = Company.objects.create(ticker=row['ticker'], name=row['ticker'], sector='technology')
        FinancialMetrics.objects.create(company=company, as_of=row['reference']['period_end'],
            source='Preserved source', market_cap=123, per=0, roe=-2, reference_basis='provider_normalized')
    args = (str(root/'docs/datos/altman_sec_references.json.gz'),)
    options = {'prices':str(root/'docs/datos/catalog_prices.json.gz'),'stdout':StringIO()}
    call_command('import_altman_references', *args, **options)
    call_command('import_altman_references', *args, **options)
    for row in FinancialMetrics.objects.all():
        assert row.market_cap == 123 and row.per == 0 and row.roe == -2
        assert row.source == 'Preserved source' and row.altman_reference
