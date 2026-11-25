"""Strict annual SEC fact selection for the existing five-factor Altman formula.

Only whole-entity US-GAAP facts from one annual filing are combined. No cover-page,
weighted-average or diluted share count is substituted for fiscal-end common shares.
"""
import hashlib
import json
import re
from datetime import date, datetime
from decimal import Decimal
from zoneinfo import ZoneInfo

REVENUE_TAGS = ('RevenueFromContractWithCustomerExcludingAssessedTax',
                'Revenues', 'SalesRevenueNet', 'RevenueFromContractWithCustomerIncludingAssessedTax')
PRETAX_TAGS = ('IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest',
              'IncomeLossFromContinuingOperationsBeforeIncomeTaxesMinorityInterestAndIncomeLossFromEquityMethodInvestments')


class IncompleteReference(ValueError):
    pass


class MissingFact(IncompleteReference):
    pass


def value(point):
    item = point.get('val')
    if isinstance(item, bool):
        raise ValueError('Hecho no numérico.')
    number = Decimal(str(item))
    if not number.is_finite() or abs(number) >= Decimal('1e18'):
        raise ValueError('Hecho no finito o fuera de rango.')
    return number


def pick(facts, tags, accession, end, start=None, unit='USD'):
    for tag in tags:
        rows = [row for row in facts.get(tag, {}).get('units', {}).get(unit, [])
                if row.get('accn') == accession and row.get('form') == '10-K'
                and row.get('end') == end and (row.get('start') == start if start else not row.get('start'))]
        if not rows:
            continue
        if len({value(row) for row in rows}) != 1:
            raise IncompleteReference('Hecho ambiguo: ' + tag)
        return {'tag': tag, 'unit': unit, 'point': rows[0]}
    raise MissingFact('Falta hecho del mismo cierre: ' + '/'.join(tags))


def derive(evidence, today=None):
    """Validate the evidence embedded in a packaged reference and reproduce inputs."""
    today = today or date.today()
    cik, accession = evidence['cik'], evidence['accession']
    if isinstance(cik, bool) or not isinstance(cik, int) or not 0 < cik < 10**10:
        raise ValueError('CIK no válido.')
    if not re.fullmatch(r'\d{10}-\d{2}-\d{6}', accession):
        raise ValueError('Informe no válido.')
    if evidence['facts_url'] != f'https://data.sec.gov/api/xbrl/companyfacts/CIK{cik:010d}.json':
        raise ValueError('Origen SEC no correspondiente.')
    if not re.fullmatch(r'[a-f0-9]{64}', evidence['snapshot_sha256']):
        raise ValueError('Huella SEC ausente.')
    start, end = evidence['period_start'], evidence['period_end']
    if not 350 <= (date.fromisoformat(end)-date.fromisoformat(start)).days+1 <= 380:
        raise ValueError('El período SEC no es anual.')
    points = evidence['facts']
    instant = {'assets', 'current_assets', 'current_liabilities', 'retained', 'liabilities',
               'noncurrent_liabilities', 'shares', 'issued_shares', 'treasury_shares'}
    allowed = {'assets': ('Assets',), 'current_assets': ('AssetsCurrent',),
               'current_liabilities': ('LiabilitiesCurrent',), 'retained': ('RetainedEarningsAccumulatedDeficit',),
               'liabilities': ('Liabilities',), 'shares': ('CommonStockSharesOutstanding',),
               'noncurrent_liabilities': ('LiabilitiesNoncurrent',),
               'issued_shares': ('CommonStockSharesIssued',), 'treasury_shares': ('TreasuryStockShares',),
               'revenue': REVENUE_TAGS, 'operating_income': ('OperatingIncomeLoss',),
               'pretax': PRETAX_TAGS, 'interest': ('InterestExpense', 'InterestExpenseNonOperating')}
    for key, fact in points.items():
        if key not in allowed or fact['tag'] not in allowed[key]:
            raise ValueError('Etiqueta SEC no admitida.')
        point = fact['point']
        if fact['unit'] != ('shares' if key in ('shares', 'issued_shares', 'treasury_shares') else 'USD'):
            raise ValueError('Unidad no compatible.')
        if point.get('form') != '10-K' or point.get('accn') != accession or point.get('end') != end:
            raise ValueError('Los hechos no pertenecen al mismo informe y cierre.')
        if (key in instant and point.get('start') is not None) or (key not in instant and point.get('start') != start):
            raise ValueError('Duración no compatible.')
        if not date.fromisoformat(end) <= date.fromisoformat(point['filed']) <= today:
            raise ValueError('Fecha de publicación no válida.')
        value(point)
    required = {'assets', 'current_assets', 'current_liabilities', 'retained', 'revenue'}
    if not required.issubset(points):
        raise ValueError('Referencia SEC incompleta.')
    amounts = {key: value(fact['point']) for key, fact in points.items()}
    if any(amounts.get(key, Decimal(0)) < 0 for key in (
            'assets', 'current_assets', 'current_liabilities', 'liabilities',
            'noncurrent_liabilities', 'shares', 'issued_shares', 'treasury_shares', 'revenue')):
        raise ValueError('Componente de balance o ventas negativo no compatible.')
    if evidence['ebit_basis'] == 'operating_income' and 'operating_income' in amounts:
        ebit = amounts['operating_income']
    elif evidence['ebit_basis'] == 'pretax_plus_interest' and {'pretax', 'interest'}.issubset(amounts):
        if amounts['interest'] < 0:
            raise ValueError('Conciliar el signo de los intereses.')
        ebit = amounts['pretax'] + amounts['interest']
    else:
        raise ValueError('Método EBIT no válido.')
    if evidence.get('liabilities_basis', 'reported_total') == 'reported_total':
        liabilities = amounts['liabilities']
    elif evidence['liabilities_basis'] == 'current_plus_noncurrent' and 'noncurrent_liabilities' in amounts:
        liabilities = amounts['current_liabilities'] + amounts['noncurrent_liabilities']
    else:
        raise ValueError('Base de pasivos no válida.')
    if evidence.get('shares_basis', 'reported_outstanding') == 'reported_outstanding':
        shares = amounts['shares']
    elif evidence['shares_basis'] == 'issued_less_treasury' and {'issued_shares', 'treasury_shares'}.issubset(amounts):
        if amounts['treasury_shares'] < 0:
            raise ValueError('Acciones en tesorería negativas.')
        shares = amounts['issued_shares'] - amounts['treasury_shares']
    else:
        raise ValueError('Base de acciones no válida.')
    if shares <= 0:
        raise ValueError('Acciones no positivas.')
    return {'working_capital': amounts['current_assets'] - amounts['current_liabilities'],
            'total_assets': amounts['assets'], 'retained_earnings': amounts['retained'],
            'ebit': ebit, 'total_liabilities': liabilities, 'sales': amounts['revenue']}, shares


