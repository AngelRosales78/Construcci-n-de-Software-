import pytest
import fakeredis
from django.contrib.auth import get_user_model
from django.urls import reverse
from rest_framework.test import APIClient
from api.models import Company
pytestmark=pytest.mark.django_db

def test_cached_search_is_validated_and_isolated(monkeypatch,django_assert_num_queries):
    redis=fakeredis.FakeRedis(decode_responses=True)
    monkeypatch.setattr('api.redis_utils.get_redis_connection',lambda:redis)
    user=get_user_model().objects.create_user(username='cache')
    client=APIClient();client.force_authenticate(user)
    Company.objects.create(ticker='AAA',name='Alpha',sector='technology')
    url=reverse('auth:company-search')
    first=client.get(url,{'sector':'technology'})
    with django_assert_num_queries(0):second=client.get(url,{'sector':'technology'})
    assert first.json()==second.json()
    assert client.get(url,{'ordering':'invalid'}).status_code==400
    assert len(redis.keys('sp500:search:*'))==1
    other=get_user_model().objects.create_user(username='another')
    client.force_authenticate(other)
    client.get(url,{'sector':'technology'})
    assert len(redis.keys('sp500:search:*'))==2
    for key in redis.keys('sp500:search:*'):assert 0<redis.ttl(key)<=30
