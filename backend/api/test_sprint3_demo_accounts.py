import json
from types import SimpleNamespace
import pytest
from django.contrib.auth import get_user_model
from django.core.management import call_command
from django.core.management.base import CommandError
from .management.commands import prepare_sprint3_demo

pytestmark = pytest.mark.django_db


def isolated_settings(monkeypatch):
    monkeypatch.setattr(prepare_sprint3_demo, 'settings', SimpleNamespace(
        DATABASES={'default': {'NAME': 'sprint3_demo_validation'}}))


def test_demo_keeps_locally_chosen_password_on_repeated_preparation(tmp_path, monkeypatch):
    isolated_settings(monkeypatch)
    target = tmp_path/'accounts.json'
    # Contraseña de fixture; la clave de la presentación solo vive en el archivo local.
    accounts = {plan: {'username': f'sprint3_{plan}', 'password': 'Fixture!2468'} for plan in ('pro', 'free')}
    target.write_text(json.dumps(accounts), encoding='utf-8')
    call_command('prepare_sprint3_demo', credentials_file=str(target))
    call_command('prepare_sprint3_demo', credentials_file=str(target))
    assert json.loads(target.read_text(encoding='utf-8')) == accounts
    for row in accounts.values():
        assert get_user_model().objects.get(username=row['username']).check_password(row['password'])


def test_invalid_file_is_rejected_before_changing_any_account(tmp_path, monkeypatch):
    isolated_settings(monkeypatch)
    target = tmp_path/'accounts.json'
    target.write_text(json.dumps({'free': {'password': 'Fixture!2468'}, 'pro': {'password': '123'}}), encoding='utf-8')
    with pytest.raises(CommandError):
        call_command('prepare_sprint3_demo', credentials_file=str(target))
    assert not get_user_model().objects.exists()


def test_demo_refuses_other_database_and_creates_no_accounts(tmp_path):
    with pytest.raises(CommandError, match='Solo se admite'):
        call_command('prepare_sprint3_demo', credentials_file=str(tmp_path/'accounts.json'))
    assert not get_user_model().objects.exists()
