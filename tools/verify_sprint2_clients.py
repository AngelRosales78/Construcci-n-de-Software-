"""Ejecuta las pruebas Android por HTTP real contra la demo aislada ya iniciada."""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import secrets
import subprocess
import urllib.request
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--java-home', required=True)
    parser.add_argument('--gradle-home', required=True)
    parser.add_argument('--api-url', default='http://127.0.0.1:58010/api/v1/')
    parser.add_argument('--offline', action='store_true')
    args = parser.parse_args()
    if args.api_url != 'http://127.0.0.1:58010/api/v1/':
        parser.error('Este ejecutor crea una cuenta solo en la demo aislada 58010.')
    root = Path(__file__).resolve().parents[1]
    state = root.parent / 'presentation_runtime_sprint2'
    state.mkdir(parents=True, exist_ok=True)
    build_root = (state / 'android-build').as_posix()
    (state / 'android-build.init.gradle').write_text(
        f"allprojects {{ buildDir = new File('{build_root}', project.name) }}\n", encoding='utf-8')
    identifier = secrets.token_hex(6)
    password = 'Aa9!' + secrets.token_urlsafe(30)
    body = {'username': 'jvm_' + identifier, 'email': f'jvm_{identifier}@example.test',
            'password': password, 'password_confirm': password, 'user_type': 'retail'}
    request = urllib.request.Request(args.api_url + 'auth/register/', data=json.dumps(body).encode(),
                                     headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=30) as response:
        session = json.load(response)
    env = os.environ.copy()
    env['JAVA_HOME'] = args.java_home
    env['SPRINT_API_URL'] = args.api_url
    env['SPRINT_TEST_ACCESS'] = session['access']
    command = [str(Path(args.gradle_home) / 'bin/gradle.bat'), '-p', str(root / 'android'),
               '--project-cache-dir', str(state / 'android-project-cache'),
               '-I', str(state / 'android-build.init.gradle'),
               '-PapiBaseUrl=http://10.0.2.2:58010/api/v1/',
               'testDebugUnitTest', 'assembleDebugAndroidTest', '--no-daemon', '--console=plain']
    if args.offline:
        command.append('--offline')
    print('Pruebas JVM contra la demo real; sesión de prueba en memoria, sin imprimir secretos.', flush=True)
    result = subprocess.run(command, env=env)
    if result.returncode:
        return result.returncode
    suites = [ET.parse(path).getroot() for path in (state / 'android-build/app/test-results/testDebugUnitTest').glob('TEST-*.xml')]
    report = {'checked_at': datetime.now(timezone.utc).isoformat(),
              'mode': 'Android JVM + Retrofit + Django + PostgreSQL + proveedor IA local',
              'tests': sum(int(s.get('tests', '0')) for s in suites),
              'failures': sum(int(s.get('failures', '0')) for s in suites),
              'errors': sum(int(s.get('errors', '0')) for s in suites),
              'skipped': sum(int(s.get('skipped', '0')) for s in suites),
              'live_cases': ['pilares y puntaje', 'catálogo/ficha', 'periodo ausente',
                             'percentiles/restauración', 'IA sencilla/técnica', 'datos ausentes'],
              'device_test': 'Se registra por separado; una prueba JVM no demuestra interfaz en dispositivo.'}
    assert report['tests'] and not any(report[k] for k in ('failures', 'errors', 'skipped')), report
    target = root / 'docs/evidencias/android_integracion_real.json'
    target.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f"Pruebas Android aprobadas: {report['tests']}; evidencia sin credenciales guardada.")
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
