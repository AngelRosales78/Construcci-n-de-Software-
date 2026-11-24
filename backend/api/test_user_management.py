from concurrent.futures import ThreadPoolExecutor
from io import BytesIO
from threading import Barrier
import pytest
from PIL import Image
from django.core.files.uploadedfile import SimpleUploadedFile
from django.db import close_old_connections
from rest_framework.test import APIClient
from .billing import assign_plan
from .models import AdministrationEvent, Company, Favorite, User, UserAvatar, UserSubscription

pytestmark = pytest.mark.django_db
PASSWORD = 'Cuenta!Prueba2026'


@pytest.fixture
def administration():
    admin = User.objects.create_superuser(username='admin', email='admin@example.test', password=PASSWORD)
    user = User.objects.create_user(username='persona', email='persona@example.test', password=PASSWORD)
    client = APIClient()
    client.force_authenticate(admin)
    return client, admin, user


def patch_user(client, user, payload):
    return client.patch(f'/api/v1/admin/users/{user.pk}/', payload, format='json')


def login(user):
    client = APIClient()
    data = client.post('/api/v1/auth/token/', {'username': user.username, 'password': PASSWORD}, format='json').data
    client.credentials(HTTP_AUTHORIZATION='Bearer ' + data['access'])
    return client, data


def test_admin_table_is_paginated_filtered_and_private(administration):
    client, _, user = administration
    User.objects.bulk_create([User(username=f'page{i}', email=f'page{i}@example.test') for i in range(23)])
    assign_plan(user, 'pro')
    result = client.get('/api/v1/admin/users/').data
    assert result['count'] == 25 and len(result['results']) == 20 and result['next']
    filtered = client.get('/api/v1/admin/users/?q=persona&plan=pro&state=active').data
    assert [row['id'] for row in filtered['results']] == [user.pk]
    assert set(filtered['results'][0]) == {'id', 'username', 'first_name', 'last_name', 'email', 'role', 'plan', 'is_active', 'blocked_reason'}
    client.force_authenticate(user)
    assert client.get('/api/v1/admin/users/').status_code == 403
    assert patch_user(client, user, {'role': 'admin'}).status_code == 403
    assert APIClient().get('/api/v1/admin/users/').status_code == 401


def test_role_and_plan_are_independent_and_audited(administration):
    client, _, user = administration
    response = patch_user(client, user, {'plan': 'pro'})
    assert response.status_code == 200 and response.data['plan'] == 'pro' and response.data['role'] == 'user'
    user.refresh_from_db()
    assert not user.is_superuser
    assert AdministrationEvent.objects.get().changes['plan'] == {'before': 'free', 'after': 'pro'}
    assert patch_user(client, user, {'role': 'admin'}).data['role'] == 'admin'
    user.refresh_from_db()
    assert user.is_staff and user.is_superuser and user.session_version == 1
    assert patch_user(client, user, {'password': 'injection'}).status_code == 400


@pytest.mark.parametrize('changes', [{'role': 'user'}, {'is_active': False, 'reason': 'prueba'}])
def test_last_active_administrator_is_protected(administration, changes):
    client, admin, _ = administration
    assert patch_user(client, admin, changes).status_code == 409
    admin.refresh_from_db()
    assert admin.is_active and admin.is_superuser
    assert client.delete('/api/v1/auth/account/', {'password': PASSWORD}, format='json').status_code == 409


def test_blocking_invalidates_old_sessions_even_after_unblock(administration):
    admin_client, _, user = administration
    user_client, tokens = login(user)
    assert user_client.get('/api/v1/auth/profile/').status_code == 200
    assert patch_user(admin_client, user, {'is_active': False, 'reason': 'Bloqueo de prueba'}).status_code == 200
    assert user_client.get('/api/v1/auth/profile/').status_code == 401
    assert APIClient().post('/api/v1/auth/token/', {'username': user.username, 'password': PASSWORD}, format='json').status_code == 401
    assert patch_user(admin_client, user, {'is_active': True}).status_code == 200
    assert user_client.get('/api/v1/auth/profile/').status_code == 401
    assert APIClient().post('/api/v1/auth/refresh/', {'refresh': tokens['refresh']}, format='json').status_code == 401
    fresh, _ = login(user)
    assert fresh.get('/api/v1/auth/profile/').status_code == 200


