"""Pruebas optativas contra servicios temporales reales, activadas por variable de entorno."""
import os
import time
from types import SimpleNamespace
import pytest
import redis
from api.search_cache import preload_autocomplete, autocomplete

@pytest.mark.skipif(not os.environ.get('SPRINT_LIVE_REDIS_URL'), reason='Requiere Redis de validación')
def test_real_redis_extraction_under_100ms(monkeypatch):
    r=redis.from_url(os.environ['SPRINT_LIVE_REDIS_URL'],decode_responses=True)
    monkeypatch.setattr('api.search_cache.get_redis_connection',lambda:r)
    preload_autocomplete([SimpleNamespace(ticker=f'A{i:03}',name=f'Alpha {i}') for i in range(500)])
    autocomplete('a')
    durations=[]
    for _ in range(100):
        start=time.perf_counter();rows=autocomplete('a');durations.append((time.perf_counter()-start)*1000)
        assert len(rows)==10
    print(f'Redis real: n=100, máximo={max(durations):.3f} ms, promedio={sum(durations)/100:.3f} ms')
    assert max(durations)<100
