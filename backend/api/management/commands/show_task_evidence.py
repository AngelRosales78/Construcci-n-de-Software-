from django.core.management.base import BaseCommand
from django.db import connection
from api.models import Company, FinancialMetrics, PriceHistory

class Command(BaseCommand):
    help = 'Mostrar evidencia de tablas e indices sin modificar datos'
    def handle(self, *args, **options):
        self.stdout.write(f'Motor: {connection.vendor}; base: {connection.settings_dict["NAME"]}')
        for model in (Company, FinancialMetrics, PriceHistory):
            self.stdout.write(f'{model._meta.db_table}: {model.objects.count()} filas')
        for ticker in ('MSFT', 'T000'):
            self.stdout.write(f'Precios {ticker}: {PriceHistory.objects.filter(company__ticker=ticker).count()}')
        if connection.vendor == 'postgresql':
            with connection.cursor() as cursor:
                cursor.execute("SELECT indexname,indexdef FROM pg_indexes WHERE tablename IN ('companies','price_history','search_history') ORDER BY tablename,indexname")
                for name, definition in cursor.fetchall():self.stdout.write(f'{name}: {definition}')
        else:
            self.stdout.write('Este motor no demuestra indices PostgreSQL.')
