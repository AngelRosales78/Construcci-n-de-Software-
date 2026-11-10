"""Local Android development with a separate SQLite database, never Script 1's DB."""
from .settings import *

ANDROID_PROJECT_NAME = BASE_DIR.name
DATABASES = {'default': {'ENGINE': 'django.db.backends.sqlite3',
    'NAME': config('ANDROID_DATABASE_PATH', default=str(BASE_DIR / 'android-local.sqlite3'))}}
EMAIL_BACKEND = 'django.core.mail.backends.console.EmailBackend'
# Academic local installation only. Production does not grant Pro implicitly.
LOCAL_DEVELOPMENT_PRO = config('ANDROID_LOCAL_PRO', default=True, cast=bool)
ALLOWED_HOSTS = config('ANDROID_ALLOWED_HOSTS', default='localhost,127.0.0.1,10.0.2.2').split(',')

# Fail quickly when optional search caching is unavailable. Session rotation/revocation still requires Redis.
REDIS_URL = config('ANDROID_REDIS_URL', default='redis://127.0.0.1:6379/0?socket_connect_timeout=0.5&socket_timeout=0.5')
