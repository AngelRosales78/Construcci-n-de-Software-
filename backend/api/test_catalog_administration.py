from datetime import date
from io import StringIO
from pathlib import Path
import fakeredis
import pytest
from django.core.management import call_command
from django.core.cache import caches
from rest_framework.test import APIClient
from .models import AdministrationEvent, AnnualFinancialStatement, Company, Favorite, FinancialMetrics, PriceHistory, User
from .catalog_services import PILOT_TICKERS, catalog_revision
from .search_cache import preload_autocomplete

pytestmark = pytest.mark.django_db
URL = '/api/v1/admin/companies/'


@pytest.fixture
def catalog(monkeypatch):
    redis = fakeredis.FakeRedis(decode_responses=True)
    monkeypatch.setattr('api.search_cache.get_redis_connection', lambda: redis)
    monkeypatch.setattr('api.redis_utils.get_redis_connection', lambda: redis)
    caches['search_results'].clear()
    admin = User.objects.create_superuser(username='catalog_admin', email='admin@example.test', password='Prueba!2026')
    regular = User.objects.create_user(username='catalog_regular')
    client = APIClient(); client.force_authenticate(admin)
    company = Company.objects.create(ticker='MSFT', name='Microsoft', sector='technology', is_pilot=True)
    return client, admin, regular, company, redis


def test_catalog_list_is_private_paginated_and_sorted(catalog):
    client, _, regular, _, _ = catalog
    Company.objects.bulk_create([Company(ticker=f'X{i:02}', name=f'Empresa {i:02}', sector='technology') for i in range(23)])
    response = client.get(URL)
    assert response.status_code == 200 and response.data['count'] == 24
    assert len(response.data['results']) == 20 and response.data['next']
    assert response.data['results'][0]['name'] == 'Empresa 00'
    assert response['Cache-Control'] == 'private, no-store'
    client.force_authenticate(regular)
    assert client.get(URL).status_code == 403
    assert client.patch(URL+'1/', {'is_active': False}, format='json').status_code == 403
    assert APIClient().get(URL).status_code == 401


def test_pilot_is_fifteen_reproducible_companies_not_full_coverage(catalog):
    client, _, _, _, _ = catalog
    source = Path(__file__).resolve().parents[2] / 'docs/datos/catalogo_sp500.csv'
    call_command('import_company_catalog', str(source), stdout=StringIO())
    response = client.get(URL, {'scope': 'pilot'})
    assert response.data['count'] == 15
    assert {row['ticker'] for row in response.data['results']} == set(PILOT_TICKERS)
    assert response.data['pilot']['selection_status'] == 'proposal'
    assert all(not row['coverage']['has_data'] for row in response.data['results'])
    assert client.get('/api/v1/companies/search/').data['count'] == 503


def test_coverage_retains_dates_nulls_sources_and_price_only_companies(catalog):
    client, _, _, company, _ = catalog
    FinancialMetrics.objects.create(company=company, as_of='2024-12-31', source='SEC FY2024', per=0)
    annual = AnnualFinancialStatement.objects.create(company=company, fiscal_year=2024,
        period_start='2024-01-01', period_end='2024-12-31', filed='2025-02-01',
        source_url='https://data.sec.gov/example', accession='0000000001-25-000001', snapshot_sha256='a'*64,
        components={'net_income': {'value': '100', 'tag': 'NetIncomeLoss'}})
    other = Company.objects.create(ticker='PRICE', name='Solo precios', sector='technology')
    quote = PriceHistory.objects.create(company=other, date='2026-11-18', adjusted_close=100, source='https://example.test/prices')
    other.latest_price = quote; other.save(update_fields=['latest_price'])
    Company.objects.create(ticker='EMPTY', name='Sin datos', sector='technology')
    loaded = client.get(URL, {'coverage': 'loaded'}).data
    assert loaded['count'] == 2
    coverage = next(row['coverage'] for row in loaded['results'] if row['ticker'] == 'MSFT')
    assert coverage['summary_fields'] == 1 and coverage['summary_as_of'] == date(2024, 12, 31)
    assert coverage['fiscal_years'] == [2024] and annual.source_url in coverage['sources']
    assert next(row for row in loaded['results'] if row['ticker'] == 'PRICE')['coverage']['price_as_of'] == date(2026, 11, 18)
    assert client.get(URL, {'coverage': 'missing'}).data['count'] == 1
    assert client.get(URL, {'coverage': 'invalid'}).status_code == 400


