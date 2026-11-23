from contextvars import ContextVar
from datetime import datetime, time, timedelta
from decimal import Decimal
from zoneinfo import ZoneInfo
from django.db import transaction
from django.utils import timezone
from rest_framework.exceptions import APIException
from .models import AIUsage, AITariff, ProviderBudget, BudgetWindow, SystemErrorLog

usage_owner = ContextVar('usage_owner', default=None)
ERRORS = {
    'provider_unavailable': 'El proveedor no pudo completar la solicitud.',
    'invalid_provider_response': 'La respuesta del proveedor no superó la validación.',
    'budget_warning': 'El consumo alcanzó el 85 % del presupuesto interno.',
    'budget_exhausted': 'Se alcanzó el presupuesto interno de llamadas.',
    'unexpected_error': 'La operación no pudo completarse.',
    'delivery_failed': 'No se pudo entregar un aviso por el canal indicado.',
}


def record_error(code, component, level='error', **context):
    """Solo códigos y contexto permitido: nunca texto de excepciones, peticiones o claves."""
    if code not in ERRORS or component not in ('local_ai', 'yahoo_chart', 'api', 'email', 'push'):
        return
    safe = {key: value for key, value in context.items() if key in ('http_status', 'attempt', 'retryable')
            and type(value) in (int, bool)}
    try:
        # Un fallo del registro no puede romper ni volver a registrar la operación original.
        with transaction.atomic():
            SystemErrorLog.objects.create(code=code, component=component,
                level=level if level in ('warning', 'error') else 'error', summary=ERRORS[code], context=safe)
            SystemErrorLog.objects.filter(created_at__lt=timezone.now()-timedelta(days=30)).delete()
            boundary = list(SystemErrorLog.objects.values_list('id', flat=True)[9999:10000])
            if boundary:
                SystemErrorLog.objects.filter(id__lt=boundary[0]).delete()
    except Exception:
        pass


class BudgetExceeded(APIException):
    status_code = 429
    default_detail = 'Se alcanzó el límite de llamadas. Los datos guardados siguen disponibles.'
    default_code = 'provider_budget_exhausted'


def window_for(budget, now=None):
    local = (now or timezone.now()).astimezone(ZoneInfo(budget.reset_timezone))
    row, _ = BudgetWindow.objects.get_or_create(budget=budget, date=local.date(), timezone=budget.reset_timezone)
    start = datetime.combine(local.date(), time.min, ZoneInfo(budget.reset_timezone))
    return row, start, start+timedelta(days=1)


def reserve_call(provider):
    with transaction.atomic():
        budget, _ = ProviderBudget.objects.get_or_create(provider=provider)
        budget = ProviderBudget.objects.select_for_update().get(pk=budget.pk)
        row, _, _ = window_for(budget)
        if row.calls >= budget.daily_limit:
            raise BudgetExceeded()
        row.calls += 1
        if row.calls*100 >= budget.daily_limit*85 and row.warned_at is None:
            row.warned_at = timezone.now()
            record_error('budget_warning', provider, 'warning')
        row.save(update_fields=['calls', 'warned_at'])


def record_ai(started, duration, outcome, provider_data=None):
    data = provider_data or {}
    def token_count(key):
        value = data.get(key)
        return value if type(value) is int and 0 <= value <= 2_000_000_000 else None
    incoming, outgoing = token_count('tokens_evaluated'), token_count('tokens_predicted')
    rate = AITariff.objects.filter(provider='local_ai', model='sp500-local-qwen3', effective_at__lte=started).first()
    tariff, cost = {}, None
    if rate:
        tariff = {'version': rate.version, 'currency': rate.currency, 'input_per_million': str(rate.input_per_million),
                  'output_per_million': str(rate.output_per_million)}
        if incoming is not None and outgoing is not None:
            cost = ((Decimal(incoming)*rate.input_per_million + Decimal(outgoing)*rate.output_per_million)
                    / Decimal(1_000_000)).quantize(Decimal('.00000001'))
    AIUsage.objects.create(model='sp500-local-qwen3', user_id=usage_owner.get(), started_at=started,
        duration_ms=max(0, int(duration*1000)), outcome=outcome, input_tokens=incoming, output_tokens=outgoing,
        tariff=tariff, estimated_cost=cost)
    if outcome != 'success':
        record_error('provider_unavailable', 'local_ai')
