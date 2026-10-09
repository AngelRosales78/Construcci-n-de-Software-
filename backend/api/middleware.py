"""
Middleware for Redis-based JWT token blacklist verification.

Intercepts all authenticated requests and checks if the bearer token
has been blacklisted in Redis before allowing access.
"""
import re
from django.utils.deprecation import MiddlewareMixin
from django.http import JsonResponse
from rest_framework_simplejwt.tokens import AccessToken
from rest_framework_simplejwt.exceptions import InvalidToken, TokenError
from .redis_utils import is_token_blacklisted


class RedisTokenBlacklistMiddleware(MiddlewareMixin):
    """
    Middleware that checks the Redis blacklist for JWT tokens.

    This middleware intercepts requests with Authorization headers,
    extracts the JWT token, and verifies it hasn't been blacklisted
    after logout (HU01_04).
    """

    def process_request(self, request):
        """
        Check if the request's JWT token is blacklisted.
        Skips authentication checks for unauthenticated endpoints.
        """
        auth_header = request.META.get('HTTP_AUTHORIZATION', '')

        if not auth_header.startswith('Bearer '):
            return None

        token_string = auth_header.split('Bearer ')[1].split()[0]

        # Skip blacklist check for token endpoint
        blacklist_exempt_patterns = [
            r'^/api/v1/auth/token/$',
            r'^/api/v1/auth/register/$',
            r'^/api/v1/auth/password/reset/$',
            r'^/api/v1/auth/password/reset/confirm/$',
            r'^/api/v1/auth/refresh/$',
        ]

        for pattern in blacklist_exempt_patterns:
            if re.match(pattern, request.path):
                return None

        try:
            if is_token_blacklisted(token_string):
                return JsonResponse(
                    {'error': 'Token has been blacklisted. Please login again.',
                     'code': 'token_blacklisted'},
                    status=401
                )
        except Exception:
            pass

        return None
