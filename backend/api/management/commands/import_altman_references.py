"""Import reviewed primary-report inputs and verify their historical quote."""
import gzip
import hashlib
import json
from datetime import date, datetime
from decimal import Decimal
from pathlib import Path
from zoneinfo import ZoneInfo
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from api.models import FinancialMetrics
from api.altman_reference import validate_reference

class Command(BaseCommand):
    help = 'Cargar referencias Altman verificando informes revisados y cierres originales.'
    def add_arguments(self, parser):
        parser.add_argument('snapshot')
        parser.add_argument('--prices', required=True)
        parser.add_argument('--dry-run', action='store_true')
    def handle(self, *args, **options):
        try:
            path = Path(options['snapshot'])
            captured = path.read_bytes()
            payload = json.loads(gzip.decompress(captured) if path.suffix == '.gz' else captured)
            raw_prices = Path(options['prices']).read_bytes()
            if payload['version'] != 1 or hashlib.sha256(raw_prices).hexdigest() != payload['price_snapshot_sha256']:
                raise ValueError('Copia de precios no correspondiente.')
            entries = {row['ticker']:row for row in json.loads(gzip.decompress(raw_prices))['companies']}
            prepared, seen = [], set()
            for row in payload['companies']:
                ticker, reference = row['ticker'], row['reference']
                if ticker in seen: raise ValueError('Empresa repetida.')
                seen.add(ticker)
                validate_reference(reference)
                if reference.get('sec_evidence', {}).get('ticker', ticker) != ticker:
                    raise ValueError('Los hechos SEC pertenecen a otro símbolo.')
                entry = entries[ticker]; raw = entry['raw_response'].encode()
                if hashlib.sha256(raw).hexdigest() != entry['sha256'] or reference['price_sha256'] != entry['sha256']:
                    raise ValueError('Huella de cotización no válida.')
                chart = json.loads(raw)['chart']['result'][0]
                if chart['meta']['symbol'].replace('-','.') != ticker or chart['meta']['currency'] != 'USD':
                    raise ValueError('Símbolo o moneda incorrectos.')
                end = date.fromisoformat(reference['period_end'])
                points = [(datetime.fromtimestamp(t,ZoneInfo('America/New_York')).date(),Decimal(str(p)))
                          for t,p in zip(chart['timestamp'],chart['indicators']['quote'][0]['close']) if p is not None]
                day, close = max((d,p) for d,p in points if d <= end)
                if day.isoformat() != reference['price_date'] or close != Decimal(reference['unadjusted_close']):
                    raise ValueError('No coincide el último cierre previo al cierre contable.')
                if any(datetime.fromtimestamp(e['date'],ZoneInfo('America/New_York')).date() > end
                       for e in chart.get('events',{}).get('splits',{}).values()):
                    raise ValueError('Desdoblamiento posterior: requiere base por acción conciliada.')
                summary = FinancialMetrics.objects.get(company__ticker=ticker)
                if summary.as_of.year != end.year:
                    raise ValueError('La referencia no corresponde al año del resumen.')
                prepared.append((summary.pk,reference))
        except (OSError, ValueError, KeyError, TypeError, ArithmeticError, FinancialMetrics.DoesNotExist) as error:
            raise CommandError(f'Referencia Altman inválida: {error}') from error
        changed = 0
        if not options['dry_run']:
            with transaction.atomic():
                for pk,reference in prepared:
                    summary = FinancialMetrics.objects.select_for_update().get(pk=pk)
                    if summary.altman_reference and summary.altman_reference['period_end'] > reference['period_end']:
                        continue
                    if summary.altman_reference != reference:
                        summary.altman_reference = reference
                        summary.save(update_fields=['altman_reference']); changed += 1
        self.stdout.write(json.dumps({'validated':len(prepared),'updated':changed,'dry_run':options['dry_run']}))
