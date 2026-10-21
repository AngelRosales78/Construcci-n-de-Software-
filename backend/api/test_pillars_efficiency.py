"""HU05_05 T16: cálculo manual, series incompletas y aplicabilidad documentada."""
from datetime import date
import pytest
from django.core.exceptions import ValidationError
from .test_pillars_profitability import annual
from .test_pillars_liquidity import case, URL
from .models import AnnualFinancialStatement

pytestmark = pytest.mark.django_db


@pytest.fixture
def operations(annual):
    client, current, previous = annual
    current.components.update({k: {'value': str(v), 'tag': k} for k, v in {
        'inventory': 30, 'cost_of_sales': 100}.items()})
    previous.components['inventory'] = {'value': '10', 'tag': 'InventoryNet'}
    current.save(); previous.save()
    return client, current, previous


def pillar(client, year=None):
    data = client.get(URL, {'fiscal_year': year} if year else {}).json()
    return next(p for p in data['pillars'] if p['id'] == 'efficiency')


def test_manual_efficiency_and_source_of_opening_balance(operations):
    result = pillar(operations[0])
    values = {m['id']: m for m in result['metrics']}
    assert values['asset_turnover']['display_value'] == '1,33'  # 200 / 150
    assert values['inventory_days']['display_value'] == '73,00'  # average(10,30) / 100 * 365
    assert values['inventory_days']['components'][1]['period']['end'] == '2023-06-30'
    assert len(result['history']) == 2


def test_missing_previous_year_makes_averages_unavailable(operations):
    client, _, previous = operations
    previous.delete()
    assert all(m['reason'] == 'insufficient_history' for m in pillar(client)['metrics'])
    assert [r['fiscal_year'] for r in pillar(client)['history']] == [2024]


def test_zero_inventory_is_reported_zero_not_not_applicable(operations):
    client, current, previous = operations
    for row in [current, previous]:
        row.components['inventory']['value'] = '0'; row.save()
    value = pillar(client)['metrics'][1]
    assert value['status'] == 'available' and value['display_value'] == '0,00'


def test_zero_cost_and_missing_inventory_have_distinct_reasons(operations):
    client, current, _ = operations
    current.components['cost_of_sales']['value'] = '0'; current.save()
    assert pillar(client)['metrics'][1]['reason'] == 'zero_denominator'
    current.components['inventory']['value'] = None; current.save()
    assert pillar(client)['metrics'][1]['reason'] == 'missing_components'


def test_not_applicable_requires_an_explanation_and_is_not_missing(operations):
    client, current, _ = operations
    current.inventory_applicable = False
    with pytest.raises(ValidationError, match='motivo documentado'):
        current.full_clean()
    current.inventory_applicability_note = 'Caso didáctico: actividad sin inventario físico.'
    current.full_clean(); current.save()
    result = pillar(client)['metrics'][1]
    assert result['status'] == 'not_applicable' and result['value'] is None
    assert 'actividad sin inventario' in result['explanation']


def test_history_does_not_invent_years_or_include_later_periods(operations):
    client, current, _ = operations
    for year in (2020, 2021, 2022):
        AnnualFinancialStatement.objects.create(
            company=current.company, fiscal_year=year, period_start=date(year-1, 7, 1),
            period_end=date(year, 6, 30), currency='USD', source_url=current.source_url,
            accession=current.accession, filed=date(year, 7, 30), snapshot_sha256='a'*64,
            components=current.components)
    assert [r['fiscal_year'] for r in pillar(client)['history']] == [2024, 2023, 2022]
    assert [r['fiscal_year'] for r in pillar(client, 2021)['history']] == [2021, 2020]
    assert pillar(client, 2019)['history'] == []
