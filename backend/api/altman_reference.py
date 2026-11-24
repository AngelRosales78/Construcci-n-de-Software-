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
    return values
