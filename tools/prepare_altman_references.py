"""Package reviewed 10-K figures with the original unadjusted historical close."""
import gzip
import hashlib
import json
from datetime import date, datetime
from decimal import Decimal
from pathlib import Path
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[1]
# Amounts below are USD millions; shares are actual outstanding units,
# not weighted-average or diluted shares used for earnings per share.
REVIEWED = [
    dict(ticker='CAT', start='2025-01-01', end='2025-12-31', shares=465287332,
         current_assets=52485, current_liabilities=36558, assets=98585, retained=65448,
         ebit=11151, liabilities=77267, sales=67589,
         report='https://www.sec.gov/Archives/edgar/data/18230/000001823026000008/cat-20251231.htm',
         locator='10-K 2025: portada (acciones al 31/12/2025) y estados consolidados 1 y 3.',
         method='EBIT operativo: beneficio de explotación consolidado publicado. Ventas e ingresos consolidados, incluidos Productos Financieros; su coste financiero forma parte de los costes operativos. Uso académico, sin validación predictiva para el grupo.'),
    dict(ticker='KO', start='2025-01-01', end='2025-12-31', shares=(7040-2738)*1000000,
         current_assets=31044, current_liabilities=21281, assets=104816, retained=80382,
         ebit=15998+1654, liabilities=104816-34275, sales=47941,
         report='https://investors.coca-colacompany.com/filings-reports/annual-filings-10-k/content/0001628280-26-010047/ko-20251231.htm',
         locator='10-K 2025: estados consolidados de resultados y balance, columna 2025; importes y acciones en millones.',
         method='EBIT: resultado antes de impuestos más gasto por intereses. Pasivos: activos menos patrimonio total. Acciones al cierre estimadas: 7.040 millones emitidas menos 2.738 millones en tesorería, con la precisión redondeada del balance; no se utiliza la cifra de la portada de una fecha posterior.'),
    dict(ticker='AAPL', start='2025-09-28', end='2026-09-26', shares=14532259000,
         current_assets=173857, current_liabilities=169915, assets=404548, retained=13700,
         ebit=158063, liabilities=292455, sales=478834,
         report='https://www.sec.gov/Archives/edgar/data/320193/000032019326000026/aapl-20260926.htm',
         locator='10-K 2026: balances y operaciones, páginas 32 y 34; acciones en miles.',
         method='EBIT operativo: resultado de explotación publicado; no incluye otros ingresos netos.'),
    dict(ticker='AMZN', start='2025-01-01', end='2025-12-31', shares=10731000000,
         current_assets=229083, current_liabilities=218005, assets=818042, retained=250536,
         ebit=97311+2274, liabilities=818042-411065, sales=716924,
         report='https://s2.q4cdn.com/299287126/files/doc_financials/2026/ar/Amazon-2025-Annual-Report.pdf',
         locator='Informe 2025: operaciones y balance, páginas impresas 37 y 39.',
         method='EBIT: resultado antes de impuestos más gasto por intereses. Pasivos: activos menos patrimonio.'),
    dict(ticker='WMT', start='2025-02-01', end='2026-01-31', shares=7969000000,
         current_assets=84874, current_liabilities=107469, assets=284668, retained=104774,
         ebit=29469+2318+481, liabilities=107469+34624+13941+5905+16549, sales=706413,
         report='https://stock.walmart.com/sec-filings/all-sec-filings/content/0000104169-26-000055/0000104169-26-000055.pdf',
         locator='10-K 2026: operaciones, balance y cambios patrimoniales, páginas impresas 52, 54 y 55.',
         method='EBIT: resultado antes de impuestos más intereses de deuda y arrendamiento financiero. Ventas netas, sin cuotas ni otros ingresos. Pasivos: suma de sus partidas; excluye interés no controlador rescatable.'),
]

def prepare():
    prices_path = ROOT/'docs/datos/catalog_prices.json.gz'
    prices = {entry['ticker']:entry for entry in json.loads(gzip.decompress(prices_path.read_bytes()))['companies']}
    companies = []
    for row in REVIEWED:
        entry = prices[row['ticker']]
        chart = json.loads(entry['raw_response'])['chart']['result'][0]
        end = date.fromisoformat(row['end'])
        points = [(datetime.fromtimestamp(t,ZoneInfo('America/New_York')).date(),Decimal(str(p)))
                  for t,p in zip(chart['timestamp'],chart['indicators']['quote'][0]['close']) if p is not None]
        day,close = max((d,p) for d,p in points if d <= end)
        amounts = dict(working_capital=row['current_assets']-row['current_liabilities'],total_assets=row['assets'],
                       retained_earnings=row['retained'],ebit=row['ebit'],total_liabilities=row['liabilities'],sales=row['sales'])
        inputs = {k:str(v*1000000) for k,v in amounts.items()}
        inputs['equity_value'] = str(close*row['shares'])
        companies.append({'ticker':row['ticker'],'reference':dict(version=1,currency='USD',
            period_start=row['start'],period_end=row['end'],price_date=day.isoformat(),unadjusted_close=str(close),
            shares_outstanding=str(row['shares']),inputs=inputs,report_url=row['report'],locator=row['locator'],
            price_url='https://finance.yahoo.com/quote/'+row['ticker']+'/history/',price_sha256=entry['sha256'],
            reviewed_at='2026-11-24',method=row['method']+' Valor bursátil: acciones al cierre × último cierre sin ajustar de la sesión del cierre contable o inmediatamente anterior. Fórmula original de cinco factores, uso académico; no validada como predictor para esta empresa.')})
    payload = dict(version=1,price_snapshot_sha256=hashlib.sha256(prices_path.read_bytes()).hexdigest(),companies=companies)
    (ROOT/'docs/datos/altman_references.json').write_text(json.dumps(payload,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('Referencias Altman preparadas:',len(companies))

if __name__ == '__main__':
    prepare()
