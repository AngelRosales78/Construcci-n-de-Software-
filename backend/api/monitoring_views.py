from collections import defaultdict
from datetime import date, datetime, time, timedelta
from decimal import Decimal, ROUND_HALF_UP
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError
from django.db import transaction
from django.db.models import Count, Sum, Q
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import serializers
from rest_framework.exceptions import ValidationError
from rest_framework.pagination import PageNumberPagination
from rest_framework.response import Response
from rest_framework.views import APIView
from .models import AIUsage, AITariff, ProviderBudget, SystemErrorLog, SubscriptionTransaction, User
from .market_views import ExactSerializer
from .personal_views import Conflict
from .user_management import IsPlatformAdministrator
from .monitoring_services import window_for


class MonitoringView(APIView):
    permission_classes = (IsPlatformAdministrator,)

    def finalize_response(self, request, response, *args, **kwargs):
        response = super().finalize_response(request, response, *args, **kwargs)
        response['Cache-Control'] = 'private, no-store'
        return response


class PeriodInput(serializers.Serializer):
    start = serializers.DateField(required=False)
    end = serializers.DateField(required=False)
    provider = serializers.ChoiceField(choices=('local_ai',), required=False)
    model = serializers.CharField(max_length=100, required=False)

    def validate(self, values):
        today = timezone.localdate()
        values.setdefault('start', today.replace(day=1))
        values.setdefault('end', today)
        if values['end'] < values['start'] or (values['end']-values['start']).days > 366 or values['end'] > today:
            raise ValidationError('Elige un período de hasta 366 días, sin fechas futuras.')
        return values


def period_rows(request):
    form = PeriodInput(data=request.query_params); form.is_valid(raise_exception=True)
    data = form.validated_data
    zone = timezone.get_current_timezone()
    start = datetime.combine(data['start'], time.min, zone)
    end = datetime.combine(data['end']+timedelta(days=1), time.min, zone)
    return data, start, end


class AIUsageView(MonitoringView):
    def get(self, request):
        filters, start, end = period_rows(request)
        rows = AIUsage.objects.filter(started_at__gte=start, started_at__lt=end)
        if filters.get('provider'): rows = rows.filter(provider=filters['provider'])
        if filters.get('model'): rows = rows.filter(model=filters['model'])
        breakdown = list(rows.values('provider', 'model').annotate(calls=Count('id'),
            errors=Count('id', filter=Q(outcome='error')), input_total=Sum('input_tokens'), output_total=Sum('output_tokens'),
            unreported_tokens=Count('id', filter=Q(input_tokens__isnull=True)|Q(output_tokens__isnull=True))))
        for row in breakdown:
            row['input_tokens']=row.pop('input_total'); row['output_tokens']=row.pop('output_total')
        costs = defaultdict(Decimal)
        unknown_cost = 0
        for row in rows.only('estimated_cost', 'tariff'):
            if row.estimated_cost is None: unknown_cost += 1
            else: costs[row.tariff['currency']] += row.estimated_cost
        return Response({'schema_version': 'ai-usage-v1', 'start': filters['start'], 'end': filters['end'],
            'timezone': str(timezone.get_current_timezone()), 'generated_at': timezone.now(), 'calls': rows.count(),
            'breakdown': breakdown, 'estimated_costs': [{'currency': key, 'amount': str(value)} for key, value in sorted(costs.items())],
            'unpriced_calls': unknown_cost,
            'explanation': 'Llamadas observadas a los motores de IA configurados. Los aciertos de caché no generan llamadas. Tokens y costes solo se muestran cuando el motor y una tarifa versionada los permiten.'})


class TariffInput(ExactSerializer):
    version = serializers.RegexField(r'^[A-Za-z0-9_.-]{1,60}$')
    input_per_million = serializers.DecimalField(max_digits=12, decimal_places=6, min_value=Decimal('0'))
    output_per_million = serializers.DecimalField(max_digits=12, decimal_places=6, min_value=Decimal('0'))
    currency = serializers.ChoiceField(choices=('USD', 'PEN', 'EUR'))


class TariffView(MonitoringView):
    def post(self, request):
        form=TariffInput(data=request.data); form.is_valid(raise_exception=True)
        row=AITariff.objects.create(provider='local_ai', model='sp500-local-qwen3', effective_at=timezone.now(), **form.validated_data)
        return Response({'id': row.pk, 'version': row.version, 'effective_at': row.effective_at}, status=201)


class PlanDistributionView(MonitoringView):
    def get(self, request):
        now = timezone.now()
        users = User.objects.all()
        pro = users.filter(is_active=True, account_plan__plan='pro', account_plan__active=True).filter(
            Q(account_plan__expires_at__isnull=True)|Q(account_plan__expires_at__gt=now)).count()
        total = users.count(); free = total-pro
        return Response({'schema_version': 'plan-distribution-v1', 'generated_at': now, 'total': total,
            'blocked': users.filter(is_active=False).count(), 'rule': 'Todas las cuentas cuentan una vez. Pro requiere cuenta y plan activos y vigencia no vencida; las bloqueadas cuentan en Free.',
            'results': [{'plan': name, 'count': count, 'percentage': str((Decimal(count)*100/total).quantize(Decimal('.01'), rounding=ROUND_HALF_UP)) if total else '0.00'}
                        for name, count in [('free', free), ('pro', pro)]]})


