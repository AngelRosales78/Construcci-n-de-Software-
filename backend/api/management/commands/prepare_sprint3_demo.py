"""Dos cuentas desechables; nunca habilita Pro en una base de trabajo."""
import json
import secrets
from pathlib import Path
from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand, CommandError
from api.models import AccountPlan


class Command(BaseCommand):
    help = 'Preparar cuentas Free y Pro en la base aislada sprint3_demo_validation'

    def add_arguments(self, parser):
        parser.add_argument('--credentials-file', required=True)

    def handle(self, *args, **options):
        if settings.DATABASES['default']['NAME'] != 'sprint3_demo_validation':
            raise CommandError('Solo se admite la base aislada sprint3_demo_validation.')
        target = Path(options['credentials_file']).resolve()
        if target.is_relative_to(Path(__file__).resolve().parents[4]):
            raise CommandError('Las credenciales deben guardarse fuera del repositorio.')
        credentials = {}
        for plan in ('free', 'pro'):
            user, _ = get_user_model().objects.get_or_create(username=f'sprint3_{plan}',
                defaults={'email': f'sprint3_{plan}@example.test'})
            password = 'Demo!' + secrets.token_urlsafe(20)
            user.set_password(password)
            user.save(update_fields=['password'])
            AccountPlan.objects.update_or_create(user=user, defaults={'plan': plan, 'active': True, 'expires_at': None})
            credentials[plan] = {'username': user.username, 'password': password}
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(credentials), encoding='utf-8')
        self.stdout.write('Cuentas Free y Pro preparadas; credenciales fuera de Git.')
