from django.shortcuts import get_object_or_404
from rest_framework import serializers
from rest_framework.response import Response
from rest_framework.views import APIView
from .models import Company, MarketDataOperation
from .market_updates import create_verified_company, execute, provider_usage, public_operation
from .market_provider import SourceError
from .user_management import IsPlatformAdministrator


class ExactSerializer(serializers.Serializer):
    def to_internal_value(self, data):
        if not isinstance(data, dict) or set(data) - set(self.fields):
            raise serializers.ValidationError({'non_field_errors': ['La operación contiene campos no admitidos.']})
        return super().to_internal_value(data)


class RefreshRequest(ExactSerializer):
    request_id = serializers.UUIDField()


class VerifyRequest(RefreshRequest):
    ticker = serializers.RegexField(r'^[A-Za-z][A-Za-z0-9.-]{0,9}$', max_length=10)
    sector = serializers.ChoiceField(choices=Company.SECTOR_CHOICES)


class CreateRequest(ExactSerializer):
    verification_id = serializers.UUIDField()


def operation_response(operation, created=False):
    status = 202 if operation.status == 'running' else 201 if created else 200
    result = {'operation': public_operation(operation), 'provider': provider_usage()}
    if operation.status == 'failed':
        status = operation.result.get('http_status', 503)
        result['detail'] = operation.message
    return Response(result, status=status, headers={'Cache-Control': 'private, no-store'})


class RefreshCompanyView(APIView):
    permission_classes = (IsPlatformAdministrator,)

    def post(self, request, company_id):
        form = RefreshRequest(data=request.data); form.is_valid(raise_exception=True)
        company = get_object_or_404(Company, pk=company_id)
        return operation_response(execute(request.user, form.validated_data['request_id'], company.ticker,
                                          'refresh', company=company))


class VerifyCompanyView(APIView):
    permission_classes = (IsPlatformAdministrator,)

    def post(self, request):
        form = VerifyRequest(data=request.data); form.is_valid(raise_exception=True)
        return operation_response(execute(request.user, **form.validated_data, kind='verify'))


class MarketOperationView(APIView):
    permission_classes = (IsPlatformAdministrator,)

    def get(self, request, operation_id):
        operation = get_object_or_404(MarketDataOperation.objects.defer('raw_response'),
                                     pk=operation_id, actor=request.user)
        return operation_response(operation)


def add_company(request):
    form = CreateRequest(data=request.data); form.is_valid(raise_exception=True)
    try:
        operation, created = create_verified_company(request.user, **form.validated_data)
        return operation_response(operation, created)
    except SourceError as error:
        return Response({'detail': error.message, 'code': error.code}, status=error.status)
