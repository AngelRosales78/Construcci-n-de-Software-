"""Cuenta desechable únicamente en la base de presentación aislada."""
import json
import secrets
from pathlib import Path
from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand, CommandError

class Command(BaseCommand):
    help = 'Preparar cuenta de presentación; no modifica bases de trabajo'

    def add_arguments(self, parser):
        parser.add_argument('--credentials-file', required=True)

    def handle(self, *args, **options):
        if settings.DATABASES['default']['NAME'] != 'sprint_delivery_validation':
            raise CommandError('Solo se permite la base aislada sprint_delivery_validation.')
        target = Path(options['credentials_file']).resolve()
        repository = Path(__file__).resolve().parents[4]
        if target.is_relative_to(repository):
            raise CommandError('Las credenciales deben guardarse fuera del repositorio.')
        password = 'Demo!' + secrets.token_urlsafe(20)
        user, _ = get_user_model().objects.get_or_create(username='demo_diego', defaults={'email': 'demo_diego@example.test'})
        user.set_password(password)
        user.save(update_fields=['password'])
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps({'username': user.username, 'password': password}), encoding='utf-8')
        self.stdout.write('Cuenta desechable preparada; credenciales fuera de Git.')
