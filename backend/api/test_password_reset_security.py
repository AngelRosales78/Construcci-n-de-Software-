"""Regresiones de HU01_03 para restablecimiento y revocación de sesiones."""
from datetime import datetime, timedelta
from unittest.mock import patch

import pytest
from django.conf import settings
from django.contrib.auth import get_user_model
from django.contrib.auth.tokens import default_token_generator
from django.core import mail
from django.db import DatabaseError
from django.test import override_settings
from django.urls import reverse
from django.utils.encoding import force_bytes
from django.utils.http import urlsafe_base64_encode
from rest_framework.test import APIClient
from rest_framework_simplejwt.exceptions import TokenError
from rest_framework_simplejwt.token_blacklist.models import BlacklistedToken
from rest_framework_simplejwt.tokens import RefreshToken

pytestmark = pytest.mark.django_db
User = get_user_model()
RESET_URL = reverse("auth:password-reset-confirm")
OLD_PASSWORD = "Anterior!Clave2026"
NEW_PASSWORD = "Nueva!Clave2026"


@pytest.fixture
def reset_user():
    return User.objects.create_user(
        username="reset_user", email="reset@example.com", password=OLD_PASSWORD
    )


@pytest.fixture
def client():
    return APIClient()


@pytest.fixture(autouse=True)
def isolated_redis(monkeypatch):
    from unittest.mock import MagicMock
    connection = MagicMock()
    connection.exists.return_value = 0
    monkeypatch.setattr("api.redis_utils.get_redis_connection", lambda: connection)


def signed_reset(user):
    uid = urlsafe_base64_encode(force_bytes(user.pk))
    return f"{uid}:{default_token_generator.make_token(user)}"


def submit_reset(client, token, password=NEW_PASSWORD):
    return client.post(
        RESET_URL,
        {
            "token": token,
            "new_password": password,
            "new_password_confirm": password,
        },
        format="json",
    )


@pytest.mark.parametrize(
    "case",
    [
        "uid_only", "empty_signature", "empty_uid", "extra_separator",
        "invalid_uid", "non_numeric_uid", "oversized_uid", "unknown_user",
    ],
)
def test_malformed_token_never_changes_password(client, reset_user, case):
    uid, signature = signed_reset(reset_user).split(":")
    candidates = {
        "uid_only": uid,
        "empty_signature": f"{uid}:",
        "empty_uid": f":{signature}",
        "extra_separator": f"{uid}:{signature}:extra",
        "invalid_uid": f"!:{signature}",
        "non_numeric_uid": (
            f"{urlsafe_base64_encode(force_bytes('not-a-user'))}:{signature}"
        ),
        "oversized_uid": (
            f"{urlsafe_base64_encode(force_bytes(2**63))}:{signature}"
        ),
        "unknown_user": (
            f"{urlsafe_base64_encode(force_bytes(reset_user.pk + 1000))}:{signature}"
        ),
    }
    previous_hash = reset_user.password

    response = submit_reset(client, candidates[case])

    assert response.status_code == 400
    reset_user.refresh_from_db()
    assert reset_user.password == previous_hash


def test_valid_token_resets_password(client, reset_user):
    response = submit_reset(client, signed_reset(reset_user))

    assert response.status_code == 200
    reset_user.refresh_from_db()
    assert reset_user.check_password(NEW_PASSWORD)
    assert not reset_user.check_password(OLD_PASSWORD)


def test_modified_signature_is_rejected(client, reset_user):
    uid, signature = signed_reset(reset_user).split(":")
    replacement = "0" if signature[-1] != "0" else "1"

    response = submit_reset(client, f"{uid}:{signature[:-1]}{replacement}")

    assert response.status_code == 400
    reset_user.refresh_from_db()
    assert reset_user.check_password(OLD_PASSWORD)


def test_another_users_signature_is_rejected(client, reset_user):
    other = User.objects.create_user(
        username="other_user", email="other@example.com", password=OLD_PASSWORD
    )
    _, signature = signed_reset(other).split(":")
    uid = urlsafe_base64_encode(force_bytes(reset_user.pk))

    assert submit_reset(client, f"{uid}:{signature}").status_code == 400
    reset_user.refresh_from_db()
    assert reset_user.check_password(OLD_PASSWORD)


def test_reset_token_expires_after_fifteen_minutes(client, reset_user):
    issued_at = datetime.now().replace(microsecond=0)
    with patch.object(default_token_generator, "_now", return_value=issued_at):
        token = signed_reset(reset_user)
    expired_at = issued_at + timedelta(minutes=15, seconds=1)

    with patch.object(default_token_generator, "_now", return_value=expired_at):
        response = submit_reset(client, token)

    assert settings.PASSWORD_RESET_TIMEOUT == 900
    assert response.status_code == 400
    reset_user.refresh_from_db()
    assert reset_user.check_password(OLD_PASSWORD)


