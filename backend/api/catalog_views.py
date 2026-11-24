"""HU14_01, HU14_02 y HU14_04: catálogo, cobertura y datos descriptivos."""
from django.db import transaction
from django.db.models import Exists, OuterRef, Prefetch, Q
from django.db.models.functions import Lower
from django.core.validators import URLValidator
from django.core.exceptions import ValidationError as DjangoValidationError
from django.shortcuts import get_object_or_404
from rest_framework import serializers
from rest_framework.exceptions import ValidationError
from rest_framework.pagination import PageNumberPagination
from rest_framework.response import Response
from rest_framework.views import APIView
from .models import AdministrationEvent, AnnualFinancialStatement, Company, MarketDataOperation, PriceHistory
from .market_views import add_company
from .market_updates import provider_usage, public_operation
from .user_management import IsPlatformAdministrator, lock_administration
from .catalog_services import PILOT_TICKERS, invalidate_catalog


def catalog_query():
    return Company.objects.select_related('financial_metrics', 'latest_price').prefetch_related(
        'annual_statements', Prefetch('market_updates', queryset=MarketDataOperation.objects.filter(kind='refresh')
            .defer('raw_response').order_by('-started_at','-id')[:1], to_attr='latest_update')).annotate(
        has_annual=Exists(AnnualFinancialStatement.objects.filter(company_id=OuterRef('pk'))),
        has_prices=Exists(PriceHistory.objects.filter(company_id=OuterRef('pk'))))


def company_data(company):
    summary = getattr(company, 'financial_metrics', None)
    statements = sorted(company.annual_statements.all(), key=lambda item: item.fiscal_year, reverse=True)
    summary_count = sum(getattr(summary, field, None) is not None
                        for field in ('market_cap', 'per', 'roe', 'beta', 'dividend_yield'))
    quote = company.latest_price
    sources = sorted({item.source_url for item in statements}
                     | ({summary.source} if summary else set())
                     | ({quote.source} if quote else set()))
    return {'id': company.pk, 'ticker': company.ticker, 'name': company.name,
            'sector': company.sector, 'sector_display': company.get_sector_display(),
            'is_active': company.is_active, 'is_pilot': company.is_pilot,
            'description': company.description, 'website_url': company.website_url,
            'is_index_member': company.is_index_member, 'identity_source_url': company.identity_source_url,
            'identity_verified_at': company.identity_verified_at,
            'last_update': public_operation(company.latest_update[0]) if company.latest_update else None,
            'updated_at': company.updated_at,
            'coverage': {'summary_fields': summary_count, 'summary_total': 5,
                         'summary_as_of': summary.as_of if summary else None,
                         'field_sources': summary.field_sources if summary else {},
                         'fiscal_years': [item.fiscal_year for item in statements],
                         'price_as_of': quote.date if quote else None,
                         'sources': sources,
                         'has_data': bool(summary_count or statements or company.has_prices)}}


class CatalogFilters(serializers.Serializer):
    q = serializers.CharField(max_length=120, allow_blank=True, default='')
    state = serializers.ChoiceField(choices=('all', 'active', 'inactive'), default='all')
    scope = serializers.ChoiceField(choices=('all', 'pilot'), default='all')
    coverage = serializers.ChoiceField(choices=('all', 'loaded', 'missing'), default='all')


class CompanyChange(serializers.Serializer):
    is_active = serializers.BooleanField(required=False)
    description = serializers.CharField(max_length=3000, allow_blank=True, required=False)
    sector = serializers.ChoiceField(choices=Company.SECTOR_CHOICES, required=False)
    website_url = serializers.URLField(max_length=500, allow_blank=True, required=False)

    def to_internal_value(self, data):
        if not isinstance(data, dict) or not data or set(data) - set(self.fields):
            raise ValidationError({'non_field_errors': ['Selecciona únicamente estado o datos descriptivos de la empresa.']})
        return super().to_internal_value(data)

    def validate_website_url(self, value):
        if value:
            try:
                URLValidator(schemes=['https'])(value)
            except DjangoValidationError as error:
                raise ValidationError('Utiliza una dirección HTTPS válida.') from error
        return value


class AdminCompaniesView(APIView):
    permission_classes = (IsPlatformAdministrator,)

    def get(self, request):
        form = CatalogFilters(data=request.query_params)
        form.is_valid(raise_exception=True)
        filters = form.validated_data
        companies = catalog_query().order_by(Lower('name'), 'ticker')
        if filters['q']:
            companies = companies.filter(Q(ticker__icontains=filters['q']) | Q(name__icontains=filters['q']))
        if filters['state'] != 'all':
            companies = companies.filter(is_active=filters['state'] == 'active')
        if filters['scope'] == 'pilot':
            companies = companies.filter(is_pilot=True)
        loaded = Q(has_annual=True) | Q(has_prices=True)
        for field in ('market_cap', 'per', 'roe', 'beta', 'dividend_yield'):
            loaded |= Q(**{f'financial_metrics__{field}__isnull': False})
        if filters['coverage'] == 'loaded':
            companies = companies.filter(loaded)
        elif filters['coverage'] == 'missing':
            companies = companies.exclude(loaded)
        page = PageNumberPagination()
        page.page_size = 20
        rows = page.paginate_queryset(companies, request, view=self)
        response = page.get_paginated_response([company_data(company) for company in rows])
        response.data['pilot'] = {'selected': Company.objects.filter(is_pilot=True).count(),
                                  'target': len(PILOT_TICKERS), 'selection_status': 'proposal',
                                  'tickers': list(PILOT_TICKERS)}
        response.data['market_provider'] = provider_usage()
        response['Cache-Control'] = 'private, no-store'
        return response

    def post(self, request):
        return add_company(request)


class AdminCompanyView(APIView):
    permission_classes = (IsPlatformAdministrator,)

    def patch(self, request, company_id):
        form = CompanyChange(data=request.data)
        form.is_valid(raise_exception=True)
        with transaction.atomic():
            lock_administration(request.user, request.user.pk)
            company = get_object_or_404(Company.objects.select_for_update(), pk=company_id)
            changes = {field: {'before': getattr(company, field), 'after': value}
                       for field, value in form.validated_data.items() if getattr(company, field) != value}
            if changes:
                for field, change in changes.items():
                    setattr(company, field, change['after'])
                if 'sector' in changes:
                    company.sector_overridden = True
                company.save(update_fields=[*changes, 'sector_overridden', 'updated_at'])
                AdministrationEvent.objects.create(actor=request.user, action='company_changed',
                    changes={'ticker': company.ticker, 'fields': changes})
                invalidate_catalog()
            result = company_data(catalog_query().get(pk=company.pk))
        return Response(result, headers={'Cache-Control': 'private, no-store'})
