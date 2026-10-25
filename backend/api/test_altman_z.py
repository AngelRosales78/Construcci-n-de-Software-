"""HU04_03: fórmula, límites de zonas y estados sin información suficiente."""
from types import SimpleNamespace
from decimal import Decimal
from unittest.mock import MagicMock
import pytest
from django.contrib.auth import get_user_model
from django.urls import reverse
from rest_framework.test import APIClient
from api.models import Company
from api.views import calculate_altman_z

FIELDS = ('working_capital','total_assets','retained_earnings','ebit',
          'equity_value','total_liabilities','sales')
def balance(**changes):
    values = dict(working_capital=0, total_assets=1, retained_earnings=0, ebit=0,
                  equity_value=0, total_liabilities=1, sales=0)
    values.update(changes)
    return SimpleNamespace(**values)

def test_reference_formula_and_components():
    result = calculate_altman_z(balance(working_capital=100, total_assets=1000,
        retained_earnings=200, ebit=100, equity_value=1000, total_liabilities=500, sales=1500))
    assert result == {'score': 3.43, 'zone': 'safe', 'risk_level': 'low',
        'components': {'x1': 0.1, 'x2': 0.2, 'x3': 0.1, 'x4': 2.0, 'x5': 1.5}}

@pytest.mark.parametrize('score,zone,risk', [
    ('1.8099','distress','high'), ('1.81','grey','medium'),
    ('1.8101','grey','medium'), ('2.9899','grey','medium'),
    ('2.99','grey','medium'), ('2.9901','safe','low')])
def test_exact_boundaries_before_display_rounding(score, zone, risk):
    result = calculate_altman_z(balance(sales=Decimal(score)))
    assert (result['zone'], result['risk_level']) == (zone, risk)

@pytest.mark.parametrize('field', FIELDS)
@pytest.mark.parametrize('invalid', [None, 'NaN', 'Infinity', '-Infinity', 'incorrecto'])
def test_invalid_financial_value_is_not_zero_risk(field, invalid):
    result = calculate_altman_z(balance(**{field: invalid}))
    assert result == {'score': None, 'zone': 'unknown', 'risk_level': 'unknown'}

@pytest.mark.parametrize('field', ['total_assets', 'total_liabilities'])
@pytest.mark.parametrize('invalid', [0, -1])
def test_nonpositive_denominator_is_unknown(field, invalid):
    assert calculate_altman_z(balance(**{field: invalid}))['score'] is None

@pytest.mark.parametrize('field', ['equity_value', 'sales'])
def test_negative_sales_or_equity_is_unknown(field):
    assert calculate_altman_z(balance(**{field: -1}))['zone'] == 'unknown'

def test_operating_losses_are_valid():
    result = calculate_altman_z(balance(working_capital=-1, retained_earnings=-1, ebit=-1))
    assert result['score'] == -5.9
    assert result['zone'] == 'distress'

def test_zero_score_is_valid_when_denominators_are_valid():
    result = calculate_altman_z(balance())
    assert result['score'] == 0
    assert result['zone'] == 'distress'

@pytest.mark.django_db
def test_overview_preserves_unknown_as_json_null(monkeypatch):
    redis = MagicMock()
    redis.exists.return_value = 0
    monkeypatch.setattr('api.redis_utils.get_redis_connection', lambda: redis)
    user = get_user_model().objects.create_user(username='analista')
    Company.objects.create(ticker='NONE', name='Sin balance', sector='technology')
    client = APIClient()
    client.force_authenticate(user)
    response = client.get(reverse('auth:company-overview', kwargs={'ticker': 'NONE'}))
    assert response.status_code == 200
    assert response.json()['altman_z']['score'] is None
    assert response.json()['altman_z']['zone'] == 'unknown'