def prepare_reference(ticker, cik, facts, identity, source, price_entry, target_year, today=None):
    today = today or date.today()
    if int(facts['cik']) != cik or int(identity['cik']) != cik:
        raise ValueError('Identidad CIK no correspondiente.')
    tickers = [symbol.replace('-', '.') for symbol in identity.get('tickers', [])]
    if ticker not in tickers:
        raise IncompleteReference('El símbolo no corresponde a la identidad SEC vigente.')
    sic = int(identity.get('sic') or 0)
    if 6000 <= sic <= 6799 or not sic:
        raise IncompleteReference('Revisión sectorial requerida para el modelo original.')
    if len(set(tickers)) != 1:
        raise IncompleteReference('Revisar las clases de acciones y otros valores cotizados del emisor.')
    gaap = facts.get('facts', {}).get('us-gaap', {})
    candidates = set()
    for tag in REVENUE_TAGS:
        for point in gaap.get(tag, {}).get('units', {}).get('USD', []):
            if point.get('form') != '10-K' or not point.get('start'):
                continue
            start, end = date.fromisoformat(point['start']), date.fromisoformat(point['end'])
            if end.year == target_year and 350 <= (end-start).days+1 <= 380 and end <= today:
                if end <= date.fromisoformat(point['filed']) <= today:
                    candidates.add((point['end'], point['filed'], point['start'], point['accn']))
    if not candidates:
        raise IncompleteReference('Falta un ejercicio anual USD compatible con el año del resumen.')
    last_error = None
    for end, filed, start, accession in sorted(candidates, reverse=True):
        try:
            selected = {key: pick(gaap, (tag,), accession, end)
                for key, tag in {'assets': 'Assets', 'current_assets': 'AssetsCurrent',
                    'current_liabilities': 'LiabilitiesCurrent', 'retained': 'RetainedEarningsAccumulatedDeficit'}.items()}
            try:
                selected['liabilities'] = pick(gaap, ('Liabilities',), accession, end)
                liabilities_basis = 'reported_total'
            except MissingFact:
                selected['noncurrent_liabilities'] = pick(gaap, ('LiabilitiesNoncurrent',), accession, end)
                liabilities_basis = 'current_plus_noncurrent'
            try:
                selected['shares'] = pick(gaap, ('CommonStockSharesOutstanding',), accession, end, unit='shares')
                shares_basis = 'reported_outstanding'
            except MissingFact:
                selected['issued_shares'] = pick(gaap, ('CommonStockSharesIssued',), accession, end, unit='shares')
                selected['treasury_shares'] = pick(gaap, ('TreasuryStockShares',), accession, end, unit='shares')
                shares_basis = 'issued_less_treasury'
            selected['revenue'] = pick(gaap, REVENUE_TAGS, accession, end, start)
            try:
                selected['operating_income'] = pick(gaap, ('OperatingIncomeLoss',), accession, end, start)
                basis = 'operating_income'
            except MissingFact:
                selected['pretax'] = pick(gaap, PRETAX_TAGS, accession, end, start)
                selected['interest'] = pick(gaap, ('InterestExpense', 'InterestExpenseNonOperating'), accession, end, start)
                basis = 'pretax_plus_interest'
            evidence = {'version': 1, 'ticker': ticker, 'cik': cik, 'sic': sic, 'tickers': tickers,
                'accession': accession, 'facts_url': source['url'], 'snapshot_sha256': source['sha256'],
                'retrieved_at': source['retrieved_at'], 'period_start': start, 'period_end': end,
                'ebit_basis': basis, 'liabilities_basis': liabilities_basis, 'shares_basis': shares_basis, 'facts': selected}
            amounts, shares = derive(evidence, today)
            raw_prices = price_entry['raw_response'].encode()
            if hashlib.sha256(raw_prices).hexdigest() != price_entry['sha256']:
                raise ValueError('Huella de precios incorrecta.')
            chart = json.loads(raw_prices)['chart']['result'][0]
            if chart['meta']['symbol'].replace('-', '.') != ticker or chart['meta']['currency'] != 'USD':
                raise ValueError('Cotización de otra empresa o moneda.')
            boundary = date.fromisoformat(end)
            day = lambda epoch: datetime.fromtimestamp(epoch, ZoneInfo('America/New_York')).date()
            if any(day(event['date']) > boundary for event in chart.get('events', {}).get('splits', {}).values()):
                raise IncompleteReference('Desdoblamiento posterior: requiere conciliar la base por acción.')
            prices = [(day(t), Decimal(str(p))) for t, p in zip(chart['timestamp'], chart['indicators']['quote'][0]['close'])
                      if p is not None and day(t) <= boundary]
            if not prices:
                raise IncompleteReference('Sin precio histórico para el cierre contable.')
            price_date, close = max(prices)
            if (boundary-price_date).days > 3 or not close.is_finite() or close <= 0:
                raise IncompleteReference('El precio no corresponde al cierre contable.')
            amounts['equity_value'] = shares * close
            method = ('EBIT operativo: resultado de explotación XBRL.' if basis == 'operating_income'
                      else 'EBIT: resultado antes de impuestos más gasto por intereses XBRL.')
            if liabilities_basis == 'current_plus_noncurrent':
                method += ' Pasivos: corrientes más no corrientes publicados en el mismo cierre.'
            if shares_basis == 'issued_less_treasury':
                method += ' Acciones en circulación: emitidas menos tesorería del mismo cierre.'
            return {'version': 1, 'currency': 'USD', 'period_start': start, 'period_end': end,
                'price_date': price_date.isoformat(), 'unadjusted_close': str(close), 'shares_outstanding': str(shares),
                'inputs': {key: str(amount) for key, amount in amounts.items()},
                'report_url': f'https://www.sec.gov/Archives/edgar/data/{cik}/{accession.replace("-", "")}/{accession}-index.htm',
                'price_url': f'https://finance.yahoo.com/quote/{ticker.replace(".", "-")}/history/',
                'price_sha256': price_entry['sha256'], 'reviewed_at': today.isoformat(), 'sec_evidence': evidence,
                'locator': f'Hechos US-GAAP no dimensionales del informe {accession}; etiquetas y contextos conservados.',
                'method': method + ' Acciones comunes en circulación del cierre, no promedio diluido, por precio sin ajustar. Extracción automática con validación de identidad, informe, moneda y períodos; no revisión contable manual. Fórmula original de cinco factores, uso académico; no validada como predictor para esta empresa.'}
        except IncompleteReference as error:
            last_error = error
    raise last_error or IncompleteReference('No se pudo conciliar una referencia anual.')
