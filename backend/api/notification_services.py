"""Cambios reales del perfil Altman-Z; el worker distribuye únicamente avisos web."""
from django.db import IntegrityError, transaction
from django.utils import timezone
from .models import Company, CompanyRatingState, Favorite, FinancialMetrics, Notification, RatingChangeEvent


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
        RatingChangeEvent.objects.create(company=company, revision=state.revision, before=previous, after=current)
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
    # Un error o una cuenta eliminada concurrentemente no revierte la importación.
    try:
        with transaction.atomic():
            event = RatingChangeEvent.objects.select_for_update(skip_locked=True).filter(
                processed_at__isnull=True,is_demo=False).order_by('created_at','id').first()
            if not event:
                return False
            users = Favorite.objects.filter(company=event.company, created_at__lte=event.created_at,
                user__is_active=True).exclude(user__notification_preferences__rating_changes=False).exclude(
                user__notification_preferences__web_enabled=False).values_list('user_id', flat=True)
            Notification.objects.bulk_create([Notification(user_id=user_id,event=event) for user_id in users], ignore_conflicts=True)
            event.processed_at = timezone.now()
            event.save(update_fields=['processed_at'])
            return True
    except IntegrityError:
        return False
