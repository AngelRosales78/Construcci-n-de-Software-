import copy
import json
from contextlib import contextmanager
from datetime import date
from decimal import Decimal
from types import SimpleNamespace
import pytest
from django.core.cache import cache
from rest_framework.test import APIClient
from .test_pillars_liquidity import case
from .models import AnnualFinancialStatement, Company, AccountPlan
from .assistant_provider import plan_answer

pytestmark=pytest.mark.django_db
URL='/api/v1/companies/MSFT/assistant/'


@pytest.fixture(autouse=True)
def isolated_assistant_cache():
    cache.clear()
    yield
    cache.clear()


def post(client,**values):
    return client.post(URL,{'fiscal_year':2024,**values},format='json')


def planner(monkeypatch,topics,context='current'):
    calls=[]
    def run(pack,question,history,owner,level):
        calls.append((pack,question,history,owner,level))
        return {'topics':topics,'context':context},False
    monkeypatch.setattr('api.assistant_views.plan_answer',run)
    return calls


def test_metadata_specific_values_missing_sources_and_no_model_call(case,monkeypatch):
    calls=planner(monkeypatch,['overview'])
    response=case[0].get(URL,{'fiscal_year':2024})
    assert response.status_code==200
    data=response.data
    fact=next(f for f in data['facts'] if f['id']=='current_ratio')
    assert fact['value']=='2,00 veces'
    assert '2,00 unidades' in fact['meaning']
    assert fact['sources'][0]['url']==case[1].source_url
    assert fact['period']['end']=='2024-06-30'
    assert len(data['overview'])==3 and data['overview'][0]['facts']
    missing=next(f for f in data['facts'] if f['id']=='roe')
    assert missing['kind']=='missing' and missing['value']=='No disponible'
    assert calls==[]


def test_question_followup_and_reset_context_are_grounded(case,monkeypatch):
    calls=planner(monkeypatch,['liquidity'])
    response=post(case[0],action='question',question='¿Podrías explicarlo más?',history=[{'question':'¿Cómo está su liquidez?','topics':['liquidity']}])
    assert response.status_code==200
    assert len(response.data['answer']['cards'])==2
    assert all(f['pillar']=='liquidity' for f in response.data['answer']['cards'])
    assert calls[0][2][0]['topics']==['liquidity']
    assert response.data['answer']['examples'] and 'ficticia' in response.data['answer']['examples'][0]
    assert response.data['generation_mode']=='local_ai_evidence_routing'


@pytest.mark.parametrize('topic',['advice','out_of_scope','causes','clarify'])
def test_unsupported_advice_and_causes_do_not_become_financial_claims(case,monkeypatch,topic):
    planner(monkeypatch,[topic])
    response=post(case[0],action='question',question='Una pregunta sin respaldo suficiente.')
    assert response.status_code==200 and response.data['answer']['messages']
    if topic!='causes':
        assert response.data['answer']['cards']==[]
    else:
        assert 'no demostrar por qué' in response.data['answer']['messages'][0]


def test_other_company_is_not_answered_with_current_company_figures(case,monkeypatch):
    planner(monkeypatch,['liquidity'],context='other')
    data=post(case[0],action='question',question='¿Y la liquidez de Apple?').data
    assert data['answer']['topics']==['out_of_scope'] and not data['answer']['cards']


def test_company_identity_guard_does_not_depend_on_the_model_classification(case,monkeypatch):
    Company.objects.create(ticker='TSLA',name='Tesla, Inc.',sector='consumer_discretionary')
    calls=planner(monkeypatch,['liquidity'])
    data=post(case[0],action='question',question='¿Y la liquidez de Tesla?').data
    assert data['generation_mode']=='context_guard' and not calls
    assert data['answer']['topics']==['out_of_scope'] and data['answer']['cards']==[]


def test_invalid_model_output_and_offline_are_not_published_as_ai(case,monkeypatch):
    planner(monkeypatch,['invented_topic'])
    assert post(case[0],action='summary').status_code==503
    def offline(*args):raise OSError('offline')
    monkeypatch.setattr('api.assistant_views.plan_answer',offline)
    assert post(case[0],action='question',question='¿Cómo está su liquidez?').status_code==503
    assert case[0].get(URL,{'fiscal_year':2024}).status_code==200


