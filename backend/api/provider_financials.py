"""Annual provider references and calculated summaries with independent provenance.

An asOfDate is a provider reference, not a verified filing or fiscal-period start.
Missing amounts, unsupported currencies and ambiguous duplicate values stay absent.
"""
import hashlib
import json
from datetime import date, datetime, timedelta
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
from statistics import covariance, variance
from urllib.parse import urlsplit
from zoneinfo import ZoneInfo

FIELD_MAP = {
    'current_assets': ('annualCurrentAssets',), 'current_liabilities': ('annualCurrentLiabilities',),
    'cash': ('annualCashAndCashEquivalents',), 'short_term_investments': ('annualOtherShortTermInvestments',),
    'receivables': ('annualAccountsReceivable', 'annualReceivables'), 'net_income': ('annualNetIncome',),
    'book_equity': ('annualStockholdersEquity',), 'total_assets': ('annualTotalAssets',),
    'revenue': ('annualTotalRevenue',), 'total_liabilities': ('annualTotalLiabilitiesNetMinorityInterest',),
    'income_before_tax': ('annualPretaxIncome',), 'interest_expense': ('annualInterestExpense',),
    'inventory': ('annualInventory',), 'cost_of_sales': ('annualCostOfRevenue',),
}
EXTRA_FIELDS = {'annualRetainedEarnings', 'annualEBIT', 'annualCashCashEquivalentsAndShortTermInvestments',
                'trailingMarketCap', 'trailingPeRatio', 'annualOrdinarySharesNumber'}
FIELDS = {value for choices in FIELD_MAP.values() for value in choices} | EXTRA_FIELDS
SIGNED = {'annualNetIncome', 'annualStockholdersEquity', 'annualPretaxIncome', 'annualRetainedEarnings', 'annualEBIT', 'trailingPeRatio'}


def canonical(value):
    return value.upper().replace('.', '-')


def number(value):
    if isinstance(value, bool) or not isinstance(value, (int, float, str)):
        raise ValueError('Valor financiero no numérico.')
    try:
        result = Decimal(str(value))
        if not result.is_finite() or abs(result) >= Decimal('1e25'):
            raise ValueError('Valor financiero no finito o fuera de rango.')
        return result
    except InvalidOperation as error:
        raise ValueError('Valor financiero inválido.') from error


def parse_capture(ticker, raw, evidence, today):
    if hashlib.sha256(raw).hexdigest() != evidence['sha256']:
        raise ValueError('La huella de los datos financieros no coincide.')
    url = urlsplit(evidence['url'])
    if (url.scheme != 'https' or url.netloc != 'query1.finance.yahoo.com'
            or url.path != '/ws/fundamentals-timeseries/v1/finance/timeseries/' + canonical(ticker)):
        raise ValueError('La fuente no corresponde a la empresa.')
    captured = datetime.fromisoformat(evidence['retrieved_at'])
    if captured.tzinfo is None: raise ValueError('La captura requiere zona horaria.')
    cutoff = min(today, captured.astimezone(ZoneInfo('America/New_York')).date())
    series = json.loads(raw)['timeseries']
    if series.get('error') or not isinstance(series.get('result'), list):
        raise ValueError('Respuesta financiera no válida.')
    values, ambiguous = {}, set()
    for result in series['result']:
        meta = result['meta']
        if not isinstance(meta.get('symbol'), list) or len(meta['symbol']) != 1 or canonical(meta['symbol'][0]) != canonical(ticker):
            raise ValueError('La identidad del proveedor no coincide.')
        if not isinstance(meta.get('type'), list) or len(meta['type']) != 1: raise ValueError('Serie ambigua.')
        field = meta['type'][0]
        if field not in FIELDS: continue
        expected_period = '12M' if field.startswith('annual') else 'TTM'
        for item in result.get(field, []):
            if item.get('periodType') != expected_period: continue
            when = date.fromisoformat(item['asOfDate'])
            if when > cutoff: continue
            if field != 'trailingPeRatio' and item.get('currencyCode') != 'USD': continue
            try:
                value = number(item['reportedValue']['raw'])
                if value < 0 and field not in SIGNED: continue
            except (KeyError, TypeError, ValueError): continue
            key = (field, when)
            if key in values and values[key] != value: ambiguous.add(key)
            values[key] = value
    for key in ambiguous: values.pop(key, None)
    records = []
    dates = sorted(when for field, when in values if field == 'annualTotalAssets' and values[field, when] > 0)
    for when in dates:
        # A fiscal-year change can create two dates in the same calendar year.
        # Do not silently choose one or combine their flows.
        if sum(other.year == when.year for other in dates) != 1: continue
        components = {}
        for field, choices in FIELD_MAP.items():
            selected = next((choice for choice in choices if (choice, when) in values), choices[0])
            value = values.get((selected, when))
            components[field] = {'value': str(value) if value is not None else None, 'tag': selected}
        if components['short_term_investments']['value'] is None:
            combined, cash = values.get(('annualCashCashEquivalentsAndShortTermInvestments', when)), values.get(('annualCashAndCashEquivalents', when))
            if combined is not None and cash is not None and combined >= cash:
                components['short_term_investments'] = {'value': str(combined-cash),
                    'tag': 'annualCashCashEquivalentsAndShortTermInvestments - annualCashAndCashEquivalents'}
        records.append({'fiscal_year': when.year, 'period_start': None, 'period_end': when,
            'currency': 'USD', 'source_url': f'https://finance.yahoo.com/quote/{canonical(ticker)}/financials/',
            'source_kind': 'yahoo', 'accession': '', 'filed': None, 'snapshot_sha256': evidence['sha256'],
            'is_demo': False, 'components': components,
            'provenance': {'provider': 'Yahoo Finance', 'period_type': '12M', 'period_basis': 'provider_normalized',
                'as_of': when.isoformat(), 'retrieved_at': evidence['retrieved_at'], 'request_url': evidence['url']}})
    return records, values


