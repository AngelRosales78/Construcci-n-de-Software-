import json
from contextlib import contextmanager
from types import SimpleNamespace
import pytest
from django.core.cache import cache
from django.test import override_settings
from .test_pillars_liquidity import case
from .models import AIUsage
from .assistant_chat import validate_narrative
from .assistant_connection import complete_json, NoRedirect

pytestmark=pytest.mark.django_db
URL='/api/v1/companies/MSFT/assistant/'


@pytest.fixture(autouse=True)
def clean_cache():
    with override_settings(ASSISTANT_API_BASE_URL='',ASSISTANT_MODEL='',ASSISTANT_API_KEY=''):
        cache.clear()
        yield
        cache.clear()


@pytest.mark.parametrize('question,topic',[('Hola','greeting'),('hoa','greeting'),('holaaa','greeting'),('👋','greeting'),('Hola, ¿me ayudas?','greeting'),('Gracias','thanks'),('hasta luego','goodbye')])
def test_small_talk_never_calls_model_or_invents_financial_card(case,monkeypatch,question,topic):
    monkeypatch.setattr('api.assistant_views.plan_answer',lambda *args:pytest.fail('Small talk must be immediate'))
    response=case[0].post(URL,{'action':'question','fiscal_year':2024,'question':question},format='json')
    assert response.status_code==200
    assert response.data['answer']['topics']==[topic]
    assert response.data['answer']['messages'] and response.data['answer']['cards']==[]
    assert response.data['generation_mode']=='conversational_quick_reply'
    assert not AIUsage.objects.exists()


def configure(settings):
    settings.ASSISTANT_API_BASE_URL='http://provider.test/api'
    settings.ASSISTANT_MODEL='Qwen/Qwen3.6-35B-A3B-FP8'
    settings.ASSISTANT_API_KEY='test-private-key'


def test_remote_answer_uses_evidence_and_exposes_no_connection_secrets(case,monkeypatch,settings):
    configure(settings)
    monkeypatch.setattr('api.assistant_views.plan_answer',lambda *args:({'topics':['liquidity'],'context':'current'},False))
    calls=[]
    def prose(messages,schema,max_tokens=650):
        calls.append(messages)
        return {'paragraphs':['Su liquidez corriente es 2,00 veces. Hay recursos corrientes para cubrir sus obligaciones a corto plazo.'],
                'evidence_ids':['current_ratio']}
    monkeypatch.setattr('api.assistant_chat.complete_json',prose)
    response=case[0].post(URL,{'action':'question','fiscal_year':2024,'question':'¿Qué significa su liquidez?'},format='json')
    assert response.status_code==200
    assert response.data['generation_mode']=='conversational_ai'
    assert response.data['assistant']['model']==settings.ASSISTANT_MODEL
    assert response.data['answer']['messages'][0].startswith('Su liquidez')
    assert response.data['answer']['evidence_ids']==['current_ratio']
    assert 'test-private-key' not in str(response.data) and 'provider.test' not in str(response.data)
    assert 'EVIDENCIA' in calls[0][1]['content']


@pytest.mark.parametrize('result',[
    {'paragraphs':['La liquidez es 999,99 veces.'],'evidence_ids':['current_ratio']},
    {'paragraphs':['Te recomiendo comprar ahora.'],'evidence_ids':['current_ratio']},
    {'paragraphs':['![imagen](https://example.com/a.png)'],'evidence_ids':['current_ratio']},
    {'paragraphs':['Todo está bien.'],'evidence_ids':['invented']},
    {'paragraphs':['<img src=x>'],'evidence_ids':['current_ratio']},
    {'paragraphs':['Compra estas acciones.'],'evidence_ids':['current_ratio']},
])
def test_unsupported_numbers_markup_advice_or_evidence_are_rejected(result):
    with pytest.raises(ValueError):
        validate_narrative(result,[{'id':'current_ratio','value':'2,00 veces','period':{'end':'2024-06-30'}}])


def test_chat_transport_records_actual_model_and_tokens(case,monkeypatch,settings):
    configure(settings)
    requests=[]
    @contextmanager
    def reply(request,**kwargs):
        requests.append(request)
        yield SimpleNamespace(read=lambda limit:json.dumps({'choices':[{'finish_reason':'stop','message':{'content':'{"ok":true}'}}],
            'usage':{'prompt_tokens':20,'completion_tokens':8}}).encode())
    monkeypatch.setattr('api.assistant_connection.urllib.request.build_opener',lambda *args:SimpleNamespace(open=reply))
    assert complete_json([{'role':'user','content':'A question'}],{'type':'object'})=={'ok':True}
    payload=json.loads(requests[0].data)
    assert payload['model']==settings.ASSISTANT_MODEL and payload['chat_template_kwargs']['enable_thinking'] is False
    usage=AIUsage.objects.get()
    assert usage.model==settings.ASSISTANT_MODEL and usage.input_tokens==20 and usage.output_tokens==8
    assert usage.estimated_cost is None
    assert NoRedirect().redirect_request(None,None,302,'',{},'https://other.test') is None


def test_remote_planner_cache_isolated_by_owner_model_and_evidence(case,monkeypatch,settings):
    configure(settings)
    from .assistant_provider import plan_answer
    calls=[]
    def plan(*args,**kwargs):
        calls.append(True)
        return {'topics':['liquidity'],'context':'current'}
    monkeypatch.setattr('api.assistant_provider.complete_json',plan)
    pack=case[0].get(URL,{'fiscal_year':2024}).data
    assert plan_answer(pack,'Liquidez',[],1)[1] is False
    assert plan_answer(pack,'Liquidez',[],1)[1] is True
    assert plan_answer(pack,'Liquidez',[],2)[1] is False
    settings.ASSISTANT_MODEL='another-model'
    assert plan_answer(pack,'Liquidez',[],1)[1] is False
    assert len(calls)==3


def test_deictic_followup_keeps_financial_topic_after_a_thank_you(case,monkeypatch,settings):
    configure(settings)
    monkeypatch.setattr('api.assistant_views.plan_answer',lambda *args:pytest.fail('Known follow-up must keep its topic'))
    monkeypatch.setattr('api.assistant_chat.complete_json',lambda *args,**kwargs:{'paragraphs':['La liquidez corriente es 2,00 veces.'], 'evidence_ids':['current_ratio']})
    response=case[0].post(URL,{'action':'question','question':'¿Y eso qué significa?','fiscal_year':2024,
        'history':[{'question':'¿Qué significa su liquidez?','topics':['liquidity']},{'question':'Gracias','topics':['thanks']}]},format='json')
    assert response.status_code==200 and response.data['answer']['topics']==['liquidity']
    assert response.data['generation_mode']=='conversational_ai'
