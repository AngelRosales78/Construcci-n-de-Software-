"""HU05_02 T04: cálculos manuales, procedencia e importación sin escrituras parciales."""
import copy
import json
from datetime import date
from pathlib import Path
import pytest
from django.contrib.auth import get_user_model
from django.core.management import call_command
from django.core.management.base import CommandError
from rest_framework.test import APIClient
from .models import AnnualFinancialStatement, Company

pytestmark = pytest.mark.django_db
URL = '/api/v1/companies/MSFT/pillars/'


@pytest.fixture
def case():
    company = Company.objects.create(ticker='MSFT', name='Microsoft', sector='technology')
    user = get_user_model().objects.create_user(username='pillars')
    client = APIClient(); client.force_authenticate(user)
    statement = AnnualFinancialStatement.objects.create(
        company=company, fiscal_year=2024, period_start=date(2023, 7, 1), period_end=date(2024, 6, 30),
        currency='USD', source_url='https://data.sec.gov/api/xbrl/companyfacts/CIK0000789019.json',
        accession='0000950170-24-087843', filed=date(2024, 7, 30), snapshot_sha256='a'*64,
        components={k: {'value': str(v), 'tag': k} for k, v in {
            'current_assets': 200, 'current_liabilities': 100, 'cash': 50,
            'short_term_investments': 20, 'receivables': 40}.items()})
    return client, statement


def metrics(response):
    return {row['id']: row for row in response.json()['pillars'][0]['metrics']}


def test_liquidity_manual_example_and_component_provenance(case):
    client, _ = case
    response = client.get(URL)
    assert response.status_code == 200
    data = response.json(); results = metrics(response)
    assert data['fiscal_year'] == 2024 and data['schema_version'] == 'pillars-v1'
    assert len(data['pillars']) == 5
    assert results['current_ratio']['value'] == '2'
    assert results['quick_ratio']['value'] == '1.1'
    assert results['quick_ratio']['display_value'] == '1,10'
    component = results['current_ratio']['components'][0]
    assert component['period']['end'] == '2024-06-30'
    assert component['source']['accession'] == '0000950170-24-087843'
    assert results['current_ratio']['thresholds']['version'] == 'academic-thresholds-v1'
    assert data['overall_score']['value'] is None


@pytest.mark.parametrize('divisor,reason', [('0', 'zero_denominator'), ('-1', 'invalid_denominator'),
                                           (None, 'missing_components')])
def test_invalid_or_missing_denominator_is_never_a_zero_result(case, divisor, reason):
    client, statement = case
    statement.components['current_liabilities']['value'] = divisor
    statement.save()
    for value in metrics(client.get(URL)).values():
        assert value['value'] is None and value['reason'] == reason


def test_missing_quick_component_does_not_erase_current_ratio(case):
    client, statement = case
    del statement.components['short_term_investments']; statement.save()
    result = metrics(client.get(URL))
    assert result['current_ratio']['value'] == '2'
    assert result['quick_ratio']['value'] is None
    assert result['quick_ratio']['missing_fields'] == ['short_term_investments']
    assert client.get(URL).json()['pillars'][0]['status'] == 'partial'


def test_real_zero_numerator_and_half_up_rounding(case):
    client, statement = case
    statement.components['current_assets']['value'] = '0'; statement.save()
    assert metrics(client.get(URL))['current_ratio']['display_value'] == '0,00'
    statement.components['current_assets']['value'] = '100.5'; statement.save()
    assert metrics(client.get(URL))['current_ratio']['display_value'] == '1,01'


def test_requested_missing_year_is_not_replaced_by_another_year(case):
    data = case[0].get(URL, {'fiscal_year': 2023}).json()
    assert data['fiscal_year'] == 2023 and data['period']['end'] is None
    assert data['available_years'] == [2024]
    assert all(m['value'] is None for p in data['pillars'] for m in p['metrics'])


@pytest.mark.parametrize('year', ['xx', '2024.0', '3000', '1899', '', '２０２４'])
def test_invalid_fiscal_year_is_rejected(case, year):
    assert case[0].get(URL, {'fiscal_year': year}).status_code == 400


def test_endpoint_requires_authentication_and_known_company(case):
    assert APIClient().get(URL).status_code == 401
    assert case[0].get(URL.replace('MSFT', 'UNKNOWN')).status_code == 404


@pytest.fixture
def import_case(case, tmp_path):
    payload = json.loads((Path(__file__).resolve().parents[2]/'docs/datos/sprint2_disponibilidad_MSFT.json').read_text(encoding='utf-8'))
    path = tmp_path/'inventory.json'
    def run(data=None):
        path.write_text(json.dumps(payload if data is None else data), encoding='utf-8')
        call_command('import_annual_financials', str(path))
    return payload, run


def test_import_is_idempotent_keeps_years_and_missing_values(import_case):
    _, run = import_case
    run(); run()
    assert AnnualFinancialStatement.objects.count() == 5
    row = AnnualFinancialStatement.objects.get(fiscal_year=2020)
    assert row.components['income_before_tax']['value'] is None
    assert AnnualFinancialStatement.objects.get(fiscal_year=2024).components['current_assets']['value'] == '159734000000'


@pytest.mark.parametrize('fault', ['NaN', 'negative', 'date', 'duplicate', 'availability', 'currency'])
def test_bad_last_row_never_causes_partial_import(import_case, fault):
    payload, run = import_case
    payload = copy.deepcopy(payload)
    row = payload['annual_records'][-1]
    if fault == 'NaN': row['values']['current_assets']['value'] = 'NaN'
    if fault == 'negative': row['values']['current_assets']['value'] = '-1'
    if fault == 'date': row['start'] = '2024-06-01'
    if fault == 'duplicate': row['fiscal_year'] = 2023
    if fault == 'availability': row['values']['current_assets']['available'] = False
    if fault == 'currency': row['unit'] = 'EUR'
    before = list(AnnualFinancialStatement.objects.values())
    with pytest.raises(CommandError): run(payload)
    assert list(AnnualFinancialStatement.objects.values()) == before
