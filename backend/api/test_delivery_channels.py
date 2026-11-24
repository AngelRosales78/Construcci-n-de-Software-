import uuid
from unittest.mock import patch
import pytest
from django.core import mail
from django.test import override_settings
from django.utils import timezone
from rest_framework.test import APIClient
from .models import User, Company, RatingChangeEvent, Notification, NotificationPreferences, NotificationDelivery, PushInstallation
from .delivery_services import process_delivery, queue_deliveries

pytestmark=pytest.mark.django_db


@pytest.fixture
def notice():
    user=User.objects.create_user(username='delivery',email='delivery@example.test')
    company=Company.objects.create(ticker='AAA',name='Company',sector='technology')
    event=RatingChangeEvent.objects.create(company=company,revision=1,before={'label':'Fuerte'},after={'label':'Intermedio','as_of':'2026-01-01'})
    notification=Notification.objects.create(user=user,event=event)
    preferences=NotificationPreferences.objects.create(user=user,email_enabled=True,push_enabled=True)
    return user,notification,preferences


@override_settings(NOTIFICATION_EMAIL_ENABLED=True,NOTIFICATION_ALLOWED_RECIPIENTS=['delivery@example.test'],EMAIL_BACKEND='django.core.mail.backends.locmem.EmailBackend')
def test_email_delivered_once_and_disabling_channel_cancels_queued_job(notice):
    user,notification,preferences=notice
    queue_deliveries(notification,preferences);queue_deliveries(notification,preferences)
    assert NotificationDelivery.objects.count()==1
    assert process_delivery() and not process_delivery()
    assert len(mail.outbox)==1 and mail.outbox[0].to==[user.email]
    assert NotificationDelivery.objects.get().status=='sent'
    NotificationDelivery.objects.all().delete()
    queue_deliveries(notification,preferences)
    preferences.email_enabled=False;preferences.save()
    assert process_delivery()
    assert NotificationDelivery.objects.get().status=='cancelled'
    assert len(mail.outbox)==1


@override_settings(NOTIFICATION_EMAIL_ENABLED=True,NOTIFICATION_ALLOWED_RECIPIENTS=['delivery@example.test'])
def test_transient_error_has_bounded_retry_and_no_secret_log(notice):
    _,notification,preferences=notice
    queue_deliveries(notification,preferences)
    with patch('api.delivery_services.EmailMultiAlternatives.send',side_effect=OSError('password=secret private@example.com')):
        assert process_delivery()
    row=NotificationDelivery.objects.get()
    assert row.status=='pending' and row.attempts==1 and row.next_attempt>timezone.now()
    from .models import SystemErrorLog
    assert 'secret' not in str(list(SystemErrorLog.objects.values()))


def test_push_respects_device_owner_and_channel_before_send(notice):
    user,notification,preferences=notice
    install=PushInstallation.objects.create(user=user,installation_id=uuid.uuid4(),token='a'*30)
    with patch('api.delivery_services.available_channels',return_value={'web':True,'email':False,'push':True}):
        queue_deliveries(notification,preferences)
        with patch('api.delivery_services.send_push') as send:
            assert process_delivery();send.assert_called_once()
    assert NotificationDelivery.objects.get().status=='sent'
    NotificationDelivery.objects.all().delete()
    with patch('api.delivery_services.available_channels',return_value={'web':True,'email':False,'push':True}):
        queue_deliveries(notification,preferences)
        install.user=User.objects.create_user(username='other');install.save()
        with patch('api.delivery_services.send_push') as send:
            process_delivery();send.assert_not_called()
    assert NotificationDelivery.objects.get().status=='cancelled'


def test_installation_registration_is_private_and_logout_can_unlink(notice):
    user,_,_=notice
    client=APIClient();client.force_authenticate(user)
    installation=str(uuid.uuid4())
    path='/api/v1/user/push-installations/'
    payload={'installation_id':installation,'token':'secret-device-token-'+'a'*30,'permission_granted':True}
    assert client.post(path,payload,format='json').status_code==200
    assert payload['token'] not in str(client.get(path).json())
    other=User.objects.create_user(username='another');client.force_authenticate(other)
    assert client.get(path).json()['installations']==[]
    assert client.delete(path+installation+'/').status_code==204
    assert PushInstallation.objects.get().active
    client.force_authenticate(user);client.delete(path+installation+'/')
    assert not PushInstallation.objects.get().active

@override_settings(NOTIFICATION_EMAIL_ENABLED=True,NOTIFICATION_ALLOWED_RECIPIENTS=['delivery@example.test'])
def test_email_capability_is_limited_to_the_authorized_recipient(notice):
    from .delivery_services import available_channels
    user,_,_=notice
    assert available_channels(user)['email']
    user.email='not-authorized@example.test'
    assert not available_channels(user)['email']
