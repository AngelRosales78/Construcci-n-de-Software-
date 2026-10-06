"""
Redis utility module for token blacklist operations.

Provides connection and operations for Redis-based token blacklisting,
which is essential for the logout flow (HU01_04) and refresh token
management (HU01_05).
"""
import redis
from django.conf import settings
import json
import logging

logger = logging.getLogger(__name__)

_redis_connection = None


def get_redis_connection():
    """
    Returns a singleton Redis connection from the connection pool.
    Uses the REDIS_URL from Django settings.
    """
    global _redis_connection
    if _redis_connection is None:
        try:
            _redis_connection = redis.from_url(
                settings.REDIS_URL,
                encoding='utf-8',
                decode_responses=True
            )
            _redis_connection.ping()
        except redis.ConnectionError as e:
            logger.error(f"Redis connection error: {e}")
            raise
    return _redis_connection


def blacklist_token(token):
    """
    Blacklists a JWT token in Redis with TTL.
    Used during logout (HU01_04) and token rotation.

    Args:
        token: The JWT token string to blacklist.
    """
    try:
        r = get_redis_connection()
        # Extract expiration from token to set matching TTL
        from rest_framework_simplejwt.tokens import Token
        from jwt import decode as jwt_decode
        import datetime

        try:
            payload = jwt_decode(token, options={"verify_signature": False})
            exp = payload.get('exp', 0)
            ttl = max(int(exp - datetime.datetime.fromtimestamp(exp).timestamp()), 0) + 60
        except Exception:
            ttl = 86400  # Default 24 hours

        r.setex(f"blacklist:{token}", ttl, "blacklisted")
        logger.info(f"Token blacklisted successfully. TTL: {ttl}s")
        return True
    except Exception as e:
        logger.error(f"Error blacklisting token: {e}")
        return False


def is_token_blacklisted(token):
    """
    Checks if a token exists in the Redis blacklist.

    Args:
        token: The JWT token string to check.

    Returns:
        bool: True if the token is blacklisted, False otherwise.
    """
    try:
        r = get_redis_connection()
        return r.exists(f"blacklist:{token}") > 0
    except Exception as e:
        logger.error(f"Error checking token blacklist: {e}")
        return False


def blacklist_jwt_token(token):
    """
    Blacklists a SimpleJWT Token object by its jti.
    Used as an alternative to string-based blacklisting.

    Args:
        token: A SimpleJWT Token object.
    """
    try:
        r = get_redis_connection()
        jti = token['jti']
        # Get expiration from the token
        from rest_framework_simplejwt.utils import aware_utcnow, datetime_from_epoch
        exp = datetime_from_epoch(token['exp'])
        now = aware_utcnow()
        ttl = max(int((exp - now).total_seconds()), 0) + 60
        r.setex(f"blacklist:jti:{jti}", ttl, "blacklisted")
        return True
    except Exception as e:
        logger.error(f"Error blacklisting JWT token by JTI: {e}")
        return False


def refresh_token_metadata(old_refresh_token, new_refresh_token):
    """
    Transfers metadata when rotating refresh tokens.

    Args:
        old_refresh_token: The old refresh token string.
        new_refresh_token: The new refresh token string.
    """
    try:
        r = get_redis_connection()
        # Copy user session data if any
        old_key = f"refresh_session:{old_refresh_token}"
        new_key = f"refresh_session:{new_refresh_token}"
        session_data = r.get(old_key)
        if session_data:
            r.setex(new_key, 604800, session_data)  # 7 days
        blacklist_token(old_refresh_token)
        return True
    except Exception as e:
        logger.error(f"Error refreshing token metadata: {e}")
        return False
