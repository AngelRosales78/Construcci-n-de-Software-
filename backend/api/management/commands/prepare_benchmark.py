"""Fixtures reproducibles de rendimiento; se rechaza su ejecución en bases de entrega."""
import json
from pathlib import Path
from datetime import timedelta
from django.core.management.base import BaseCommand, CommandError
from django.db import connection
from django.utils import timezone
from api.models import Company, User, FinancialMetrics, PriceHistory
from api.search_cache import preload_autocomplete
from rest_framework_simplejwt.tokens import RefreshToken

class Command(BaseCommand):
    help = 'Preparar 500 empresas y 1200 precios sintéticos solo en una base de validación'
    def add_arguments(self, parser):
        parser.add_argument('--confirm-test-database', action='store_true')
        parser.add_argument('--access-file')
    def handle(self, *args, **options):
        name=connection.settings_dict['NAME']
        if not options['confirm_test_database'] or name not in ('sprint_validation','test_sprint_validation'):
            raise CommandError('Solo se admite una base sprint_validation explícitamente confirmada.')
        Company.objects.bulk_create([Company(ticker=f'T{i:03}',name=f'Empresa de prueba {i}',
            sector='technology',market_cap=1000000+i) for i in range(500)],ignore_conflicts=True)
        company=Company.objects.get(ticker='T000')
        FinancialMetrics.objects.update_or_create(company=company,defaults={
            'as_of':timezone.localdate(),'source':'Fixture de rendimiento','is_demo':True,
            'per':0,'roe':10,'beta':1.1,'dividend_yield':0,'market_cap':1000000,
            'total_assets':100,'total_liabilities':50,'working_capital':10,
            'retained_earnings':10,'ebit':10,'equity_value':100,'sales':200})
        PriceHistory.objects.bulk_create([PriceHistory(company=company,date=timezone.localdate()-timedelta(days=i),
            adjusted_close=100+i/100,source='Fixture de rendimiento',is_demo=True) for i in range(1200)],ignore_conflicts=True)
        company.latest_price=company.prices.order_by('-date').first();company.save(update_fields=['latest_price'])
        user,_=User.objects.get_or_create(username='benchmark')
        preload_autocomplete(Company.objects.all())
        if options['access_file']:
            Path(options['access_file']).write_text(json.dumps({'access':str(RefreshToken.for_user(user).access_token),
                'user':{'id':user.pk}}),encoding='utf-8')
        self.stdout.write('Se prepararon fixtures de validación; no son datos financieros reales.')
