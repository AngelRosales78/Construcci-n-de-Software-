from django.core.management.base import BaseCommand
from api.models import SubscriptionTransaction
from api.subscription_views import deliver_receipt


class Command(BaseCommand):
    help = 'Reintenta copias de comprobantes simulados con correo pendiente.'

    def handle(self, *args, **options):
        pending = SubscriptionTransaction.objects.filter(outcome='approved', email_status='pending')
        for identifier in pending.values_list('id', flat=True).iterator():
            deliver_receipt(identifier)
        self.stdout.write('Se procesaron los comprobantes pendientes.')
