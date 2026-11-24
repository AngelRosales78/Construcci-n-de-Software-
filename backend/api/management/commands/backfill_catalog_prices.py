"""Download each catalog company's historical prices using the audited provider flow."""
import gzip
import hashlib
import json
import time
import uuid
from datetime import timezone as datetime_timezone
from pathlib import Path
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.utils import timezone
from api.market_updates import execute, require_admin
from api.models import AdministrationEvent, Company, MarketDataOperation, ProviderDailyUsage, User
from api.market_provider import PROVIDER


class Command(BaseCommand):
    help = 'Ampliar históricos del catálogo con fuentes, cuotas, reanudación y copia reproducible.'

    def add_arguments(self, parser):
        parser.add_argument('--administrator', required=True)
        parser.add_argument('--batch-id', required=True)
        parser.add_argument('--snapshot', required=True)
        parser.add_argument('--report', required=True)
        parser.add_argument('--daily-limit', type=int)
        parser.add_argument('--pause', type=float, default=1.2)
        parser.add_argument('--max-companies', type=int, default=1000)

    def handle(self, *args, **options):
        if options['pause'] < 1 or options['max_companies'] < 1:
            raise CommandError('Se requiere una pausa de al menos un segundo y un límite positivo.')
        actor = User.objects.get(username=options['administrator'])
        with transaction.atomic():
            require_admin(actor)
            if options['daily_limit'] is not None:
                if not 1 <= options['daily_limit'] <= 1000:
                    raise CommandError('El límite explícito debe estar entre 1 y 1000 consultas.')
                usage, _ = ProviderDailyUsage.objects.select_for_update().get_or_create(
                    provider=PROVIDER, day=timezone.now().astimezone(datetime_timezone.utc).date(),
                    defaults={'limit': options['daily_limit']})
                old = usage.limit
                usage.limit = options['daily_limit']
                usage.save(update_fields=['limit'])
                AdministrationEvent.objects.create(actor=actor, action='catalog_backfill_budget',
                    changes={'batch_id': options['batch_id'], 'previous_limit': old, 'daily_limit': usage.limit})
        companies = list(Company.objects.order_by('-is_pilot', 'ticker')[:options['max_companies']])
        identifiers = [uuid.uuid5(uuid.NAMESPACE_URL, 'sp500/catalog-prices/' + options['batch_id'] + '/' + company.ticker) for company in companies]
        report = {'batch_id': options['batch_id'], 'started_at': timezone.now().isoformat(),
                  'total': len(companies), 'results': []}
        report_path = Path(options['report'])
        report_path.parent.mkdir(parents=True, exist_ok=True)
        consecutive_failures = 0
        try:
            for number, (company, request_id) in enumerate(zip(companies, identifiers), 1):
                previous = MarketDataOperation.objects.filter(actor=actor, request_id=request_id).first()
                if previous and previous.status in ('succeeded', 'failed'):
                    report['results'].append({'ticker': company.ticker, 'status': previous.status,
                        'message': previous.message, 'error_code': previous.error_code,
                        'price_count': previous.result.get('price_count'), 'as_of': previous.result.get('price_as_of')})
                    continue
                operation = execute(actor, request_id, company.ticker, 'refresh', company=company)
                report['results'].append({'ticker': company.ticker, 'status': operation.status,
                    'message': operation.message, 'error_code': operation.error_code,
                    'price_count': operation.result.get('price_count'), 'as_of': operation.result.get('price_as_of')})
                report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
                self.stdout.write(f'{number}/{len(companies)} {company.ticker}: {operation.status} {operation.result.get("price_count", "")} {operation.error_code}')
                self.stdout.flush()
                consecutive_failures = consecutive_failures + 1 if operation.status == 'failed' else 0
                if operation.error_code in ('provider_rate', 'internal_quota') or consecutive_failures >= 5:
                    self.stderr.write('Se detiene el lote para respetar el límite o la indisponibilidad de la fuente.')
                    break
                time.sleep(options['pause'])
        finally:
            records = []
            operations = MarketDataOperation.objects.select_related('company').filter(actor=actor, request_id__in=identifiers, status='succeeded').order_by('ticker')
            for operation in operations:
                raw = bytes(operation.raw_response)
                records.append({'ticker': operation.company.ticker, 'retrieved_at': operation.finished_at.isoformat(),
                    'source_url': operation.source_url, 'price_as_of': operation.result['price_as_of'],
                    'sha256': hashlib.sha256(raw).hexdigest(), 'raw_response': raw.decode('utf-8')})
            snapshot = {'version': 1, 'batch_id': options['batch_id'], 'prepared_at': timezone.now().isoformat(), 'companies': records}
            snapshot_path = Path(options['snapshot'])
            snapshot_path.parent.mkdir(parents=True, exist_ok=True)
            snapshot_path.write_bytes(gzip.compress(json.dumps(snapshot, ensure_ascii=False).encode(), mtime=0))
            report['finished_at'] = timezone.now().isoformat()
            report['downloaded'] = len(records)
            report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
            self.stdout.write(f'Históricos conservados en la copia: {len(records)} de {len(companies)} empresas.')
