from rest_framework_simplejwt.exceptions import AuthenticationFailed
from rest_framework_simplejwt.authentication import JWTAuthentication


class SessionJWTAuthentication(JWTAuthentication):
    def get_user(self, validated_token):
        user = super().get_user(validated_token)
        if validated_token.get('sv', 0) != user.session_version:
            raise AuthenticationFailed('La sesión fue revocada. Inicia sesión nuevamente.', code='token_blacklisted')
        return user
