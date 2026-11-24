import time
from django.core.management.base import BaseCommand
from django.db import close_old_connections
from api.notification_services import process_notification_event, seed_rating_states
from api.delivery_services import process_delivery


class Command(BaseCommand):
    help='Procesar eventos y entregas de avisos.'

    def add_arguments(self,parser):
        parser.add_argument('--once',action='store_true')

    def handle(self,*args,**options):
        seed_rating_states()
        if options['once']:
            process_notification_event(); process_delivery(); return
        self.stdout.write('Procesador de avisos iniciado.')
        try:
            while True:
                close_old_connections()
                event=process_notification_event()
                delivered=process_delivery()
                if not event and not delivered: time.sleep(2)
        except KeyboardInterrupt:
            self.stdout.write('Procesador detenido; los eventos permanecen en la base.')
