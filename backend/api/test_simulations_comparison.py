"""HU06_05 T44: tabla coherente y recuperación privada sin recalcular."""
from django.contrib.auth import get_user_model
from .test_simulations_base import market, URL


def test_final_table_matches_curves_and_retrieval_does_not_run_engine(market, monkeypatch):
    client = market[0]
    result = client.post(URL, {}, format='json').data
    for key, curve in result['scenarios'].items():
        assert result['final_prices'][key]['price'] == curve[-1]['price']
        assert result['final_prices'][key]['date'] == curve[-1]['date']
    monkeypatch.setattr('api.simulation_views.run_simulation', lambda *a: (_ for _ in ()).throw(AssertionError('No recalcular')))
    route = f"/api/v1/simulations/{result['simulation_id']}/"
    assert client.get(route).data == result
    client.force_authenticate(get_user_model().objects.create_user(username='otra_cuenta'))
    assert client.get(route).status_code == 404
