import pytest
from django.contrib.auth import get_user_model
from django.db import IntegrityError, transaction
from rest_framework.test import APIClient
from .models import Company, Favorite

pytestmark = pytest.mark.django_db
URL = '/api/v1/favorites/'


@pytest.fixture
def accounts():
    users = [get_user_model().objects.create_user(username=name) for name in ('diego', 'angel')]
    clients = [APIClient(), APIClient()]
    for client, user in zip(clients, users):
        client.force_authenticate(user)
    company = Company.objects.create(ticker='MSFT', name='Microsoft', sector='technology')
    return clients, users, company


def test_persisted_add_is_idempotent_private_and_matches_android(accounts):
    (diego, angel), users, company = accounts
    response = diego.post(URL, {'ticker': 'msft', 'user': users[1].pk}, format='json')
    assert response.status_code == 201
    data = response.json()
    assert data['schema_version'] == 'favorites-v1' and data['owner_id'] == str(users[0].pk)
    assert data['favorite']['name'] == 'Microsoft'
    again = diego.post(URL, {'ticker': 'MSFT'}, format='json')
    assert again.status_code == 200 and again.json()['favorite']['id'] == data['favorite']['id']
    assert diego.get(URL).json()['results'] == [data['favorite']]
    assert angel.get(URL).json()['results'] == []
    assert Favorite.objects.count() == 1
    with pytest.raises(IntegrityError), transaction.atomic():
        Favorite.objects.create(user=users[0], company=company)


@pytest.mark.parametrize('ticker', [None, '', 123, 'A'*11, '../MSFT'])
def test_invalid_ticker_does_not_write(accounts, ticker):
    response = accounts[0][0].post(URL, {'ticker': ticker}, format='json')
    assert response.status_code == 400 and not Favorite.objects.exists()


def test_unknown_and_unauthenticated_requests_do_not_write(accounts):
    assert accounts[0][0].post(URL, {'ticker': 'UNKNOWN'}).status_code == 404
    assert APIClient().get(URL).status_code == 401
    assert APIClient().post(URL, {'ticker': 'MSFT'}).status_code == 401
    assert not Favorite.objects.exists()


def test_owner_removes_only_their_favorite_and_repeating_returns_404(accounts):
    (owner, other), users, company = accounts
    saved = [Favorite.objects.create(user=user, company=company) for user in users]
    detail = f'{URL}{saved[0].pk}/'

    response = owner.delete(detail)
    assert response.status_code == 204
    assert response.content == b''
    assert owner.get(URL).json()['results'] == []
    assert [row['id'] for row in other.get(URL).json()['results']] == [saved[1].pk]
    assert owner.delete(detail).status_code == 404


def test_foreign_favorite_is_indistinguishable_from_missing_favorite(accounts):
    (owner, other), users, company = accounts
    saved = Favorite.objects.create(user=users[0], company=company)

    foreign = other.delete(f'{URL}{saved.pk}/')
    missing = other.delete(f'{URL}0/')
    assert foreign.status_code == missing.status_code == 404
    assert foreign.json() == missing.json()
    assert [row['id'] for row in owner.get(URL).json()['results']] == [saved.pk]


def test_anonymous_cannot_remove_favorite(accounts):
    _, users, company = accounts
    saved = Favorite.objects.create(user=users[0], company=company)
    assert APIClient().delete(f'{URL}{saved.pk}/').status_code == 401
    assert Favorite.objects.filter(pk=saved.pk).exists()
