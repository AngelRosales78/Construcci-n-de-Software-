"""Transformar snapshots públicos locales; no descargar ni generar cotizaciones.

Uso: python tools/prepare_real_data.py <directorio_snapshots> <directorio_salida>
El balance de 2024 se eligió porque contiene el gasto financiero anual completo.
"""
import csv
import hashlib
import json
import sys
from datetime import date, datetime, timezone, timedelta
from decimal import Decimal
from pathlib import Path
from statistics import covariance, variance


def prepare(source, output):
    output.mkdir(parents=True, exist_ok=True)
    facts = json.loads((source / 'MSFT_sec.json').read_text())['facts']['us-gaap']
    end = date(2024, 6, 30)
    start = '2023-07-01'
    used = {}

    def fact(tag, unit='USD', instant=False, when=end):
        rows = [r for r in facts[tag]['units'][unit]
                if r['end'] == when.isoformat() and r.get('form') == '10-K'
                and (instant or r.get('start') == start)]
        if not rows:
            raise ValueError(f'Falta el dato anual {tag}')
        row = max(rows, key=lambda r: r['filed'])
        used[f'{tag}@{when}'] = row
        return Decimal(str(row['val']))

    charts = {}
    for symbol in ('MSFT', 'SPY'):
        result = json.loads((source / f'{symbol}_precios.json').read_text())['chart']['result'][0]
        quote = result['indicators']['quote'][0]
        adjusted = result['indicators']['adjclose'][0]['adjclose']
        rows = []
        for i, timestamp in enumerate(result['timestamp']):
            when = datetime.fromtimestamp(timestamp, timezone.utc).date()
            if when > date.today() or quote['close'][i] is None or adjusted[i] is None:
                continue
            rows.append({'date': when, 'close': Decimal(str(quote['close'][i])),
                         'adjusted_close': Decimal(str(adjusted[i])), 'volume': quote['volume'][i]})
        charts[symbol] = (result, rows)
    stock, rows = charts['MSFT']
    close = max((r for r in rows if r['date'] <= end), key=lambda r: r['date'])['close']
    cap = close * fact('CommonStockSharesOutstanding', 'shares', instant=True)
    assets = fact('Assets', instant=True)
    equity = fact('StockholdersEquity', instant=True)
    previous = fact('StockholdersEquity', instant=True, when=date(2023, 6, 30))
    distributions = sum(Decimal(str(r['amount'])) for r in stock.get('events', {}).get('dividends', {}).values()
                        if end - timedelta(days=365) < datetime.fromtimestamp(r['date'], timezone.utc).date() <= end)
    returns = {}
    for symbol, (_, series) in charts.items():
        returns[symbol] = {b['date']: float(b['adjusted_close'] / a['adjusted_close'] - 1)
                           for a, b in zip(series, series[1:])
                           if end - timedelta(days=730) < b['date'] <= end}
    dates = sorted(returns['MSFT'].keys() & returns['SPY'].keys())
    if len(dates) < 100:
        raise ValueError('Insuficientes retornos para calcular beta.')
    market = [returns['SPY'][d] for d in dates]
    beta = covariance([returns['MSFT'][d] for d in dates], market) / variance(market)
    values = {
        'ticker': 'MSFT', 'as_of': end.isoformat(), 'market_cap': cap,
        'per': close / fact('EarningsPerShareDiluted', 'USD/shares'),
        'roe': 100 * fact('NetIncomeLoss') / ((equity + previous) / 2),
        'beta': Decimal(str(beta)), 'dividend_yield': 100 * distributions / close,
        'working_capital': fact('AssetsCurrent', instant=True) - fact('LiabilitiesCurrent', instant=True),
        'total_assets': assets, 'retained_earnings': fact('RetainedEarningsAccumulatedDeficit', instant=True),
        'ebit': fact('IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest') + fact('InterestExpense'),
        'total_liabilities': fact('Liabilities', instant=True),
        'sales': fact('RevenueFromContractWithCustomerExcludingAssessedTax'), 'equity_value': cap,
    }
    for key, value in values.items():
        if isinstance(value, Decimal):
            values[key] = str(value.quantize(Decimal('.0001') if key in ('beta', 'dividend_yield') else Decimal('.01')))
    with (output / 'MSFT_balance_2024.csv').open('w', newline='', encoding='utf-8') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(values))
        writer.writeheader()
        writer.writerow(values)
    with (output / 'MSFT_cierres_5y.csv').open('w', newline='', encoding='utf-8') as stream:
        writer = csv.DictWriter(stream, fieldnames=['ticker', 'date', 'adjusted_close', 'volume', 'currency'])
        writer.writeheader()
        for row in rows:
            writer.writerow({'ticker': 'MSFT', 'date': row['date'], 'adjusted_close': str(row['adjusted_close'].quantize(Decimal('.0001'))),
                             'volume': row['volume'], 'currency': 'USD'})
    provenance = {
        'balance_as_of': str(end), 'price_dates': [str(rows[0]['date']), str(rows[-1]['date'])],
        'price_count': len(rows), 'beta_observations': len(dates), 'sec_facts_used': used,
        'sources': {'facts': 'https://data.sec.gov/api/xbrl/companyfacts/CIK0000789019.json',
                    'prices': 'https://query1.finance.yahoo.com/v8/finance/chart/MSFT?range=5y&interval=1d&events=div%2Csplits',
                    'benchmark': 'https://query1.finance.yahoo.com/v8/finance/chart/SPY?range=5y&interval=1d&events=div%2Csplits'},
        'raw_sha256': {name: hashlib.sha256((source / name).read_bytes()).hexdigest()
                       for name in ('MSFT_sec.json', 'MSFT_precios.json', 'SPY_precios.json')},
        'methodology': 'Balance anual FY2024. EBIT = beneficio antes de impuestos + InterestExpense. Capitalización = acciones al cierre fiscal * cierre no ajustado anterior al 30/06. PER = cierre/EPS diluido. ROE = beneficio/equidad media de 2023 y 2024. Beta = covarianza de retornos diarios ajustados MSFT/SPY dividida por varianza SPY en los dos años previos al balance. Dividend Yield = dividendos de los 365 días previos/cierre. No son indicadores actuales ni una cobertura financiera de todo el índice.',
    }
    (output / 'MSFT_procedencia.json').write_text(json.dumps(provenance, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f'Balance anual 2024 y {len(rows)} cierres reales preparados; beta sobre {len(dates)} retornos.')


if __name__ == '__main__':
    prepare(Path(sys.argv[1]), Path(sys.argv[2]))
