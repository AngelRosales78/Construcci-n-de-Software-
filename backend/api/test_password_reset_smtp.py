"""Exercise Django's SMTP transport and the reset link delivered in the message."""
from email import policy
from email.parser import BytesParser
import re
from urllib.parse import urlparse

import pytest
from django.contrib.auth import get_user_model
from django.test import override_settings
from rest_framework.test import APIClient
from .tests_smtp_support import smtp_receiver


@pytest.mark.django_db
def test_smtp_message_contains_a_working_single_use_reset_link():
    user = get_user_model().objects.create_user(
        username='smtp_test', email='smtp-test@example.test', password='Anterior!Clave2026')
    client = APIClient()
    with smtp_receiver() as receiver, override_settings(
        EMAIL_BACKEND='django.core.mail.backends.smtp.EmailBackend',
        EMAIL_HOST='127.0.0.1', EMAIL_PORT=receiver.server_address[1],
        EMAIL_HOST_USER='', EMAIL_HOST_PASSWORD='', EMAIL_USE_TLS=False,
        EMAIL_USE_SSL=False, EMAIL_TIMEOUT=5, FRONTEND_URL='http://localhost:55180',
    ):
        response = client.post('/api/v1/auth/password/reset/', {'email': user.email}, format='json')
        assert response.status_code == 200
        assert len(receiver.messages) == 1
        message = BytesParser(policy=policy.default).parsebytes(receiver.messages[0])
        assert message['To'] == user.email
        plain = message.get_body(preferencelist=('plain',)).get_content()
        link = re.search(r'http://localhost:55180/reset-password/\S+', plain).group(0)
        _, uid, signature = urlparse(link).path.strip('/').split('/')
        payload = {'token': f'{uid}:{signature}', 'new_password': 'Nueva!Clave2026',
                   'new_password_confirm': 'Nueva!Clave2026'}
        assert client.post('/api/v1/auth/password/reset/confirm/', payload, format='json').status_code == 200
        user.refresh_from_db()
        assert user.check_password('Nueva!Clave2026')
        assert not user.check_password('Anterior!Clave2026')
        assert client.post('/api/v1/auth/password/reset/confirm/', payload, format='json').status_code == 400
        assert client.post('/api/v1/auth/token/', {'username': user.username, 'password': 'Anterior!Clave2026'}, format='json').status_code == 401
        assert client.post('/api/v1/auth/token/', {'username': user.username, 'password': 'Nueva!Clave2026'}, format='json').status_code == 200
