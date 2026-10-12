"""HU03_01, HU03_03 y HU03_04: contrato, filtros y orden numérico."""
from unittest.mock import MagicMock
import pytest
from django.contrib.auth import get_user_model
from django.db import connection
from django.urls import reverse
from rest_framework.test import APIClient
from api.models import Company, SearchHistory

pytestmark = pytest.mark.django_db
URL = reverse('auth:company-search')

@pytest.fixture
def client(monkeypatch):
    redis = MagicMock()
    redis.exists.return_value = 0
    monkeypatch.setattr('api.redis_utils.get_redis_connection', lambda: redis)
    user = get_user_model().objects.create_user(username='buscador')
    client = APIClient()
    client.force_authenticate(user)
    return client

@pytest.fixture
def companies():
    return [Company.objects.create(ticker=ticker, name=name, sector=sector, market_cap=cap)
            for ticker, name, sector, cap in [
                ('AAA', 'Alpha Technology', 'technology', '100.00'),
                ('BBB', 'Beta Technology', 'technology', '100.00'),
                ('CCC', 'Gamma Health', 'healthcare', '20.00'),
                ('DDD', 'Delta Energy', 'energy', '9.00')]]

@pytest.mark.parametrize('sector,expected', [
    ('technology', ['AAA', 'BBB']), ('healthcare', ['CCC']), ('energy', ['DDD'])])
def test_exact_sector(client, companies, sector, expected):
    response = client.get(URL, {'sector': sector})
    assert response.status_code == 200
    assert [row['ticker'] for row in response.data['results']] == expected
    assert response.data['count'] == len(expected)

@pytest.mark.parametrize('ordering,expected', [
    ('market_cap', ['DDD', 'CCC', 'AAA', 'BBB']),
    ('-market_cap', ['AAA', 'BBB', 'CCC', 'DDD']),
    ('', ['AAA', 'BBB', 'CCC', 'DDD'])])
def test_numeric_order_and_stable_ties(client, companies, ordering, expected):
    response = client.get(URL, {'ordering': ordering})
    assert response.status_code == 200
    assert [row['ticker'] for row in response.data['results']] == expected

@pytest.mark.parametrize('params', [{'sector': 'invalid'}, {'ordering': 'name'}, {'q': 'a'*201}])
def test_invalid_parameters(client, params):
    assert client.get(URL, params).status_code == 400

def test_no_hidden_fifty_company_limit(client):
    Company.objects.bulk_create([Company(ticker=f'T{i:03}', name=f'Empresa {i}',
                                         sector='technology', market_cap=i) for i in range(65)])
    tickers = []
    for page in range(1, 5):
        response = client.get(URL, {'page': page, 'ordering': '-market_cap'})
        assert response.status_code == 200
        assert response.data['count'] == 65
        tickers.extend(row['ticker'] for row in response.data['results'])
    assert len(tickers) == len(set(tickers)) == 65
    assert tickers[0] == 'T064' and tickers[-1] == 'T000'

def test_filter_applies_before_pagination(client):
    Company.objects.bulk_create([Company(ticker=f'T{i:03}', name=f'Empresa {i}',
        sector='technology' if i % 2 else 'energy', market_cap=i) for i in range(45)])
    response = client.get(URL, {'sector': 'technology', 'ordering': 'market_cap', 'page': 2})
    assert response.data['count'] == 22
    assert [row['ticker'] for row in response.data['results']] == ['T041', 'T043']

def test_history_is_paginated_and_private(client):
    other = get_user_model().objects.create_user(username='otro')
    SearchHistory.objects.create(user=client.handler._force_user, query='AAPL')
    SearchHistory.objects.create(user=other, query='MSFT')
    response = client.get(reverse('auth:search-history'))
    assert response.status_code == 200
    assert [row['query'] for row in response.data['results']] == ['AAPL']

@pytest.mark.parametrize('route', ['company-search', 'company-sectors', 'search-history'])
def test_requires_authentication(monkeypatch, route):
    redis = MagicMock()
    redis.exists.return_value = 0
    monkeypatch.setattr('api.redis_utils.get_redis_connection', lambda: redis)
    assert APIClient().get(reverse(f'auth:{route}')).status_code == 401

@pytest.mark.skipif(connection.vendor != 'postgresql', reason='Requiere PostgreSQL real para FTS')
@pytest.mark.parametrize('query,expected', [('aaa', ['AAA']), ('Technology', ['AAA','BBB']),
                                          ('ZZZZ', [])])
def test_full_text_and_ticker(client, companies, query, expected):
    response = client.get(URL, {'q': query})
    assert response.status_code == 200
    assert [row['ticker'] for row in response.data['results']] == expected
