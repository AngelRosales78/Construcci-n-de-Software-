"""Start Sprint 3 with its own database, retaining local accounts on first setup."""
import argparse
import os
from pathlib import Path
import sqlite3
import sys

project = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser()
parser.add_argument('--port', type=int, default=8000)
parser.add_argument('--base-project', type=Path, default=project.parent / 'Script 2')
parser.add_argument('--prepare-only', action='store_true')
args = parser.parse_args()
sys.path.insert(0, str(project / '.server-packages'))
sys.path.insert(0, str(project / 'backend'))
from decouple import AutoConfig

# Reuse only the required local settings in memory. Never copy or print secrets.
previous = AutoConfig(search_path=str(args.base_project))
for key in ('SECRET_KEY', 'LOCAL_AI_MODEL', 'LOCAL_AI_URL', 'ANDROID_REDIS_URL'):
    value = previous(key, default=None)
    if value is not None:
        os.environ.setdefault(key, str(value))
database = project / 'android-local.sqlite3'
if not database.exists() or database.stat().st_size == 0:
    original = args.base_project / 'android-local.sqlite3'
    if not original.is_file() or original.stat().st_size == 0:
        raise SystemExit('Falta la base preparada de Script 2. No se creará una base vacía silenciosamente.')
    with sqlite3.connect(original.as_uri() + '?mode=ro', uri=True) as source:
        with sqlite3.connect(database) as target:
            source.backup(target)
    print('Base independiente preparada mediante copia SQLite consistente; cuentas y empresas conservadas.', flush=True)
os.environ['ANDROID_DATABASE_PATH'] = str(database)
os.environ['DJANGO_SETTINGS_MODULE'] = 'config.android_local'
import django
django.setup()
from django.core.management import call_command
call_command('migrate', interactive=False, verbosity=1)
call_command('check')
if not args.prepare_only:
    call_command('runserver', f'127.0.0.1:{args.port}', use_reloader=False)
