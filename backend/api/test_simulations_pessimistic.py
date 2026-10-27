"""HU06_03 T36: percentil 10 ordenado respecto de mediana y percentil 90."""
from .test_simulations_base import market, URL


def test_percentiles_share_horizon_and_never_cross(market):
    result = market[0].post(URL, {'seed': 1}, format='json').data
    assert result['percentiles']['pessimistic'] == .1
    for low, base, high in zip(*(result['scenarios'][k] for k in ['pessimistic', 'base', 'optimistic'])):
        assert low['date'] == base['date'] == high['date']
        assert 0 < low['price'] <= base['price'] <= high['price']
