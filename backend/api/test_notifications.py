from concurrent.futures import ThreadPoolExecutor
from io import StringIO
from threading import Barrier
from urllib.parse import urlsplit
import pytest
from django.core.management import call_command, CommandError
from django.db import close_old_connections, IntegrityError, transaction
from rest_framework.test import APIClient
from .models import Company, CompanyRatingState, Favorite, FinancialMetrics, Notification, NotificationPreferences, RatingChangeEvent, User
from .notification_services import observe_rating, process_notification_event

pytestmark=pytest.mark.django_db
URL='/api/v1/notifications/'
PREF='/api/v1/user/notification-preferences/'
READ=URL+'mark-read/'


@pytest.fixture
def notices():
    user=User.objects.create_user(username='notice_user')
    other=User.objects.create_user(username='notice_other')
    company=Company.objects.create(ticker='TEST',name='Empresa de ensayo',sector='technology')
    metrics=FinancialMetrics.objects.create(company=company,as_of='2024-12-31',source='Fixture unitaria, no datos reales',
        working_capital=200,total_assets=1000,retained_earnings=100,ebit=100,total_liabilities=500,sales=1000,equity_value=3000)
    Favorite.objects.create(user=user,company=company)
    client=APIClient();client.force_authenticate(user)
    with transaction.atomic(): observe_rating(company)
    return client,user,other,company,metrics


def change(company,equity=10):
    with transaction.atomic():
        Company.objects.select_for_update().get(pk=company.pk)
        FinancialMetrics.objects.filter(company=company).update(equity_value=equity)
        observe_rating(company)


def test_baseline_valid_change_and_reimport_emit_one_event(notices):
    _,user,_,company,_=notices
    assert not RatingChangeEvent.objects.exists()
    change(company);change(company)
    event=RatingChangeEvent.objects.get()
    assert event.before['category']=='strong' and event.after['category']=='weak'
    assert process_notification_event() is True and process_notification_event() is False
    assert Notification.objects.get().user==user
    change(company,3000);change(company,10)
    assert RatingChangeEvent.objects.count()==3


def test_missing_demo_and_older_data_do_not_create_transitions(notices):
    _,_,_,company,_=notices
    for changes in ({'equity_value':None},{'equity_value':10,'is_demo':True},
                    {'equity_value':10,'is_demo':False,'as_of':'2023-12-31'}):
        with transaction.atomic():
            FinancialMetrics.objects.filter(company=company).update(**changes)
            observe_rating(company)
    assert not RatingChangeEvent.objects.exists()
    assert CompanyRatingState.objects.get().last_valid['category']=='strong'


def test_worker_respects_preferences_favorites_and_account_state(notices):
    _,user,other,company,_=notices
    NotificationPreferences.objects.create(user=user,web_enabled=False)
    Favorite.objects.create(user=other,company=company)
    other.is_active=False;other.save(update_fields=['is_active'])
    change(company)
    assert process_notification_event()
    assert not Notification.objects.exists()
    NotificationPreferences.objects.filter(user=user).update(web_enabled=True,rating_changes=False)
    change(company,3000);process_notification_event()
    assert not Notification.objects.exists()
    NotificationPreferences.objects.filter(user=user).update(rating_changes=True)
    change(company);Favorite.objects.filter(user=user).delete();process_notification_event()
    assert not Notification.objects.exists()


def test_later_favorite_does_not_receive_old_event(notices):
    _,_,other,company,_=notices
    change(company)
    Favorite.objects.create(user=other,company=company)
    process_notification_event()
    assert not Notification.objects.filter(user=other).exists()


def test_list_and_read_are_private_and_idempotent(notices):
    client,user,other,company,_=notices
    change(company);process_notification_event()
    item=Notification.objects.get(user=user)
    result=client.get(URL)
    assert result.data['unread_count']==1 and result.data['results'][0]['url']=='/company/TEST'
    assert result['Cache-Control']=='private, no-store'
    client.force_authenticate(other)
    assert client.get(URL).data['count']==0
    assert client.post(READ,{'ids':[item.pk]},format='json').status_code==404
    client.force_authenticate(user)
    assert client.post(READ,{'ids':[item.pk]},format='json').data['updated']==1
    item.refresh_from_db(); read_at=item.read_at
    assert client.post(READ,{'ids':[item.pk]},format='json').data['updated']==0
    item.refresh_from_db(); assert item.read_at==read_at
    assert APIClient().get(URL).status_code==401


