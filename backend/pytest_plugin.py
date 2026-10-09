"""
Pytest plugin to ensure DATABASES is always configured for pytest-django.
"""
import pytest


@pytest.hookimpl(tryfirst=True)
def pytest_configure(config):
    """Ensure settings are configured before pytest-django runs."""
    import os
    os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'settings_test')
    
    # Import and patch settings_test before pytest-django can touch it
    import importlib.util
    spec = importlib.util.spec_from_file_location('settings_test', 'settings_test.py')
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    
    from django.conf import settings
    if not hasattr(settings, 'DATABASES') or not settings.DATABASES:
        # Re-configure if pytest-django cleared it
        settings.configure(
            DEBUG=True,
            SECRET_KEY='test-secret-key',
            DATABASES={
                'default': {
                    'ENGINE': 'django.db.backends.sqlite3',
                    'NAME': ':memory:',
                }
            },
            INSTALLED_APPS=[
                'django.contrib.auth',
                'django.contrib.contenttypes',
                'django.contrib.sessions',
                'django.contrib.messages',
                'rest_framework',
                'rest_framework_simplejwt',
                'corsheaders',
                'api',
            ],
            MIDDLEWARE=[
                'django.middleware.security.SecurityMiddleware',
                'django.contrib.sessions.middleware.SessionMiddleware',
                'django.middleware.common.CommonMiddleware',
                'django.middleware.csrf.CsrfViewMiddleware',
                'django.contrib.auth.middleware.AuthenticationMiddleware',
                'django.contrib.messages.middleware.MessageMiddleware',
            ],
            AUTH_USER_MODEL='api.User',
            ROOT_URLCONF='',
            USE_TZ=True,
            DEFAULT_AUTO_FIELD='django.db.models.BigAutoField',
        )


@pytest.hookimpl(trylast=True)
def pytest_django_modify_settings(config, settings):
    """Prevent pytest-django from clearing DATABASES."""
    pass
