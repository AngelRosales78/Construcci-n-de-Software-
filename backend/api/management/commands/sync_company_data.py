"""Descarga explícita de estados SEC y cierres ajustados, sin completar datos ausentes."""
import csv
import hashlib
import json
import math
import os
import time
from datetime import date, datetime, timezone
from pathlib import Path
from urllib.request import Request, urlopen
from django.core.management import call_command
from django.core.management.base import BaseCommand, CommandError
from django.conf import settings

TAGS = {
    'current_assets': ['AssetsCurrent'], 'current_liabilities': ['LiabilitiesCurrent'],
    'cash': ['CashAndCashEquivalentsAtCarryingValue'], 'short_term_investments': ['ShortTermInvestments', 'MarketableSecuritiesCurrent'],
    'receivables': ['AccountsReceivableNetCurrent'], 'net_income': ['NetIncomeLoss', 'ProfitLoss'],
    'total_assets': ['Assets'], 'book_equity': ['StockholdersEquity', 'StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest'],
    'revenue': ['RevenueFromContractWithCustomerExcludingAssessedTax', 'Revenues', 'SalesRevenueNet', 'RevenueFromContractWithCustomerIncludingAssessedTax'],
    'total_liabilities': ['Liabilities'], 'income_before_tax': ['IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest', 'IncomeLossFromContinuingOperationsBeforeIncomeTaxesMinorityInterestAndIncomeLossFromEquityMethodInvestments'],
    'interest_expense': ['InterestExpense', 'InterestAndDebtExpense', 'InterestExpenseNonOperating'],
    'inventory': ['InventoryNet'], 'cost_of_sales': ['CostOfRevenue', 'CostOfGoodsAndServicesSold', 'CostOfGoodsSold'],
}
INSTANT = {'current_assets', 'current_liabilities', 'cash', 'short_term_investments', 'receivables', 'total_assets', 'book_equity', 'total_liabilities', 'inventory'}

def annual_inventory(payload, ticker, cik, raw):
    facts = payload['facts'].get('us-gaap', {})
    anchors = []
    for tag in TAGS['revenue'] + TAGS['net_income']:
        for row in facts.get(tag, {}).get('units', {}).get('USD', []):
            if row.get('form') != '10-K' or row.get('fp') != 'FY' or not row.get('start'):
                continue
            start, end = date.fromisoformat(row['start']), date.fromisoformat(row['end'])
            if 2020 <= end.year <= date.today().year and row.get('fy') == end.year and 350 <= (end-start).days+1 <= 380 and end <= date.today() and date.fromisoformat(row['filed']) <= date.today():
                anchors.append(row)
    years = {}
    for row in sorted(anchors, key=lambda r: (r['filed'], r['accn'])):
        years.setdefault(date.fromisoformat(row['end']).year, row)
    records = []
    for year, anchor in sorted(years.items()):
        values = {}
        for component, tags in TAGS.items():
            chosen, selected = None, tags[0]
            for tag in tags:
                matches = [r for r in facts.get(tag, {}).get('units', {}).get('USD', [])
                           if r.get('accn') == anchor['accn'] and r.get('end') == anchor['end']
                           and (not r.get('start') if component in INSTANT else r.get('start') == anchor['start'])]
                distinct = {str(r['val']) for r in matches}
                if len(distinct) == 1:
                    chosen, selected = matches[0]['val'], tag
                    break
            if chosen is not None and component not in {'net_income', 'book_equity', 'income_before_tax'} and chosen < 0:
                chosen = None
            values[component] = {'value': chosen, 'tag': selected, 'available': chosen is not None}
        records.append({'fiscal_year': year, 'start': anchor['start'], 'end': anchor['end'], 'filed': anchor['filed'],
                        'accession': anchor['accn'], 'unit': 'USD', 'values': values})
    return {'ticker': ticker, 'entity': payload['entityName'], 'source': f'https://data.sec.gov/api/xbrl/companyfacts/CIK{cik:010d}.json',
            'snapshot_sha256': hashlib.sha256(raw).hexdigest(), 'selection': 'Mismo 10-K original por ejercicio; etiquetas ausentes o ambiguas permanecen nulas.',
            'annual_records': records}

