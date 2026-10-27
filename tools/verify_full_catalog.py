"""Verificar las 503 fichas mediante HTTP sin guardar credenciales ni modificar cuentas existentes."""
import json,os,secrets,sys
from pathlib import Path
from urllib.request import Request,urlopen
from datetime import datetime,timezone
root=Path(__file__).resolve().parents[1];sys.path.insert(0,str(root/'backend'));os.environ.setdefault('DJANGO_SETTINGS_MODULE','config.android_local')
import django;django.setup()
from django.contrib.auth import get_user_model
from api.models import Company
user=get_user_model().objects.create_user(username='catalog_check_'+secrets.token_hex(6),password=secrets.token_urlsafe(30))
from rest_framework_simplejwt.tokens import RefreshToken
token=str(RefreshToken.for_user(user).access_token)
base='http://127.0.0.1:8000/api/v1/'
def call(path,payload=None):
    with urlopen(Request(base+path,data=None if payload is None else json.dumps(payload).encode(),headers={'Authorization':'Bearer '+token,'Content-Type':'application/json'}),timeout=160) as response:return json.load(response)
try:
    companies=list(Company.objects.order_by('ticker').values_list('ticker',flat=True));verified=[]
    for ticker in companies:
        summary=call(f'companies/{ticker}/summary/')
        assert summary['ticker']==ticker and summary['is_demo'] is False
        assert summary['market_cap'] is not None and summary['per'] is not None
        for field in ['roe','beta','dividend_yield']:
            if summary[field] is None:assert summary['field_sources'][field].get('reason')
        defaults=call(f'companies/{ticker}/simulations/defaults/')
        assert defaults['ticker']==ticker and defaults['observations']>=10
        if defaults['observations']<60:assert defaults['history_warning']
        year=Company.objects.get(ticker=ticker).annual_statements.order_by('-fiscal_year').first().fiscal_year
        pillars=call(f'companies/{ticker}/pillars/?fiscal_year={year}')
        assert pillars['ticker']==ticker and any(m['status']=='available' for p in pillars['pillars'] for m in p['metrics'])
        verified.append({'ticker':ticker,'fiscal_year':year,'observations':defaults['observations'],'market_cap':summary['market_cap'],'per':summary['per'],
                         'reporting_note':summary.get('reporting_note'),'missing_metrics':[k for k in ['roe','beta','dividend_yield'] if summary[k] is None]})
        if len(verified)%50==0:print('Verificadas por HTTP',len(verified),'/',len(companies),flush=True)
    for ticker in ['AAPL','HONA','VYLR']:
        defaults=call(f'companies/{ticker}/simulations/defaults/')
        parameters={k:defaults[k] for k in ['annual_return','annual_volatility','seed','paths']}|{'horizon_months':12}
        result=call(f'companies/{ticker}/simulations/',parameters)
        assert len(result['points'])==13 and result['ticker']==ticker
    ready=call('companies/search/?ready=true')['count']
    evidence={'verified_at':datetime.now(timezone.utc).isoformat(),'http_verified_count':len(verified),'ready_count':ready,
              'simulation_posts':['AAPL','HONA','VYLR'],'companies':verified}
    (root/'docs/android/evidence/full_catalog.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('Todas las fichas y escenarios verificados. Total:',len(verified),flush=True)
finally:user.delete()
