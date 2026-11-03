from datetime import date, timedelta
import pytest
from django.contrib.auth import get_user_model
from django.utils import timezone
from rest_framework.test import APIClient
from .models import AccountPlan, AnnualFinancialStatement, Company
from .comparison_views import comparison_rows

pytestmark = pytest.mark.django_db
URL = '/api/v1/compare/'


@pytest.fixture
def case():
    user = get_user_model().objects.create_user(username='comparer')
    plan = AccountPlan.objects.create(user=user, plan='pro')
    client = APIClient(); client.force_authenticate(user)
    for ticker, current in [('AAA', 200), ('BBB', 100)]:
        company = Company.objects.create(ticker=ticker, name=ticker, sector='technology')
        AnnualFinancialStatement.objects.create(company=company, fiscal_year=2024,
            period_start=date(2024, 1, 1), period_end=date(2024, 12, 31), filed=date(2025, 2, 1),
            source_url='https://data.sec.gov/example', accession='fixture', snapshot_sha256='a'*64,
            components={k: {'value': str(v), 'tag': k} for k, v in {
                'current_assets': current, 'current_liabilities': 100, 'cash': current,
                'short_term_investments': 0, 'receivables': 0}.items()})
    return client, plan


def query(client, **kwargs):
    return client.get(URL, {'a': 'AAA', 'b': 'BBB', 'fiscal_year': '2024'} | kwargs)


def test_two_company_contract_reuses_pillars_and_keeps_order(case):
    client, _ = case
    response = query(client)
    assert response.status_code == 200
    data = response.json()
    assert data['schema_version'] == 'comparison-v1'
    assert data['a'] == client.get('/api/v1/companies/AAA/pillars/?fiscal_year=2024').json()
    assert data['b']['ticker'] == 'BBB' and data['fiscal_year'] == 2024
    liquid = data['comparisons'][0]
    # AAA: (100 + 100)/2; BBB: (50 + 100)/2. Diferencia = 25 puntos.
    assert liquid['id'] == 'liquidity' and liquid['result'] == 'a' and liquid['delta'] == '25.00'
    assert all(r['delta'] is None for r in data['comparisons'][1:])
    reversed_data = query(client, a='BBB', b='AAA').json()
    assert reversed_data['comparisons'][0]['result'] == 'b'
    assert reversed_data['comparisons'][0]['delta'] == '-25.00'


def test_missing_year_is_not_silently_replaced(case):
    response = query(case[0], fiscal_year='2023')
    assert response.status_code == 200
    data = response.json()
    assert data['a']['fiscal_year'] == 2023 and data['a']['period']['start'] is None
    assert data['status'] == 'not_comparable'
    assert all(row['delta'] is None for row in data['comparisons'])


def test_same_fiscal_year_with_different_periods_has_no_winner(case):
    AnnualFinancialStatement.objects.filter(company__ticker='BBB').update(period_start=date(2023, 7, 1), period_end=date(2024, 6, 30))
    data = query(case[0]).json()
    assert data['status'] == 'not_comparable'
    assert all(r['result'] == 'not_comparable' for r in data['comparisons'])


@pytest.mark.parametrize('changes', [{'a': 'AAA', 'b': 'aaa'}, {'a': ''}, {'b': 'A'*11},
                                     {'fiscal_year': ''}, {'fiscal_year': '２０２４'}, {'fiscal_year': '3000'}])
def test_invalid_selection_does_not_return_other_company_data(case, changes):
    assert query(case[0], **changes).status_code == 400


def test_access_is_enforced_at_api_and_expiration_is_immediate(case):
    client, plan = case
    assert query(APIClient()).status_code == 401
    for field, value in [('active', False), ('active', True), ('plan', 'free')]:
        setattr(plan, field, value); plan.save()
        assert query(client).status_code == (200 if field == 'active' and value else 403)
    plan.plan = 'pro'; plan.active = True; plan.expires_at = timezone.now()-timedelta(seconds=1); plan.save()
    assert query(client).status_code == 403


def test_unknown_company_is_not_replaced(case):
    assert query(case[0], a='UNKNOWN').status_code == 404


def test_tie_rounding_and_incompatible_method_are_explicit(case):
    data = query(case[0]).json()
    a, b = data['a'], data['b']
    b['pillars'][0]['score']['value'] = '99.999'
    assert comparison_rows(a, b)[0]['result'] == 'tie'
    b['methodology_version'] = 'other'
    assert all(r['result'] == 'not_comparable' for r in comparison_rows(a, b))