def chart_capture(ticker, raw, today, benchmark=False, price_as_of=None):
    result = json.loads(raw)['chart']['result'][0]
    meta = result['meta']
    expected = '^GSPC' if benchmark else canonical(ticker)
    if canonical(meta['symbol']) != expected or meta['currency'] != 'USD' or meta['exchangeTimezoneName'] != 'America/New_York':
        raise ValueError('Identidad, moneda o zona del histórico inválidas.')
    timestamps = result['timestamp']
    adjusted = result['indicators']['adjclose'][0]['adjclose']
    closes = result['indicators']['quote'][0]['close']
    if len(timestamps) != len(adjusted) or len(timestamps) != len(closes): raise ValueError('Series incompatibles.')
    rows = {}
    for timestamp, price, close in zip(timestamps, adjusted, closes):
        if isinstance(timestamp, bool) or not isinstance(timestamp, int): raise ValueError('Fecha inválida.')
        when = datetime.fromtimestamp(timestamp, ZoneInfo('America/New_York')).date()
        if when >= today or (price_as_of and when > price_as_of) or price is None or close is None: continue
        adjusted_value, close_value = number(price), number(close)
        if adjusted_value <= 0 or close_value <= 0: raise ValueError('Precio no positivo.')
        if when in rows: raise ValueError('Fecha repetida.')
        rows[when] = {'adjusted': adjusted_value, 'close': close_value}
    dividends = []
    for event in result.get('events', {}).get('dividends', {}).values():
        when = datetime.fromtimestamp(event['date'], ZoneInfo('America/New_York')).date()
        amount = number(event['amount'])
        if amount < 0: raise ValueError('Dividendo negativo.')
        dividends.append((when, amount))
    if rows:
        last = max(rows)
        # Per-share payments around a split need a documented common basis.
        # This capture does not certify that adjustment, so withhold the yield.
        if any(last-timedelta(days=365) < datetime.fromtimestamp(event['date'], ZoneInfo('America/New_York')).date() <= last
               for event in result.get('events', {}).get('splits', {}).values()):
            dividends = []
    return rows, dividends


