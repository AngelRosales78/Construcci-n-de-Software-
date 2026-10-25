from unittest.mock import MagicMock
import pytest
from django.contrib.auth import get_user_model
from rest_framework.test import APIClient

pytestmark = pytest.mark.django_db
User = get_user_model()
PASSWORD = 'Prueba!Integracion2026'


@pytest.fixture(autouse=True)
def isolated_blacklist(monkeypatch):
    entries = {}
    redis = MagicMock()
    redis.exists.side_effect = lambda key: int(key in entries)
    redis.setex.side_effect = lambda key, ttl, value: entries.update({key: value})
    monkeypatch.setattr('api.redis_utils.get_redis_connection', lambda: redis)
    monkeypatch.setattr('api.middleware.get_redis_connection', lambda: redis)


def register(client, username='integrated'):
    response = client.post('/api/v1/auth/register/', {
        'username': username, 'email': f'{username}@example.test',
        'password': PASSWORD, 'password_confirm': PASSWORD,
    }, format='json')
    assert response.status_code == 201
    return response.data


@pytest.mark.parametrize('password', ['weakpassword', '12345678', 'Abcdef12', 'abcdef!2', 'Abc!2'])
def test_complexity_is_enforced_by_server(password):
    response = APIClient().post('/api/v1/auth/register/', {
        'username': 'weak', 'email': 'weak@example.test',
        'password': password, 'password_confirm': password,
    }, format='json')
    assert response.status_code == 400
    assert not User.objects.filter(username='weak').exists()


def test_android_contract_registration_rotation_logout():
    client = APIClient()
    initial = register(client)
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {initial['access']}")
    assert client.get('/api/v1/auth/profile/').status_code == 200
    rotated = client.post('/api/v1/auth/refresh/', {'refresh': initial['refresh']}, format='json')
    assert rotated.status_code == 200
    assert rotated.data['refresh'] != initial['refresh']
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {rotated.data['access']}")
    assert client.post('/api/v1/auth/logout/', {'refresh_token': rotated.data['refresh']}, format='json').status_code == 200
    assert client.get('/api/v1/auth/profile/').status_code == 401
    # Rotation must not leave the earlier access token from the same session active.
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {initial['access']}")
    assert client.get('/api/v1/auth/profile/').status_code == 401
    client.credentials()
    assert client.post('/api/v1/auth/refresh/', {'refresh': rotated.data['refresh']}, format='json').status_code == 401


def test_logout_cannot_revoke_another_users_session():
    client = APIClient()
    first = register(client, 'first')
    second = register(APIClient(), 'second')
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {first['access']}")
    assert client.post('/api/v1/auth/logout/', {'refresh_token': second['refresh']}, format='json').status_code == 400
    assert APIClient().post('/api/v1/auth/refresh/', {'refresh': second['refresh']}, format='json').status_code == 200


def test_admin_role_is_distinct_from_investor_type():
    client = APIClient()
    normal = register(client)
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {normal['access']}")
    assert normal['user']['role'] == 'user'
    assert client.get('/api/v1/auth/admin/summary/').status_code == 403
    user = User.objects.get(username='integrated')
    user.is_staff = True
    user.save(update_fields=['is_staff'])
    client.credentials()
    response = client.post('/api/v1/auth/token/', {'username': user.email, 'password': PASSWORD}, format='json')
    assert response.status_code == 200
    assert response.data['user']['role'] == 'admin'
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {response.data['access']}")
    assert client.get('/api/v1/auth/admin/summary/').status_code == 200


def test_existing_pbkdf2_password_remains_usable():
    from django.contrib.auth.hashers import make_password
    user = User.objects.create(username='legacy', email='legacy@example.test',
                               password=make_password(PASSWORD, hasher='pbkdf2_sha256'))
    response = APIClient().post('/api/v1/auth/token/', {'username': user.username, 'password': PASSWORD}, format='json')
    assert response.status_code == 200
    user.refresh_from_db()
    assert user.password.startswith('bcrypt_sha256$')


def test_logout_with_earlier_refresh_revokes_the_rotated_session():
    client = APIClient()
    initial = register(client)
    rotated = client.post('/api/v1/auth/refresh/', {'refresh': initial['refresh']}, format='json').data
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {initial['access']}")
    assert client.post('/api/v1/auth/logout/', {'refresh_token': initial['refresh']}, format='json').status_code == 200
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {rotated['access']}")
    assert client.get('/api/v1/auth/profile/').status_code == 401
    client.credentials()
    assert client.post('/api/v1/auth/refresh/', {'refresh': rotated['refresh']}, format='json').status_code == 401


def test_logout_does_not_accept_an_access_token_as_refresh():
    client = APIClient()
    initial = register(client)
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {initial['access']}")
    assert client.post('/api/v1/auth/logout/', {'refresh_token': initial['access']}, format='json').status_code == 400
    assert client.get('/api/v1/auth/profile/').status_code == 200
