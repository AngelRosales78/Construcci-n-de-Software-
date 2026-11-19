"""Precios ajustados de Yahoo: identidad verificada y sesiones ya terminadas."""
import hashlib
import json
import re
import socket
from datetime import datetime, timezone as datetime_timezone
from decimal import Decimal, InvalidOperation
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import Request, HTTPRedirectHandler, build_opener
from zoneinfo import ZoneInfo
from django.core.exceptions import ValidationError
from django.utils import timezone
from .models import PriceHistory

PROVIDER = 'yahoo_chart'
MAX_BYTES = 2 * 1024 * 1024


class SourceError(Exception):
    def __init__(self, code, message, status=503):
        self.code, self.message, self.status = code, message, status
        super().__init__(message)


def normalize_ticker(value):
    ticker = value.strip().upper()
    if not re.fullmatch(r'[A-Z][A-Z0-9.-]{0,9}', ticker):
        raise SourceError('invalid_ticker', 'Introduce un ticker válido de hasta diez caracteres.', 400)
    return ticker.replace('.', '-')


def source_url(ticker):
    return f'https://query1.finance.yahoo.com/v8/finance/chart/{quote(normalize_ticker(ticker))}?range=5y&interval=1d&events=div%2Csplits'


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def download(ticker):
    request = Request(source_url(ticker), headers={
        'User-Agent': 'SP500Academic/1.0 (https://github.com/AngelRosales78/Construcci-n-de-Software-/issues)',
        'Accept': 'application/json',
    })
    try:
        with build_opener(NoRedirect()).open(request, timeout=12) as response:
            raw = response.read(MAX_BYTES + 1)
            if len(raw) > MAX_BYTES:
                raise SourceError('invalid_response', 'La respuesta de precios excede el tamaño admitido.', 502)
            return raw
    except HTTPError as error:
        if error.code in (404, 422):
            raise SourceError('ticker_not_found', 'La fuente no reconoce ese ticker o no ofrece precios.', 400) from error
        if error.code == 429:
            raise SourceError('provider_rate', 'La fuente limitó las consultas. Intenta más tarde.', 429) from error
        raise SourceError('source_unavailable', 'La fuente de precios no está disponible. Se conservan los datos anteriores.') from error
    except (URLError, TimeoutError, socket.timeout, OSError) as error:
        raise SourceError('source_unavailable', 'No se pudo consultar la fuente. Se conservan los datos anteriores.') from error


def parse_prices(raw, ticker):
    try:
        if len(raw) > MAX_BYTES:
            raise ValueError('Respuesta demasiado grande.')
        chart = json.loads(raw)['chart']
        if chart.get('error'):
            raise ValueError('La fuente devolvió un error.')
        results = chart['result']
        if not isinstance(results, list) or len(results) != 1:
            raise ValueError('Resultado ambiguo.')
        result = results[0]
        meta = result['meta']
        if normalize_ticker(meta['symbol']) != normalize_ticker(ticker):
            raise ValueError('Ticker no coincidente.')
        if meta['instrumentType'] != 'EQUITY' or meta['currency'] != 'USD' or meta['exchangeTimezoneName'] != 'America/New_York':
            raise SourceError('unsupported_instrument', 'Se admiten acciones en USD de mercados estadounidenses con horario de Nueva York.', 400)
        name = (meta.get('longName') or meta.get('shortName') or '').strip()
        if not name or len(name) > 200 or any(ord(char) < 32 for char in name):
            raise ValueError('Nombre no válido.')
        timestamps = result['timestamp']
        adjusted = result['indicators']['adjclose'][0]['adjclose']
        volumes = result['indicators']['quote'][0]['volume']
        if not 1 <= len(timestamps) <= 2000 or len(timestamps) != len(adjusted) or len(timestamps) != len(volumes):
            raise ValueError('Series incompatibles.')
        exchange = ZoneInfo('America/New_York')
        today = timezone.now().astimezone(exchange).date()
        rows, dates = [], set()
        for timestamp, price, volume in zip(timestamps, adjusted, volumes):
            if type(timestamp) is not int:
                raise ValueError('Fecha no válida.')
            day = datetime.fromtimestamp(timestamp, datetime_timezone.utc).astimezone(exchange).date()
            # No se etiqueta como cierre definitivo la vela de la sesión actual.
            if day >= today or price is None:
                continue
            if day in dates or type(price) not in (int, float) or type(volume) not in (int, type(None)):
                raise ValueError('Precio, volumen o fecha duplicada no válidos.')
            value = Decimal(str(price))
            if not value.is_finite() or value <= 0:
                raise ValueError('Precio no válido.')
            row = {'date': day, 'adjusted_close': value.quantize(Decimal('.000001')),
                   'volume': volume, 'currency': 'USD', 'source': source_url(ticker), 'is_demo': False}
            PriceHistory(**row).full_clean(exclude=['company'], validate_unique=False, validate_constraints=False)
            dates.add(day); rows.append(row)
        rows.sort(key=lambda row: row['date'])
        if not rows:
            raise ValueError('No existen sesiones terminadas con cierre ajustado.')
        return {'name': name, 'prices': rows, 'source': source_url(ticker),
                'sha256': hashlib.sha256(raw).hexdigest(), 'exchange': str(meta.get('exchangeName', ''))[:40]}
    except SourceError:
        raise
    except (KeyError, TypeError, ValueError, IndexError, InvalidOperation, ValidationError, OverflowError, AttributeError, OSError) as error:
        raise SourceError('invalid_response', 'La fuente devolvió datos incompletos o incompatibles. No se guardó ningún precio.', 502) from error
