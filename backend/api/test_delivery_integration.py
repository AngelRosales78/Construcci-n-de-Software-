import csv
import pytest
from django.core.management import call_command
from django.core.management.base import CommandError
from django.contrib.auth import get_user_model
from rest_framework.test import APIClient
from api.models import Company

pytestmark = pytest.mark.django_db


def catalog(tmp_path, rows):
    path = tmp_path / 'companies.csv'
    with path.open('w', newline='', encoding='utf-8') as stream:
        writer = csv.writer(stream)
        writer.writerow(['Symbol', 'Security', 'GICS Sector'])
        writer.writerows(rows)
    return str(path)


def test_catalog_retains_financial_data_and_is_idempotent(tmp_path):
    Company.objects.create(ticker='MSFT', name='Old', sector='technology', market_cap=123)
    path = catalog(tmp_path, [('MSFT', 'Microsoft', 'Information Technology'), ('NEW', 'New issuer', 'Real Estate')])
    call_command('import_company_catalog', path)
    call_command('import_company_catalog', path)
    assert Company.objects.count() == 2
    assert Company.objects.get(ticker='MSFT').market_cap == 123
    assert Company.objects.get(ticker='NEW').market_cap is None


@pytest.mark.parametrize('bad', [('MSFT', 'Duplicate', 'Information Technology'),
                                 ('BAD/KEY', 'Bad ticker', 'Financials'),
                                 ('UNKNOWN', 'Unknown sector', 'Invalid')])
def test_invalid_catalog_has_no_partial_writes(tmp_path, bad):
    path = catalog(tmp_path, [('MSFT', 'Microsoft', 'Information Technology'), bad])
    with pytest.raises(CommandError):
        call_command('import_company_catalog', path)
    assert Company.objects.count() == 0


def test_actual_web_auth_contract_and_private_search():
    client = APIClient()
    assert client.get('/api/v1/companies/search/').status_code == 401
    data = {'username': 'integration', 'email': 'integration@example.test',
            'password': 'Integration!59341', 'password_confirm': 'Integration!59341'}
    registered = client.post('/api/v1/auth/register/', data)
    assert registered.status_code == 201
    login = client.post('/api/v1/auth/token/', {'username': data['username'], 'password': data['password']})
    assert login.status_code == 200
    client.credentials(HTTP_AUTHORIZATION='Bearer ' + login.json()['access'])
    assert client.get('/api/v1/companies/search/').status_code == 200
    assert client.get('/api/v1/auth/profile/').json()['username'] == data['username']
    renewed = client.post('/api/v1/auth/refresh/', {'refresh': login.json()['refresh']})
    assert renewed.status_code == 200
    assert renewed.json()['refresh'] != login.json()['refresh']
    assert client.post('/api/v1/auth/refresh/', {'refresh': login.json()['refresh']}).status_code == 401


@pytest.mark.parametrize('ordering', ['market_cap', '-market_cap'])
def test_unknown_cap_is_last_and_is_not_zero(ordering, monkeypatch):
    import fakeredis
    redis = fakeredis.FakeRedis(decode_responses=True)
    monkeypatch.setattr('api.redis_utils.get_redis_connection', lambda: redis)
    for ticker, cap in [('UNKNOWN', None), ('ZERO', 0), ('LARGE', 123)]:
        Company.objects.create(ticker=ticker, name=ticker, sector='technology', market_cap=cap)
    client = APIClient()
    client.force_authenticate(get_user_model().objects.create_user(username='sort'))
    rows = client.get('/api/v1/companies/search/', {'ordering': ordering}).json()['results']
    assert rows[-1]['ticker'] == 'UNKNOWN'
    assert rows[-1]['market_cap'] is None
    assert next(r for r in rows if r['ticker'] == 'ZERO')['market_cap'] == '0.00'


def test_presentation_account_refuses_work_database(tmp_path):
    with pytest.raises(CommandError, match='Solo se permite'):
        call_command('prepare_presentation', credentials_file=str(tmp_path / 'account.json'))
    assert not (tmp_path / 'account.json').exists()
    assert get_user_model().objects.count() == 0


def test_presentation_refuses_credentials_inside_git(monkeypatch):
    from pathlib import Path
    from django.conf import settings
    monkeypatch.setitem(settings.DATABASES['default'], 'NAME', 'sprint_delivery_validation')
    target = Path(__file__).resolve().parents[2] / 'credentials.json'
    with pytest.raises(CommandError, match='fuera del repositorio'):
        call_command('prepare_presentation', credentials_file=str(target))
    assert not target.exists()
