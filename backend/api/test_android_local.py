"""Android local dev fallback; PostgreSQL retains the existing indexed path."""
import pytest
from django.contrib.auth import get_user_model
from rest_framework.test import APIClient
from .models import Company

pytestmark = pytest.mark.django_db

def test_android_local_search_by_symbol_and_name():
    Company.objects.create(ticker='MSFT', name='Microsoft Corporation', sector='technology')
    client = APIClient()
    client.force_authenticate(get_user_model().objects.create_user(username='android_local'))
    for query in ('MSFT', 'Microsoft'):
        response = client.get('/api/v1/companies/search/', {'q': query})
        assert response.status_code == 200
        assert response.json()['results'][0]['ticker'] == 'MSFT'

def test_android_local_search_still_requires_session():
    assert APIClient().get('/api/v1/companies/search/').status_code == 401
