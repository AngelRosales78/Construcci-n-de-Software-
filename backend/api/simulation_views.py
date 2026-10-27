"""Escenarios reproducibles basados en los cierres ajustados disponibles."""
import calendar
import math
import random
import statistics
import uuid
from datetime import date
from django.utils import timezone
from rest_framework import generics, serializers
from rest_framework.exceptions import ValidationError
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from django.shortcuts import get_object_or_404
from .models import Company

class SimulationInput(serializers.Serializer):
    annual_return = serializers.FloatField(min_value=-0.8, max_value=1.5)
    annual_volatility = serializers.FloatField(min_value=0, max_value=2)
    seed = serializers.IntegerField(min_value=0, max_value=2**63-1)
    paths = serializers.IntegerField(min_value=100, max_value=10000)
    horizon_months = serializers.IntegerField(min_value=12, max_value=12)
    def validate(self, attrs):
        if not all(math.isfinite(attrs[k]) for k in ('annual_return', 'annual_volatility')):
            raise ValidationError('Los supuestos deben ser finitos.')
        return attrs

def price_inputs(company):
    rows = list(company.prices.filter(is_demo=False).order_by('-date')[:756])[::-1]
    if len(rows) < 60:
        raise ValidationError({'detail': 'Se requieren al menos 60 cierres ajustados reales. Carga el histórico de esta empresa.'})
    if len({r.currency for r in rows}) != 1 or any(r.adjusted_close <= 0 for r in rows):
        raise ValidationError({'detail': 'La serie de precios debe ser positiva y usar una sola moneda.'})
    logs = [math.log(float(b.adjusted_close / a.adjusted_close)) for a, b in zip(rows, rows[1:])]
    sigma = statistics.stdev(logs) * math.sqrt(252)
    mu = statistics.mean(logs) * 252 + sigma * sigma / 2
    return rows, mu, sigma

def month_after(start, offset):
    year = start.year + (start.month - 1 + offset) // 12
    month = (start.month - 1 + offset) % 12 + 1
    return date(year, month, min(start.day, calendar.monthrange(year, month)[1]))

def percentile(values, q):
    ordered = sorted(values)
    position = (len(ordered) - 1) * q
    low = math.floor(position)
    high = math.ceil(position)
    return ordered[low] + (ordered[high] - ordered[low]) * (position - low)

def simulate(company, rows, parameters):
    rng = random.Random(parameters['seed'])
    mu, sigma = parameters['annual_return'], parameters['annual_volatility']
    paths = [float(rows[-1].adjusted_close)] * parameters['paths']
    start = rows[-1].date
    points = []
    for month in range(13):
        if month:
            paths = [value * math.exp((mu - sigma*sigma/2)/12 + sigma/math.sqrt(12)*rng.gauss(0, 1)) for value in paths]
        points.append({'date': month_after(start, month).isoformat(),
                       **{label: round(percentile(paths, q), 6) for label, q in [('p10', .1), ('p50', .5), ('p90', .9)]}})
    return {'schema_version': 'simulation-v1', 'ticker': company.ticker,
            'simulation_id': str(uuid.uuid4()), 'currency': rows[-1].currency,
            'model_version': 'gbm-monthly-v1', 'generated_at': timezone.now().isoformat(),
            'start_date': start.isoformat(), 'end_date': month_after(start, 12).isoformat(),
            'horizon_months': 12, 'parameters': parameters, 'points': points,
            'history': [{'date': r.date.isoformat(), 'price': float(r.adjusted_close)} for r in rows[-60:]],
            'sources': sorted({r.source for r in rows}),
            'assumptions': 'GBM con supuestos anuales, 252 sesiones y pasos mensuales. No es una predicción ni garantiza resultados.'}

class SimulationDefaultsView(generics.GenericAPIView):
    permission_classes = (IsAuthenticated,)
    def get(self, request, ticker):
        company = get_object_or_404(Company, ticker=ticker.upper())
        rows, mu, sigma = price_inputs(company)
        return Response({'schema_version': 'simulation-defaults-v1', 'ticker': company.ticker,
                         'annual_return': round(max(-.8, min(1.5, mu)), 8),
                         'annual_volatility': round(min(2, sigma), 8),
                         'min_return': -.8, 'max_return': 1.5, 'min_volatility': 0, 'max_volatility': 2,
                         'seed': 42, 'paths': 2000, 'data_as_of': rows[-1].date.isoformat(),
                         'observations': len(rows), 'defaults_method': 'daily-log-returns-252-v1'})

class SimulationView(generics.GenericAPIView):
    permission_classes = (IsAuthenticated,)
    serializer_class = SimulationInput
    def post(self, request, ticker):
        company = get_object_or_404(Company, ticker=ticker.upper())
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        rows, _, _ = price_inputs(company)
        return Response(simulate(company, rows, dict(serializer.validated_data)))
