"""Capture public annual fundamentals before importing or calculating financial data."""
import json
import hashlib
import time
from pathlib import Path
from datetime import datetime, timezone as dt_timezone
from urllib.parse import urlencode, quote
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.utils import timezone
from api.models import Company, User, ProviderDailyUsage, ProviderBudget, AdministrationEvent
from api.market_updates import reserve_call, require_admin
from api.market_provider import SourceError

FIELDS = ['annualTotalAssets', 'annualCurrentAssets', 'annualCurrentLiabilities', 'annualStockholdersEquity',
    'annualTotalRevenue', 'annualNetIncome', 'annualRetainedEarnings', 'annualInterestExpense', 'annualPretaxIncome',
    'annualCashAndCashEquivalents', 'annualOtherShortTermInvestments', 'annualAccountsReceivable', 'annualReceivables',
    'annualInventory', 'annualCostOfRevenue', 'annualTotalLiabilitiesNetMinorityInterest', 'annualOrdinarySharesNumber',
    'annualCashCashEquivalentsAndShortTermInvestments', 'annualEBIT', 'trailingMarketCap', 'trailingPeRatio']


class Command(BaseCommand):
    help = 'Guardar fuentes anuales con hash, pausas y cuotas; no inventa fechas ni balances.'

    def add_arguments(self, parser):
        parser.add_argument('--administrator', required=True)
        parser.add_argument('--directory', required=True)
        parser.add_argument('--daily-limit', type=int)
        parser.add_argument('--max-companies', type=int, default=1000)

    def handle(self, *args, **options):
        actor = User.objects.get(username=options['administrator'])
        with transaction.atomic():
            require_admin(actor)
            if options['daily_limit']:
                limit = options['daily_limit']
                if not 1 <= limit <= 2000: raise CommandError('Límite explícito fuera de rango.')
                usage, _ = ProviderDailyUsage.objects.select_for_update().get_or_create(provider='yahoo_chart',
                    day=timezone.now().astimezone(dt_timezone.utc).date(), defaults={'limit': limit})
                budget, _ = ProviderBudget.objects.select_for_update().get_or_create(provider='yahoo_chart')
                AdministrationEvent.objects.create(actor=actor, action='financial_capture_budget', changes={
                    'previous_market_limit': usage.limit, 'previous_daily_budget': budget.daily_limit, 'new_limit': limit})
                usage.limit = limit; usage.save(update_fields=['limit'])
                budget.daily_limit = limit; budget.version += 1; budget.save(update_fields=['daily_limit','version'])
        directory = Path(options['directory']); directory.mkdir(parents=True, exist_ok=True)
        companies = list(Company.objects.order_by('-is_pilot', 'ticker')[:options['max_companies']])
        manifest_file = directory / 'manifest.json'
        manifest = json.loads(manifest_file.read_text(encoding='utf-8')) if manifest_file.exists() else {'version': 1, 'companies': {}}
        failures = 0
        for number, company in enumerate(companies, 1):
            target = directory / (company.ticker + '.json')
            existing = manifest['companies'].get(company.ticker, {})
            if target.exists() and existing.get('sha256') == hashlib.sha256(target.read_bytes()).hexdigest():
                continue
            url = 'https://query1.finance.yahoo.com/ws/fundamentals-timeseries/v1/finance/timeseries/' + quote(company.ticker.replace('.', '-')) + '?' + urlencode({
                'type': ','.join(FIELDS), 'period1': '1577836800', 'period2': str(int(timezone.now().timestamp()))})
            stop = False
            try:
                reserve_call()
                request = Request(url, headers={'User-Agent': 'SP500Academic/1.0 (https://github.com/AngelRosales78/Construcci-n-de-Software-/issues)', 'Accept': 'application/json'})
                with urlopen(request, timeout=15) as response:
                    raw = response.read(2_000_001)
                if len(raw) > 2_000_000: raise ValueError('Respuesta demasiado grande.')
                data = json.loads(raw)
                if data.get('timeseries', {}).get('error') or not isinstance(data.get('timeseries', {}).get('result'), list):
                    raise ValueError('Respuesta sin series válidas.')
                target.write_bytes(raw)
                manifest['companies'][company.ticker] = {'status': 'downloaded', 'url': url,
                    'retrieved_at': timezone.now().isoformat(), 'sha256': hashlib.sha256(raw).hexdigest()}
                failures = 0
            except (HTTPError, URLError, OSError, ValueError, SourceError) as error:
                code = getattr(error, 'code', None)
                manifest['companies'][company.ticker] = {'status': 'unavailable', 'url': url,
                    'retrieved_at': timezone.now().isoformat(), 'http_status': code if isinstance(code, int) else None}
                failures += 1
                stop = code == 429 or code in ('internal_quota', 'provider_rate') or failures >= 5
            manifest_file.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
            self.stdout.write(f'{number}/{len(companies)} {company.ticker}: {manifest["companies"][company.ticker]["status"]}')
            self.stdout.flush()
            if stop:
                self.stderr.write('Lote detenido por límite o indisponibilidad; se conservan las capturas anteriores.')
                break
            time.sleep(1.2)
        self.stdout.write(f'Capturas disponibles: {sum(row.get("status") == "downloaded" for row in manifest["companies"].values())}.')
