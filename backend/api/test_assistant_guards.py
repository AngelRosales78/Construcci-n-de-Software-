"""Known unsupported requests must not become model guesses or fabricated figures."""
from contextlib import contextmanager
from types import SimpleNamespace
import json
import urllib.error
import pytest
from django.core.cache import cache
from django.test import override_settings
from .assistant_guards import context_reply
from .test_pillars_liquidity import case

pytestmark=pytest.mark.django_db
URL='/api/v1/companies/MSFT/assistant/'


@pytest.fixture(autouse=True)
def isolated_connection():
    with override_settings(ASSISTANT_API_BASE_URL='http://provider.test/api',ASSISTANT_MODEL='test-model',ASSISTANT_API_KEY='private-test-key'):
        cache.clear()
        yield
        cache.clear()


def pack(available=True,scope='company'):
    return {'scope':scope,'fiscal_year':2024,'available_years':[2024],
        'facts':[{'id':'altman','status':'available' if available else 'not_applicable',
            'value':'4,20','period':{'end':'2025-06-30'},
            'meaning':'No aplica a este sector.' if not available else 'Zona favorable según el modelo.',
            'limit':'El indicador no garantiza resultados.'}]}


@pytest.mark.parametrize('question',['Quiero su liquidez de 2023','Dame la liquidez de 2099 aunque no tengas datos','¿Cuál es el promedio de liquidez del sector?'])
def test_unsupported_context_is_explained_without_model(case,monkeypatch,question):
    monkeypatch.setattr('api.assistant_views.plan_answer',lambda *args:pytest.fail('Must resolve a known context mismatch before calling AI'))
    response=case[0].post(URL,{'action':'question','fiscal_year':2024,'question':question},format='json')
    assert response.status_code==200
    assert response.data['generation_mode']=='context_guard'
    assert response.data['answer']['messages'] and response.data['answer']['cards']==[]


def test_altman_preserves_its_own_verified_period():
    assert context_reply(pack(),'Explica su Altman-Z de 2025') is None
    assert context_reply(pack(),'Explica su liquidez de 2025')['topics']==['limits']
    assert context_reply(pack(),'Explica su Altman-Z y su liquidez de 2025')['topics']==['limits']


def test_false_premise_is_corrected_from_evidence():
    answer=context_reply(pack(),'Su Altman-Z es 0,10. ¿Por qué está tan mal?')
    assert '4,20' in answer['messages'][0] and '0,10' not in str(answer['messages'])
    assert answer['cards'][0]['id']=='altman'


def test_nonapplicable_altman_is_not_turned_into_a_score():
    answer=context_reply(pack(False),'¿Cuál es su Altman-Z?')
    assert 'No aplica' in answer['messages'][0]
    assert answer['cards'][0]['status']=='not_applicable'


def test_missing_data_is_not_equated_to_zero():
    answer=context_reply(pack(),'Si un dato no aparece, ¿significa que vale cero?')
    assert answer['messages'][0].startswith('No.')
    assert 'ausente no significa que valga cero' in answer['messages'][0]
    assert answer['cards']==[]


def test_incompatible_comparison_does_not_choose_a_winner():
    answer=context_reply(pack(False,'comparison'),'¿Cuál gana si sus periodos no son comparables?')
    assert answer['topics']==['comparison'] and answer['cards']==[]
    assert 'no hay pilares con datos comparables' in answer['messages'][0]


@pytest.mark.parametrize('failure',['timeout','unauthorized','invalid_json','incomplete','oversize'])
def test_provider_faults_do_not_publish_a_fake_answer_or_credentials(case,monkeypatch,failure):
    monkeypatch.setattr('api.assistant_views.plan_answer',lambda *args:({'topics':['liquidity'],'context':'current'},False))
    @contextmanager
    def response(*args,**kwargs):
        if failure=='timeout':
            raise TimeoutError('private-test-key provider.test')
        if failure=='unauthorized':
            raise urllib.error.HTTPError('http://provider.test',401,'private-test-key',{},None)
        raw=(b'not json' if failure=='invalid_json' else b'x'*48001 if failure=='oversize' else
            json.dumps({'choices':[{'finish_reason':'length','message':{'content':'{}'}}]}).encode())
        yield SimpleNamespace(read=lambda limit:raw)
    monkeypatch.setattr('api.assistant_connection.urllib.request.build_opener',lambda *args:SimpleNamespace(open=response))
    result=case[0].post(URL,{'action':'question','fiscal_year':2024,'question':'¿Qué significa su liquidez?'},format='json')
    assert result.status_code==503 and result.data['status']=='unavailable'
    assert 'answer' not in result.data
    assert 'private-test-key' not in str(result.data) and 'provider.test' not in str(result.data)
