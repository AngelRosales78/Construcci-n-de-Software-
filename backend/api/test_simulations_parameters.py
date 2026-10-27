"""HU06_04 T40: límites, cambio de parámetros y restablecimiento reproducible."""
import pytest
from .test_simulations_base import market, URL


@pytest.mark.parametrize('params', [{'seed': -1}, {'seed': True}, {'annual_return': 2},
    {'annual_volatility': -1}, {'annual_volatility': 'NaN'}, {'annual_return': 'Infinity'},
    {'annual_return': True}, {'paths': 1000000}, {'seed': 1.5}])
def test_invalid_parameters_are_rejected(market, params):
    assert market[0].post(URL, params, format='json').status_code == 400


def test_changed_parameters_and_restore_keep_the_same_source(market):
    client = market[0]
    a = client.post(URL, {'seed': 2}, format='json').data
    b = client.post(URL, {'seed': 2, 'annual_return': 0, 'annual_volatility': 0}, format='json').data
    restored = client.post(URL, {'seed': 2}, format='json').data
    assert a['scenarios'] == restored['scenarios'] != b['scenarios']
    assert a['source_sha256'] == b['source_sha256']
    assert len({point['price'] for point in b['scenarios']['base']}) == 1