def budget_data(budget):
    row, start, end = window_for(budget)
    percentage = Decimal(row.calls)*100/budget.daily_limit
    return {'provider': budget.provider, 'limit': budget.daily_limit, 'used': row.calls, 'version': budget.version,
        'percentage': str(percentage.quantize(Decimal('.01'))), 'window_start': start, 'resets_at': end,
        'timezone': budget.reset_timezone, 'status': 'exhausted' if row.calls >= budget.daily_limit else 'warning' if percentage >= 85 else 'normal',
        'warned_at': row.warned_at, 'kind': 'internal_budget'}


class BudgetView(MonitoringView):
    def get(self, request):
        budgets = [ProviderBudget.objects.get_or_create(provider=p)[0] for p in ('local_ai', 'yahoo_chart')]
        return Response({'schema_version': 'provider-budgets-v1', 'results': [budget_data(row) for row in budgets],
            'explanation': 'Presupuestos internos diarios, independientes de las cuotas oficiales de los proveedores. Los datos guardados permanecen disponibles.'})

    def patch(self, request, provider):
        if provider not in ('local_ai', 'yahoo_chart'): raise ValidationError('Proveedor no admitido.')
        class BudgetInput(ExactSerializer):
            version=serializers.IntegerField(min_value=0)
            daily_limit=serializers.IntegerField(min_value=1, max_value=1_000_000)
            reset_timezone=serializers.CharField(max_length=60)

            def validate_reset_timezone(self, value):
                try: ZoneInfo(value)
                except (ZoneInfoNotFoundError, ValueError): raise ValidationError('Zona horaria no válida.')
                return value
        form=BudgetInput(data=request.data); form.is_valid(raise_exception=True)
        with transaction.atomic():
            ProviderBudget.objects.get_or_create(provider=provider)
            row=ProviderBudget.objects.select_for_update().get(provider=provider)
            if row.version != form.validated_data['version']: raise Conflict('El límite cambió. Actualiza antes de guardar.')
            # Cambiar la zona solo se permite al comenzar una ventana sin llamadas.
            current, _, _=window_for(row)
            if current.calls and row.reset_timezone != form.validated_data['reset_timezone']:
                raise Conflict('La zona de reinicio se conserva durante una ventana con consumo.')
            row.daily_limit=form.validated_data['daily_limit']; row.reset_timezone=form.validated_data['reset_timezone']; row.version+=1; row.save()
            return Response(budget_data(row))


class MonthlyRevenueView(MonitoringView):
    def get(self, request):
        month=request.query_params.get('month', timezone.localdate().strftime('%Y-%m'))
        try:
            start=date.fromisoformat(month+'-01')
            if len(month) != 7: raise ValueError()
        except (ValueError, TypeError): raise ValidationError({'month': 'Usa año y mes: AAAA-MM.'})
        end=(start.replace(day=28)+timedelta(days=4)).replace(day=1)
        zone=timezone.get_current_timezone()
        lower=datetime.combine(start,time.min,zone); upper=datetime.combine(end,time.min,zone)
        rows=SubscriptionTransaction.objects.filter(outcome='approved', starts_at__lt=upper, ends_at__gt=lower)
        totals=defaultdict(Decimal); included=set()
        for row in rows:
            overlap=(min(row.ends_at,upper)-max(row.starts_at,lower)).total_seconds()
            duration=(row.ends_at-row.starts_at).total_seconds()
            if duration > 0:
                totals[row.currency] += row.amount*Decimal(str(overlap))/Decimal(str(duration))
                if row.user_id: included.add(row.user_id)
        return Response({'schema_version':'monthly-subscription-value-v1','month':month,'generated_at':timezone.now(),
            'simulated':True, 'plans_included':len(included), 'transactions':rows.count(),
            'results':[{'currency':key,'amount':str(value.quantize(Decimal('.01'),rounding=ROUND_HALF_UP))} for key,value in sorted(totals.items())],
            'explanation':'Estimación proporcional a la vigencia dentro del mes, usando el importe histórico de cada suscripción. No representa dinero cobrado. Las asignaciones administrativas sin comprobante no suman importe.'})


class ErrorLogsView(MonitoringView):
    def get(self, request, log_id=None):
        if log_id is not None:
            row=get_object_or_404(SystemErrorLog,pk=log_id)
            return Response(self.data(row)|{'context':row.context})
        rows=SystemErrorLog.objects.all()
        level=request.query_params.get('level','all'); component=request.query_params.get('component','all')
        if level not in ('all','warning','error') or component not in ('all','api','local_ai','yahoo_chart','email','push'):
            raise ValidationError('Filtro no válido.')
        if level != 'all': rows=rows.filter(level=level)
        if component != 'all': rows=rows.filter(component=component)
        page=PageNumberPagination();page.page_size=15
        result=page.get_paginated_response([self.data(row) for row in page.paginate_queryset(rows,request,self)])
        result.data.update(retention_days=30,generated_at=timezone.now())
        return result

    @staticmethod
    def data(row):
        return {'id':row.pk,'created_at':row.created_at,'level':row.level,'component':row.component,'code':row.code,'summary':row.summary}
