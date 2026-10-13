"""Catálogo independiente de los balances: no inventa capitalizaciones."""
import csv
import re
from pathlib import Path
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from api.models import Company

SECTORS = {
    'Information Technology': 'technology', 'Health Care': 'healthcare',
    'Financials': 'financials', 'Consumer Discretionary': 'consumer_discretionary',
    'Consumer Staples': 'consumer_staples', 'Industrials': 'industrials',
    'Energy': 'energy', 'Materials': 'materials', 'Utilities': 'utilities',
    'Real Estate': 'real_estate', 'Communication Services': 'communication_services',
}

class Command(BaseCommand):
    help = 'Importar símbolos, nombres y sectores; conservar balances existentes'

    def add_arguments(self, parser):
        parser.add_argument('csv_file')

    def handle(self, *args, **options):
        entries = []
        seen = set()
        try:
            with Path(options['csv_file']).open(encoding='utf-8-sig', newline='') as stream:
                reader = csv.DictReader(stream)
                if not {'Symbol', 'Security', 'GICS Sector'}.issubset(reader.fieldnames or []):
                    raise ValueError('Faltan columnas del catálogo.')
                for row in reader:
                    ticker = row['Symbol'].strip().upper()
                    name = row['Security'].strip()
                    if not re.fullmatch(r'[A-Z][A-Z0-9.-]{0,9}', ticker) or ticker in seen:
                        raise ValueError(f'Símbolo inválido o duplicado: {ticker}')
                    if not name or len(name) > 200 or row['GICS Sector'] not in SECTORS:
                        raise ValueError(f'Nombre o sector inválido: {ticker}')
                    seen.add(ticker)
                    entries.append((ticker, name, SECTORS[row['GICS Sector']]))
            if not entries:
                raise ValueError('El catálogo está vacío.')
        except (OSError, ValueError, KeyError) as exc:
            raise CommandError(str(exc)) from exc
        with transaction.atomic():
            for ticker, name, sector in entries:
                Company.objects.update_or_create(ticker=ticker, defaults={'name': name, 'sector': sector})
        self.stdout.write(self.style.SUCCESS(f'Se importaron {len(entries)} símbolos sin sustituir balances.'))