def test_token_is_still_valid_before_expiry(client, reset_user):
    issued_at = datetime.now().replace(microsecond=0)
    with patch.object(default_token_generator, "_now", return_value=issued_at):
        token = signed_reset(reset_user)

    with patch.object(
        default_token_generator, "_now",
        return_value=issued_at + timedelta(minutes=14, seconds=59),
    ):
        assert submit_reset(client, token).status_code == 200


def test_used_token_cannot_be_reused(client, reset_user):
    token = signed_reset(reset_user)
    assert submit_reset(client, token).status_code == 200

    response = submit_reset(client, token, password="Tercera!Clave2026")

    assert response.status_code == 400
    reset_user.refresh_from_db()
    assert reset_user.check_password(NEW_PASSWORD)


def test_inactive_user_cannot_reset_password(client, reset_user):
    token = signed_reset(reset_user)
    reset_user.is_active = False
    reset_user.save(update_fields=["is_active"])

    assert submit_reset(client, token).status_code == 400
    reset_user.refresh_from_db()
    assert reset_user.check_password(OLD_PASSWORD)


def test_reset_revokes_all_existing_refresh_tokens(client, reset_user):
    tokens = [RefreshToken.for_user(reset_user) for _ in range(2)]
    raw_tokens = [str(token) for token in tokens]

    assert submit_reset(client, signed_reset(reset_user)).status_code == 200

    for raw in raw_tokens:
        with pytest.raises(TokenError):
            RefreshToken(raw)
    assert BlacklistedToken.objects.filter(token__user=reset_user).count() == 2


def test_access_token_before_reset_is_rejected(client, reset_user):
    refresh = RefreshToken.for_user(reset_user)
    access = str(refresh.access_token)
    assert submit_reset(client, signed_reset(reset_user)).status_code == 200

    client.credentials(HTTP_AUTHORIZATION=f"Bearer {access}")
    assert client.get(reverse("auth:profile")).status_code == 401


def test_new_access_token_after_reset_is_accepted(client, reset_user):
    assert submit_reset(client, signed_reset(reset_user)).status_code == 200
    reset_user.refresh_from_db()
    access = str(RefreshToken.for_user(reset_user).access_token)

    client.credentials(HTTP_AUTHORIZATION=f"Bearer {access}")
    assert client.get(reverse("auth:profile")).status_code == 200


def test_other_users_tokens_are_not_revoked(client, reset_user):
    other = User.objects.create_user(
        username="other_user", email="other@example.com", password=OLD_PASSWORD
    )
    other_refresh = RefreshToken.for_user(other)
    raw_refresh = str(other_refresh)
    access = str(other_refresh.access_token)

    assert submit_reset(client, signed_reset(reset_user)).status_code == 200

    RefreshToken(raw_refresh)
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {access}")
    assert client.get(reverse("auth:profile")).status_code == 200


def test_failed_revocation_rolls_back_password_change(client, reset_user):
    RefreshToken.for_user(reset_user)
    token = signed_reset(reset_user)
    previous_hash = reset_user.password

    with patch(
        "api.views.BlacklistedToken.objects.get_or_create",
        side_effect=DatabaseError("Simulated database failure"),
    ):
        with pytest.raises(DatabaseError):
            submit_reset(client, token)

    reset_user.refresh_from_db()
    assert reset_user.password == previous_hash
    assert default_token_generator.check_token(
        reset_user, token.split(":", 1)[1]
    )
    assert not BlacklistedToken.objects.filter(token__user=reset_user).exists()


@override_settings(AUTH_PASSWORD_VALIDATORS=[{
    "NAME": "django.contrib.auth.password_validation.MinimumLengthValidator",
    "OPTIONS": {"min_length": 8},
}])
def test_weak_password_does_not_consume_token(client, reset_user):
    token = signed_reset(reset_user)
    assert submit_reset(client, token, password="short").status_code == 400
    reset_user.refresh_from_db()
    assert reset_user.check_password(OLD_PASSWORD)
    assert submit_reset(client, token).status_code == 200


@override_settings(AUTH_PASSWORD_VALIDATORS=[{
    "NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator",
}])
def test_password_validation_uses_user_context(client, reset_user):
    token = signed_reset(reset_user)
    response = submit_reset(client, token, password="reset_user")

    assert response.status_code == 400
    reset_user.refresh_from_db()
    assert reset_user.check_password(OLD_PASSWORD)


def test_mismatched_passwords_are_rejected(client, reset_user):
    response = client.post(
        RESET_URL,
        {
            "token": signed_reset(reset_user),
            "new_password": NEW_PASSWORD,
            "new_password_confirm": "Distinta!Clave2026",
        },
        format="json",
    )

    assert response.status_code == 400
    reset_user.refresh_from_db()
    assert reset_user.check_password(OLD_PASSWORD)


@override_settings(EMAIL_BACKEND="django.core.mail.backends.locmem.EmailBackend")
def test_email_describes_actual_expiry(client, reset_user):
    response = client.post(
        reverse("auth:password-reset"),
        {"email": reset_user.email},
        format="json",
    )

    assert response.status_code == 200
    assert len(mail.outbox) == 1
    assert "15 minutes" in mail.outbox[0].alternatives[0][0]
