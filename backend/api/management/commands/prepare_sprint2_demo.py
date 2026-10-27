"""Cuenta desechable solo para la base aislada del cierre Sprint 2."""
import json
import secrets
from pathlib import Path
from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand, CommandError


class Command(BaseCommand):
    def add_arguments(self, parser):
        parser.add_argument('--credentials-file', required=True)

    def handle(self, *args, **options):
        if settings.DATABASES['default']['NAME'] != 'sprint2_completion_validation':
            raise CommandError('Solo se admite la base aislada sprint2_completion_validation.')
        target = Path(options['credentials_file']).resolve()
        if target.is_relative_to(Path(__file__).resolve().parents[4]):
            raise CommandError('Las credenciales deben guardarse fuera del repositorio.')
        password = 'Demo!' + secrets.token_urlsafe(20)
        user, _ = get_user_model().objects.get_or_create(username='magallanes_sprint2', defaults={'email': 'sprint2@example.test'})
        user.set_password(password); user.save(update_fields=['password'])
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps({'username': user.username, 'password': password}), encoding='utf-8')
        self.stdout.write('Cuenta de demostración preparada; credenciales fuera de Git.')
