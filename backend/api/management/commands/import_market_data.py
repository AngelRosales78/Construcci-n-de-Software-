"""Importación explícita de datos; no descarga ni genera cifras financieras."""
import csv
from datetime import date
from decimal import Decimal, InvalidOperation
from pathlib import Path
from django.core.management.base import BaseCommand, CommandError
from django.core.exceptions import ValidationError
from django.core.validators import URLValidator
from django.db import transaction
from django.db.models import DecimalField
from django.utils import timezone
from api.models import Company, FinancialMetrics, PriceHistory

class Command(BaseCommand):
    help = 'Importar balances y cierres ajustados CSV con fuente y fecha documentadas'
    def add_arguments(self, parser):
        parser.add_argument('--metrics')
        parser.add_argument('--prices')
        parser.add_argument('--source', required=True)
        parser.add_argument('--demo', action='store_true')
    def handle(self, *args, **options):
        source=options['source'].strip()
        if not source or len(source)>250 or not (options['metrics'] or options['prices']):
            raise CommandError('Se requieren una fuente y al menos un archivo CSV.')
        companies={c.ticker:c for c in Company.objects.all()}
        metrics=[];prices=[];logos={};seen=set();currencies={}
        for kind in ('metrics','prices'):
            if not options[kind]:continue
            try:
                with Path(options[kind]).open(encoding='utf-8-sig', newline='') as stream:
                    reader=csv.DictReader(stream)
                    required={'ticker','as_of'} if kind=='metrics' else {'ticker','date','adjusted_close'}
                    if not required.issubset(reader.fieldnames or []):raise ValueError(f'Faltan columnas: {required}')
                    for number,row in enumerate(reader,start=2):
                        ticker=row['ticker'].strip().upper()
                        if ticker not in companies:raise ValueError(f'Ticker desconocido: {ticker}')
                        company=companies[ticker]
                        raw_date=row['as_of'] if kind=='metrics' else row['date']
                        when=date.fromisoformat(raw_date.strip())
                        if when>timezone.localdate():raise ValueError('No se admiten fechas futuras.')
                        identity=(kind,ticker,when if kind=='prices' else None)
                        if identity in seen:raise ValueError('Hay filas duplicadas en el archivo.')
                        seen.add(identity)
                        data={'company':company,'source':source,'is_demo':options['demo']}
                        if kind=='metrics':
                            data['as_of']=when
                            for field in FinancialMetrics._meta.fields:
                                if isinstance(field, DecimalField):
                                    raw=(row.get(field.name) or '').strip()
                                    data[field.name]=Decimal(raw) if raw else None
                            old=getattr(company,'financial_metrics',None)
                            if old and when<old.as_of:raise ValueError('El balance importado es anterior al guardado.')
                            item=FinancialMetrics(**data)
                            if row.get('logo_url','').strip():
                                URLValidator(schemes=['https'])(row['logo_url'].strip())
                                logos[ticker]=row['logo_url'].strip()
                            metrics.append(item)
                        else:
                            currency=(row.get('currency') or 'USD').strip().upper()
                            if currency not in ('USD','EUR','GBP','CAD','JPY','CHF','AUD','CNY','PEN'):
                                raise ValueError('Moneda no admitida.')
                            if ticker in currencies and currencies[ticker]!=currency:raise ValueError('No se mezclan monedas por ticker.')
                            currencies[ticker]=currency
                            raw_volume=(row.get('volume') or '').strip()
                            item=PriceHistory(**data,date=when,adjusted_close=Decimal(row['adjusted_close']),
                                volume=int(raw_volume) if raw_volume else None,currency=currency)
                            prices.append(item)
                        item.full_clean(exclude=['company'],validate_unique=False,validate_constraints=False)
            except (OSError,ValueError,InvalidOperation,ValidationError) as exc:
                raise CommandError(f'CSV {kind} inválido: {exc}') from exc
        with transaction.atomic():
            for item in metrics:
                values={f.name:getattr(item,f.name) for f in item._meta.fields if f.name not in ('id','company')}
                FinancialMetrics.objects.update_or_create(company=item.company,defaults=values)
                searchable = {'per': item.per, 'roe': item.roe}
                if item.market_cap is not None: searchable['market_cap'] = item.market_cap
                Company.objects.filter(pk=item.company.pk).update(**searchable)
            for ticker,currency in currencies.items():
                if PriceHistory.objects.filter(company=companies[ticker]).exclude(currency=currency).exists():
                    raise CommandError('La moneda no coincide con el historial existente.')
            PriceHistory.objects.bulk_create(prices,update_conflicts=True,
                update_fields=['adjusted_close','volume','currency','source','is_demo'],unique_fields=['company','date'])
            for ticker in currencies:
                company=companies[ticker]
                company.latest_price=PriceHistory.objects.filter(company=company).order_by('-date').first()
                company.save(update_fields=['latest_price'])
            for ticker,logo in logos.items():
                Company.objects.filter(pk=companies[ticker].pk).update(logo_url=logo)
        self.stdout.write(self.style.SUCCESS(f'Se importaron {len(metrics)} balances y {len(prices)} precios.'))
