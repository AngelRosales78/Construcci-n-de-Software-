"""Exercise real local financial data, rolling back every verification-only write."""
import json
import os
from pathlib import Path
import runpy
import sys
import time
import uuid

project = Path(__file__).resolve().parent.parent
sys.argv = [str(project / 'tools/run_android_backend.py'), '--prepare-only']
runpy.run_path(sys.argv[0], run_name='server_verification_setup')
from django.db import transaction
from django.test import override_settings
from rest_framework.test import APIClient
from api.models import User, Company

output = Path(os.environ.get('SPRINT3_QA_OUTPUT', str(project / 'docs/android/evidence')))
output.mkdir(parents=True, exist_ok=True)
summary = {'schema_version': 'sprint3-services-verification-v1', 'checks': {}, 'real_companies': Company.objects.count()}
client = APIClient(HTTP_HOST='127.0.0.1')

def call(method, path, body=None, expected=(200,)):
    response = getattr(client, method)('/api/v1/' + path, body, format='json')
    if response.status_code not in expected:
        raise AssertionError(f'{method} {path}: HTTP {response.status_code}: {getattr(response, "data", None)}')
    return response

with transaction.atomic(), override_settings(LOCAL_DEVELOPMENT_PRO=True):
    user = User.objects.create_user(username='verification-' + uuid.uuid4().hex[:15])
    client.force_authenticate(user)
    favorite = call('post', 'favorites/', {'ticker': 'MSFT'}, (201,)).data['favorite']
    assert call('get', 'favorites/').data['results'][0]['ticker'] == 'MSFT'
    tag = call('post', 'tags/', {'name': 'Verificación temporal', 'color': '#00ff87', 'version': '0'}, (201,)).data
    assigned = call('put', f'favorites/{favorite["id"]}/tags/', {'tag_ids': [tag['tags'][0]['id']], 'version': tag['version']}).data
    assert assigned['assignments'][0]['tag_ids'] == [tag['tags'][0]['id']]
    call('post', 'companies/MSFT/visits/', {'event_id': 'verify-visit'})
    assert call('get', 'companies/recent/').data['results'][0]['ticker'] == 'MSFT'
    summary['checks']['personal_collections'] = 'passed'
    assert call('get', 'user/subscription/').data['plan'] == 'pro'
    comparison = call('get', 'compare/?a=MSFT&b=AAPL&fiscal_year=2024').data
    assert len(comparison['comparisons']) == 5
    summary['checks']['comparison_and_access'] = 'passed'
    conclusion = call('get', 'compare/conclusion/?a=MSFT&b=AAPL&fiscal_year=2024&level=technical').data
    os.environ['LOCAL_AI_MODEL'] = ''  # PDF verification labels this deterministic snapshot as rules.
    individual = call('post', 'companies/MSFT/analysis/', {'fiscal_year': 2024, 'level': 'technical'}).data
    for kind, analysis in [('company', individual), ('comparison', conclusion)]:
        version = (f'{analysis["data_version"]}/{analysis["generated_at"]}' if kind == 'comparison' else
                   f'{analysis["metrics_version"]}/{analysis["data_as_of"]}/{analysis["generated_at"]}')
        value = {'request_id': 'verification-' + kind, 'kind': kind, 'a': 'MSFT', 'b': 'AAPL' if kind == 'comparison' else None,
                 'fiscal_year': 2024, 'range': '1Y', 'include_charts': True, 'include_analysis': True,
                 'analysis_level': 'technical', 'analysis_version': version, 'analysis_origin': 'rules'}
        started = time.monotonic()
        job = call('post', 'reports/jobs/', value, (202,)).data
        ready = call('get', f'reports/jobs/{job["id"]}/').data
        assert ready['status'] == 'ready', ready
        downloaded = call('get', f'reports/jobs/{job["id"]}/file/')
        file = output / f'sprint3-{kind}.pdf'
        file.write_bytes(downloaded.content)
        summary['checks'][kind + '_pdf'] = {'status': 'passed', 'size_bytes': len(downloaded.content),
            'pages': ready['page_count'], 'generation_and_download_seconds': round(time.monotonic()-started, 3),
            'chart_format': ready['chart_format'], 'analysis_origin': ready['analysis_origin']}
    defaults = call('get', 'companies/MSFT/simulations/defaults/').data
    summary['checks']['simulation_defaults'] = 'passed'
    transaction.set_rollback(True)
summary['verification_writes'] = 'rolled back; no user favorites/accounts changed'
(output / 'sprint3_services.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(summary, ensure_ascii=False, indent=2))
