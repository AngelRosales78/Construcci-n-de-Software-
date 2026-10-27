from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import generics, serializers
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.throttling import UserRateThrottle
from .models import Company, SimulationRun
from .simulations import run_simulation


class SimulationInput(serializers.Serializer):
    seed = serializers.IntegerField(min_value=0, max_value=2147483647, default=2026)
    annual_return = serializers.FloatField(min_value=-1, max_value=1.5, required=False)
    annual_volatility = serializers.FloatField(min_value=0, max_value=2, required=False)

    def to_internal_value(self, data):
        if not isinstance(data, dict) or set(data) - set(self.fields):
            raise serializers.ValidationError('Use solo semilla, tasa anual y volatilidad admitidas.')
        if any(isinstance(value, bool) for value in data.values()):
            raise serializers.ValidationError('Los parámetros deben ser números, no booleanos.')
        return super().to_internal_value(data)

    def validate(self, attrs):
        import math
        if any(not math.isfinite(value) for value in attrs.values()):
            raise serializers.ValidationError('Los parámetros deben ser números finitos.')
        return attrs


class SimulationThrottle(UserRateThrottle):
    rate = '12/min'


class CompanySimulationView(generics.GenericAPIView):
    permission_classes = (IsAuthenticated,)
    throttle_classes = (SimulationThrottle,)

    def post(self, request, ticker):
        company = get_object_or_404(Company, ticker=ticker.upper())
        serializer = SimulationInput(data=request.data)
        serializer.is_valid(raise_exception=True)
        rows = list(company.prices.filter(date__lte=timezone.localdate())
                    .order_by('-date').values('date', 'adjusted_close', 'currency', 'source', 'is_demo')[:1261])[::-1]
        params = {key: value for key, value in serializer.validated_data.items() if key != 'seed'}
        result, snapshot = run_simulation(rows, serializer.validated_data['seed'], params)
        result['ticker'] = company.ticker
        if result['status'] == 'available':
            result['schema_version'] = 'simulation-v1'
            result['final_prices'] = {key: {'date': curve[-1]['date'], 'price': curve[-1]['price'],
                                           'percentile': result['percentiles'][key]}
                                      for key, curve in result['scenarios'].items()}
            result['parameter_limits'] = {'annual_return': [-1, 1.5], 'annual_volatility': [0, 2]}
            run = SimulationRun.objects.create(user=request.user, company=company, source_snapshot=snapshot, result=result)
            result['simulation_id'] = str(run.pk)
        return Response(result)


class SimulationDetailView(generics.GenericAPIView):
    permission_classes = (IsAuthenticated,)

    def get(self, request, run_id):
        run = get_object_or_404(SimulationRun, pk=run_id, user=request.user)
        return Response(run.result | {'simulation_id': str(run.pk)})
