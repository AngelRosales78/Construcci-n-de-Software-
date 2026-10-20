"""Importar el inventario anual revisado sin cambiar balances ni precios previos."""
import json
import re
from datetime import date
from decimal import Decimal
from pathlib import Path
from django.core.exceptions import ValidationError
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from api.models import AnnualFinancialStatement, Company


class Command(BaseCommand):
    help = 'Importar componentes anuales SEC desde el inventario JSON con procedencia'

    def add_arguments(self, parser):
        parser.add_argument('inventory')
        parser.add_argument('--demo', action='store_true')

    def handle(self, *args, **options):
        try:
            payload = json.loads(Path(options['inventory']).read_text(encoding='utf-8'))
            company = Company.objects.get(ticker=payload['ticker'])
            # El inventario conserva la URL oficial; no consulta servicios externos.
            source = payload['source']
            if not re.fullmatch(r'https://data\.sec\.gov/api/xbrl/companyfacts/CIK\d{10}\.json', source):
                raise ValueError('Se esperaba una URL de procedencia SEC companyfacts.')
            rows = payload['annual_records']
            if not isinstance(rows, list) or not rows:
                raise ValueError('No hay ejercicios para importar.')
            prepared, seen = [], set()
            for row in rows:
                year = row['fiscal_year']
                if type(year) is not int or year in seen:
                    raise ValueError('Ejercicio inválido o duplicado.')
                seen.add(year)
                components = {}
                for name, field in row['values'].items():
                    value = field['value']
                    if type(field.get('available')) is not bool or field['available'] != (value is not None):
                        raise ValueError(f'Disponibilidad incoherente en {name}.')
                    if isinstance(value, bool):
                        raise ValueError('Booleano no monetario.')
                    components[name] = {'value': str(Decimal(str(value))) if value is not None else None,
                                        'tag': field['tag']}
                if row['unit'] != 'USD':
                    raise ValueError('Unidad monetaria no admitida.')
                statement = AnnualFinancialStatement(
                    company=company, fiscal_year=year,
                    period_start=date.fromisoformat(row['start']), period_end=date.fromisoformat(row['end']),
                    currency=row['unit'], source_url=source, accession=row['accession'],
                    filed=date.fromisoformat(row['filed']), is_demo=options['demo'],
                    snapshot_sha256=payload['snapshot_sha256'], components=components)
                statement.full_clean(validate_unique=False, validate_constraints=False)
                prepared.append(statement)
        except (OSError, KeyError, TypeError, ValueError, ArithmeticError, ValidationError,
                Company.DoesNotExist, AttributeError) as exc:
            raise CommandError(f'Inventario anual inválido: {exc}') from exc
        with transaction.atomic():
            for statement in prepared:
                values = {field.name: getattr(statement, field.name) for field in statement._meta.fields
                          if field.name not in ('id', 'company', 'fiscal_year')}
                AnnualFinancialStatement.objects.update_or_create(
                    company=company, fiscal_year=statement.fiscal_year, defaults=values)
        self.stdout.write(self.style.SUCCESS(f'{len(prepared)} ejercicios importados para {company.ticker}.'))
