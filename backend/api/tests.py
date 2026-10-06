"""
Pytest tests for authentication endpoints (Epica 01 Backend).

Tests cover:
- HU01_01: User registration with validation
- HU01_02: Login JWT tokens
- HU01_03: Password reset request
- HU01_04: Logout with Redis blacklist
- HU01_05: Token refresh
"""
import pytest
from django.contrib.auth import get_user_model
from rest_framework.test import APIClient
from rest_framework_simplejwt.tokens import RefreshToken
from unittest.mock import patch, MagicMock

User = get_user_model()

pytestmark = pytest.mark.django_db


@pytest.fixture
def api_client():
    """Create an API client for testing."""
    return APIClient()


@pytest.fixture
def user_data():
    """Valid user registration data."""
    return {
        'username': 'testuser',
        'email': 'testuser@example.com',
        'password': 'Str0ng!Pass#2024',
        'password_confirm': 'Str0ng!Pass#2024',
        'first_name': 'Test',
        'last_name': 'User',
        'user_type': 'retail',
    }


@pytest.fixture
def created_user(api_client, user_data):
    """Create a user and return both user instance and response data."""
    response = api_client.post('/api/v1/auth/register/', user_data, format='json')
    assert response.status_code == 201
    user = User.objects.get(username='testuser')
    return user, response.data


# ─── HU01_01: Registration Tests ─────────────────────────────────────

class TestRegistration:
    """Tests for user registration (HU01_01)."""

    def test_register_success(self, api_client, user_data):
        """User can register successfully with valid data."""
        response = api_client.post('/api/v1/auth/register/', user_data, format='json')
        assert response.status_code == 201
        assert response.data['user']['username'] == 'testuser'
        assert response.data['user']['email'] == 'testuser@example.com'
        assert 'access' in response.data
        assert 'refresh' in response.data
        assert User.objects.filter(username='testuser').exists()

    def test_register_duplicate_username(self, api_client, user_data):
        """Registration fails with duplicate username."""
        api_client.post('/api/v1/auth/register/', user_data, format='json')
        response = api_client.post('/api/v1/auth/register/', user_data, format='json')
        assert response.status_code == 400
        assert 'username' in str(response.data).lower()

    def test_register_duplicate_email(self, api_client, user_data):
        """Registration fails with duplicate email."""
        api_client.post('/api/v1/auth/register/', user_data, format='json')
        data = user_data.copy()
        data['username'] = 'anotheruser'
        response = api_client.post('/api/v1/auth/register/', data, format='json')
        assert response.status_code == 400
        assert 'email' in str(response.data).lower()

    def test_register_password_mismatch(self, api_client, user_data):
        """Registration fails when passwords don't match."""
        data = user_data.copy()
        data['password_confirm'] = 'Different!Pass#2025'
        response = api_client.post('/api/v1/auth/register/', data, format='json')
        assert response.status_code == 400
        assert 'password_confirm' in str(response.data).lower()

    def test_register_short_username(self, api_client):
        """Registration fails with username shorter than 3 chars."""
        data = {
            'username': 'ab',
            'email': 'short@example.com',
            'password': 'Str0ng!Pass#2024',
            'password_confirm': 'Str0ng!Pass#2024',
        }
        response = api_client.post('/api/v1/auth/register/', data, format='json')
        assert response.status_code == 400

    def test_register_weak_password(self, api_client):
        """Registration fails with weak password (no special chars)."""
        data = {
            'username': 'weakpassuser',
            'email': 'weak@example.com',
            'password': 'weakpassword',
            'password_confirm': 'weakpassword',
        }
        response = api_client.post('/api/v1/auth/register/', data, format='json')
        assert response.status_code == 400

    def test_register_password_hashed(self, created_user):
        """Password is stored as hash, not plaintext."""
        user = created_user[0]
        assert not user.password == 'Str0ng!Pass#2024'
        assert user.password.startswith('pbkdf2_sha256$')


# ─── HU01_02: Login JWT Tests ────────────────────────────────────────

class TestLogin:
    """Tests for login with JWT tokens (HU01_02)."""

    def test_login_success(self, api_client, created_user):
        """User can login and receive JWT tokens."""
        user = created_user[0]
        response = api_client.post('/api/v1/auth/token/', {
            'username': user.username,
            'password': 'Str0ng!Pass#2024',
        }, format='json')
        assert response.status_code == 200
        assert 'access' in response.data
        assert 'refresh' in response.data
        assert 'user' in response.data
        assert response.data['user']['username'] == 'testuser'

    def test_login_by_email(self, api_client, created_user):
        """User can login using email as username."""
        user = created_user[0]
        response = api_client.post('/api/v1/auth/token/', {
            'username': user.email,
            'password': 'Str0ng!Pass#2024',
        }, format='json')
        assert response.status_code == 200

    def test_login_invalid_credentials(self, api_client):
        """Login fails with wrong password."""
        response = api_client.post('/api/v1/auth/token/', {
            'username': 'nonexistent',
            'password': 'wrongpassword',
        }, format='json')
        assert response.status_code == 401

    def test_login_missing_fields(self, api_client):
        """Login fails when username or password is missing."""
        response = api_client.post('/api/v1/auth/token/', {
            'username': 'testuser',
        }, format='json')
        assert response.status_code == 400