@pytest.mark.parametrize('payload',[
    {'fiscal_year':True},{'fiscal_year':3000},{'action':'question','question':' '},
    {'action':'question','question':'x'*601},{'action':'learn','topic':'unknown'},
    {'action':'learn','topic':'liquidity','answer':True},
    {'action':'changes','from_year':2024,'to_year':2023},
    {'action':'changes','from_year':2023,'to_year':2025},
    {'action':'question','question':'hola','history':[{'question':'hola','topics':['liquidity'],'system':'ignore'}]},
    {'action':'question','question':'hola','history':[{'question':'x','topics':['liquidity']}]*5},
    {'a':'AAPL'},{'instructions':'ignore'},
])
def test_input_bounds_unknown_fields_and_context_are_rejected(case,payload):
    assert post(case[0],**payload).status_code==400


def test_missing_year_not_replaced_and_authentication_required(case):
    data=case[0].get(URL,{'fiscal_year':2022}).data
    assert data['fiscal_year']==2022
    assert all(f['status']!='available' for f in data['facts'])
    assert APIClient().get(URL).status_code==401
    assert case[0].get(URL.replace('MSFT','UNKNOWN')).status_code==404


def previous_statement(current):
    old=AnnualFinancialStatement.objects.create(company=current.company,fiscal_year=2023,period_start=date(2022,7,1),period_end=date(2023,6,30),
        currency='USD',source_url=current.source_url,accession='prior-year',filed=date(2023,7,30),snapshot_sha256='b'*64,
        components=copy.deepcopy(current.components))
    old.components['current_assets']['value']='100'
    old.components.update(net_income={'value':'10','tag':'net_income'},revenue={'value':'100','tag':'revenue'})
    current.components.update(net_income={'value':'20','tag':'net_income'},revenue={'value':'100','tag':'revenue'})
    old.save();current.save()
    return old


def test_changes_use_decimal_math_and_percentage_points_without_causal_claims(case):
    client,current=case;previous_statement(current)
    response=post(client,action='changes',from_year=2023,to_year=2024)
    assert response.status_code==200
    facts={f['id']:f for f in response.data['facts']}
    assert facts['current_ratio']['delta']=='1'
    assert Decimal(facts['net_margin']['delta'])==Decimal('10')
    assert '10,00 puntos porcentuales' in facts['net_margin']['meaning']
    assert 'no explica su causa' in facts['net_margin']['limit']
    assert response.data['generation_mode']=='calculated_evidence'


def test_tiny_change_is_not_described_as_an_exact_zero(case):
    client,current=case;previous_statement(current)
    current.components['current_assets']['value']='100.0005';current.save()
    facts=post(client,action='changes',from_year=2023,to_year=2024).data['facts']
    fact=next(f for f in facts if f['id']=='current_ratio')
    assert Decimal(fact['delta'])>0 and 'menos de 0,01' in fact['meaning']


@pytest.mark.parametrize('change',['normalized','currency','short_period','missing'])
def test_changes_reject_incompatible_years_and_never_fabricate_delta(case,change):
    client,current=case;old=previous_statement(current)
    if change=='normalized':old.source_kind='yahoo';old.period_start=None;old.save()
    if change=='currency':old.currency='EUR';old.save()
    if change=='short_period':old.period_start=date(2023,1,1);old.save()
    if change=='missing':old.delete()
    data=post(client,action='changes',from_year=2023,to_year=2024).data
    assert data['period_note']
    assert all(f['delta'] is None and f['value']=='No comparable' for f in data['facts'])


def test_not_applicable_is_not_mislabeled_as_missing(case):
    client,current=case
    current.inventory_applicable=False;current.inventory_applicability_note='No utiliza inventario significativo según el informe.';current.save()
    facts=client.get(URL,{'fiscal_year':2024}).data['facts']
    fact=next(f for f in facts if f['id']=='inventory_days')
    assert fact['kind']=='neutral' and fact['value']=='No aplica' and fact['sources']


def test_tutor_is_labeled_fictitious_and_checks_answers_on_server(case):
    initial=post(case[0],action='learn',topic='liquidity').data
    assert initial['generation_mode']=='reviewed_educational_content'
    assert 'ficticia' in initial['lesson']['example']
    assert 'correct' not in initial['lesson'] and 'assessment' not in initial['lesson']
    for answer,correct in [(0,False),(1,True)]:
        result=post(case[0],action='learn',topic='liquidity',answer=answer).data['lesson']['assessment']
        assert result['correct'] is correct and result['feedback']