class Command(BaseCommand):
    help = 'Importar datos reales para los tickers indicados; requiere acceso externo, conserva snapshots y procedencia.'
    def add_arguments(self, parser):
        parser.add_argument('--tickers', default='AAPL,NVDA,JPM,JNJ,GOOGL,AMZN,META,TSLA')
        parser.add_argument('--refresh', action='store_true')
    def handle(self, *args, **options):
        root = Path(__file__).resolve().parents[4]
        cache = root / 'backend' / 'data-cache'
        cache.mkdir(exist_ok=True)
        packaged = root / 'docs' / 'datos' / 'empresas'
        packaged.mkdir(exist_ok=True)
        with (root / 'docs/datos/catalogo_sp500.csv').open(encoding='utf-8-sig') as stream:
            catalog = {r['Symbol']: r for r in csv.DictReader(stream)}
        tickers = list(dict.fromkeys(t.strip().upper() for t in options['tickers'].split(',') if t.strip()))
        if not tickers or any(t not in catalog for t in tickers):
            raise CommandError('Indica símbolos existentes en el catálogo, separados por comas.')
        headers = {'User-Agent': os.environ.get('SEC_USER_AGENT', 'ConstruccionSoftwareAcademic/2.0 financial-data-import')}
        def download(url, target):
            if target.exists() and not options['refresh']:
                return target.read_bytes()
            with urlopen(Request(url, headers=headers), timeout=30) as response:
                raw = response.read(30_000_000)
            target.write_bytes(raw)
            time.sleep(.3)
            return raw
        failures = []
        for ticker in tickers:
            try:
                cik = int(catalog[ticker]['CIK'])
                raw = download(f'https://data.sec.gov/api/xbrl/companyfacts/CIK{cik:010d}.json', cache / f'{ticker}-companyfacts.json')
                inventory = annual_inventory(json.loads(raw), ticker, cik, raw)
                if not inventory['annual_records']:
                    raise ValueError('No hay ejercicios originales comparables entre 2020 y 2025.')
                inventory_path = packaged / f'{ticker}-annual.json'
                inventory_path.write_text(json.dumps(inventory, ensure_ascii=False, indent=2), encoding='utf-8')
                call_command('import_annual_financials', str(inventory_path), stdout=self.stdout)
                price_raw = download(f'https://query1.finance.yahoo.com/v8/finance/chart/{ticker.replace(".", "-")}?range=5y&interval=1d', cache / f'{ticker}-prices.json')
                result = json.loads(price_raw)['chart']['result'][0]
                if result['meta']['currency'] != 'USD':
                    raise ValueError('Los cierres deben estar expresados en USD.')
                adjusted = result['indicators']['adjclose'][0]['adjclose']
                volume = result['indicators']['quote'][0]['volume']
                rows = []
                for stamp, value, vol in zip(result['timestamp'], adjusted, volume):
                    if value is None or not math.isfinite(value) or value <= 0:
                        continue
                    when = datetime.fromtimestamp(stamp, timezone.utc).date()
                    if when > date.today(): continue
                    rows.append({'ticker': ticker, 'date': when.isoformat(), 'adjusted_close': f'{value:.6f}', 'volume': '' if vol is None else vol, 'currency': 'USD'})
                target = packaged / f'{ticker}-prices.csv'
                with target.open('w', encoding='utf-8', newline='') as stream:
                    writer = csv.DictWriter(stream, fieldnames=['ticker', 'date', 'adjusted_close', 'volume', 'currency'])
                    writer.writeheader(); writer.writerows(rows)
                provenance = {'ticker': ticker, 'source': f'https://finance.yahoo.com/quote/{ticker}/history/',
                              'raw_sha256': hashlib.sha256(price_raw).hexdigest(), 'rows': len(rows), 'downloaded_at': datetime.now(timezone.utc).isoformat()}
                (packaged / f'{ticker}-prices-source.json').write_text(json.dumps(provenance, indent=2), encoding='utf-8')
                call_command('import_market_data', prices=str(target), source=provenance['source'], stdout=self.stdout)
                self.stdout.write(self.style.SUCCESS(f'{ticker}: datos importados.'))
            except Exception as exc:
                failures.append(ticker)
                self.stderr.write(f'{ticker}: no se completó la carga ({type(exc).__name__}: {exc}). No se generaron valores sustitutos.')
        if failures:
            raise CommandError('Carga incompleta para: ' + ', '.join(failures))
