"""Restore annual provider references without replacing verified SEC inventories."""
import gzip
import hashlib
import json
from datetime import date
from pathlib import Path
from django.core.exceptions import ValidationError
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.utils import timezone
from api.models import AnnualFinancialStatement, Company, FinancialMetrics
from api.catalog_services import invalidate_catalog
from api.provider_financials import parse_capture, chart_capture, summary_values


def read_gzip(path):
    with gzip.open(path, 'rb') as stream: raw = stream.read(200_000_001)
    if len(raw) > 200_000_000: raise ValueError('La copia excede el tamaño permitido.')
    payload = json.loads(raw)
    if payload['version'] != 1: raise ValueError('Versión no admitida.')
    return payload


class Command(BaseCommand):
    help = 'Cargar referencias anuales, ratios calculados y fuentes; conservar datos SEC y valores ausentes.'

    def add_arguments(self, parser):
        parser.add_argument('snapshot')
        parser.add_argument('--prices', required=True)
        parser.add_argument('--report')
        parser.add_argument('--dry-run', action='store_true')

    def handle(self, *args, **options):
        try:
            source = Path(options['snapshot']); prices_path = Path(options['prices'])
            snapshot_sha = hashlib.sha256(source.read_bytes()).hexdigest()
            bundle = read_gzip(source)
            if bundle.get('calculation_version', 'provider-financials-v1') != 'provider-financials-v1':
                raise ValueError('Versión de cálculos financieros no admitida.')
            if hashlib.sha256(prices_path.read_bytes()).hexdigest() != bundle['price_snapshot_sha256']:
                raise ValueError('La copia de precios no corresponde a la preparación financiera.')
            prices = {entry['ticker']: entry for entry in read_gzip(prices_path)['companies']}
            benchmark = bundle['benchmark']
            benchmark_raw = benchmark['raw_response'].encode('utf-8')
            if hashlib.sha256(benchmark_raw).hexdigest() != benchmark['evidence']['sha256']:
                raise ValueError('La huella del índice no coincide.')
            benchmark_rows, _ = chart_capture('^GSPC', benchmark_raw, timezone.localdate(), benchmark=True)
            seen = set()
            prepared = []
            # All identities, hashes, annual rows and summary bounds are checked
            # before the first database write, including the dry-run path.
            companies = {company.ticker: company for company in Company.objects.select_related('financial_metrics')}
            current_statements = {(row.company_id, row.fiscal_year): row for row in AnnualFinancialStatement.objects.all()}
            counters = {'companies': 0, 'annual_rows': 0, 'summaries': 0, 'preserved_sec_rows': 0, 'preserved_summaries': 0,
                        'already_current': 0, 'indicators': {field: 0 for field in ('market_cap','per','roe','beta','dividend_yield')}}
            for entry in bundle['companies']:
                ticker = entry['ticker']
                if ticker in seen: raise ValueError('Ticker financiero repetido.')
                seen.add(ticker)
                records, values = parse_capture(ticker, entry['raw_response'].encode('utf-8'), entry['evidence'], timezone.localdate())
                company = companies.get(ticker)
                if not company: continue
                counters['companies'] += 1
                statements = []
                for record in records:
                    candidate = AnnualFinancialStatement(company=company, **record)
                    candidate.full_clean(validate_unique=False, validate_constraints=False)
                    old = current_statements.get((company.pk, candidate.fiscal_year))
                    if old and old.source_kind == 'sec': counters['preserved_sec_rows'] += 1; continue
                    if old and old.snapshot_sha256 == candidate.snapshot_sha256: continue
                    if old and old.provenance.get('retrieved_at', '') > candidate.provenance['retrieved_at']: continue
                    statements.append(candidate)
                summary = getattr(company, 'financial_metrics', None)
                if summary and summary.reference_basis == 'reported':
                    counters['preserved_summaries'] += 1; summary_candidate = None
                elif summary and summary.source_snapshot_sha256 == snapshot_sha:
                    counters['already_current'] += 1; summary_candidate = None
                else:
                    chart_evidence = prices.get(ticker)
                    chart_rows, dividends = {}, []
                    if chart_evidence:
                        raw = chart_evidence['raw_response'].encode('utf-8')
                        if hashlib.sha256(raw).hexdigest() != chart_evidence['sha256']: raise ValueError('Huella de precios inválida.')
                        chart_rows, dividends = chart_capture(ticker, raw, timezone.localdate(),
                            price_as_of=date.fromisoformat(chart_evidence['price_as_of']))
                    data = summary_values(ticker, records, values, chart_rows, dividends, benchmark_rows,
                        entry['evidence'], chart_evidence or {}, benchmark['evidence'])
                    summary_candidate = FinancialMetrics(company=company, source_snapshot_sha256=snapshot_sha, **data) if data else None
                    if summary_candidate: summary_candidate.full_clean(validate_unique=False, validate_constraints=False)
                counters['annual_rows'] += len(statements)
                if summary_candidate:
                    counters['summaries'] += 1
                    for field in counters['indicators']: counters['indicators'][field] += getattr(summary_candidate, field) is not None
                prepared.append((company, statements, summary_candidate))
        except (OSError, ValueError, KeyError, TypeError, IndexError, AttributeError, ArithmeticError, ValidationError) as error:
            raise CommandError(f'Copia financiera inválida: {error}') from error
        if not options['dry_run']:
            with transaction.atomic():
                for company, statements, summary in prepared:
                    Company.objects.select_for_update().get(pk=company.pk)
                    for statement in statements:
                        # Recheck preferred provenance under the write lock.
                        existing = AnnualFinancialStatement.objects.filter(company=company, fiscal_year=statement.fiscal_year).first()
                        if existing and existing.source_kind == 'sec': continue
                        values = {field.name: getattr(statement, field.name) for field in statement._meta.fields if field.name not in ('id','company','fiscal_year')}
                        AnnualFinancialStatement.objects.update_or_create(company=company, fiscal_year=statement.fiscal_year, defaults=values)
                    if summary:
                        existing = FinancialMetrics.objects.filter(company=company).first()
                        if existing and existing.reference_basis == 'reported': continue
                        fields = {field.name: getattr(summary, field.name) for field in summary._meta.fields if field.name not in ('id','company','altman_reference')}
                        FinancialMetrics.objects.update_or_create(company=company, defaults=fields)
                        Company.objects.filter(pk=company.pk).update(market_cap=summary.market_cap, per=summary.per, roe=summary.roe)
                if counters['annual_rows'] or counters['summaries']: invalidate_catalog()
        report = {'verified_at': timezone.now().isoformat(), 'dry_run': options['dry_run'], 'snapshot_sha256': snapshot_sha, **counters}
        if options['report']:
            path = Path(options['report']); path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
        self.stdout.write(json.dumps(report, ensure_ascii=False))