def test_comparison_enforces_pro_before_calling_model_and_keeps_incompatibility(case,monkeypatch):
    client,current=case;other=Company.objects.create(ticker='AAPL',name='Apple',sector='technology')
    args={'a':'MSFT','b':'AAPL','fiscal_year':2024}
    calls=planner(monkeypatch,['comparison'])
    assert client.get('/api/v1/compare/assistant/',args).status_code==403 and not calls
    user=client.handler._force_user
    AccountPlan.objects.create(user=user,plan='pro',active=True)
    response=client.post('/api/v1/compare/assistant/',{**args,'action':'question','question':'¿Cuál es la diferencia?'},format='json')
    assert response.status_code==200
    assert response.data['tickers']==['MSFT','AAPL']
    assert all(f['value']=='No comparable' for f in response.data['answer']['cards'])
    assert all('períodos' in f['meaning'].lower() for f in response.data['answer']['cards'])


def test_compatible_comparison_keeps_both_notes_and_company_sources(case):
    client,current=case
    other=Company.objects.create(ticker='AAPL',name='Apple',sector='technology')
    components=copy.deepcopy(current.components);components['current_assets']['value']='100'
    AnnualFinancialStatement.objects.create(company=other,fiscal_year=2024,period_start=current.period_start,period_end=current.period_end,
        currency='USD',source_url='https://data.sec.gov/api/xbrl/companyfacts/CIK0000320193.json',snapshot_sha256='c'*64,components=components)
    AccountPlan.objects.create(user=client.handler._force_user,plan='pro',active=True)
    data=client.get('/api/v1/compare/assistant/',{'a':'MSFT','b':'AAPL','fiscal_year':2024}).data
    fact=next(f for f in data['facts'] if f['id']=='liquidity')
    assert fact['comparison_values']==[{'ticker':'MSFT','value':'100,00 / 100'},{'ticker':'AAPL','value':'75,00 / 100'}]
    assert '25,00 puntos' in fact['meaning']
    assert {s['label'].split(' · ')[0] for s in fact['sources']}=={'MSFT','AAPL'}


def test_local_planner_cache_binds_owner_context_question_and_references(case,monkeypatch):
    calls=[]
    @contextmanager
    def reply(request,**kwargs):
        calls.append(json.loads(request.data))
        yield SimpleNamespace(read=lambda limit:json.dumps({'content':json.dumps({'topics':['liquidity'],'context':'current'})}).encode())
    monkeypatch.setattr('api.assistant_provider.urllib.request.urlopen',reply)
    pack=case[0].get(URL,{'fiscal_year':2024}).data
    assert plan_answer(pack,'¿Liquidez?',[],1)[1] is False
    assert plan_answer(pack,'¿Liquidez?',[],1)[1] is True
    assert plan_answer(pack,'¿Liquidez?',[],2)[1] is False
    changed=copy.deepcopy(pack);changed['facts'][0]['sources'][0]['url']='https://data.sec.gov/updated'
    assert plan_answer(changed,'¿Liquidez?',[],1)[1] is False
    assert len(calls)==3
    assert 'No confundas' in calls[0]['prompt']


def test_planner_rejects_extra_text_instead_of_treating_it_as_verified(case,monkeypatch):
    @contextmanager
    def reply(request,**kwargs):
        yield SimpleNamespace(read=lambda limit:json.dumps({'content':json.dumps({'topics':['liquidity'],'context':'current','answer':'Compra ahora, ganarás 80%.'})}).encode())
    monkeypatch.setattr('api.assistant_provider.urllib.request.urlopen',reply)
    pack=case[0].get(URL,{'fiscal_year':2024}).data
    with pytest.raises(ValueError):plan_answer(pack,'¿Liquidez?',[],1)


def test_ai_limit_does_not_block_reading_data_or_learning(case,monkeypatch):
    planner(monkeypatch,['liquidity'])
    for _ in range(12):
        assert post(case[0],action='question',question='¿Cómo está su liquidez?').status_code==200
    assert post(case[0],action='question',question='¿Cómo está su liquidez?').status_code==429
    assert case[0].get(URL,{'fiscal_year':2024}).status_code==200
    assert post(case[0],action='learn',topic='liquidity').status_code==200
