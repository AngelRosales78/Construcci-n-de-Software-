"""Restaurar la cobertura empaquetada sin consultas externas ni procesos por cada archivo."""
import io
import json
from datetime import date
from decimal import Decimal
from pathlib import Path
from django.core.management import call_command
from django.core.management.base import BaseCommand,CommandError
from api.models import Company,FinancialMetrics

class Command(BaseCommand):
    def add_arguments(self,parser):parser.add_argument('directory')
    def handle(self,*args,**options):
        root=Path(options['directory'])
        sink=io.StringIO()
        for path in sorted(root.glob('*-annual.json')):
            call_command('import_annual_financials',str(path),stdout=sink)
        for path in sorted(root.glob('*-prices.csv')):
            symbol=path.stem.removesuffix('-prices')
            call_command('import_market_data',prices=str(path),source=f'https://finance.yahoo.com/quote/{symbol}/history/',stdout=sink)
        count=0
        for path in sorted(root.glob('*-metrics.json')):
            payload=json.loads(path.read_text(encoding='utf-8'))
            if 'values' not in payload:continue
            company=Company.objects.get(ticker=payload['ticker']);values=payload['values']
            allowed={field.name for field in FinancialMetrics._meta.fields}-{'id','company'}
            if set(values)-allowed:raise CommandError('Campos no admitidos en la ficha.')
            for field in FinancialMetrics._meta.fields:
                if field.get_internal_type()=='DecimalField' and values.get(field.name) is not None:
                    if isinstance(values[field.name],bool):raise CommandError('Valor monetario inválido.')
                    values[field.name]=Decimal(str(values[field.name]))
            values['as_of']=date.fromisoformat(values['as_of'])
            if values['as_of']>date.today() or values.get('is_demo') is not False:raise CommandError('Solo se admiten snapshots reales y no futuros.')
            instance=FinancialMetrics(company=company,**values)
            instance.full_clean(exclude=['company'],validate_unique=False,validate_constraints=False)
            FinancialMetrics.objects.update_or_create(company=company,defaults=values)
            Company.objects.filter(pk=company.pk).update(**{k:values.get(k) for k in ('market_cap','per','roe')})
            count+=1
        self.stdout.write(f'Paquete restaurado: {count} fichas con procedencia, balances e históricos.')
