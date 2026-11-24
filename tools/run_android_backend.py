"""Start the local backend, retaining local accounts on first setup."""
import argparse
import os
from pathlib import Path
import sqlite3
import sys

project = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser()
parser.add_argument('--port', type=int, default=8000)
parser.add_argument('--base-project', type=Path, default=project.parent / 'Script 2')
parser.add_argument('--data-source', type=Path, help='Existing SQLite database used only on first setup')
parser.add_argument('--database', type=Path, help='Otra base SQLite ya preparada, por ejemplo una copia de ensayo')
parser.add_argument('--prepare-only', action='store_true')
parser.add_argument('--grant-admin', metavar='USUARIO', help='Dar rol de administrador a una cuenta existente y salir')
parser.add_argument('--revoke-admin', metavar='USUARIO', help='Retirar el rol de administrador y salir')
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
database = args.database.resolve() if args.database else project / 'android-local.sqlite3'
if not database.exists() or database.stat().st_size == 0:
    original = (args.data_source or args.base_project / 'android-local.sqlite3').resolve()
    if not original.is_file() or original.stat().st_size == 0:
        raise SystemExit('Falta la base local preparada. Indícala con --data-source.')
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


def notices():
    """HU12: avisos y entregas mientras el servidor está abierto; lo pendiente queda en la base."""
    import time
    from django.db import close_old_connections
    from api.delivery_services import process_delivery
    from api.notification_services import process_notification_event, seed_rating_states
    seeded = False
    while True:
        try:
            close_old_connections()
            if not seeded:
                seed_rating_states()
                seeded = True
            while process_notification_event() or process_delivery():
                pass
        except Exception as error:
            print(f'Avisos: se reintentará ({type(error).__name__}).', file=sys.stderr, flush=True)
            time.sleep(10)
        time.sleep(2)


if args.grant_admin or args.revoke_admin:
    call_command('grant_admin', args.grant_admin or args.revoke_admin, revoke=bool(args.revoke_admin))
elif not args.prepare_only:
    try:
        call_command('preload_search')
    except Exception as error:
        print(f'Autocompletado sin precargar ({type(error).__name__}).', file=sys.stderr, flush=True)
    import threading
    threading.Thread(target=notices, name='notices', daemon=True).start()
    call_command('runserver', f'127.0.0.1:{args.port}', use_reloader=False)
