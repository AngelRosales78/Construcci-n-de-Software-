import json
from django.core.management.base import BaseCommand, CommandError
from api.payments import setup_status, verify_seller, PaymentUnavailable

class Command(BaseCommand):
    help='Comprobar configuración de Mercado Pago de prueba sin crear pagos ni imprimir secretos.'
    requires_system_checks=[]
    def add_arguments(self,parser):
        parser.add_argument('--verify-seller',action='store_true')
    def handle(self,*args,**options):
        status=setup_status()
        if options['verify_seller']:
            try:
                verify_seller()
                status['seller_verified']=True
            except PaymentUnavailable as error:
                raise CommandError(str(error.detail)) from None
        self.stdout.write(json.dumps(status,ensure_ascii=False))
