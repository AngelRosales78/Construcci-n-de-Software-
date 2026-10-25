"""HU04_05 T24: promedio ponderado, redondeo y cobertura de cinco notas."""
from decimal import Decimal
import pytest
from .pillar_scoring import overall_score, METRICS, VERSION
from .test_pillars_liquidity import case, URL

pytestmark = pytest.mark.django_db


def pillars(values):
    return [{'id': ident, 'label': ident, 'score': {'status': 'available', 'value': value, 'version': VERSION}}
            for ident, value in zip(METRICS, values)]


def test_manual_weighted_mean_is_not_altman_and_keeps_five_equal_weights():
    result = overall_score(pillars(['0', '25', '50', '75', '100']))
    assert result['display_value'] == '50,00'
    assert Decimal(result['value']) == 50
    assert sum(Decimal(p['weight']) for p in result['contributions']) == 1
    assert len(result['contributions']) == 5


@pytest.mark.parametrize('value,display', [('0', '0,00'), ('100', '100,00'), ('50.005', '50,01')])
def test_limits_and_half_up_rounding(value, display):
    assert overall_score(pillars([value]*5))['display_value'] == display


def test_mean_uses_raw_notes_not_rounded_values():
    result = overall_score(pillars(['10.004', '10.004', '10.004', '10.004', '10.024']))
    assert Decimal(result['value']) == Decimal('10.008')
    assert result['display_value'] == '10,01'  # rounding the five notes first would produce 10,00


@pytest.mark.parametrize('bad', [None, 'NaN', 'Infinity', '-1', '101'])
def test_one_invalid_note_prevents_a_partial_average(bad):
    result = overall_score(pillars(['100', '100', '100', '100', bad]))
    assert result['value'] is None and result['reason'] == 'incomplete_pillars'
    assert result['missing_pillars'] == ['growth']


@pytest.mark.parametrize('case_type', ['missing', 'duplicate', 'version', 'unavailable'])
def test_missing_duplicate_old_version_and_unavailable_notes_cannot_be_combined(case_type):
    rows = pillars(['50']*5)
    if case_type == 'missing': rows.pop()
    if case_type == 'duplicate': rows.append(rows[-1])
    if case_type == 'version': rows[-1]['score']['version'] = 'other-version'
    if case_type == 'unavailable': rows[-1]['score']['status'] = 'unavailable'
    assert overall_score(rows)['value'] is None


def test_api_does_not_publish_score_for_unknown_coverage(case):
    data = case[0].get(URL).json()
    assert data['overall_score']['value'] is None
    assert data['overall_score']['period']['end'] == '2024-06-30'
    assert data['overall_score']['version'] == VERSION
