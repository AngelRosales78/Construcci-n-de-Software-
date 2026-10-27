"""Carga reanudable del catálogo completo; descargas limitadas y escritura secuencial."""
import concurrent.futures
import csv
import hashlib
import io
import json
import os
import threading
import time
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request,urlopen
from datetime import datetime,timezone
from django.core.management.base import BaseCommand,CommandError
from django.core.management import call_command
from api.company_snapshots import chart_prices,update_snapshot
from api.models import Company
from .sync_company_data import annual_inventory

class Command(BaseCommand):
    help='Cargar balances, precios y métricas del catálogo completo con un reporte de cobertura real.'
    def add_arguments(self,parser):
        parser.add_argument('--tickers',default='')
        parser.add_argument('--workers',type=int,default=3)
        parser.add_argument('--resume',action='store_true')
    def handle(self,*args,**options):
        root=Path(__file__).resolve().parents[4]
        cache=root/'backend/data-cache';cache.mkdir(exist_ok=True)
        target=root/'docs/datos/empresas';target.mkdir(exist_ok=True)
        with (root/'docs/datos/catalogo_sp500.csv').open(encoding='utf-8-sig') as stream:catalog={r['Symbol']:r for r in csv.DictReader(stream)}
        tickers=[t.strip().upper() for t in options['tickers'].split(',') if t.strip()] or list(catalog)
        if any(t not in catalog for t in tickers):raise CommandError('Hay símbolos que no están en el catálogo.')
        report_path=target/'coverage.json'
        report=json.loads(report_path.read_text(encoding='utf-8')) if report_path.exists() else {'companies':{}}
        if options['resume']:tickers=[t for t in tickers if report['companies'].get(t,{}).get('status')!='ready']
        lock=threading.Lock();last_sec=[0.0]
        headers={'User-Agent':os.environ.get('SEC_USER_AGENT','ConstruccionSoftwareAcademic/2.0 financial-data-import')}
        def download(url,path):
            if path.exists():return json.loads(path.read_bytes())
            if 'data.sec.gov' in url:
                with lock:
                    time.sleep(max(0,.35-(time.monotonic()-last_sec[0])));last_sec[0]=time.monotonic()
            for attempt in range(3):
                try:
                    with urlopen(Request(url,headers=headers),timeout=25) as response:raw=response.read(30_000_000)
                    parsed=json.loads(raw)
                    path.write_bytes(raw)
                    return parsed
                except HTTPError as exc:
                    if exc.code not in (429,500,502,503,504) or attempt==2:raise
                    time.sleep(min(30,2**(attempt+1)))
                except (TimeoutError,OSError):
                    if attempt==2:raise
                    time.sleep(2)
        benchmark_payload=download('https://query1.finance.yahoo.com/v8/finance/chart/%5EGSPC?range=5y&interval=1d',cache/'benchmark-sp500.json')
        _,benchmark=chart_prices(benchmark_payload)
        def prepare(ticker):
            cik=int(catalog[ticker]['CIK']);symbol=ticker.replace('.','-');errors=[]
            facts={};inventory=None;chart={};prices=[];valuations={}
            try:
                facts=download(f'https://data.sec.gov/api/xbrl/companyfacts/CIK{cik:010d}.json',cache/f'{ticker}-companyfacts.json')
                raw=(cache/f'{ticker}-companyfacts.json').read_bytes()
                inventory=annual_inventory(facts,ticker,cik,raw)
                if not inventory['annual_records']:errors.append('No hay ejercicios SEC comparables completos.');inventory=None
            except Exception as exc:errors.append('SEC: '+type(exc).__name__+' '+str(exc)[:160])
            try:
                price_payload=download(f'https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?range=5y&interval=1d&events=div%2Csplits',cache/f'{ticker}-prices-actions.json')
                chart,prices=chart_prices(price_payload)
                if not prices:errors.append('No hay cierres ajustados válidos.')
            except Exception as exc:errors.append('Precios: '+type(exc).__name__+' '+str(exc)[:160])
            try:
                types='trailingMarketCap,trailingPeRatio,annualMarketCap,annualPeRatio'
                valuations=download(f'https://query1.finance.yahoo.com/ws/fundamentals-timeseries/v1/finance/timeseries/{symbol}?type={types}&period1=1577836800&period2={int(time.time())}',cache/f'{ticker}-valuations.json')
            except Exception as exc:errors.append('Valoración: '+type(exc).__name__+' '+str(exc)[:160])
            return ticker,facts,inventory,chart,prices,valuations,errors
        complete=0
        with concurrent.futures.ThreadPoolExecutor(max_workers=max(1,min(options['workers'],3))) as pool:
            futures={pool.submit(prepare,t):t for t in tickers}
            for future in concurrent.futures.as_completed(futures):
                ticker=futures[future];errors=[]
                try:
                    ticker,facts,inventory,chart,prices,valuations,errors=future.result()
                    company=Company.objects.get(ticker=ticker)
                    if inventory:
                        inventory_path=target/f'{ticker}-annual.json';inventory_path.write_text(json.dumps(inventory,ensure_ascii=False,indent=2),encoding='utf-8')
                        try:call_command('import_annual_financials',str(inventory_path),stdout=io.StringIO())
                        except Exception as exc:errors.append('Balance: '+str(exc)[:200])
                    if prices:
                        price_path=target/f'{ticker}-prices.csv'
                        with price_path.open('w',newline='',encoding='utf-8') as stream:
                            writer=csv.DictWriter(stream,fieldnames=['ticker','date','adjusted_close','volume','currency']);writer.writeheader()
                            writer.writerows({'ticker':ticker,**{k:r[k] if r[k] is not None else '' for k in ['date','adjusted_close','volume','currency']}} for r in prices)
                        call_command('import_market_data',prices=str(price_path),source=f'https://finance.yahoo.com/quote/{ticker}/history/',stdout=io.StringIO())
                        price_raw=(cache/f'{ticker}-prices-actions.json').read_bytes()
                        provenance={'ticker':ticker,'source':f'https://finance.yahoo.com/quote/{ticker}/history/','raw_sha256':hashlib.sha256(price_raw).hexdigest(),'rows':len(prices),'downloaded_at':datetime.now(timezone.utc).isoformat()}
                        (target/f'{ticker}-prices-source.json').write_text(json.dumps(provenance,indent=2),encoding='utf-8')
                    sources=update_snapshot(company,facts,chart,prices,valuations,benchmark)
                    if sources:
                        (target/f'{ticker}-metrics.json').write_text(json.dumps({'ticker':ticker,'field_sources':sources,'source':f'https://finance.yahoo.com/quote/{ticker}/key-statistics/','valuation_snapshot_sha256':hashlib.sha256(json.dumps(valuations,sort_keys=True).encode()).hexdigest()},ensure_ascii=False,indent=2),encoding='utf-8')
                    count=company.prices.filter(is_demo=False).count();years=list(company.annual_statements.filter(is_demo=False).values_list('fiscal_year',flat=True))
                    report['companies'][ticker]={'status':'ready' if count>=10 and years else 'partial','prices':count,'annual_years':years,
                        'available_metrics':[k for k,v in (sources or {}).items() if v.get('status')!='unavailable'],'errors':errors}
                except Exception as exc:
                    report['companies'][ticker]={'status':'error','errors':errors+[type(exc).__name__+': '+str(exc)[:250]]}
                complete+=1
                report['checked_at']=datetime.now(timezone.utc).isoformat();report['catalog_count']=len(catalog)
                report_path.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
                if complete%10==0 or complete==len(tickers):
                    ready=sum(r.get('status')=='ready' for r in report['companies'].values())
                    self.stdout.write(f'Procesadas {complete}/{len(tickers)}; {ready} empresas con balance e histórico listos. Última: {ticker}',ending='\n');self.stdout.flush()
        ready=sum(r.get('status')=='ready' for r in report['companies'].values())
        self.stdout.write(self.style.SUCCESS(f'Proceso terminado. {ready}/{len(catalog)} empresas listas. Detalle en docs/datos/empresas/coverage.json'))
