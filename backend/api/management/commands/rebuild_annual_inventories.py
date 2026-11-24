"""Regenerar los inventarios anuales desde los companyfacts SEC ya descargados, sin consultas externas."""
import csv
import json
from pathlib import Path
from django.core.management import call_command
from django.core.management.base import BaseCommand, CommandError
from .sync_company_data import annual_inventory


class Command(BaseCommand):
    help = 'Reconstruir *-annual.json con las reglas actuales de etiquetas e importarlos, usando la caché local de la SEC.'

    def add_arguments(self, parser):
        parser.add_argument('--cache', required=True, help='Carpeta con <TICKER>-companyfacts.json')
        parser.add_argument('--tickers', default='', help='Opcional: símbolos separados por comas')
        parser.add_argument('--no-import', action='store_true', help='Solo regenerar los JSON')

    def handle(self, *args, **options):
        root = Path(__file__).resolve().parents[4]
        cache = Path(options['cache'])
        if not cache.is_dir():
            raise CommandError('La caché indicada no existe.')
        packaged = root / 'docs' / 'datos' / 'empresas'
        with (root / 'docs/datos/catalogo_sp500.csv').open(encoding='utf-8-sig') as stream:
            catalog = {r['Symbol']: r for r in csv.DictReader(stream)}
        wanted = [t.strip().upper() for t in options['tickers'].split(',') if t.strip()] or sorted(catalog)
        done, skipped = 0, []
        for ticker in wanted:
            source = cache / f'{ticker}-companyfacts.json'
            if ticker not in catalog or not source.exists():
                skipped.append(ticker); continue
            raw = source.read_bytes()
            inventory = annual_inventory(json.loads(raw), ticker, int(catalog[ticker]['CIK']), raw)
            if not inventory['annual_records']:
                skipped.append(ticker); continue
            target = packaged / f'{ticker}-annual.json'
            previous = json.loads(target.read_text(encoding='utf-8')) if target.exists() else {}
            for key in ('reporting_note', 'entity_relationship_source'):
                if key in previous:
                    inventory[key] = previous[key]
            target.write_text(json.dumps(inventory, ensure_ascii=False, indent=2), encoding='utf-8')
            if not options['no_import']:
                call_command('import_annual_financials', str(target), stdout=open(__import__('os').devnull, 'w'))
            done += 1
        self.stdout.write(self.style.SUCCESS(f'{done} inventarios regenerados.' + (f' Omitidos: {", ".join(skipped)}' if skipped else '')))
