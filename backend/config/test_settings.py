"""Configuración aislada para pruebas funcionales; no mide rendimiento PostgreSQL."""
from .settings import *

DATABASES = {'default': {'ENGINE': 'django.db.backends.sqlite3', 'NAME': ':memory:'}}
