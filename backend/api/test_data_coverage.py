import json
from pathlib import Path
import pytest
from django.contrib.auth import get_user_model
from django.core.management import call_command
from django.core.management.base import CommandError
from rest_framework.test import APIClient
from .models import AnnualFinancialStatement, Company, FinancialMetrics

pytestmark = pytest.mark.django_db
DATA = Path(__file__).resolve().parents[2] / 'docs/datos/sprint3'


@pytest.fixture
def coverage_client():
    for ticker in ('GOOGL', 'META'):
        Company.objects.create(ticker=ticker, name=ticker, sector='communication_services')
    client = APIClient()
    client.force_authenticate(get_user_model().objects.create_user(username='coverage'))
    return client


def test_summary_import_retains_each_indicator_date_and_is_idempotent(coverage_client):
    path = DATA / 'GOOGL-metrics.json'
    call_command('import_company_metrics', str(path))
    call_command('import_company_metrics', str(path))
    assert FinancialMetrics.objects.count() == 1
    result = coverage_client.get('/api/v1/companies/GOOGL/summary/').json()
    assert result['field_sources']['per']['as_of'] == '2026-10-26'
    assert result['field_sources']['roe']['as_of'] == '2025-12-31'
    assert result['per'] == 15.9
    assert result['altman_z']['score'] is None


def test_invalid_summary_never_replaces_previous_data(coverage_client, tmp_path):
    call_command('import_company_metrics', str(DATA / 'GOOGL-metrics.json'))
    payload = json.loads((DATA / 'GOOGL-metrics.json').read_text())
    payload['values']['per'] = '999'
    payload['values']['field_sources']['per']['as_of'] = '2099-01-01'
    path = tmp_path / 'future.json'
    path.write_text(json.dumps(payload))
    with pytest.raises(CommandError):
        call_command('import_company_metrics', str(path))
    assert float(FinancialMetrics.objects.get().per) == 15.9


@pytest.mark.parametrize('ticker', ['GOOGL', 'META'])
@pytest.mark.parametrize('year', [2024, 2025])
def test_solvency_uses_report_line_and_keeps_inventory_missing(coverage_client, ticker, year):
    call_command('import_annual_financials', str(DATA / f'{ticker}-annual.json'))
    response = coverage_client.get(f'/api/v1/companies/{ticker}/pillars/?fiscal_year={year}').json()
    pillars = {row['id']: row for row in response['pillars']}
    assert pillars['solvency']['score']['status'] == 'available'
    interest = next(row for row in pillars['solvency']['metrics'] if row['id'] == 'interest_coverage')
    evidence = next(row['source'] for row in interest['components'] if row['id'] == 'interest_expense')
    assert evidence['snapshot_kind'] == 'extracted_financial_facts'
    assert evidence['url'].startswith('https://www.sec.gov/Archives/')
    assert evidence['locator']
    assert pillars['efficiency']['score']['status'] == 'unavailable'
    assert response['overall_score']['value'] is None


def test_invalid_complementary_source_is_rejected_without_partial_import(coverage_client, tmp_path):
    payload = json.loads((DATA / 'META-annual.json').read_text())
    next(row for row in payload['annual_records'] if row['fiscal_year'] == 2024)['values']['interest_expense']['source']['url'] = 'http://example.test'
    path = tmp_path / 'invalid.json'
    path.write_text(json.dumps(payload))
    with pytest.raises(CommandError):
        call_command('import_annual_financials', str(path))
    assert not AnnualFinancialStatement.objects.exists()
