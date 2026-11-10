"""Pruebas reales de Sprint 3, exclusivamente contra la demo local aislada.

No imprime sesiones ni contraseñas. Guarda PDFs y resultados fuera de Git.
Con --android ejecuta además los repositorios Kotlin contra esta misma API.
"""
import argparse
from datetime import datetime, timezone
import hashlib
from io import BytesIO
import json
import os
from pathlib import Path
import secrets
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT.parent / 'presentation_runtime_sprint3'
OUTPUT = ROOT.parent / 'output/sprint3_cierre'
BASE = 'http://127.0.0.1:58020/api/v1/'


def request(path, token=None, method='GET', data=None, expected=(200,), binary=False):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(BASE + path, data=json.dumps(data).encode() if data is not None else None,
                                 headers=headers, method=method)
    try:
        response = urllib.request.urlopen(req, timeout=180)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        raw = response.read()
        assert response.code in expected, (path, response.code, raw[:600])
        return raw if binary else json.loads(raw) if raw else None


def account():
    name = 's3qa_' + secrets.token_hex(5)
    password = 'Aa9!' + secrets.token_urlsafe(25)
    session = request('auth/register/', method='POST', expected=(201,), data={
        'username': name, 'email': name + '@example.test', 'password': password,
        'password_confirm': password, 'user_type': 'retail'})
    return session | {'qa_login': {'username': name, 'password': password}}


