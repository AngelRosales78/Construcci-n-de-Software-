import re
from django.core.exceptions import ValidationError


class StrongPasswordValidator:
    def validate(self, password, user=None):
        if (len(password) < 8 or not re.search(r'[A-Z]', password)
                or not re.search(r'[a-z]', password) or not re.search(r'\d', password)
                or not re.search(r'[^\w\s]', password)):
            raise ValidationError(
                'Usa al menos 8 caracteres, mayúscula, minúscula, número y símbolo.',
                code='password_not_complex',
            )

    def get_help_text(self):
        return 'Al menos 8 caracteres, mayúscula, minúscula, número y símbolo.'
