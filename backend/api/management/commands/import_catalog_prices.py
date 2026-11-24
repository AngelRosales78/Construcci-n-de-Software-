"""Restore authenticated public snapshots without network calls or replacing newer history."""
import gzip
import hashlib
import json
from datetime import date
from pathlib import Path
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from api.catalog_services import invalidate_catalog
from api.market_provider import parse_prices, SourceError, normalize_ticker, source_url
from api.market_updates import apply_prices
from api.models import Company, PriceHistory


class Command(BaseCommand):
    help = 'Importar la copia comprimida del catálogo; conservar históricos posteriores.'

    def add_arguments(self, parser):
        parser.add_argument('snapshot')

    def handle(self, *args, **options):
        try:
            with gzip.open(Path(options['snapshot']), 'rb') as stream:
                raw = stream.read(200_000_001)
            if len(raw) > 200_000_000:
                raise ValueError('La copia excede el tamaño permitido.')
            snapshot = json.loads(raw)
            if snapshot['version'] != 1 or not isinstance(snapshot['companies'], list):
                raise ValueError('Versión no admitida.')
            prepared, tickers = [], set()
            companies = {company.ticker: company for company in Company.objects.select_related('latest_price')}
            already_current = 0
            for entry in snapshot['companies']:
                if entry['ticker'] in tickers:
                    raise ValueError('Ticker duplicado en la copia.')
                tickers.add(entry['ticker'])
                raw_response = entry['raw_response'].encode('utf-8')
                if hashlib.sha256(raw_response).hexdigest() != entry['sha256']:
                    raise ValueError('La huella de un histórico no coincide.')
                chart = json.loads(raw_response)['chart']
                if chart.get('error') or len(chart['result']) != 1:
                    raise ValueError('Respuesta de precios no válida.')
                meta = chart['result'][0]['meta']
                if normalize_ticker(meta['symbol']) != normalize_ticker(entry['ticker']) or entry['source_url'] != source_url(entry['ticker']):
                    raise ValueError('La identidad o la fuente del histórico no coincide.')
                if meta['instrumentType'] != 'EQUITY' or meta['currency'] != 'USD' or meta['exchangeTimezoneName'] != 'America/New_York':
                    raise ValueError('Instrumento no admitido.')
                company = companies.get(entry['ticker'])
                # Verify each stored response's hash, but do not rebuild 600,000
                # validated model rows on every launch when newer data already exists.
                as_of = date.fromisoformat(entry['price_as_of']) if entry.get('price_as_of') else None
                if not company or (as_of and company.latest_price and company.latest_price.date >= as_of):
                    already_current += 1
                    continue
                payload = parse_prices(raw_response, entry['ticker'])
                if payload['source'] != entry['source_url']:
                    raise ValueError('La fuente no coincide con el ticker.')
                if as_of:
                    # Keep the capture's validated cutoff even if the provider's
                    # then-current session becomes eligible on a later startup.
                    payload['prices'] = [row for row in payload['prices'] if row['date'] <= as_of]
                    if not payload['prices'] or payload['prices'][-1]['date'] != as_of:
                        raise ValueError('La fecha de cierre de la copia no coincide con sus datos.')
                prepared.append((entry['ticker'], payload))
        except (OSError, ValueError, KeyError, TypeError, IndexError, AttributeError, SourceError) as error:
            raise CommandError(f'Copia de históricos inválida: {error}') from error
        imported, preserved = 0, already_current
        with transaction.atomic():
            for ticker, payload in prepared:
                company = Company.objects.select_for_update().filter(ticker=ticker).first()
                if not company:
                    preserved += 1
                    continue
                latest = PriceHistory.objects.filter(company=company).order_by('-date').first()
                if latest and latest.date >= payload['prices'][-1]['date']:
                    preserved += 1
                    continue
                apply_prices(company, payload)
                imported += 1
            if imported:
                invalidate_catalog()
        self.stdout.write(f'Históricos importados: {imported}; conservados o ajenos al catálogo: {preserved}.')
