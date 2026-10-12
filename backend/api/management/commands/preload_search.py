from django.core.management.base import BaseCommand
from api.models import Company
from api.search_cache import preload_autocomplete

class Command(BaseCommand):
    help = 'Precargar tickers y nombres en el índice Redis de autocompletado'
    def handle(self, *args, **options):
        count = preload_autocomplete(Company.objects.only('ticker', 'name').order_by('ticker'))
        self.stdout.write(self.style.SUCCESS(f'Se precargaron {count} entradas.'))