def test_deactivation_invalidates_cached_search_and_stale_autocomplete_but_preserves_history(catalog, django_capture_on_commit_callbacks):
    client, _, regular, company, redis = catalog
    Favorite.objects.create(user=regular, company=company)
    preload_autocomplete([company])
    public = '/api/v1/companies/search/'
    assert client.get(public).data['count'] == 1
    assert len(redis.keys('sp500:search:*')) == 1
    revision = catalog_revision()
    with django_capture_on_commit_callbacks(execute=True):
        result = client.patch(URL+f'{company.pk}/', {'is_active': False}, format='json')
    assert result.status_code == 200 and not result.data['is_active']
    assert catalog_revision() > revision
    assert client.get(public).data['count'] == 0
    assert client.get('/api/v1/companies/autocomplete/?q=m').data['suggestions'] == []
    # Incluso un índice obsoleto o reconstruido por otro proceso no publica inactivas.
    preload_autocomplete([company])
    assert client.get('/api/v1/companies/autocomplete/?q=m').data['suggestions'] == []
    summary = client.get('/api/v1/companies/MSFT/summary/')
    assert summary.status_code == 200 and summary.data['is_active'] is False
    assert Favorite.objects.filter(user=regular, company=company).exists()
    assert client.get(URL, {'state': 'inactive'}).data['count'] == 1
    with django_capture_on_commit_callbacks(execute=True):
        assert client.patch(URL+f'{company.pk}/', {'is_active': True}, format='json').status_code == 200
    assert client.get(public).data['count'] == 1
    assert client.get('/api/v1/companies/autocomplete/?q=m').data['suggestions'][0]['ticker'] == 'MSFT'


def test_editing_descriptive_data_is_audited_idempotent_and_survives_reimport(catalog, tmp_path):
    client, admin, _, company, _ = catalog
    data = {'description': 'Descripción de ensayo.', 'website_url': 'https://www.microsoft.com', 'sector': 'industrials'}
    response = client.patch(URL+f'{company.pk}/', data, format='json')
    assert response.status_code == 200
    event = AdministrationEvent.objects.get(action='company_changed')
    assert event.actor == admin and set(event.changes['fields']) == set(data)
    revision = catalog_revision()
    assert client.patch(URL+f'{company.pk}/', data, format='json').status_code == 200
    assert AdministrationEvent.objects.count() == 1 and catalog_revision() == revision
    file = tmp_path/'catalog.csv'
    file.write_text('Symbol,Security,GICS Sector\nMSFT,Microsoft,Information Technology\n', encoding='utf-8')
    client.patch(URL+f'{company.pk}/', {'is_active': False}, format='json')
    call_command('import_company_catalog', str(file), stdout=StringIO())
    company.refresh_from_db()
    assert company.sector == 'industrials' and company.description == data['description'] and not company.is_active
    public = client.get('/api/v1/companies/MSFT/summary/').data
    assert public['website_url'] == data['website_url'] and public['description'] == data['description']
    assert public['market_cap'] is None and public['altman_z']['score'] is None


@pytest.mark.parametrize('data', [
    {'website_url': 'javascript:alert(1)'}, {'website_url': 'http://example.test'},
    {'sector': 'imaginary'}, {'description': 'a'*3001}, {'per': 999}, {'is_pilot': True}, {},
])
def test_invalid_changes_cannot_mutate_company(catalog, data):
    client, _, _, company, _ = catalog
    assert client.patch(URL+f'{company.pk}/', data, format='json').status_code == 400
    company.refresh_from_db()
    assert company.sector == 'technology' and company.description == '' and company.is_active
    assert not AdministrationEvent.objects.exists()
