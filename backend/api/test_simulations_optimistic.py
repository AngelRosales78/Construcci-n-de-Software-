"""HU06_02 T32: percentil 90 y mediana provienen de las mismas trayectorias."""
from .test_simulations_base import market, URL


def test_optimistic_is_above_base_with_same_dates_currency_and_run(market):
    result = market[0].post(URL, {}, format='json').data
    assert result['percentiles']['optimistic'] == .9
    for base, optimistic in zip(result['scenarios']['base'], result['scenarios']['optimistic']):
        assert base['date'] == optimistic['date'] and base['price'] <= optimistic['price']
    assert result['simulation_id'] and result['currency'] == 'USD'
