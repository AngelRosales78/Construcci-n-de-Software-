from django.conf import settings
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.utils import timezone
from api.models import Company, Notification, RatingChangeEvent, User


class Command(BaseCommand):
    help='Crear un aviso claramente identificado de prueba, sin alterar balances ni enviar mensajes.'

    def add_arguments(self,parser):
        parser.add_argument('--username',required=True)

    def handle(self,*args,**options):
        if settings.DATABASES['default']['NAME']!='sprint3_demo_validation' or options['username'] not in ('sprint4_admin','sprint3_free','sprint3_pro'):
            raise CommandError('Solo se admiten las cuentas de ensayo de la base aislada.')
        user=User.objects.get(username=options['username'])
        company=Company.objects.get(ticker='MSFT')
        base={'as_of':'2024-06-30','methodology':'altman-z-original-v1',
              'source':'Escenario ilustrativo. No representa un cambio real de Microsoft.','score':None}
        with transaction.atomic():
            event,_=RatingChangeEvent.objects.get_or_create(company=company,revision=1,is_demo=True,
                defaults={'before':dict(base,category='strong',label='Muy sólido'),
                          'after':dict(base,category='moderate',label='Moderado'),'processed_at':timezone.now()})
            Notification.objects.get_or_create(user=user,event=event)
        self.stdout.write('Aviso de ejemplo preparado solo para la cuenta de ensayo. No se cambiaron balances ni se enviaron correos o Push.')