def report(token, options):
    started = time.perf_counter()
    payload = {'request_id': str(uuid.uuid4()), 'kind': 'company', 'a': 'GOOGL', 'b': None,
               'fiscal_year': 2024, 'range': '1Y', 'include_charts': True, 'include_analysis': False,
               'analysis_level': 'simple', 'analysis_version': None, 'analysis_origin': None} | options
    job = request('reports/jobs/', token, 'POST', payload, (200, 202))
    accepted = time.perf_counter() - started
    same = request('reports/jobs/', token, 'POST', payload, (200, 202))
    assert same['id'] == job['id'], 'El reintento duplicó un reporte'
    while job['status'] in ('queued', 'running'):
        assert time.perf_counter() - started < 60, 'No terminó el generador PDF'
        time.sleep(.05)
        job = request(f"reports/jobs/{job['id']}/", token)
    assert job['status'] == 'ready', job
    ready = time.perf_counter() - started
    raw = request(f"reports/jobs/{job['id']}/file/", token, binary=True)
    total = time.perf_counter() - started
    assert raw.startswith(b'%PDF-') and b'%%EOF' in raw[-1024:]
    assert len(raw) == job['size_bytes'] and hashlib.sha256(raw).hexdigest() == job['content_sha256']
    from pypdf import PdfReader
    reader = PdfReader(BytesIO(raw))
    assert len(reader.pages) == job['page_count']
    assert all(not page.images for page in reader.pages), 'El PDF contiene imágenes rasterizadas'
    text = '\n'.join(page.extract_text() for page in reader.pages)
    assert payload['a'] in text and str(payload['fiscal_year']) in text and 'Fuentes y metodología' in text
    if payload['kind'] == 'comparison':
        assert len(reader.pages) <= 2 and all(page.mediabox.width > page.mediabox.height for page in reader.pages)
    (OUTPUT / f"{payload['kind']}_{payload['a']}_{payload['range']}.pdf").write_bytes(raw)
    return job, {'kind': payload['kind'], 'range': payload['range'], 'cache_hit': job['cache_hit'],
                 'acceptance_ms': round(accepted * 1000, 2), 'ready_ms': round(ready * 1000, 2),
                 'full_download_ms': round(total * 1000, 2), 'size_bytes': len(raw), 'pages': len(reader.pages),
                 'generation_ms': job['generation_ms']}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--android', action='store_true')
    args = parser.parse_args()
    os.environ.update(DJANGO_SETTINGS_MODULE='config.settings', USE_POSTGRES='True', DB_NAME='sprint3_demo_validation',
                      DB_HOST='127.0.0.1', DB_PORT='55432', DB_USER='postgres', DB_PASSWORD='')
    sys.path.insert(0, str(ROOT / 'backend'))
    import django
    django.setup()
    from django.db import connection
    from api.models import AccountPlan
    assert connection.settings_dict['NAME'] == 'sprint3_demo_validation'
    OUTPUT.mkdir(parents=True, exist_ok=True)
    STATE.mkdir(parents=True, exist_ok=True)
    pro, free = account(), account()
    # Solo estas cuentas nuevas de prueba reciben el permiso local, nunca las cuentas del usuario.
    pro_id = pro['user']['id']
    AccountPlan.objects.update_or_create(user_id=pro_id, defaults={'plan': 'pro', 'active': True})
    (STATE / 'qa_session.json').write_text(json.dumps(pro), encoding='utf-8')
    p, f = pro['access'], free['access']
    evidence = {'checked_at': datetime.now(timezone.utc).isoformat(), 'api': BASE, 'cases': [], 'pdf_trials': []}
    checks = evidence['cases']
    query = 'a=GOOGL&b=META&fiscal_year=2024'
    for route in ('compare/', 'compare/history/', 'compare/conclusion/'):
        request(route + '?' + query, f, expected=(403,))
        request(route + '?' + query, expected=(401,))
    pair = request('compare/?' + query, p)
    assert len(pair['comparisons']) == 5 and pair['status'] == 'partial'
    assert sum(row['status'] == 'available' for row in pair['comparisons']) == 3
    assert all(row['delta'] is None and row['reason'] for row in pair['comparisons'] if row['status'] != 'available')
    assert request('compare/?a=MSFT&b=GOOGL&fiscal_year=2024', p)['status'] == 'not_comparable'
    history = request('compare/history/?' + query + '&range=1Y', p)
    assert history['status'] == 'available' and len(history['points']) > 200
    assert history['points'][0]['a'] == history['points'][0]['b'] == 0
    (OUTPUT / 'history.json').write_text(json.dumps(history, indent=2), encoding='utf-8')
    checks += ['Pro, Free y anónimo en las tres rutas comparativas', 'Cinco filas, tres notas disponibles y dos ausentes sin inventar valores; rechazo de periodos diferentes',
               'Histórico normalizado por servidor con fechas comunes y base cero']
    conclusion = request('compare/conclusion/?' + query + '&level=simple', p)
    assert conclusion['generation_mode'] == 'ai' and conclusion['sources'], 'No se acreditó inferencia local real'
    again = request('compare/conclusion/?' + query + '&level=simple', p)
    assert again['cache_hit'] and again['generated_at'] == conclusion['generated_at']
    checks.append('Qwen local real y caché sin alterar la fecha ni la versión del análisis')
    (OUTPUT / 'conclusion.json').write_text(json.dumps(conclusion, ensure_ascii=False, indent=2), encoding='utf-8')
    fav = request('favorites/', p, 'POST', {'ticker': 'GOOGL'}, (200, 201))['favorite']
    assert request('favorites/', p, 'POST', {'ticker': 'GOOGL'})['favorite']['id'] == fav['id']
    tags = request('tags/', p, 'POST', {'name': 'Tecnología QA', 'color': '#158569'}, (201,))
    tag = tags['tags'][0]
    tags = request(f"favorites/{fav['id']}/tags/", p, 'PUT', {'tag_ids': [tag['id']], 'version': tags['version']})
    request(f"tags/{tag['id']}/", p, 'PATCH', {'name': 'Cambio obsoleto', 'color': '#158569', 'version': 0}, (409,))
    request(f"tags/{tag['id']}/", f, 'PATCH', {'name': 'Ajeno', 'color': '#158569', 'version': tags['version']}, (404,))
    assert request('tags/', f)['tags'] == []
    request(f"tags/{tag['id']}/?version={tags['version']}", p, 'DELETE')
    assert request('favorites/', p)['results'][0]['id'] == fav['id']
    request(f"favorites/{fav['id']}/", p, 'DELETE', expected=(204,))
    checks += ['Favorito único, eliminación real y aislamiento de cuentas', 'Etiquetas privadas, asignación, conflicto 409 y eliminación sin borrar el favorito']
    companies = request('companies/search/?page_size=12', p)
    rows = companies['results'][:12]
    assert len(rows) == 12
    for row in rows:
        request(f"companies/{row['ticker']}/visits/", p, 'POST', {'event_id': str(uuid.uuid4())}, (200, 201))
    recent = request('companies/recent/', p)['results']
    assert len(recent) == 10 and len({r['ticker'] for r in recent}) == 10
    checks.append('Doce visitas conservan las diez empresas únicas más recientes')
    for kind in ('company', 'comparison'):
        for period in ('1M', '6M', '1Y', '5Y', '1Y'):
            options = {'kind': kind, 'b': 'META' if kind == 'comparison' else None, 'range': period}
            if kind == 'comparison':
                options |= {'include_analysis': True, 'analysis_version': conclusion['data_version'] + '/' + conclusion['generated_at'], 'analysis_origin': 'ai'}
            job, trial = report(p, options)
            evidence['pdf_trials'].append(trial)
    analysis = request('companies/MSFT/analysis/?fiscal_year=2024&level=simple', p)
    assert analysis['validated'] and analysis['generation_mode'] == 'ai'
    from api.models import AnalysisRecord
    count_before = AnalysisRecord.objects.count()
    _, trial = report(p, {'a': 'MSFT', 'include_analysis': True, 'analysis_origin': 'ai',
        'analysis_version': f"{analysis['metrics_version']}/{analysis['period']['end']}/{analysis['generated_at']}"})
    assert AnalysisRecord.objects.count() == count_before, 'Exportar no debe volver a generar IA'
    from pypdf import PdfReader
    text = ''.join(page.extract_text() for page in PdfReader(OUTPUT / 'company_MSFT_1Y.pdf').pages)
    assert ''.join(analysis['analysis']['summary']['text'].split()) in ''.join(text.split())
    evidence['individual_ai_pdf'] = trial
    checks.append('PDF individual conserva el análisis validado de Microsoft sin volver a ejecutar el modelo')
    request(f"reports/jobs/{job['id']}/", f, expected=(404,))
    request(f"reports/jobs/{job['id']}/file/", f, expected=(404,))
    AccountPlan.objects.filter(user_id=pro_id).update(active=False)
    request(f"reports/jobs/{job['id']}/", p, expected=(403,))
    request(f"reports/jobs/{job['id']}/file/", p, expected=(403,))
    AccountPlan.objects.filter(user_id=pro_id).update(active=True)
    checks += ['PDFs reales vectoriales, sin imágenes, hash/longitud íntegros y reintento idempotente',
               'Reporte comparativo horizontal de hasta dos páginas con la misma IA preparada',
               'Descarga privada y revocación Pro comprobadas sobre un PDF ya generado']
    if args.android:
        build_root = (STATE / 'android-build').as_posix()
        (STATE / 'android-build.init.gradle').write_text(
            f"allprojects {{ buildDir = new File('{build_root}', project.name) }}\n", encoding='utf-8')
        env = os.environ.copy()
        env.update(JAVA_HOME='C:/Users/Magal/.jdks/ms-17.0.20.1', ANDROID_HOME='C:/Users/Magal/AppData/Local/Android/Sdk',
                   SPRINT_API_URL=BASE, SPRINT_TEST_ACCESS=p, SPRINT_FREE_ACCESS=f)
        gradle = 'C:/Users/Magal/.gradle/wrapper/dists/gradle-8.4-bin/1w5dpkrfk8irigvoxmyhowfim/gradle-8.4/bin/gradle.bat'
        command = [gradle, '-p', str(ROOT / 'android'), '--project-cache-dir', str(STATE / 'android-project-cache'),
                   '-I', str(STATE / 'android-build.init.gradle'), '-PapiBaseUrl=http://10.0.2.2:58020/api/v1/',
                   'cleanTestDebugUnitTest', 'testDebugUnitTest', '--offline', '--no-daemon', '--console=plain']
        with (OUTPUT / 'android-live.log').open('w', encoding='utf-8') as log:
            result = subprocess.run(command, env=env, stdout=log, stderr=subprocess.STDOUT)
        assert result.returncode == 0, 'Revisar android-live.log'
        checks.append('Pruebas Kotlin por HTTP real, con permisos y cuentas aisladas')
    evidence['pdf_goal_3sec_2mb'] = all(t['full_download_ms'] < 3000 and t['size_bytes'] < 2_000_000 for t in evidence['pdf_trials'])
    (OUTPUT / 'live_results.json').write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f"Verificados {len(checks)} escenarios, {len(evidence['pdf_trials'])} ensayos PDF y una ficha con IA. Evidencia: {OUTPUT}")
    print('Objetivo PDF de 3 s / 2 MB:', evidence['pdf_goal_3sec_2mb'])


if __name__ == '__main__':
    main()
