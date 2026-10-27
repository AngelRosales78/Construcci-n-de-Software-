"""HU07_05 T64: niveles válidos, mismo contexto y caché separada."""
from .test_pillars_liquidity import case
from .test_analysis_validation import analysis_case, URL


def test_levels_have_same_metrics_but_independent_cache(analysis_case):
    client = analysis_case[0]
    simple = client.get(URL + '&level=simple').data
    technical = client.get(URL + '&level=technical').data
    assert simple['metrics'] == technical['metrics']
    assert simple['context_hash'] != technical['context_hash']
    assert technical['level'] == 'technical' and not technical['cache_hit']
    assert client.get(URL + '&level=technical').data['cache_hit']


def test_unsupported_level_is_not_silently_accepted(analysis_case):
    assert analysis_case[0].get(URL + '&level=fiction').status_code == 400