def test_stable_pages_and_read_all_do_not_absorb_new_alerts(notices):
    client,user,_,company,_=notices
    change(company); template=RatingChangeEvent.objects.get()
    for revision in range(2,26):
        event=RatingChangeEvent.objects.create(company=company,revision=revision,before=template.before,after=template.after)
        Notification.objects.create(user=user,event=event)
    Notification.objects.create(user=user,event=template)
    first=client.get(URL).data
    assert first['count']==25 and len(first['results'])==10 and 'through_id=' in first['next']
    new_event=RatingChangeEvent.objects.create(company=company,revision=26,before=template.before,after=template.after)
    newest=Notification.objects.create(user=user,event=new_event)
    next_url=urlsplit(first['next'])
    second=client.get(next_url.path+'?'+next_url.query).data
    assert second['count']==25 and not ({row['id'] for row in first['results']}&{row['id'] for row in second['results']})
    response=client.post(READ,{'all':True,'through_id':first['read_through']},format='json')
    assert response.data['unread_count']==1
    newest.refresh_from_db();assert newest.read_at is None


def test_preferences_persist_versions_and_reject_unavailable_channels(notices):
    client,user,other,_,_=notices
    initial=client.get(PREF).data
    assert initial['channels']=={'web':True,'email':False,'push':False}
    payload={key:initial[key] for key in ('version','rating_changes','channels')}
    payload['channels']['web']=False
    saved=client.put(PREF,payload,format='json')
    assert saved.status_code==200 and saved.data['version']==1
    assert client.put(PREF,payload,format='json').status_code==409
    for channel in ('push','email'):
        invalid=dict(payload,version=1,channels=dict(payload['channels'],**{channel:True}))
        assert client.put(PREF,invalid,format='json').status_code==400
    client.force_authenticate(other)
    assert client.get(PREF).data['channels']['web'] is True
    assert NotificationPreferences.objects.get(user=user).web_enabled is False


def test_import_command_records_change_and_rolls_back_together(notices,tmp_path,django_capture_on_commit_callbacks):
    _,_,_,company,_=notices
    file=tmp_path/'balance.csv'
    file.write_text('ticker,as_of,working_capital,total_assets,retained_earnings,ebit,total_liabilities,sales,equity_value\nTEST,2024-12-31,200,1000,100,100,500,1000,10\n',encoding='utf-8')
    with django_capture_on_commit_callbacks(execute=True):
        call_command('import_market_data',metrics=str(file),source='Prueba unitaria',stdout=StringIO())
        call_command('import_market_data',metrics=str(file),source='Prueba unitaria',stdout=StringIO())
    assert RatingChangeEvent.objects.count()==1
    with pytest.raises(RuntimeError):
        with transaction.atomic():
            change(company,3000)
            raise RuntimeError('rollback')
    assert RatingChangeEvent.objects.count()==1
    assert CompanyRatingState.objects.get().last_valid['category']=='weak'


def test_failed_delivery_does_not_lose_event_or_undo_balance(notices,monkeypatch):
    _,_,_,company,_=notices
    change(company)
    with monkeypatch.context() as scoped:
        def fail(*args,**kwargs): raise IntegrityError('Simulación de cuenta eliminada concurrentemente')
        scoped.setattr(Notification.objects,'get_or_create',fail)
        assert not process_notification_event()
    assert RatingChangeEvent.objects.get().processed_at is None
    assert process_notification_event() and Notification.objects.count()==1


def test_demo_command_refuses_test_or_work_database(notices):
    with pytest.raises(CommandError):call_command('prepare_notification_demo',username='notice_user')
    assert not RatingChangeEvent.objects.exists()


@pytest.mark.django_db(transaction=True)
def test_two_workers_do_not_duplicate_an_alert(notices):
    _,user,_,company,_=notices
    change(company);barrier=Barrier(2)
    def process():
        close_old_connections()
        try:
            barrier.wait(timeout=10)
            return process_notification_event()
        finally: close_old_connections()
    with ThreadPoolExecutor(max_workers=2) as pool:list(pool.map(lambda _:process(),range(2)))
    assert Notification.objects.filter(user=user).count()==1
