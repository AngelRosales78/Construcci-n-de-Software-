"""Carga real de HU03_01 con credenciales desechables fuera del repositorio."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
import requests

parser = argparse.ArgumentParser()
parser.add_argument('--credentials-file', required=True)
parser.add_argument('--host', default='http://127.0.0.1:58000')
parser.add_argument('--output', required=True)
args = parser.parse_args()
credentials = json.loads(Path(args.credentials_file).read_text())
response = requests.post(args.host + '/api/v1/auth/token/', json=credentials, timeout=10)
response.raise_for_status()
env = os.environ.copy()
env['LOCUST_ACCESS_TOKEN'] = response.json()['access']
env['LOCUST_SEARCH_QUERIES'] = 'MSFT,AAPL,Microsoft,Apple'
output = Path(args.output).resolve()
output.parent.mkdir(parents=True, exist_ok=True)
env['LOCUST_FINAL_REPORT'] = str(output.with_suffix('.json'))
result = subprocess.run([sys.executable, '-m', 'locust', '-f', 'backend/locustfile.py',
                         'SearchPerformanceUser', '--headless', '--host', args.host,
                         '-u', '10', '-r', '2', '--run-time', '30s', '--only-summary',
                         '--csv', str(output)], env=env)
sys.exit(result.returncode)
