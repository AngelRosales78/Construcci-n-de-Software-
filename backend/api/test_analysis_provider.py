"""HU07_04 T48: el proveedor no puede salir del vocabulario revisado."""
from contextlib import contextmanager
from types import SimpleNamespace
import json
import pytest
from .analysis_provider import generate


def context():
    return {'pillars': dict.fromkeys(['liquidity', 'profitability', 'solvency', 'efficiency', 'growth']),
            'metrics': {
                'quick_ratio': {'status': 'available', 'label': 'Prueba ácida', 'pillar': 'liquidity', 'band': 'high'},
                'interest_coverage': {'status': 'available', 'label': 'Cobertura de intereses', 'pillar': 'solvency', 'band': 'high'},
            }}


def fake_provider(monkeypatch, alter=None):
    captured = []
    @contextmanager
    def response(request, **kwargs):
        payload = json.loads(request.data)
        captured.append(payload)
        texts = {key: prop['enum'][0] for key, prop in payload['json_schema']['properties'].items()}
        if alter: alter(texts)
        raw = json.dumps({'content': json.dumps(texts)}).encode()
        yield SimpleNamespace(read=lambda limit: raw)
    monkeypatch.setattr('api.analysis_provider.urllib.request.urlopen', response)
    return captured


def test_provider_cannot_substitute_acid_test_for_interest_coverage(monkeypatch):
    fake_provider(monkeypatch, lambda texts: texts.update(solvency='La cobertura de intereses se mide mediante la prueba ácida.'))
    with pytest.raises(ValueError, match='unsupported_claim'):
        generate(context(), 'technical')


def test_each_level_has_reviewed_language_and_missing_pillars_are_explicit(monkeypatch):
    captured = fake_provider(monkeypatch)
    simple = generate(context(), 'simple')
    technical = generate(context(), 'technical')
    assert simple['pillars']['liquidity']['text'] != technical['pillars']['liquidity']['text']
    assert 'EBIT' in technical['pillars']['solvency']['text']
    assert 'Prueba ácida' not in technical['pillars']['solvency']['text']
    assert simple['pillars']['growth']['metric_ids'] == []
    assert 'No disponible' in simple['pillars']['growth']['text']
    assert captured[0]['json_schema']['properties']['liquidity']['enum']


def test_offline_model_does_not_return_a_reviewed_explanation_as_fallback(monkeypatch):
    def offline(*args, **kwargs): raise OSError('Servidor local desconectado')
    monkeypatch.setattr('api.analysis_provider.urllib.request.urlopen', offline)
    with pytest.raises(OSError): generate(context(), 'simple')
