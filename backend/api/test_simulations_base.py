"""HU06_01 T28: semilla, horizonte, entradas y persistencia del histórico."""
from datetime import date, timedelta
from decimal import Decimal
import pytest
from django.contrib.auth import get_user_model
from rest_framework.test import APIClient
from .models import Company, PriceHistory, SimulationRun
from .simulations import run_simulation

URL = '/api/v1/companies/SIM/simulations/'


def history(count=260):
    return [dict(date=date(2024, 1, 1) + timedelta(days=i), adjusted_close=Decimal('100') + Decimal(i)/10,
                 currency='USD', source='Fixture de prueba', is_demo=True) for i in range(count)]


@pytest.fixture
def market(db, monkeypatch):
    monkeypatch.setattr('api.simulations.PATHS', 200)
    monkeypatch.setattr('api.simulation_views.SimulationThrottle.allow_request', lambda *a: True)
    company = Company.objects.create(ticker='SIM', name='Empresa de prueba', sector='technology')
    PriceHistory.objects.bulk_create([PriceHistory(company=company, **row) for row in history()])
    user = get_user_model().objects.create_user(username='simulador')
    client = APIClient(); client.force_authenticate(user)
    return client, company, user


def test_seed_reproduces_monthly_prices_and_calendar_horizon(monkeypatch):
    monkeypatch.setattr('api.simulations.PATHS', 200)
    a, _ = run_simulation(history(), 42)
    b, _ = run_simulation(history(), 42)
    assert a == b and len(a['scenarios']['base']) == 13
    assert a['scenarios']['base'][0]['date'] == '2024-09-16'
    assert a['scenarios']['base'][-1]['date'] == '2025-09-16'
    assert a['horizon_months'] == 12


def test_zero_volatility_has_known_exponential_path(monkeypatch):
    import math
    monkeypatch.setattr('api.simulations.PATHS', 50)
    a, _ = run_simulation(history(), 1, {'annual_return': .1, 'annual_volatility': 0})
    assert a['scenarios']['base'][-1]['price'] == pytest.approx(125.9 * math.exp(.1), abs=1e-5)


def test_missing_history_is_explicit_and_does_not_store_a_fake_run(market):
    client, company, _ = market
    company.prices.all().delete()
    response = client.post(URL, {}, format='json')
    assert response.status_code == 200
    assert response.data['status'] == 'unavailable' and SimulationRun.objects.count() == 0


def test_snapshot_hash_and_seed_are_persisted(market):
    client, _, _ = market
    response = client.post(URL, {'seed': 9}, format='json')
    assert response.status_code == 200
    run = SimulationRun.objects.get(pk=response.data['simulation_id'])
    assert len(run.source_snapshot) == 260 and run.result['seed'] == 9
    assert len(run.result['source_sha256']) == 64
    assert run.result['is_demo'] is True


def test_anonymous_requests_are_rejected(market):
    assert APIClient().post(URL, {}, format='json').status_code == 401


@pytest.mark.parametrize('change', ['currency', 'negative', 'duplicate', 'reversed'])
def test_invalid_series_cannot_be_presented_as_a_forecast(change):
    rows = history()
    if change == 'currency': rows[-1]['currency'] = 'EUR'
    if change == 'negative': rows[-1]['adjusted_close'] = -1
    if change == 'duplicate': rows[-1]['date'] = rows[-2]['date']
    if change == 'reversed': rows.reverse()
    assert run_simulation(rows, 1)[0]['reason'] == 'invalid_history'
