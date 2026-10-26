"""Run Android JVM tests against isolated Django; no credentials are saved."""
import argparse
import io
import json
import os
from pathlib import Path
import secrets
import subprocess
import sys
import threading
import urllib.request
from wsgiref.simple_server import make_server, WSGIRequestHandler


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--work-dir', required=True)
    parser.add_argument('--java-home')
    parser.add_argument('--gradle-home')
    parser.add_argument('--gradle-cache')
    parser.add_argument('--offline', action='store_true')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    work = Path(args.work_dir).resolve()
    work.mkdir(parents=True, exist_ok=True)
    os.environ['DJANGO_SETTINGS_MODULE'] = 'config.android_local'
    os.environ['ANDROID_DATABASE_PATH'] = str(work / 'integration.sqlite3')
    sys.path.insert(0, str(root / 'backend'))
    import django
    django.setup()
    from django.core.management import call_command
    from django.contrib.auth import get_user_model
    from django.core.wsgi import get_wsgi_application
    from django.db import connections
    output = io.StringIO()
    call_command('migrate', interactive=False, stdout=output)
    call_command('import_company_catalog', str(root / 'docs/datos/catalogo_sp500.csv'), stdout=output)
    call_command('import_annual_financials', str(root / 'docs/datos/sprint2_disponibilidad_MSFT.json'), stdout=output)
    call_command('import_market_data', metrics=str(root / 'docs/datos/MSFT_balance_2024.csv'), source='https://www.sec.gov/edgar/browse/?CIK=789019', stdout=output)
    username = 'android_test_' + secrets.token_hex(5)
    password = secrets.token_urlsafe(30)
    user = get_user_model().objects.create_user(username=username, password=password, email=username+'@example.test')
    connections.close_all()
    class QuietHandler(WSGIRequestHandler):
        def log_message(self, *unused):
            pass
    server = make_server('127.0.0.1', 0, get_wsgi_application(), handler_class=QuietHandler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    base = f'http://127.0.0.1:{server.server_port}/api/v1/'
    try:
        request = urllib.request.Request(base+'auth/token/', data=json.dumps({'username':username,'password':password}).encode(), headers={'Content-Type':'application/json'})
        with urllib.request.urlopen(request, timeout=30) as response:
            token = json.load(response)['access']
        env = os.environ.copy()
        env['SPRINT_API_URL'] = base
        env['SPRINT_TEST_ACCESS'] = token
        if args.java_home:
            env['JAVA_HOME'] = args.java_home
        if args.gradle_home:
            java = str(Path(args.java_home)/'bin/java.exe') if args.java_home else 'java'
            launcher = next((Path(args.gradle_home)/'lib').glob('gradle-launcher-*.jar'))
            command = [java, '-classpath', str(launcher), 'org.gradle.launcher.GradleMain']
        else:
            command = [str(root/'android/gradlew.bat')]
        if args.gradle_cache:
            command += ['-g',args.gradle_cache]
        command += ['--project-cache-dir',str(work/'project-cache'),'-p',str(root/'android'),'testDebugUnitTest','compileDebugAndroidTestKotlin','wrapper','--console=plain','--no-daemon']
        if args.offline:
            command += ['--offline']
        print('Servidor aislado preparado con catálogo real y ejercicios MSFT. Ejecutando Android.', flush=True)
        result = subprocess.run(command, env=env)
        if result.returncode:
            return result.returncode
        with urllib.request.urlopen(urllib.request.Request(base+'companies/MSFT/pillars/?fiscal_year=2024', headers={'Authorization':'Bearer '+token}),timeout=30) as response:
            data = json.load(response)
        report = {'verified_on':'2026-10-25','mode':'real Django HTTP + Android Retrofit JVM',
            'django_version':django.get_version(),'ticker':data['ticker'],'fiscal_year':data['fiscal_year'],
            'methodology_version':data['methodology_version'],'overall_score':data['overall_score']['display_value'],
            'pillar_count':len(data['pillars']),'metric_count':sum(len(p['metrics']) for p in data['pillars']),
            'visual_device_test':False,'simulations_server':'not implemented','analysis_server':'not implemented'}
        target=root/'docs/android/evidence/live_server.json'
        target.parent.mkdir(parents=True,exist_ok=True)
        target.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        print('Integración real verificada; evidencia guardada sin credenciales.',flush=True)
        return 0
    finally:
        server.shutdown()
        server.server_close()
        get_user_model().objects.filter(pk=user.pk).delete()

if __name__ == '__main__':
    raise SystemExit(main())
