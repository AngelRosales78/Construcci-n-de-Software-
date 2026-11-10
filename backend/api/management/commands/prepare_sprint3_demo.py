"""Dos cuentas desechables; nunca habilita Pro en una base de trabajo."""
import json
import secrets
from pathlib import Path
from django.conf import settings
from django.contrib.auth import get_user_model
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
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
        try:
            previous = json.loads(target.read_text(encoding='utf-8')) if target.exists() else {}
            if not isinstance(previous, dict):
                raise ValueError('El archivo de cuentas debe ser un objeto JSON.')
            credentials = {}
            for plan in ('free', 'pro'):
                stored = previous.get(plan, {})
                # La contraseña elegida localmente se conserva. Una instalación nueva
                # genera una clave corta válida, sin guardar contraseñas en el código.
                password = stored.get('password') or f'Demo!{secrets.randbelow(9000) + 1000}'
                validate_password(password)
                credentials[plan] = {'username': f'sprint3_{plan}', 'password': password}
        except (OSError, ValueError, AttributeError, TypeError, ValidationError) as exc:
            raise CommandError('Revise el archivo local de cuentas: cada contraseña debe cumplir las reglas de acceso.') from exc
        with transaction.atomic():
            for plan, account in credentials.items():
                user, _ = get_user_model().objects.get_or_create(username=account['username'],
                    defaults={'email': f'sprint3_{plan}@example.test'})
                user.set_password(account['password'])
                user.save(update_fields=['password'])
                AccountPlan.objects.update_or_create(user=user, defaults={'plan': plan, 'active': True, 'expires_at': None})
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(credentials), encoding='utf-8')
        self.stdout.write('Cuentas Free y Pro preparadas; claves conservadas fuera de Git.')
