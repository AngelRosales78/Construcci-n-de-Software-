"""Cambios observados y distribución por los canales elegidos por cada cuenta."""
from django.db import IntegrityError, transaction
from django.db.models import Max
from django.utils import timezone
from .models import Company, CompanyRatingState, Favorite, FinancialMetrics, Notification, RatingChangeEvent
from .delivery_services import queue_deliveries


def observe_rating(company):
    # Cada importador mantiene bloqueada la empresa hasta confirmar balance y evento.
    from .views import calculate_altman_z, financial_profile
    metrics = FinancialMetrics.objects.filter(company=company).first()
    if not metrics or metrics.is_demo or metrics.as_of > timezone.localdate():
        return
    score = calculate_altman_z(metrics)
    if score['score'] is None or score['zone'] == 'unknown':
        return
    profile = financial_profile(score)
    current = {'category': profile['category'], 'label': profile['label'], 'score': score['score'],
               'as_of': metrics.as_of.isoformat(), 'source': metrics.source,
               'methodology': 'altman-z-original-v1'}
    state, _ = CompanyRatingState.objects.select_for_update().get_or_create(company=company)
    previous = state.last_valid
    if previous and previous['as_of'] > current['as_of']:
        return
    if previous and previous['methodology'] == current['methodology'] and previous['category'] != current['category']:
        state.revision += 1
        through=Favorite.objects.filter(company=company).aggregate(last=Max('id'))['last'] or 0
        RatingChangeEvent.objects.create(company=company, revision=state.revision, before=previous, after=current,favorite_through_id=through)
    state.last_valid = current
    state.save(update_fields=['last_valid','revision'])


def seed_rating_states():
    """Primera línea base; no genera alertas retroactivas al instalar la función."""
    for company_id in FinancialMetrics.objects.values_list('company_id', flat=True):
        with transaction.atomic():
            company = Company.objects.select_for_update().get(pk=company_id)
            if not CompanyRatingState.objects.filter(company=company).exists():
                observe_rating(company)


def process_notification_event():
    # Sin trabajo pendiente no se abre una transacción: en SQLite tomaría el bloqueo de escritura.
    if not RatingChangeEvent.objects.filter(processed_at__isnull=True, is_demo=False).exists():
        return False
    # Un error o una cuenta eliminada concurrentemente no revierte la importación.
    try:
        with transaction.atomic():
            event = RatingChangeEvent.objects.select_for_update(skip_locked=True).filter(
                processed_at__isnull=True,is_demo=False).order_by('created_at','id').first()
            if not event:
                return False
            favorites = Favorite.objects.filter(company=event.company, created_at__lte=event.created_at,
                user__is_active=True).select_related('user__notification_preferences')
            if event.favorite_through_id is not None:
                favorites=favorites.filter(pk__lte=event.favorite_through_id)
            for favorite in favorites:
                preference=getattr(favorite.user,'notification_preferences',None)
                if preference and (not preference.rating_changes or not any((preference.web_enabled,preference.email_enabled,preference.push_enabled))):
                    continue
                notification,_=Notification.objects.get_or_create(user=favorite.user,event=event,
                    defaults={'web_visible':preference.web_enabled if preference else True})
                queue_deliveries(notification,preference)
            event.processed_at = timezone.now()
            event.save(update_fields=['processed_at'])
            return True
    except IntegrityError:
        return False
