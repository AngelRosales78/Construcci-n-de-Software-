"""Importar resúmenes con fecha y procedencia independiente para cada indicador."""
import json
from datetime import date
from decimal import Decimal
from pathlib import Path
from django.core.exceptions import ValidationError
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.db.models import DecimalField
from api.models import Company, FinancialMetrics


class Command(BaseCommand):
    help = 'Importar una instantánea JSON de indicadores con procedencia por campo.'

    def add_arguments(self, parser):
        parser.add_argument('snapshot')

    def handle(self, *args, **options):
        try:
            payload = json.loads(Path(options['snapshot']).read_text(encoding='utf-8'))
            values = payload['values']
            allowed = {field.name for field in FinancialMetrics._meta.fields} - {'id', 'company'}
            if not isinstance(values, dict) or set(values) - allowed:
                raise ValueError('Campos no admitidos en el resumen.')
            data = dict(values, as_of=date.fromisoformat(values['as_of']))
            if type(data.get('is_demo')) is not bool:
                raise ValueError('Debe declararse si los datos son de demostración.')
            for field in FinancialMetrics._meta.fields:
                if isinstance(field, DecimalField):
                    value = data.get(field.name)
                    data[field.name] = Decimal(str(value)) if value is not None else None
            for field in ('market_cap', 'per', 'roe', 'beta', 'dividend_yield'):
                if data[field] is not None and field not in data.get('field_sources', {}):
                    raise ValueError(f'Falta la procedencia de {field}.')
            with transaction.atomic():
                company = Company.objects.select_for_update().get(ticker=payload['ticker'])
                previous = FinancialMetrics.objects.filter(company=company).first()
                if previous and previous.as_of > data['as_of']:
                    raise ValueError('El resumen importado es anterior al guardado.')
                candidate = FinancialMetrics(company=company, **data)
                candidate.full_clean(validate_unique=False, validate_constraints=False)
                FinancialMetrics.objects.update_or_create(company=company, defaults=data)
                Company.objects.filter(pk=company.pk).update(market_cap=candidate.market_cap,
                                                            per=candidate.per, roe=candidate.roe)
        except (OSError, KeyError, TypeError, ValueError, ArithmeticError, ValidationError, Company.DoesNotExist) as error:
            raise CommandError(f'Instantánea de indicadores inválida: {error}') from error
        self.stdout.write(f'Se importó el resumen de {company.ticker} con sus fuentes por indicador.')
