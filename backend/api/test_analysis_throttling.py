"""HU07_01 T52: simular no consume el cupo de explicaciones de IA."""
from types import SimpleNamespace
from django.core.cache import cache
from django.test import override_settings
from .analysis_views import AnalysisThrottle
from .simulation_views import SimulationThrottle


@override_settings(CACHES={'default': {'BACKEND': 'django.core.cache.backends.locmem.LocMemCache',
                                      'LOCATION': 'independent-sprint2-throttles'}})
def test_simulation_and_analysis_have_independent_enforced_limits():
    cache.clear()
    request = SimpleNamespace(user=SimpleNamespace(pk=1, is_authenticated=True))
    for _ in range(12):
        assert SimulationThrottle().allow_request(request, None)
    assert not SimulationThrottle().allow_request(request, None)
    for _ in range(4):
        assert AnalysisThrottle().allow_request(request, None)
    assert not AnalysisThrottle().allow_request(request, None)