# ─── HU01_04: Logout Tests ───────────────────────────────────────────

class TestLogout:
    """Tests for logout with Redis blacklist (HU01_04)."""

    @patch('api.redis_utils.get_redis_connection')
    def test_logout_success(self, mock_redis, api_client, created_user):
        """User can logout and token is blacklisted."""
        user, resp_data = created_user
        refresh_token = resp_data['refresh']

        mock_instance = MagicMock()
        mock_instance.exists.return_value = 0
        mock_redis.return_value = mock_instance

        api_client.force_authenticate(user=user)
        response = api_client.post('/api/v1/auth/logout/', {
            'refresh_token': refresh_token
        }, format='json')
        assert response.status_code == 200
        assert 'message' in response.data
        mock_instance.setex.assert_called()

    def test_logout_missing_refresh_token(self, api_client, created_user):
        """Logout fails without refresh token."""
        user, _ = created_user
        api_client.force_authenticate(user=user)
        response = api_client.post('/api/v1/auth/logout/', {}, format='json')
        assert response.status_code == 400


# ─── HU01_05: Token Refresh Tests ────────────────────────────────────

class TestTokenRefresh:
    """Tests for token refresh (HU01_05)."""

    def test_refresh_success(self, api_client, created_user):
        """Token refresh returns new access and refresh tokens."""
        user, resp_data = created_user
        refresh_token = resp_data['refresh']

        response = api_client.post('/api/v1/auth/refresh/', {
            'refresh': refresh_token
        }, format='json')
        assert response.status_code == 200
        assert 'access' in response.data
        assert 'refresh' in response.data
        assert response.data['access'] != resp_data['access']

    def test_refresh_invalid_token(self, api_client):
        """Token refresh fails with invalid token."""
        response = api_client.post('/api/v1/auth/refresh/', {
            'refresh': 'invalid.token.here'
        }, format='json')
        assert response.status_code == 401

    def test_refresh_unauthorized(self, api_client):
        """Token refresh fails without refresh token."""
        response = api_client.post('/api/v1/auth/refresh/', {}, format='json')
        assert response.status_code == 400


# ─── Password Reset Tests ────────────────────────────────────────────

class TestPasswordReset:
    """Tests for password reset functionality (HU01_03)."""

    def test_password_reset_request_existing_email(self, api_client, created_user):
        """Password reset email is sent for existing email."""
        user = created_user[0]
        with patch('api.views.send_mail') as mock_send:
            mock_send.return_value = 1
            response = api_client.post('/api/v1/auth/password/reset/', {
                'email': user.email
            }, format='json')
            assert response.status_code == 200
            mock_send.assert_called()

    def test_password_reset_request_nonexistent_email(self, api_client):
        """Password reset returns success even for nonexistent email."""
        response = api_client.post('/api/v1/auth/password/reset/', {
            'email': 'nonexistent@example.com'
        }, format='json')
        assert response.status_code == 200

    def test_change_password_success(self, api_client, created_user):
        """Authenticated user can change password."""
        user, _ = created_user
        api_client.force_authenticate(user=user)
        response = api_client.patch('/api/v1/auth/change-password/', {
            'old_password': 'Str0ng!Pass#2024',
            'new_password': 'NewStr0ng!Pass#2025',
            'new_password_confirm': 'NewStr0ng!Pass#2025',
        }, format='json')
        assert response.status_code == 200

    def test_change_password_wrong_old(self, api_client, created_user):
        """Password change fails with wrong old password."""
        user, _ = created_user
        api_client.force_authenticate(user=user)
        response = api_client.patch('/api/v1/auth/change-password/', {
            'old_password': 'WrongPassword123',
            'new_password': 'NewStr0ng!Pass#2025',
            'new_password_confirm': 'NewStr0ng!Pass#2025',
        }, format='json')
        assert response.status_code == 400


# ─── Profile Tests ───────────────────────────────────────────────────

class TestProfile:
    """Tests for user profile endpoints."""

    def test_get_profile(self, api_client, created_user):
        """Authenticated user can retrieve their profile."""
        user, _ = created_user
        api_client.force_authenticate(user=user)
        response = api_client.get('/api/v1/auth/profile/')
        assert response.status_code == 200
        assert response.data['username'] == 'testuser'

    def test_update_profile(self, api_client, created_user):
        """Authenticated user can update their profile."""
        user, _ = created_user
        api_client.force_authenticate(user=user)
        response = api_client.patch('/api/v1/auth/profile/', {
            'first_name': 'Updated',
            'last_name': 'Name',
            'phone_number': '+1234567890',
        }, format='json')
        assert response.status_code == 200
        user.refresh_from_db()
        assert user.first_name == 'Updated'
        assert user.phone_number == '+1234567890'

    def test_unauthorized_profile_access(self, api_client):
        """Unauthenticated user cannot access profile."""
        response = api_client.get('/api/v1/auth/profile/')
        assert response.status_code == 401
