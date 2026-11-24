"""
API app configuration.
"""
from django.apps import AppConfig


class ApiConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'api'
    verbose_name = 'API - Authentication & Users'

    def ready(self):
        from . import signals  # noqa: F401
