"""HU05_01 T20: anclajes, interpolación, límites y ausencia de notas parciales."""
from decimal import Decimal
import pytest
from .pillar_scoring import normalize, score_pillar, VERSION
from .test_pillars_liquidity import case, URL
from .test_pillars_profitability import annual
from .test_pillars_efficiency import operations

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('ident,value,expected', [
    ('current_ratio', '-1', '0'), ('current_ratio', '1', '50'), ('current_ratio', '1.5', '75'),
    ('current_ratio', '3', '100'), ('net_margin', '-5', '0'), ('net_margin', '15', '75'),
    ('liabilities_to_equity', '0', '100'), ('liabilities_to_equity', '1.5', '62.5'),
    ('liabilities_to_equity', '5', '0'), ('inventory_days', '45', '75'), ('inventory_days', '180', '0'),
    ('revenue_growth', '20', '100'),
])
def test_academic_anchor_interpolation_and_clamping(ident, value, expected):
    assert normalize(ident, value) == Decimal(expected)


@pytest.mark.parametrize('raw', ['NaN', 'Infinity', '-Infinity'])
def test_nonfinite_values_cannot_be_normalized(raw):
    with pytest.raises(ValueError): normalize('current_ratio', raw)


def test_note_is_mean_of_required_metrics_not_of_displayed_rounded_values(case):
    client, current = case
    current.components['current_assets']['value'] = '100'; current.save()
    score = client.get(URL).json()['pillars'][0]['score']
    assert score['display_value'] == '75,00'  # ratio=1 ->50; acid=1.1 ->100
    assert score['version'] == VERSION
    assert score['parts'][0]['input_value'] == '1'
    assert score['parts'][0]['anchors'][1] == {'input': '1', 'score': '50'}


def test_missing_indicator_is_not_replaced_by_zero_or_ignored(case):
    client, current = case
    current.components['receivables']['value'] = None; current.save()
    score = client.get(URL).json()['pillars'][0]['score']
    assert score['value'] is None and score['missing_metrics'] == ['quick_ratio']
    assert len(score['parts']) == 1


def test_only_documented_not_applicable_inventory_can_be_excluded(operations):
    client, current, _ = operations
    current.inventory_applicable = False
    current.inventory_applicability_note = 'Actividad sin inventario, clasificación de prueba.'
    current.save()
    efficiency = next(p for p in client.get(URL).json()['pillars'] if p['id'] == 'efficiency')
    score = efficiency['score']
    assert score['display_value'] == '100,00'
    assert score['excluded_metrics'][0]['id'] == 'inventory_days'
    assert len(score['parts']) == 1


def test_unknown_not_applicable_flag_cannot_hide_a_missing_indicator():
    p = {'id': 'efficiency', 'metrics': [
        {'id': 'asset_turnover', 'label': 'Rotación', 'status': 'available', 'value': '1'},
        {'id': 'inventory_days', 'status': 'not_applicable', 'reason': 'unknown', 'explanation': 'Sin datos'},
    ]}
    assert score_pillar(p)['value'] is None
