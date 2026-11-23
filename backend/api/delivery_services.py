"""Cola persistida. Los reintentos no usan datos de autenticación en registros."""
from datetime import timedelta
from pathlib import Path
from django.conf import settings
from django.core.mail import EmailMultiAlternatives
from django.db import transaction
from django.db.models import Q
from django.utils import timezone
from .models import NotificationDelivery, NotificationPreferences, PushInstallation
from .monitoring_services import record_error


def available_channels(user=None):
    email=bool(settings.NOTIFICATION_EMAIL_ENABLED)
    if user is not None:
        email=email and user.email.lower() in settings.NOTIFICATION_ALLOWED_RECIPIENTS
    return {'web': True, 'email': email,
            'push': bool(settings.FCM_CREDENTIALS_FILE and Path(settings.FCM_CREDENTIALS_FILE).is_file())}


def queue_deliveries(notification, preference):
    channels=available_channels()
    if preference and preference.email_enabled and channels['email']:
        NotificationDelivery.objects.get_or_create(notification=notification, channel='email', installation=None)
    if preference and preference.push_enabled and channels['push']:
        for installation in PushInstallation.objects.filter(user=notification.user,active=True,permission_granted=True):
            NotificationDelivery.objects.get_or_create(notification=notification,channel='push',installation=installation)


def send_push(installation, notification):
    import firebase_admin
    from firebase_admin import credentials, messaging, exceptions
    try: app=firebase_admin.get_app('sp500-alerts')
    except ValueError:
        app=firebase_admin.initialize_app(credentials.Certificate(settings.FCM_CREDENTIALS_FILE),name='sp500-alerts')
    # Ninguna categoría, saldo o nombre privado se publica en la pantalla bloqueada.
    message=messaging.Message(token=installation.token,
        notification=messaging.Notification(title='S&P 500 Finance',body='Tienes una actualización en tus avisos.'),
        data={'ticker':notification.event.company.ticker,'owner_id':str(notification.user_id),'notification_id':str(notification.pk)},
        android=messaging.AndroidConfig(ttl=timedelta(hours=12),priority='normal',
            collapse_key=notification.event.company.ticker,notification=messaging.AndroidNotification(channel_id='financial_updates',visibility='private',tag='notice-'+str(notification.pk))))
    try: messaging.send(message,app=app)
    except (messaging.UnregisteredError, messaging.SenderIdMismatchError, exceptions.InvalidArgumentError):
        PushInstallation.objects.filter(pk=installation.pk).update(active=False)
        raise PermanentDeliveryError('invalid_token')


class PermanentDeliveryError(Exception):
    pass


def process_delivery():
    now=timezone.now()
    with transaction.atomic():
        job=NotificationDelivery.objects.select_for_update(skip_locked=True).filter(
            Q(status='pending',next_attempt__lte=now)|Q(status='sending',lease_until__lt=now)).order_by('id').first()
        if not job: return False
        job.status='sending';job.attempts+=1;job.lease_until=now+timedelta(minutes=2)
        job.save(update_fields=['status','attempts','lease_until'])
        job_id=job.pk
    job=NotificationDelivery.objects.select_related('notification__user','notification__event__company','installation').get(pk=job_id)
    notification=job.notification
    preference=NotificationPreferences.objects.filter(user_id=notification.user_id).first()
    permitted=bool(preference and preference.rating_changes and getattr(preference,job.channel+'_enabled',False))
    if not notification.user.is_active or not permitted or notification.event.is_demo:
        NotificationDelivery.objects.filter(pk=job_id).update(status='cancelled',error_code='preference_or_account_disabled')
        return True
    try:
        if not available_channels().get(job.channel): raise PermanentDeliveryError('channel_unavailable')
        if job.channel=='email':
            allowed=settings.NOTIFICATION_ALLOWED_RECIPIENTS
            if notification.user.email.lower() not in allowed:
                raise PermanentDeliveryError('recipient_not_authorized')
            company=notification.event.company
            text=(f'Cambió el perfil financiero de {company.name} ({company.ticker}).\n'
                  f"{notification.event.before['label']} → {notification.event.after['label']}.\n"
                  f'Fecha del balance: {notification.event.after["as_of"]}.\n'
                  f'{settings.FRONTEND_URL}/company/{company.ticker}\n\n'
                  'Puedes gestionar tus canales en Preferencias de avisos.')
            message=EmailMultiAlternatives('Actualización de tu empresa favorita',text,settings.DEFAULT_FROM_EMAIL,[notification.user.email],headers={'Message-ID':f'<financial-notice-{job.pk}@sp500.local>'})
            if message.send()!=1: raise RuntimeError('delivery_not_confirmed')
        else:
            installation=job.installation
            if not installation or installation.user_id!=notification.user_id or not installation.active or not installation.permission_granted:
                raise PermanentDeliveryError('installation_inactive')
            send_push(installation,notification)
        NotificationDelivery.objects.filter(pk=job_id).update(status='sent',sent_at=timezone.now(),error_code='')
    except PermanentDeliveryError as error:
        NotificationDelivery.objects.filter(pk=job_id).update(status='cancelled',error_code=str(error))
    except Exception:
        retry=job.attempts<5
        NotificationDelivery.objects.filter(pk=job_id).update(status='pending' if retry else 'failed',error_code='delivery_failed',
            next_attempt=timezone.now()+timedelta(seconds=min(3600,30*2**job.attempts)))
        record_error('delivery_failed',job.channel,attempt=job.attempts,retryable=retry)
    return True
