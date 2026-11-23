from django.conf import settings
from django.core.mail import send_mail
from django.core.management.base import BaseCommand, CommandError


class Command(BaseCommand):
    help = 'Verifica SMTP exclusivamente con la cuenta propia autorizada.'

    def handle(self, *args, **options):
        recipient = settings.EMAIL_HOST_USER.strip().lower()
        if not settings.NOTIFICATION_EMAIL_ENABLED or recipient not in settings.NOTIFICATION_ALLOWED_RECIPIENTS:
            raise CommandError('El canal propio no tiene autorización o configuración.')
        if settings.EMAIL_BACKEND != 'django.core.mail.backends.smtp.EmailBackend':
            raise CommandError('Esta verificación requiere el transporte SMTP real.')
        count = send_mail(
            'Tu canal de avisos está disponible',
            'Se ha verificado la conexión de correo de S&P 500 Finance.\n'
            'Este mensaje confirma el transporte y no comunica cambios financieros.\n'
            'Puedes elegir tus canales en Preferencias de avisos.',
            settings.DEFAULT_FROM_EMAIL, [recipient], fail_silently=False,
        )
        if count != 1:
            raise CommandError('El servidor no confirmó el envío.')
        self.stdout.write(self.style.SUCCESS('El servidor SMTP aceptó el mensaje para la cuenta propia autorizada.'))
