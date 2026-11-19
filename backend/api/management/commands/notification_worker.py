import time
from django.core.management.base import BaseCommand
from django.db import close_old_connections
from api.notification_services import process_notification_event, seed_rating_states


class Command(BaseCommand):
    help='Procesar eventos persistidos de perfil financiero para avisos web.'

    def add_arguments(self,parser):
        parser.add_argument('--once',action='store_true')

    def handle(self,*args,**options):
        seed_rating_states()
        if options['once']:
            process_notification_event(); return
        self.stdout.write('Procesador de avisos web iniciado. Correo y Push no habilitados.')
        try:
            while True:
                close_old_connections()
                if not process_notification_event(): time.sleep(.5)
        except KeyboardInterrupt:
            self.stdout.write('Procesador detenido; los eventos permanecen en PostgreSQL.')
