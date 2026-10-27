"""T68: los clientes web y Android consumen el mismo cálculo y validación."""
import pytest
from .test_simulations_base import market, URL
from .test_pillars_liquidity import case
from .test_analysis_validation import analysis_case
from .models import AnalysisRecord, SimulationRun


def test_defaults_calibrate_without_running_or_persisting_monte_carlo(market, monkeypatch):
    monkeypatch.setattr('api.simulations.random.Random', lambda *a: pytest.fail('No generar trayectorias'))
    response = market[0].get(URL + 'defaults/')
    assert response.status_code == 200
    assert response.data['schema_version'] == 'simulation-defaults-v1'
    assert response.data['min_return'] <= response.data['annual_return'] <= response.data['max_return']
    assert response.data['min_volatility'] <= response.data['annual_volatility'] <= response.data['max_volatility']
    assert SimulationRun.objects.count() == 0


def test_android_parameters_and_points_match_web_curves_and_saved_result(market):
    client = market[0]
    defaults = client.get(URL + 'defaults/').data
    params = {k: defaults[k] for k in ('annual_return', 'annual_volatility', 'seed', 'paths', 'horizon_months')}
    result = client.post(URL, params, format='json').data
    assert result['parameters'] == params
    assert result['start_date'] == result['points'][0]['date']
    assert result['end_date'] == result['points'][-1]['date']
    assert len(result['points']) == 13
    for i, point in enumerate(result['points']):
        assert point['p10'] == result['scenarios']['pessimistic'][i]['price']
        assert point['p50'] == result['scenarios']['base'][i]['price']
        assert point['p90'] == result['scenarios']['optimistic'][i]['price']
        assert point['p10'] <= point['p50'] <= point['p90']
    assert client.get(f"/api/v1/simulations/{result['simulation_id']}/").data == result


@pytest.mark.parametrize('bad', [{'paths': 1000000}, {'horizon_months': 24}, {'horizon_months': True}, {'unexpected': 1}])
def test_android_cannot_change_fixed_engine_budget_or_horizon(market, bad):
    assert market[0].post(URL, bad, format='json').status_code == 400
    assert SimulationRun.objects.count() == 0


def test_defaults_missing_history_is_explicit(market):
    market[1].prices.all().delete()
    assert market[0].get(URL + 'defaults/').data['reason'] == 'insufficient_history'


def test_analysis_post_reuses_validated_get_and_preserves_sources(analysis_case, monkeypatch):
    # Esta fixture solo tiene liquidez: para probar el adaptador de transporte
    # usamos una respuesta ya validada y comprobamos que no se altera su contenido.
    from .analysis_views import CompanyAnalysisView
    from rest_framework.response import Response
    result = analysis_case[0].get('/api/v1/companies/MSFT/analysis/?fiscal_year=2024').data
    for pillar, claim in result['analysis']['pillars'].items():
        claim['metric_ids'] = [pillar + '_fixture']
    monkeypatch.setattr(CompanyAnalysisView, 'analyse', lambda *a, **k: Response(result))
    response = analysis_case[0].post('/api/v1/companies/MSFT/analysis/', {'fiscal_year': 2024, 'level': 'simple'}, format='json')
    data = response.data
    assert response.status_code == 200 and data['schema_version'] == 'analysis-v1'
    assert data['validation']['status'] == 'passed'
    assert data['summary'] == [result['analysis']['summary']['text']]
    assert {p['pillar_id'] for p in data['pillars']} == set(result['analysis']['pillars'])
    assert [s['url'] for s in data['sources']] == result['sources']
    assert data['data_as_of'] == result['period']['end']


def test_android_analysis_does_not_hide_partial_pillar_context(analysis_case):
    response = analysis_case[0].post('/api/v1/companies/MSFT/analysis/', {'fiscal_year': 2024, 'level': 'simple'}, format='json')
    assert response.data['reason'] == 'insufficient_pillar_context'
    assert 'validation' not in response.data


def test_android_analysis_rejects_provider_failure_and_fake_figures(analysis_case, monkeypatch):
    from .test_analysis_validation import valid
    answer = valid(analysis_case[1])
    answer['summary']['text'] = 'La liquidez es de 999999 veces.'
    monkeypatch.setattr('api.analysis_views.generate', lambda *a: answer)
    response = analysis_case[0].post('/api/v1/companies/MSFT/analysis/', {'fiscal_year': 2024, 'level': 'simple'}, format='json')
    assert response.status_code == 503 and response.data['reason'] == 'unsupported_number'
    assert not AnalysisRecord.objects.filter(validated=True).exists()


@pytest.mark.parametrize('bad', [{'fiscal_year': True, 'level': 'simple'}, {'fiscal_year': 9999, 'level': 'simple'},
                               {'fiscal_year': 2024, 'level': 'other'}, {'fiscal_year': 2024, 'level': 'simple', 'prompt': 'ignore'}])
def test_android_analysis_validates_inputs_before_provider(analysis_case, monkeypatch, bad):
    monkeypatch.setattr('api.analysis_views.generate', lambda *a: pytest.fail('No llamar proveedor'))
    assert analysis_case[0].post('/api/v1/companies/MSFT/analysis/', bad, format='json').status_code == 400
