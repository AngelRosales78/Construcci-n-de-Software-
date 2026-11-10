import time
from datetime import timedelta
from django.core.management.base import BaseCommand
from django.db import close_old_connections
from django.utils import timezone
from api.models import ReportJob
from api.report_views import process_report


class Command(BaseCommand):
    help = 'Procesar la cola persistente de reportes; no requiere otro broker.'

    def add_arguments(self, parser):
        parser.add_argument('--once', action='store_true')

    def handle(self, *args, **options):
        # Recupera trabajos abandonados, no los que aún procesa otro worker.
        ReportJob.objects.filter(status='running', updated_at__lt=timezone.now() - timedelta(minutes=5)).update(status='queued', progress=0)
        if options['once']:
            process_report()
            return
        self.stdout.write('Procesador de reportes iniciado.')
        try:
            while True:
                close_old_connections()
                if not process_report():
                    time.sleep(.25)
        except KeyboardInterrupt:
            self.stdout.write('Procesador detenido; la cola permanece en PostgreSQL.')
