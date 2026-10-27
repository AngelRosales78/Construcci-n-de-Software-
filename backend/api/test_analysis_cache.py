"""HU07_01 T52: caché por contexto y fallos reales del proveedor."""
from .test_pillars_liquidity import case
from .test_analysis_validation import analysis_case, URL
from .models import AnalysisRecord


def test_cache_is_reused_only_for_same_data_and_version(analysis_case, monkeypatch):
    client, _ = analysis_case
    first = client.get(URL)
    assert first.status_code == 200 and first.data['validated']
    monkeypatch.setattr('api.analysis_views.generate', lambda *a: (_ for _ in ()).throw(OSError('Offline')))
    assert client.get(URL).data['cache_hit'] is True
    AnalysisRecord.objects.filter(validated=True).update(context_hash='old-version')
    assert client.get(URL).status_code == 503


def test_validated_input_is_recorded_without_credentials(analysis_case):
    analysis_case[0].get(URL)
    record = AnalysisRecord.objects.get(validated=True)
    assert record.context['ticker'] == 'MSFT'
    assert 'password' not in str(record.context).lower()
