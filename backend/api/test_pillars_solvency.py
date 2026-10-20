"""HU05_04 T12: distinguir patrimonio contable, valor bursátil y Altman-Z."""
import pytest
from .test_pillars_liquidity import case, URL

pytestmark = pytest.mark.django_db


@pytest.fixture
def debt(case):
    client, current = case
    current.components.update({k: {'value': str(v), 'tag': k} for k, v in {
        'total_liabilities': 150, 'book_equity': 100, 'income_before_tax': 30,
        'interest_expense': 10}.items()})
    current.save()
    # The legacy market value must never substitute book equity.
    current.company.equity_value = 999999
    current.company.save()
    return client, current


def metrics(client):
    pillar = next(p for p in client.get(URL).json()['pillars'] if p['id'] == 'solvency')
    return {m['id']: m for m in pillar['metrics']}


def test_manual_debt_and_interest_calculations_are_not_altman(debt):
    result = metrics(debt[0])
    assert result['liabilities_to_equity']['display_value'] == '1,50'
    assert result['interest_coverage']['display_value'] == '4,00'  # (30 + 10) / 10
    assert result['liabilities_to_equity']['thresholds']['band'] == 'middle'
    assert result['interest_coverage']['thresholds']['band'] == 'high'
    assert 'altman_z' not in debt[0].get(URL).json()


@pytest.mark.parametrize('amount,reason', [('0', 'zero_denominator'), ('-1', 'invalid_denominator'),
                                          (None, 'missing_components')])
def test_no_finite_ratio_for_zero_negative_or_missing_equity(debt, amount, reason):
    client, current = debt
    current.components['book_equity']['value'] = amount; current.save()
    assert metrics(client)['liabilities_to_equity']['reason'] == reason
    assert metrics(client)['liabilities_to_equity']['value'] is None


def test_zero_interest_does_not_become_infinite_coverage(debt):
    client, current = debt
    current.components['interest_expense']['value'] = '0'; current.save()
    assert metrics(client)['interest_coverage']['reason'] == 'zero_denominator'
    assert metrics(client)['liabilities_to_equity']['value'] == '1.5'


def test_operating_loss_and_zero_liabilities_are_real_values(debt):
    client, current = debt
    current.components['income_before_tax']['value'] = '-30'
    current.components['total_liabilities']['value'] = '0'; current.save()
    result = metrics(client)
    assert result['interest_coverage']['display_value'] == '-2,00'
    assert result['liabilities_to_equity']['display_value'] == '0,00'


@pytest.mark.parametrize('raw', ['NaN', 'Infinity', 'oops'])
def test_unexpected_corrupt_component_is_never_published(debt, raw):
    client, current = debt
    current.components['income_before_tax']['value'] = raw; current.save()
    assert metrics(client)['interest_coverage']['reason'] == 'invalid_components'


@pytest.mark.parametrize('value,band', [('1', 'high'), ('1.01', 'middle'), ('2', 'middle'), ('2.01', 'low')])
def test_debt_threshold_boundaries_are_explicit_and_versioned(debt, value, band):
    client, current = debt
    current.components['book_equity']['value'] = '1'
    current.components['total_liabilities']['value'] = value; current.save()
    policy = metrics(client)['liabilities_to_equity']['thresholds']
    assert policy['band'] == band
    assert policy['version'] == 'academic-thresholds-v1'
    assert 'no son un óptimo universal' in policy['explanation']
