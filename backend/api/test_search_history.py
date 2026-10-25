import pytest
import fakeredis
from django.contrib.auth import get_user_model
from django.urls import reverse
from rest_framework.test import APIClient
from api.models import Company, SearchHistory
from api.search_cache import preload_autocomplete, autocomplete, cache_history, history_key

pytestmark = pytest.mark.django_db
@pytest.fixture
def setup(monkeypatch):
    redis = fakeredis.FakeRedis(decode_responses=True)
    monkeypatch.setattr('api.search_cache.get_redis_connection', lambda: redis)
    monkeypatch.setattr('api.redis_utils.get_redis_connection', lambda: redis)
    user = get_user_model().objects.create_user(username='historial')
    client = APIClient();client.force_authenticate(user)
    return client, user, redis

def test_history_keeps_last_ten_and_deduplicates(setup):
    client,user,redis = setup
    url=reverse('auth:history')
    for i in range(15):
        assert client.post(url,{'query':f'T{i}'},format='json').status_code==201
    assert SearchHistory.objects.filter(user=user).count()==10
    rows=client.get(url).json()['results']
    assert [r['query'] for r in rows]==[f'T{i}' for i in range(14,4,-1)]
    assert redis.llen(history_key(user.pk))==10
    assert client.post(url,{'query':' t10 '},format='json').status_code==201
    assert SearchHistory.objects.filter(user=user).count()==10
    assert client.get(url).json()['results'][0]['query']=='t10'

def test_delete_and_clear_are_private(setup):
    client,user,redis=setup
    other=get_user_model().objects.create_user(username='otro')
    foreign=SearchHistory.objects.create(user=other,query='OTRO')
    mine=SearchHistory.objects.create(user=user,query='MIO')
    assert client.delete(reverse('auth:history-delete',args=[foreign.pk])).status_code==404
    assert client.delete(reverse('auth:history-delete',args=[mine.pk])).status_code==204
    SearchHistory.objects.create(user=user,query='UNO')
    assert client.delete(reverse('auth:history-clear')).status_code==204
    assert not SearchHistory.objects.filter(user=user).exists()
    assert SearchHistory.objects.filter(pk=foreign.pk).exists()

@pytest.mark.parametrize('query',['','   ','a'*201])
def test_invalid_history_query(setup,query):
    assert setup[0].post(reverse('auth:history'),{'query':query},format='json').status_code==400

def test_cache_limits_and_ttl(setup):
    _,user,redis=setup
    cache_history(user.pk,[{'id':i,'query':str(i)} for i in range(20)])
    assert redis.llen(history_key(user.pk))==10
    assert 0<redis.ttl(history_key(user.pk))<=3600
    cache_history(user.pk,[])
    assert redis.llen(history_key(user.pk))==0

def test_preloaded_autocomplete_does_not_query_database(setup,django_assert_num_queries):
    Company.objects.create(ticker='AAPL',name='Apple Inc.',sector='technology')
    Company.objects.create(ticker='AMZN',name='Amazon Inc.',sector='technology')
    Company.objects.create(ticker='MSFT',name='Microsoft',sector='technology')
    preload_autocomplete(Company.objects.all())
    with django_assert_num_queries(0):
        assert autocomplete('AA')==[{'ticker':'AAPL','name':'Apple Inc.'}]
        assert {r['ticker'] for r in autocomplete('a')}=={'AAPL','AMZN'}
        assert autocomplete('doesnotexist')==[]

def test_autocomplete_limits_and_alias(setup):
    client,_,_=setup
    Company.objects.bulk_create([Company(ticker=f'A{i:03}',name=f'Alpha {i}',sector='technology') for i in range(30)])
    preload_autocomplete(Company.objects.all())
    response=client.get(reverse('auth:autocomplete'),{'q':'a'})
    assert response.status_code==200
    assert len(response.json()['suggestions'])==10
    assert client.get(reverse('auth:autocomplete'),{'q':'a'*201}).status_code==400

def test_autocomplete_unavailable_returns_503(setup,monkeypatch):
    from redis.exceptions import ConnectionError
    def unavailable():raise ConnectionError('offline')
    monkeypatch.setattr('api.search_cache.get_redis_connection',unavailable)
    assert setup[0].get(reverse('auth:autocomplete'),{'q':'AA'}).status_code==503
