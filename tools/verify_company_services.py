"""Comprueba servicios locales con una cuenta efímera; no guarda credenciales."""
import json
import os
from pathlib import Path
import secrets
import sys
from urllib.request import Request, urlopen
from datetime import datetime, timezone

root=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(root/'backend'))
os.environ.setdefault('DJANGO_SETTINGS_MODULE','config.android_local')
import django
django.setup()
from django.contrib.auth import get_user_model
from api.models import Company

base='http://127.0.0.1:8000/api/v1/'
name='service_check_'+secrets.token_hex(6)
password=secrets.token_urlsafe(30)
user=get_user_model().objects.create_user(username=name,password=password)
def call(path, payload=None, token=None):
    headers={'Content-Type':'application/json'}
    if token:headers['Authorization']='Bearer '+token
    data=None if payload is None else json.dumps(payload).encode()
    with urlopen(Request(base+path,data=data,headers=headers),timeout=160) as response:
        return json.load(response)
try:
    token=call('auth/token/',{'username':name,'password':password})['access']
    evidence={'checked_at':datetime.now(timezone.utc).isoformat(),'companies':[]}
    evidence['ready_count']=call('companies/search/?ready=true',token=token)['count']
    evidence['catalog_count']=call('companies/search/',token=token)['count']
    fixtures=root/'android/app/src/test/resources'
    fixtures.mkdir(parents=True,exist_ok=True)
    for ticker in ['MSFT','AAPL','NVDA','JPM','JNJ','GOOGL','AMZN','META','TSLA']:
        pillars=call(f'companies/{ticker}/pillars/?fiscal_year=2024',token=token)
        assert pillars['ticker']==ticker and pillars['period']['end']
        defaults=call(f'companies/{ticker}/simulations/defaults/',token=token)
        parameters={k:defaults[k] for k in ['annual_return','annual_volatility','seed','paths']}|{'horizon_months':12}
        sim=call(f'companies/{ticker}/simulations/',parameters,token)
        assert len(sim['points'])==13 and sim['ticker']==ticker
        if ticker=='AAPL': (fixtures/'simulation-aapl-real.json').write_text(json.dumps(sim,ensure_ascii=False,indent=2),encoding='utf-8')
        company=Company.objects.get(ticker=ticker)
        evidence['companies'].append({'ticker':ticker,'annual_years':pillars['available_years'],
            'price_count':company.prices.count(),'scenario_start':sim['start_date'],'scenario_end':sim['end_date'],
            'available_metrics':sum(m['status']=='available' for p in pillars['pillars'] for m in p['metrics'])})
        print(ticker+': pilares e histórico/escenarios HTTP correctos',flush=True)
    print('Comprobando generación real con IA local para AAPL…',flush=True)
    report=call('companies/AAPL/analysis/',{'fiscal_year':2024,'level':'simple'},token)
    evidence['analysis']={k:report.get(k) for k in ['ticker','fiscal_year','level','generation_mode','provider','generation_notice']}
    assert report['ticker']=='AAPL' and len(report['pillars'])==5
    target=root/'docs/android/evidence/company_services.json'
    target.write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('Modo recibido:',report['generation_mode'],'·',report.get('provider'),flush=True)
    if report['generation_mode']=='ai': (fixtures/'analysis-aapl-real.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    if report['generation_mode']!='ai':
        raise RuntimeError(report['generation_notice'])
finally:
    user.delete()
