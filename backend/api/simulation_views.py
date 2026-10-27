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
        result, snapshot = run_simulation(rows, serializer.validated_data['seed'])
        result['ticker'] = company.ticker
        if result['status'] == 'available':
            run = SimulationRun.objects.create(user=request.user, company=company, source_snapshot=snapshot, result=result)
            result['simulation_id'] = str(run.pk)
        return Response(result)
