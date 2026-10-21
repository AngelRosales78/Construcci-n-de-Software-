"""HU05_01 T20: tasa anual y compuesta sin omitir cierres intermedios."""
from datetime import date
import pytest
from .test_pillars_profitability import annual
from .test_pillars_liquidity import case, URL
from .models import AnnualFinancialStatement

pytestmark = pytest.mark.django_db


@pytest.fixture
def revenues(annual):
    client, current, previous = annual
    current.components['revenue']['value'] = '172.8'; current.save()
    previous.components['revenue'] = {'value': '144', 'tag': 'Revenue'}; previous.save()
    rows = {2024: current, 2023: previous}
    for year, amount in [(2022, '120'), (2021, '100')]:
        rows[year] = AnnualFinancialStatement.objects.create(
            company=current.company, fiscal_year=year, period_start=date(year-1, 7, 1),
            period_end=date(year, 6, 30), currency='USD', source_url=current.source_url,
            accession=current.accession, filed=date(year, 7, 30), snapshot_sha256='a'*64,
            components={'revenue': {'value': amount, 'tag': 'Revenue'}})
    return client, rows


def metrics(client):
    p = next(p for p in client.get(URL).json()['pillars'] if p['id'] == 'growth')
    return {m['id']: m for m in p['metrics']}


def test_known_twenty_percent_growth_and_four_distinct_sources(revenues):
    result = metrics(revenues[0])
    assert result['revenue_growth']['display_value'] == '20,00'
    assert result['revenue_cagr_3y']['display_value'] == '20,00'
    assert {c['period']['end'] for c in result['revenue_cagr_3y']['components']} == {
        '2021-06-30', '2022-06-30', '2023-06-30', '2024-06-30'}


@pytest.mark.parametrize('failure', ['missing_year', 'missing_revenue', 'currency', 'gap'])
def test_missing_intermediate_year_does_not_turn_into_three_point_cagr(revenues, failure):
    client, rows = revenues
    row = rows[2022]
    if failure == 'missing_year': row.delete()
    else:
        if failure == 'missing_revenue': row.components['revenue']['value'] = None
        if failure == 'currency': row.currency = 'EUR'
        if failure == 'gap': row.period_end = date(2022, 6, 29)
        row.save()
    result = metrics(client)
    assert result['revenue_growth']['display_value'] == '20,00'
    assert result['revenue_cagr_3y']['reason'] == 'insufficient_history'


def test_zero_start_and_zero_end_are_distinct(revenues):
    client, rows = revenues
    rows[2021].components['revenue']['value'] = '0'; rows[2021].save()
    assert metrics(client)['revenue_cagr_3y']['reason'] == 'zero_denominator'
    rows[2021].components['revenue']['value'] = '100'; rows[2021].save()
    rows[2024].components['revenue']['value'] = '0'; rows[2024].save()
    assert all(v['display_value'] == '-100,00' for v in metrics(client).values())


def test_decline_is_a_real_negative_growth_value(revenues):
    client, rows = revenues
    rows[2024].components['revenue']['value'] = '51.2'; rows[2024].save()
    assert metrics(client)['revenue_cagr_3y']['display_value'] == '-20,00'
