"""HU05_03 T08: promedios de saldos comparables, pérdidas y patrimonio no positivo."""
from datetime import date
import pytest
from .test_pillars_liquidity import case, URL
from .models import AnnualFinancialStatement

pytestmark = pytest.mark.django_db


@pytest.fixture
def annual(case):
    client, current = case
    current.components.update({k: {'value': str(v), 'tag': k} for k, v in {
        'net_income': 20, 'book_equity': 100, 'total_assets': 200, 'revenue': 200}.items()})
    current.save()
    previous = AnnualFinancialStatement.objects.create(
        company=current.company, fiscal_year=2023, period_start=date(2022, 7, 1), period_end=date(2023, 6, 30),
        currency='USD', source_url=current.source_url, accession='0000950170-23-087843',
        filed=date(2023, 7, 30), snapshot_sha256='b'*64,
        components={k: {'value': str(v), 'tag': k} for k, v in {'book_equity': 60, 'total_assets': 100}.items()})
    return client, current, previous


def metrics(client):
    data = client.get(URL).json()
    pillar = next(p for p in data['pillars'] if p['id'] == 'profitability')
    return {m['id']: m for m in pillar['metrics']}


def test_manual_returns_use_average_balances_and_net_income(annual):
    client, _, _ = annual
    result = metrics(client)
    assert result['roe']['display_value'] == '25,00'  # 20 / ((100 + 60)/2) * 100
    assert result['roa']['display_value'] == '13,33'  # 20 / ((200 + 100)/2) * 100
    assert result['net_margin']['display_value'] == '10,00'
    assert len(result['roe']['components']) == 3
    assert result['roe']['components'][-1]['period']['end'] == '2023-06-30'
    assert 'Patrimonio contable del cierre anterior' in result['roe']['formula_display']
    pillar = next(p for p in client.get(URL).json()['pillars'] if p['id'] == 'profitability')
    assert pillar['sector_reference']['reason'] == 'insufficient_sector_sample'


@pytest.mark.parametrize('amount,expected', [('0', '0,00'), ('-20', '-25,00')])
def test_zero_profit_and_losses_are_not_missing(annual, amount, expected):
    client, current, _ = annual
    current.components['net_income']['value'] = amount; current.save()
    assert metrics(client)['roe']['display_value'] == expected
    assert metrics(client)['roe']['status'] == 'available'


@pytest.mark.parametrize('balance', ['0', '-1'])
@pytest.mark.parametrize('which', ['current', 'previous'])
def test_nonpositive_equity_at_either_end_does_not_give_misleading_roe(annual, balance, which):
    client, current, previous = annual
    row = current if which == 'current' else previous
    row.components['book_equity']['value'] = balance; row.save()
    assert metrics(client)['roe']['reason'] == 'nonpositive_equity'
    assert metrics(client)['roe']['value'] is None


@pytest.mark.parametrize('problem', ['missing', 'currency', 'gap'])
def test_unmatched_previous_balance_does_not_affect_current_margin(annual, problem):
    client, _, previous = annual
    if problem == 'missing': previous.delete()
    else:
        if problem == 'currency': previous.currency = 'EUR'
        if problem == 'gap': previous.period_end = date(2023, 5, 31)
        previous.save()
    result = metrics(client)
    assert result['roe']['reason'] == result['roa']['reason'] == 'insufficient_history'
    assert result['net_margin']['display_value'] == '10,00'


def test_zero_revenue_and_absent_profit_do_not_produce_a_percentage(annual):
    client, current, _ = annual
    current.components['revenue']['value'] = '0'; current.save()
    assert metrics(client)['net_margin']['reason'] == 'zero_denominator'
    current.components['net_income']['value'] = None; current.save()
    assert all(m['value'] is None for m in metrics(client).values())
