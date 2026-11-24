from datetime import date, datetime, timezone as dt_timezone
from hashlib import sha256
from io import StringIO
import gzip
import json
from types import SimpleNamespace
import pytest
from django.core.management import call_command
from django.core.management.base import CommandError
from api.market_provider import source_url
from api.models import Company, FinancialMetrics, PriceHistory, User, ProviderDailyUsage, AdministrationEvent

pytestmark = pytest.mark.django_db


@pytest.fixture
def company(monkeypatch):
    monkeypatch.setattr('api.management.commands.import_company_profiles.invalidate_catalog', lambda: None)
    monkeypatch.setattr('api.management.commands.import_catalog_prices.invalidate_catalog', lambda: None)
    return Company.objects.create(ticker='MSFT', name='Microsoft', sector='technology')


def profile_file(tmp_path, entries=None):
    entry = {'ticker': 'MSFT', 'description': 'Empresa de software.', 'website_url': 'https://www.microsoft.com/',
        'details': {'industry': 'Systems Software', 'industry_label': 'Software de sistemas',
            'headquarters': 'Redmond, Washington', 'founded': '1975', 'cik': '0000789019',
            'catalog_source_url': 'https://github.com/datasets/s-and-p-500-companies',
            'catalog_snapshot_date': '2026-10-12', 'profile_source_url': 'https://www.wikidata.org/wiki/Q2283',
            'description_origin': 'wikidata'}}
    path = tmp_path / 'profiles.json'
    path.write_text(json.dumps({'version': 1, 'companies': entries or [entry]}), encoding='utf-8')
    return path, entry


def test_profiles_preserve_finances_and_administrative_sector(company, tmp_path):
    company.sector = 'industrials'; company.sector_overridden = True; company.market_cap = 123
    company.save()
    FinancialMetrics.objects.create(company=company, as_of=date(2024, 6, 30), source='Referencia', per=17)
    path, _ = profile_file(tmp_path)
    call_command('import_company_profiles', str(path), stdout=StringIO())
    company.refresh_from_db()
    assert company.description == 'Empresa de software.'
    assert company.catalog_details['cik'] == '0000789019'
    assert company.sector == 'industrials' and company.sector_overridden
    assert company.market_cap == 123 and company.financial_metrics.per == 17


def test_profile_import_is_repeatable_and_preserves_later_manual_edits(company, tmp_path):
    path, entry = profile_file(tmp_path)
    call_command('import_company_profiles', str(path), stdout=StringIO())
    company.refresh_from_db(); original_time = company.updated_at
    call_command('import_company_profiles', str(path), stdout=StringIO())
    company.refresh_from_db(); assert company.updated_at == original_time
    company.description = 'Descripción revisada por el equipo.'
    company.website_url = 'https://www.microsoft.com/es-es/'
    company.save()
    entry['description'] = 'Una reseña nueva.'
    path, _ = profile_file(tmp_path, [entry])
    call_command('import_company_profiles', str(path), stdout=StringIO())
    company.refresh_from_db()
    assert company.description == 'Descripción revisada por el equipo.'
    assert company.website_url.endswith('/es-es/')
    assert company.catalog_details['imported_description'] == 'Una reseña nueva.'


def test_intentionally_cleared_description_and_website_stay_empty(company, tmp_path):
    path, _ = profile_file(tmp_path)
    call_command('import_company_profiles', str(path), stdout=StringIO())
    company.refresh_from_db()
    company.description = ''; company.website_url = ''; company.save()
    call_command('import_company_profiles', str(path), stdout=StringIO())
    company.refresh_from_db()
    assert company.description == '' and company.website_url == ''


@pytest.mark.parametrize('damage', ['duplicate', 'unsafe_url', 'bad_cik'])
def test_invalid_profiles_do_not_partially_write(company, tmp_path, damage):
    _, entry = profile_file(tmp_path)
    other = json.loads(json.dumps(entry)); other['ticker'] = 'AAPL'
    if damage == 'duplicate': other['ticker'] = 'MSFT'
    elif damage == 'unsafe_url': other['website_url'] = 'javascript:alert(1)'
    else: other['details']['cik'] = 'invalid'
    path, _ = profile_file(tmp_path, [entry, other])
    with pytest.raises(CommandError): call_command('import_company_profiles', str(path))
    company.refresh_from_db()
    assert not company.description and not company.catalog_details


def price_file(tmp_path, ticker='MSFT'):
    raw = json.dumps({'chart': {'error': None, 'result': [{
        'meta': {'symbol': ticker, 'instrumentType': 'EQUITY', 'currency': 'USD',
            'exchangeTimezoneName': 'America/New_York', 'longName': 'Microsoft'},
        'timestamp': [int(datetime(2024, 1, 3, 21, tzinfo=dt_timezone.utc).timestamp())],
        'indicators': {'adjclose': [{'adjclose': [100.5]}], 'quote': [{'volume': [1000]}]}}]}})
    entry = {'ticker': ticker, 'raw_response': raw, 'sha256': sha256(raw.encode()).hexdigest(), 'source_url': source_url(ticker)}
    path = tmp_path / 'prices.json.gz'
    return path, {'version': 1, 'companies': [entry]}