def picture(size=(40, 30)):
    stream = BytesIO()
    Image.new('RGB', size, 'green').save(stream, format='PNG')
    return SimpleUploadedFile('avatar.png', stream.getvalue(), content_type='image/png')


def test_profile_avatar_is_normalized_private_and_saved_with_name(administration):
    client, admin, user = administration
    client.force_authenticate(user)
    response = client.patch('/api/v1/auth/profile/', {'first_name': 'Diego', 'last_name': 'Prueba', 'avatar': picture()}, format='multipart')
    assert response.status_code == 200 and response.data['avatar_url']
    user.refresh_from_db()
    assert user.first_name == 'Diego'
    avatar = UserAvatar.objects.get(user=user)
    image = Image.open(BytesIO(bytes(avatar.content)))
    assert image.format == 'PNG' and image.size == (256, 256) and image.getpixel((0, 0))[3] == 0
    assert len(response.data['avatar_version']) == 64
    private = client.get(response.data['avatar_url'])
    assert private.status_code == 200 and private['Cache-Control'] == 'private, no-store'
    client.force_authenticate(admin)
    assert client.get(response.data['avatar_url'] + f'?user_id={user.pk}').status_code == 404


@pytest.mark.parametrize('kind', ['invalid', 'oversize', 'dimensions'])
def test_invalid_avatar_does_not_overwrite_name_or_avatar(administration, kind):
    client, _, user = administration
    client.force_authenticate(user)
    if kind == 'invalid':
        upload = SimpleUploadedFile('photo.png', b'<svg>invalid</svg>', content_type='image/png')
    elif kind == 'oversize':
        upload = SimpleUploadedFile('photo.png', b'0' * (2 * 1024 * 1024), content_type='image/png')
    else:
        upload = picture((3200, 3200))
    response = client.patch('/api/v1/auth/profile/', {'first_name': 'No guardar', 'avatar': upload}, format='multipart')
    assert response.status_code == 400
    user.refresh_from_db()
    assert user.first_name == '' and not UserAvatar.objects.filter(user=user).exists()


def test_account_deletion_requires_password_and_erases_owned_data(administration):
    admin_client, _, user = administration
    patch_user(admin_client, user, {'plan': 'pro'})
    company = Company.objects.create(ticker='TEST', name='Test', sector='technology')
    Favorite.objects.create(user=user, company=company)
    client, tokens = login(user)
    assert client.patch('/api/v1/auth/profile/', {'avatar': picture()}, format='multipart').status_code == 200
    assert client.delete('/api/v1/auth/account/', {'password': 'incorrecta'}, format='json').status_code == 403
    assert User.objects.filter(pk=user.pk).exists()
    assert client.delete('/api/v1/auth/account/', {'password': PASSWORD}, format='json').status_code == 204
    assert not User.objects.filter(pk=user.pk).exists()
    assert not Favorite.objects.exists() and not UserAvatar.objects.exists() and not UserSubscription.objects.exists()
    assert Company.objects.filter(pk=company.pk).exists()
    assert AdministrationEvent.objects.filter(subject__isnull=True).count() == 2
    assert 'persona@example.test' not in str(list(AdministrationEvent.objects.values('changes')))
    assert client.get('/api/v1/auth/profile/').status_code == 401
    assert APIClient().post('/api/v1/auth/refresh/', {'refresh': tokens['refresh']}, format='json').status_code == 401


@pytest.mark.django_db(transaction=True)
def test_concurrent_admin_changes_cannot_remove_all_administrators(administration):
    _, first, second = administration
    second.is_superuser = second.is_staff = True
    second.save()
    barrier = Barrier(2)

    def change(pair):
        close_old_connections()
        try:
            actor, subject = pair
            client = APIClient()
            client.force_authenticate(User.objects.get(pk=actor))
            barrier.wait(timeout=10)
            return client.patch(f'/api/v1/admin/users/{subject}/', {'role': 'user'}, format='json').status_code
        finally:
            close_old_connections()

    with ThreadPoolExecutor(max_workers=2) as pool:
        statuses = list(pool.map(change, [(first.pk, second.pk), (second.pk, first.pk)]))
    assert statuses.count(200) == 1
    assert set(statuses) <= {200, 403, 409}
    assert User.objects.filter(is_superuser=True, is_staff=True, is_active=True).count() == 1
