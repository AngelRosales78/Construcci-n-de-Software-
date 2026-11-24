"""Completa el valor de mercado en la fecha exacta del balance (X4 de Altman) desde las valoraciones anuales guardadas."""
import json
from decimal import Decimal
from pathlib import Path
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from api.company_snapshots import balance_market_value
from api.models import FinancialMetrics


class Command(BaseCommand):
    help = 'Solo rellena equity_value cuando falta; no cambia ningún otro dato.'

    def add_arguments(self, parser):
        parser.add_argument('cache', help='Carpeta con los archivos TICKER-valuations.json')
        parser.add_argument('--no-export', action='store_true', help='No actualizar docs/datos/empresas')

    def handle(self, cache, no_export, **options):
        cache = Path(cache)
        if not cache.is_dir():
            raise CommandError('La caché indicada no existe.')
        target = Path(__file__).resolve().parents[4] / 'docs/datos/empresas'
        filled = missing = 0
        rows = FinancialMetrics.objects.select_related('company').filter(equity_value=None, is_demo=False)
        for metrics in rows.order_by('company__ticker'):
            ticker = metrics.company.ticker
            path = cache / f'{ticker}-valuations.json'
            value = balance_market_value(json.loads(path.read_bytes()), metrics.as_of) if path.exists() else None
            if value is None:
                missing += 1
                continue
            origin = {'as_of': metrics.as_of.isoformat(), 'source': f'https://finance.yahoo.com/quote/{ticker}/key-statistics/',
                      'method': 'annual market capitalization reported for the balance date'}
            with transaction.atomic():
                metrics.equity_value = value.quantize(Decimal('.01'))
                metrics.field_sources = metrics.field_sources | {'equity_value': origin}
                metrics.save(update_fields=['equity_value', 'field_sources'])
            exported = target / f'{ticker}-metrics.json'
            if not no_export and exported.exists():
                payload = json.loads(exported.read_text(encoding='utf-8'))
                if payload['values'].get('as_of') == metrics.as_of.isoformat():
                    payload['values']['equity_value'] = str(metrics.equity_value)
                    payload['values'].setdefault('field_sources', {})['equity_value'] = origin
                    exported.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding='utf-8')
            filled += 1
        self.stdout.write(f'{filled} valores de mercado completados; {missing} sin valoración en la fecha exacta del balance.')
