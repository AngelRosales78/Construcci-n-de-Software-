from datetime import timedelta
import pytest
from django.contrib.auth import get_user_model
from django.utils import timezone
from rest_framework.test import APIClient
from .models import AccountPlan

pytestmark = pytest.mark.django_db


def test_access_is_free_by_default_and_does_not_trust_request_plan():
    user = get_user_model().objects.create_user(username='free')
    client = APIClient(); client.force_authenticate(user)
    response = client.get('/api/v1/user/subscription/?plan=pro')
    data = response.json()
    assert data['schema_version'] == 'subscription-v1'
    assert data['owner_id'] == str(user.pk)
    assert data['plan'] == 'free' and data['features'] == []
    assert response['Cache-Control'] == 'private, no-store'
    assert client.post('/api/v1/user/subscription/', {'plan': 'pro'}).status_code == 405
    assert APIClient().get('/api/v1/user/subscription/').status_code == 401


@pytest.mark.parametrize('active,days,allowed', [(True, 1, True), (False, 1, False), (True, -1, False)])
def test_pro_requires_active_unexpired_assignment(active, days, allowed):
    user = get_user_model().objects.create_user(username='pro')
    AccountPlan.objects.create(user=user, plan='pro', active=active, expires_at=timezone.now()+timedelta(days=days))
    client = APIClient(); client.force_authenticate(user)
    data = client.get('/api/v1/user/subscription/').json()
    assert (data['features'] == ['compare']) is allowed
    assert data['active'] is allowed
    assert data['valid_until'] > timezone.now().isoformat()