def test_prices_restore_real_source_without_overwriting_newer_data(company, tmp_path):
    path, payload = price_file(tmp_path)
    path.write_bytes(gzip.compress(json.dumps(payload).encode()))
    call_command('import_catalog_prices', str(path), stdout=StringIO())
    company.refresh_from_db()
    assert company.latest_price.adjusted_close == 100.5
    assert company.latest_price.source == source_url('MSFT') and not company.latest_price.is_demo
    newer = PriceHistory.objects.create(company=company, date=date(2024, 1, 4), adjusted_close=110, source='Nueva fuente')
    company.latest_price = newer; company.save()
    call_command('import_catalog_prices', str(path), stdout=StringIO())
    company.refresh_from_db()
    assert company.latest_price_id == newer.pk
    assert PriceHistory.objects.filter(company=company).count() == 2


@pytest.mark.parametrize('damage', ['hash', 'ticker', 'duplicate'])
def test_damaged_price_snapshot_rejected_before_writing(company, tmp_path, damage):
    path, payload = price_file(tmp_path)
    if damage == 'hash': payload['companies'][0]['sha256'] = '0' * 64
    elif damage == 'ticker': payload['companies'][0]['ticker'] = 'AAPL'
    else: payload['companies'].append(payload['companies'][0].copy())
    path.write_bytes(gzip.compress(json.dumps(payload).encode()))
    with pytest.raises(CommandError): call_command('import_catalog_prices', str(path))
    assert not PriceHistory.objects.exists()


def test_current_snapshot_checks_hash_without_rebuilding_model_rows(company, tmp_path, monkeypatch):
    path, payload = price_file(tmp_path)
    payload['companies'][0]['price_as_of'] = '2024-01-03'
    current = PriceHistory.objects.create(company=company, date=date(2024, 1, 4), adjusted_close=110, source='Referencia')
    company.latest_price = current; company.save()
    def unexpected(*args): raise AssertionError('No se debe reconstruir un histórico que ya está cubierto.')
    monkeypatch.setattr('api.management.commands.import_catalog_prices.parse_prices', unexpected)
    path.write_bytes(gzip.compress(json.dumps(payload).encode()))
    call_command('import_catalog_prices', str(path), stdout=StringIO())
    payload['companies'][0]['sha256'] = '0' * 64
    path.write_bytes(gzip.compress(json.dumps(payload).encode()))
    with pytest.raises(CommandError): call_command('import_catalog_prices', str(path))
    company.refresh_from_db(); assert company.latest_price_id == current.pk


def test_restore_does_not_promote_the_capture_current_session_on_a_later_day(company, tmp_path):
    path, payload = price_file(tmp_path)
    entry = payload['companies'][0]
    response = json.loads(entry['raw_response'])
    series = response['chart']['result'][0]
    series['timestamp'].append(int(datetime(2024, 1, 4, 17, tzinfo=dt_timezone.utc).timestamp()))
    series['indicators']['adjclose'][0]['adjclose'].append(150)
    series['indicators']['quote'][0]['volume'].append(2000)
    entry['raw_response'] = json.dumps(response)
    entry['sha256'] = sha256(entry['raw_response'].encode()).hexdigest()
    entry['price_as_of'] = '2024-01-03'
    path.write_bytes(gzip.compress(json.dumps(payload).encode()))
    call_command('import_catalog_prices', str(path), stdout=StringIO())
    company.refresh_from_db()
    assert company.latest_price.date == date(2024, 1, 3)
    assert PriceHistory.objects.filter(company=company).count() == 1


def test_backfill_respects_provider_rate_stop_and_audits_explicit_budget(company, tmp_path, monkeypatch):
    from api.management.commands import backfill_catalog_prices as command
    admin = User.objects.create_superuser(username='enrichment_admin')
    Company.objects.create(ticker='AAPL', name='Apple', sector='technology')
    calls = []
    def execute(*args, **kwargs):
        calls.append(args[2])
        return SimpleNamespace(status='failed', message='Límite del proveedor', error_code='provider_rate', result={})
    monkeypatch.setattr(command, 'execute', execute)
    call_command('backfill_catalog_prices', administrator=admin.username, batch_id='test',
        daily_limit=600, snapshot=str(tmp_path/'snapshot.gz'), report=str(tmp_path/'report.json'), stdout=StringIO(), stderr=StringIO())
    assert len(calls) == 1
    assert ProviderDailyUsage.objects.get().limit == 600
    assert AdministrationEvent.objects.get().action == 'catalog_backfill_budget'
    assert json.loads((tmp_path/'report.json').read_text(encoding='utf-8'))['downloaded'] == 0
