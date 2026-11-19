"""Selección piloto y coherencia de cachés del catálogo administrativo."""
from django.core.cache import caches
from django.db import transaction
from django.db.models import F
from .models import CatalogRevision, Company

# Propuesta reproducible: la pertenencia al piloto no certifica cobertura financiera.
PILOT_TICKERS = ('MSFT', 'GOOGL', 'META', 'AAPL', 'AMZN', 'NVDA', 'AVGO', 'TSLA',
                 'JPM', 'V', 'JNJ', 'XOM', 'WMT', 'CAT', 'KO')


def catalog_revision():
    return CatalogRevision.objects.filter(pk=1).values_list('revision', flat=True).first() or 0


def invalidate_catalog():
    # Se invoca dentro de la misma transacción que modifica empresas.
    CatalogRevision.objects.get_or_create(pk=1)
    CatalogRevision.objects.filter(pk=1).update(revision=F('revision') + 1)
    transaction.on_commit(refresh_autocomplete, robust=True)


def refresh_autocomplete():
    from .search_cache import preload_autocomplete
    caches['search_results'].clear()
    preload_autocomplete(Company.objects.filter(is_active=True).only('ticker', 'name').order_by('ticker'))
