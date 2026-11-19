"""Actualizaciones y verificaciones persistidas; las llamadas HTTP quedan fuera de la transacción de escritura."""
from datetime import timedelta, timezone as datetime_timezone
from django.conf import settings
from django.db import IntegrityError, transaction
from django.db.models import Q
from django.utils import timezone
from rest_framework.exceptions import PermissionDenied
from .models import AdministrationEvent, Company, MarketDataOperation, PriceHistory, ProviderDailyUsage, User
from .catalog_services import invalidate_catalog
from .market_provider import PROVIDER, SourceError, download, normalize_ticker, parse_prices
from .personal_views import Conflict
from .user_management import is_administrator


def provider_usage():
    now = timezone.now().astimezone(datetime_timezone.utc)
    row = ProviderDailyUsage.objects.filter(provider=PROVIDER, day=now.date()).first()
    reset = (now + timedelta(days=1)).replace(hour=0, minute=0, second=0, microsecond=0)
    return {'provider': 'Yahoo Finance', 'calls': row.calls if row else 0,
            'limit': row.limit if row else settings.MARKET_DATA_DAILY_LIMIT,
            'reset_at': reset.isoformat(), 'budget_kind': 'internal', 'window_timezone': 'UTC'}


def reserve_call():
    now = timezone.now()
    with transaction.atomic():
        row, _ = ProviderDailyUsage.objects.select_for_update().get_or_create(
            provider=PROVIDER, day=now.astimezone(datetime_timezone.utc).date(),
            defaults={'limit': max(0, settings.MARKET_DATA_DAILY_LIMIT)})
        if row.calls >= row.limit:
            raise SourceError('internal_quota', 'Se agotó el límite interno de consultas de hoy. Los datos guardados siguen disponibles.', 429)
        if row.last_called_at and (now-row.last_called_at).total_seconds() < settings.MARKET_DATA_MIN_INTERVAL:
            raise SourceError('internal_rate', 'Espera un momento antes de consultar otra empresa.', 429)
        row.calls += 1
        row.last_called_at = now
        row.save(update_fields=['calls', 'last_called_at'])


def require_admin(actor):
    current = User.objects.select_for_update().filter(pk=actor.pk).first()
    if not current or not is_administrator(current):
        raise PermissionDenied('Se requiere un administrador activo autorizado.')


def existing_ticker(ticker):
    canonical = normalize_ticker(ticker)
    return Company.objects.filter(Q(ticker=canonical) | Q(ticker=canonical.replace('-', '.'))).exists()


def claim(actor, request_id, ticker, kind, sector, company):
    ticker = normalize_ticker(ticker)
    now = timezone.now()
    try:
        with transaction.atomic():
            require_admin(actor)
            MarketDataOperation.objects.filter(ticker=ticker, status='running',
                started_at__lt=now-timedelta(minutes=4)).update(status='failed', finished_at=now,
                    error_code='interrupted', message='La operación se interrumpió. Puede iniciarse una consulta nueva.', result={'http_status': 503})
            previous = MarketDataOperation.objects.filter(actor=actor, request_id=request_id).first()
            if previous:
                if (previous.ticker != ticker or previous.kind != kind or previous.sector != sector
                        or (kind == 'refresh' and previous.company_id != company.pk)):
                    raise Conflict('El identificador ya corresponde a otra operación.')
                return previous, False
            if kind == 'verify' and existing_ticker(ticker):
                raise Conflict('El ticker ya está registrado. Busca la empresa en el catálogo.')
            operation = MarketDataOperation.objects.create(actor=actor, request_id=request_id,
                company=company, ticker=ticker, kind=kind, sector=sector)
            return operation, True
    except IntegrityError as error:
        raise Conflict('Ya hay una consulta de esa empresa en curso. Espera su resultado.') from error


def public_operation(operation):
    expires = operation.finished_at + timedelta(minutes=10) if operation.status == 'ready' else None
    return {'id': str(operation.pk), 'status': operation.status, 'kind': operation.kind,
            'ticker': operation.ticker, 'company_id': operation.company_id,
            'started_at': operation.started_at, 'finished_at': operation.finished_at,
            'expires_at': expires, 'message': operation.message, 'error_code': operation.error_code,
            'result': {key: value for key,value in operation.result.items() if key != 'http_status'}}


def summary(payload):
    return {'name': payload['name'], 'currency': 'USD', 'exchange': payload['exchange'],
            'price_count': len(payload['prices']), 'first_price_date': payload['prices'][0]['date'].isoformat(),
            'price_as_of': payload['prices'][-1]['date'].isoformat(),
            'adjusted_close': str(payload['prices'][-1]['adjusted_close']),
            'source_url': payload['source'], 'response_sha256': payload['sha256']}


