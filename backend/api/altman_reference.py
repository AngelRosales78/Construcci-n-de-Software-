"""Validate an independent fiscal-close reference without mixing summary dates."""
from datetime import date
from decimal import Decimal
from urllib.parse import urlsplit

FIELDS = ('working_capital', 'total_assets', 'retained_earnings', 'ebit', 'equity_value', 'total_liabilities', 'sales')

def validate_reference(data, today=None):
    today = today or date.today()
    if not isinstance(data, dict) or data.get('version') != 1 or data.get('currency') != 'USD':
        raise ValueError('Referencia Altman no admitida.')
    start, end, quote = (date.fromisoformat(data[k]) for k in ('period_start','period_end','price_date'))
    if not 350 <= (end-start).days+1 <= 380 or end > today or not 0 <= (end-quote).days <= 3:
        raise ValueError('Las fechas del balance y del precio no corresponden.')
    for key in ('report_url','price_url'):
        parsed = urlsplit(data[key])
        if parsed.scheme != 'https' or not parsed.netloc: raise ValueError('Fuente HTTPS requerida.')
    if not data.get('method') or not data.get('locator'):
        raise ValueError('Falta el método o la ubicación del informe.')
    values = {key: Decimal(str(data['inputs'][key])) for key in FIELDS}
    if any(not v.is_finite() or abs(v) >= Decimal('1e18') for v in values.values()):
        raise ValueError('Importe no válido.')
    shares, close = Decimal(str(data['shares_outstanding'])), Decimal(str(data['unadjusted_close']))
    if not shares.is_finite() or not close.is_finite() or shares <= 0 or close <= 0:
        raise ValueError('Acciones o precio no válidos.')
    if values['equity_value'] != shares * close:
        raise ValueError('El valor bursátil no coincide con acciones por cierre.')
    if values['total_assets'] <= 0 or values['total_liabilities'] <= 0 or values['sales'] < 0:
        raise ValueError('Denominadores o ventas no válidos.')
    if 'sec_evidence' in data:
        from .sec_altman import derive
        evidence = data['sec_evidence']
        derived, sec_shares = derive(evidence, today)
        if evidence['version'] != 1 or evidence['period_start'] != data['period_start'] or evidence['period_end'] != data['period_end']:
            raise ValueError('Las fechas SEC no corresponden a la referencia.')
        if sec_shares != shares or any(derived[key] != values[key] for key in derived):
            raise ValueError('Los importes no coinciden con los hechos SEC conservados.')
        cik, accession = evidence['cik'], evidence['accession']
        expected_url = f'https://www.sec.gov/Archives/edgar/data/{cik}/{accession.replace("-", "")}/{accession}-index.htm'
        if data['report_url'] != expected_url:
            raise ValueError('El informe no corresponde a los hechos SEC.')
        if evidence['tickers'] != [evidence['ticker']] or 6000 <= evidence['sic'] <= 6799 or not evidence['sic']:
            raise ValueError('La identidad o el sector requieren revisión individual.')
    return values
