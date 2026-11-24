"""Pruebas sobre el mismo motor SQLite en archivo que usa el servidor local de Android."""
import tempfile
from pathlib import Path
from .settings import *

_name = str(Path(tempfile.gettempdir()) / 'sp500-android-tests.sqlite3')
DATABASES = {'default': {'ENGINE': 'config.sqlite_immediate', 'NAME': _name,
    'TEST': {'NAME': _name}, 'OPTIONS': {'timeout': 20}}}
