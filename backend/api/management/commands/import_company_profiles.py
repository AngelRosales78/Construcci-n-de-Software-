"""Import sourced company information, preserving later edits made by administrators."""
import json
import re
from pathlib import Path
from django.core.exceptions import ValidationError
from django.core.management.base import BaseCommand, CommandError
from django.core.validators import URLValidator
from django.db import transaction
from api.catalog_services import invalidate_catalog
from api.models import Company

DETAIL_FIELDS = {'industry', 'industry_label', 'headquarters', 'founded', 'cik', 'catalog_source_url',
    'catalog_snapshot_date', 'profile_source_url', 'retrieved_at', 'regulatory_url',
    'description_origin', 'description_source_url', 'website_source_url', 'identity_match'}


def read_profiles(path):
    try:
        payload = json.loads(Path(path).read_text(encoding='utf-8'))
        if payload['version'] != 1 or not isinstance(payload['companies'], list):
            raise ValueError('Versión o lista de empresas no admitida.')
        entries, seen = [], set()
        for row in payload['companies']:
            ticker, description, website, details = row['ticker'], row['description'], row['website_url'], row['details']
            if not isinstance(ticker, str) or not re.fullmatch(r'[A-Z][A-Z0-9.-]{0,9}', ticker) or ticker in seen:
                raise ValueError('Ticker duplicado o inválido.')
            seen.add(ticker)
            if not isinstance(description, str) or len(description) > 3000 or not isinstance(website, str) or len(website) > 500:
                raise ValueError('Descripción o sitio web inválido.')
            if not isinstance(details, dict) or set(details) - DETAIL_FIELDS:
                raise ValueError('Campos de perfil no admitidos.')
            if any(not isinstance(value, str) or len(value) > 500 for value in details.values()):
                raise ValueError('Los detalles deben ser textos de hasta 500 caracteres.')
            if not re.fullmatch(r'\d{10}', details.get('cik', '')):
                raise ValueError('CIK inválido.')
            for url in [website] + [value for key, value in details.items() if key.endswith('_url')]:
                if url: URLValidator(schemes=['https'])(url)
            entries.append((ticker, description, website, details))
        return entries
    except (OSError, ValueError, KeyError, TypeError, ValidationError) as error:
        raise CommandError(f'Perfil de empresa inválido: {error}') from error


class Command(BaseCommand):
    help = 'Cargar perfiles públicos sin sustituir textos editados, balances ni precios.'

    def add_arguments(self, parser):
        parser.add_argument('snapshot')

    def handle(self, *args, **options):
        entries = read_profiles(options['snapshot'])
        changed = skipped = 0
        with transaction.atomic():
            for ticker, description, website, details in entries:
                company = Company.objects.select_for_update().filter(ticker=ticker).first()
                if not company:
                    skipped += 1
                    continue
                previous = company.catalog_details
                previous_description, previous_website = company.description, company.website_url
                if ('imported_description' not in previous and not company.description) or company.description == previous.get('imported_description'):
                    company.description = description
                if ('imported_website' not in previous and not company.website_url) or company.website_url == previous.get('imported_website'):
                    company.website_url = website
                candidate_details = dict(details, imported_description=description, imported_website=website)
                if (company.catalog_details != candidate_details or company.description != previous_description
                        or company.website_url != previous_website):
                    company.catalog_details = candidate_details
                    company.full_clean(validate_unique=False, validate_constraints=False)
                    company.save(update_fields=['description', 'website_url', 'catalog_details', 'updated_at'])
                    changed += 1
            if changed: invalidate_catalog()
        self.stdout.write(f'Perfiles cargados: {changed}; ajenos al catálogo: {skipped}. Se conservaron precios, balances y ediciones propias.')