def summary_values(ticker, records, values, chart, dividends, benchmark, evidence, chart_evidence, benchmark_evidence):
    decimals = ['market_cap','per','roe','beta','dividend_yield','working_capital','total_assets','retained_earnings',
                'ebit','total_liabilities','sales','equity_value']
    summary = dict.fromkeys(decimals)
    sources = {}
    latest = max(records, key=lambda record: record['period_end']) if records else None
    previous = next((record for record in records if latest and record['fiscal_year'] == latest['fiscal_year']-1
                     and record['period_end'].month == latest['period_end'].month), None)
    reference_dates = []
    for field, source_field in [('market_cap', 'trailingMarketCap'), ('per', 'trailingPeRatio')]:
        dates = sorted(when for name, when in values if name == source_field)
        if dates:
            when = dates[-1]; value = values[source_field, when]
            if field == 'market_cap' and value <= 0: continue
            summary[field] = value; reference_dates.append(when)
            sources[field] = {'as_of': when.isoformat(), 'source': f'https://finance.yahoo.com/quote/{canonical(ticker)}/key-statistics/',
                'method': 'Valor publicado por Yahoo Finance en la serie ' + source_field + '; no se calcula con precios ajustados.',
                'snapshot_sha256': evidence['sha256']}
    if latest:
        when = latest['period_end']
        current = {name: number(component['value']) if component['value'] is not None else None for name, component in latest['components'].items()}
        if previous and current['net_income'] is not None:
            old_equity = previous['components']['book_equity']['value']
            equity = current['book_equity']
            if old_equity is not None and equity is not None and equity > 0 and number(old_equity) > 0:
                summary['roe'] = current['net_income'] / ((equity + number(old_equity)) / 2) * 100
                sources['roe'] = {'as_of': when.isoformat(), 'source': latest['source_url'],
                    'method': 'Beneficio neto / patrimonio medio de dos referencias anuales consecutivas del proveedor × 100. Las fechas de referencia están normalizadas; no acreditan el cierre contable exacto.',
                    'snapshot_sha256': evidence['sha256'], 'fiscal_year': when.year}
        for field, component in [('total_assets','total_assets'), ('total_liabilities','total_liabilities'), ('sales','revenue')]:
            summary[field] = current[component]
        if current['current_assets'] is not None and current['current_liabilities'] is not None:
            summary['working_capital'] = current['current_assets'] - current['current_liabilities']
        summary['retained_earnings'] = values.get(('annualRetainedEarnings', when))
        summary['ebit'] = values.get(('annualEBIT', when))
        if summary['ebit'] is None and current['income_before_tax'] is not None and current['interest_expense'] is not None:
            summary['ebit'] = current['income_before_tax'] + current['interest_expense']
        # Current market cap is not a verified market value at the actual fiscal
        # close. Keep Altman-Z unavailable instead of mixing those dates.
        summary['equity_value'] = None
    common = sorted(set(chart) & set(benchmark))
    if len(common) >= 253:
        days = common[-253:]
        stock_returns = [float(chart[b]['adjusted']/chart[a]['adjusted']-1) for a,b in zip(days,days[1:])]
        market_returns = [float(benchmark[b]['adjusted']/benchmark[a]['adjusted']-1) for a,b in zip(days,days[1:])]
        market_variance = variance(market_returns)
        if market_variance > 0:
            summary['beta'] = number(covariance(stock_returns, market_returns)/market_variance)
            sources['beta'] = {'as_of': days[-1].isoformat(), 'source': benchmark_evidence['url'],
                'method': 'Covarianza de 252 retornos diarios ajustados comunes / varianza de los retornos del S&P 500. Sin interpolación.',
                'snapshot_sha256': benchmark_evidence['sha256'], 'company_snapshot_sha256': chart_evidence['sha256']}
    if chart:
        when = max(chart)
        if (when-min(chart)).days >= 365:
            events = [amount for day,amount in dividends if when-timedelta(days=365) < day <= when]
            if events:
                summary['dividend_yield'] = sum(events) / chart[when]['close'] * 100
                sources['dividend_yield'] = {'as_of': when.isoformat(), 'source': f'https://finance.yahoo.com/quote/{canonical(ticker)}/history/',
                    'method': 'Dividendos en efectivo registrados en 365 días / cierre sin ajustar de la misma referencia × 100. La ausencia de eventos no se convierte en cero.',
                    'snapshot_sha256': chart_evidence['sha256']}
    if not latest and not reference_dates: return None
    summary.update(as_of=latest['period_end'] if latest else max(reference_dates), source=f'https://finance.yahoo.com/quote/{canonical(ticker)}/financials/',
                   is_demo=False, reference_basis='provider_normalized' if latest else 'market_date', field_sources=sources)
    for field in decimals:
        value = summary[field]
        if value is not None:
            step = Decimal('.0001') if field in ('beta','dividend_yield') else Decimal('.01')
            limit = Decimal('1e6') if field in ('beta','dividend_yield') else Decimal('1e8') if field in ('per','roe') else Decimal('1e18')
            if abs(value) >= limit:
                summary[field] = None; sources.pop(field, None); continue
            rounded = value.quantize(step, rounding=ROUND_HALF_UP)
            summary[field] = rounded if abs(rounded) < limit else None
            if summary[field] is None: sources.pop(field, None)
    return summary
