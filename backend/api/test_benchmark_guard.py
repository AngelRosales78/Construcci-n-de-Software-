import pytest
import fakeredis
from django.core.management import call_command
from django.core.management.base import CommandError
from django.db import connection
from api.models import Company, PriceHistory
pytestmark=pytest.mark.django_db

def test_benchmark_requires_explicit_confirmation():
    with pytest.raises(CommandError):call_command('prepare_benchmark')
    assert not Company.objects.exists()

def test_benchmark_rejects_nonvalidation_database(monkeypatch):
    monkeypatch.setitem(connection.settings_dict,'NAME','base_entrega')
    with pytest.raises(CommandError):call_command('prepare_benchmark',confirm_test_database=True)
    assert not Company.objects.exists()

def test_benchmark_fixture_is_explicit_and_reproducible(monkeypatch):
    redis=fakeredis.FakeRedis(decode_responses=True)
    monkeypatch.setattr('api.search_cache.get_redis_connection',lambda:redis)
    monkeypatch.setitem(connection.settings_dict,'NAME','test_sprint_validation')
    call_command('prepare_benchmark',confirm_test_database=True)
    call_command('prepare_benchmark',confirm_test_database=True)
    assert Company.objects.count()==500
    assert PriceHistory.objects.count()==1200
    assert not PriceHistory.objects.filter(is_demo=False).exists()
