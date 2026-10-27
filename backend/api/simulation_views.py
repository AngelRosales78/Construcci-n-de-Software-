from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import generics, serializers
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.throttling import UserRateThrottle
from .models import Company, SimulationRun
from .simulations import run_simulation
from . import simulations


def price_rows(company):
    return list(company.prices.filter(date__lte=timezone.localdate()).order_by('-date')
                .values('date', 'adjusted_close', 'currency', 'source', 'is_demo')[:1261])[::-1]


class SimulationInput(serializers.Serializer):
    seed = serializers.IntegerField(min_value=0, max_value=2147483647, default=2026)
    annual_return = serializers.FloatField(min_value=-1, max_value=1.5, required=False)
    annual_volatility = serializers.FloatField(min_value=0, max_value=2, required=False)
    paths = serializers.IntegerField(required=False)
    horizon_months = serializers.IntegerField(min_value=12, max_value=12, required=False)

    def validate_paths(self, value):
        if value != simulations.PATHS:
            raise serializers.ValidationError(f'El motor utiliza {simulations.PATHS} trayectorias.')
        return value

    def to_internal_value(self, data):
        if not isinstance(data, dict) or set(data) - set(self.fields):
            raise serializers.ValidationError({'non_field_errors': ['Use solo semilla, tasa anual y volatilidad admitidas.']})
        if any(isinstance(value, bool) for value in data.values()):
            raise serializers.ValidationError({'non_field_errors': ['Los parámetros deben ser números, no booleanos.']})
        return super().to_internal_value(data)

    def validate(self, attrs):
        import math
        if any(not math.isfinite(value) for value in attrs.values()):
            raise serializers.ValidationError('Los parámetros deben ser números finitos.')
        return attrs


class SimulationThrottle(UserRateThrottle):
    scope = 'simulation'
    rate = '12/min'


class CompanySimulationDefaultsView(generics.GenericAPIView):
    permission_classes = (IsAuthenticated,)
    throttle_classes = (SimulationThrottle,)

    def get(self, request, ticker):
        company = get_object_or_404(Company, ticker=ticker.upper())
        result, _ = simulations.prepare_simulation(price_rows(company))
        if result['status'] != 'available':
            return Response(result)
        defaults = result['defaults']
        if not (-1 <= defaults['annual_return'] <= 1.5 and 0 <= defaults['annual_volatility'] <= 2):
            return Response({'status': 'unavailable', 'reason': 'historical_parameters_out_of_bounds'})
        return Response({'schema_version': 'simulation-defaults-v1', 'ticker': company.ticker,
                         **defaults, 'min_return': -1, 'max_return': 1.5,
                         'min_volatility': 0, 'max_volatility': 2, 'seed': 2026,
                         'paths': simulations.PATHS, 'horizon_months': 12,
                         'source_sha256': result['source_sha256']})


class CompanySimulationView(generics.GenericAPIView):
    permission_classes = (IsAuthenticated,)
    throttle_classes = (SimulationThrottle,)

    def post(self, request, ticker):
        company = get_object_or_404(Company, ticker=ticker.upper())
        serializer = SimulationInput(data=request.data)
        serializer.is_valid(raise_exception=True)
        rows = price_rows(company)
        params = {key: value for key, value in serializer.validated_data.items()
                  if key in ('annual_return', 'annual_volatility')}
        result, snapshot = run_simulation(rows, serializer.validated_data['seed'], params)
        result['ticker'] = company.ticker
        if result['status'] == 'available':
            result['schema_version'] = 'simulation-v1'
            result['final_prices'] = {key: {'date': curve[-1]['date'], 'price': curve[-1]['price'],
                                           'percentile': result['percentiles'][key]}
                                      for key, curve in result['scenarios'].items()}
            result['parameter_limits'] = {'annual_return': [-1, 1.5], 'annual_volatility': [0, 2]}
            result['generated_at'] = timezone.now().isoformat()
            result['start_date'] = result['as_of']
            result['end_date'] = result['scenarios']['base'][-1]['date']
            result['parameters'] |= {'seed': result['seed'], 'paths': result['paths'], 'horizon_months': 12}
            result['points'] = [
                {'date': base['date'], 'p10': low['price'], 'p50': base['price'], 'p90': high['price']}
                for low, base, high in zip(result['scenarios']['pessimistic'],
                                           result['scenarios']['base'], result['scenarios']['optimistic'])]
            run = SimulationRun.objects.create(user=request.user, company=company, source_snapshot=snapshot, result=result)
            result['simulation_id'] = str(run.pk)
        return Response(result)


class SimulationDetailView(generics.GenericAPIView):
    permission_classes = (IsAuthenticated,)

    def get(self, request, run_id):
        run = get_object_or_404(SimulationRun, pk=run_id, user=request.user)
        return Response(run.result | {'simulation_id': str(run.pk)})
