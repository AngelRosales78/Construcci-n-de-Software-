"""El primer administrador se autoriza desde el equipo del servidor; los demás, desde la app."""
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.db.models import Q
from api.models import AdministrationEvent, User


class Command(BaseCommand):
    help = 'Dar (o retirar con --revoke) el rol de administrador a una cuenta existente.'

    def add_arguments(self, parser):
        parser.add_argument('account', help='Usuario o correo de la cuenta')
        parser.add_argument('--revoke', action='store_true')

    def handle(self, account, revoke, **options):
        with transaction.atomic():
            found = list(User.objects.select_for_update().filter(Q(username=account) | Q(email__iexact=account))[:2])
            if len(found) != 1:
                raise CommandError('No existe esa cuenta. Usa el nombre de usuario exacto.' if not found
                                   else 'Hay más de una coincidencia. Usa el nombre de usuario exacto.')
            user = found[0]
            before = user.is_staff and user.is_superuser
            if before == (not revoke):
                self.stdout.write('La cuenta ya tenía ese rol; no se cambió nada.')
                return
            if revoke and User.objects.filter(is_active=True, is_staff=True, is_superuser=True).count() <= 1:
                raise CommandError('Debe conservarse al menos un administrador activo.')
            user.is_staff = user.is_superuser = not revoke
            user.session_version += 1
            user.save(update_fields=['is_staff', 'is_superuser', 'session_version'])
            AdministrationEvent.objects.create(subject=user, action='role_set_locally',
                changes={'role': {'before': 'admin' if before else 'user', 'after': 'user' if revoke else 'admin'}})
        self.stdout.write('Rol actualizado. Inicia sesión de nuevo en la app.')