def apply_prices(company, payload):
    rows = payload['prices']
    if PriceHistory.objects.filter(company=company).exclude(currency='USD').exists():
        raise SourceError('currency_conflict', 'La moneda no coincide con el historial guardado. No se modificaron datos.', 409)
    latest = PriceHistory.objects.filter(company=company).order_by('-date').first()
    if latest and rows[-1]['date'] < latest.date:
        raise SourceError('stale_response', 'La fuente no supera la fecha de los precios guardados. Se conservó el historial.', 409)
    dates = [row['date'] for row in rows]
    existing = set(PriceHistory.objects.filter(company=company, date__in=dates).values_list('date', flat=True))
    PriceHistory.objects.bulk_create([PriceHistory(company=company, **row) for row in rows],
        update_conflicts=True, unique_fields=['company','date'],
        update_fields=['adjusted_close','volume','currency','source','is_demo'])
    company.latest_price = PriceHistory.objects.filter(company=company).order_by('-date').first()
    company.save(update_fields=['latest_price'])
    return len(set(dates) - existing)


def fail(operation, error):
    MarketDataOperation.objects.filter(pk=operation.pk, status='running').update(
        status='failed', error_code=error.code, message=error.message, finished_at=timezone.now(),
        result={'http_status': error.status})
    operation.refresh_from_db()
    return operation


def execute(actor, request_id, ticker, kind, sector='', company=None):
    operation, fresh = claim(actor, request_id, ticker, kind, sector, company)
    if not fresh:
        return operation
    try:
        reserve_call()
        raw = download(operation.ticker)
        payload = parse_prices(raw, operation.ticker)
        with transaction.atomic():
            require_admin(actor)
            operation = MarketDataOperation.objects.select_for_update().get(pk=operation.pk)
            if operation.status != 'running':
                raise Conflict('Esta consulta ya no está vigente. Inicia otra operación.')
            result = summary(payload)
            if kind == 'refresh':
                current = Company.objects.select_for_update().get(pk=company.pk)
                result['new_dates'] = apply_prices(current, payload)
                operation.status = 'succeeded'
                operation.message = 'Precios históricos actualizados. Los indicadores y balances conservan sus fechas y fuentes anteriores.'
                AdministrationEvent.objects.create(actor=actor, action='prices_updated',
                    changes={'ticker': current.ticker, 'operation_id': str(operation.pk),
                             'source': payload['source'], 'response_sha256': payload['sha256']})
                invalidate_catalog()
            else:
                operation.status = 'ready'
                operation.message = 'Ticker verificado. Revisa el nombre y los precios antes de confirmar el alta.'
            operation.result = result
            operation.source_url = payload['source']
            operation.response_sha256 = payload['sha256']
            operation.raw_response = raw
            operation.finished_at = timezone.now()
            operation.save()
        return operation
    except SourceError as error:
        return fail(operation, error)
    except PermissionDenied:
        fail(operation, SourceError('access_revoked', 'La cuenta dejó de tener permisos para esta operación.', 403))
        raise


def create_verified_company(actor, verification_id):
    with transaction.atomic():
        require_admin(actor)
        operation = MarketDataOperation.objects.select_for_update().filter(pk=verification_id,
            actor=actor, kind='verify').first()
        if not operation:
            raise SourceError('verification_not_found', 'La verificación no pertenece a esta cuenta.', 404)
        if operation.status == 'succeeded' and operation.company_id:
            return operation, False
        if operation.status != 'ready' or operation.finished_at < timezone.now()-timedelta(minutes=10):
            raise SourceError('verification_expired', 'Vuelve a verificar el ticker antes de confirmar el alta.', 409)
        if existing_ticker(operation.ticker):
            raise Conflict('El ticker ya está registrado.')
        payload = parse_prices(bytes(operation.raw_response), operation.ticker)
        try:
            with transaction.atomic():
                company = Company.objects.create(ticker=operation.ticker, name=payload['name'],
                    sector=operation.sector, sector_overridden=True, is_index_member=False,
                    identity_source_url=payload['source'], identity_verified_at=operation.finished_at)
        except IntegrityError as error:
            raise Conflict('El ticker ya está registrado.') from error
        new_dates = apply_prices(company, payload)
        operation.company = company
        operation.status = 'succeeded'
        operation.message = 'Empresa añadida con precios verificados. Su pertenencia al S&P 500 y sus balances no se han verificado.'
        operation.result = dict(operation.result, new_dates=new_dates)
        operation.save(update_fields=['company','status','message','result'])
        AdministrationEvent.objects.create(actor=actor, action='company_added', changes={
            'ticker': company.ticker, 'sector': company.sector, 'sector_origin': 'administrator',
            'operation_id': str(operation.pk), 'response_sha256': operation.response_sha256})
        invalidate_catalog()
        return operation, True
