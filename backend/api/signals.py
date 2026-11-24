"""HU12: cada resumen financiero confirmado se compara con el último perfil válido de la empresa."""
import logging
from django.db import transaction
from django.db.models.signals import post_save
from django.dispatch import receiver
from .models import Company, FinancialMetrics

logger = logging.getLogger(__name__)


def observe_company(company_id):
    from .notification_services import observe_rating
    try:
        with transaction.atomic():
            observe_rating(Company.objects.select_for_update().get(pk=company_id))
    except Exception as error:
        # Un aviso nunca revierte los datos financieros ya confirmados.
        logger.error('No se pudo evaluar el perfil de una empresa: %s', type(error).__name__)


@receiver(post_save, sender=FinancialMetrics)
def metrics_saved(sender, instance, raw=False, **kwargs):
    if not raw:
        transaction.on_commit(lambda: observe_company(instance.company_id), robust=True)
