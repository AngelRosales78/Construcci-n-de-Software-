"""Capture official SEC facts and filer identity; never publish scores from a failed request."""
import argparse
import csv
import gzip
import hashlib
import json
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
from pathlib import Path
import requests

ROOT = Path(__file__).resolve().parents[1]


def collect(catalog, destination, contact_file, workers=3):
    contact = contact_file.read_text(encoding='utf-8').strip()
    if '@' not in contact or any(c in contact for c in '\r\n'):
        raise ValueError('Indique un correo de contacto válido en el archivo privado.')
    headers = {'User-Agent': 'DiegoMagallanesFinanceResearch ' + contact,
               'Accept-Encoding': 'gzip, deflate', 'Accept': 'application/json'}
    with catalog.open(encoding='utf-8-sig', newline='') as stream:
        companies = list(csv.DictReader(stream))
    identifiers = sorted({int(row['CIK']) for row in companies})
    extra_path = ROOT/'docs/datos/altman_extra_identities.json'
    if extra_path.exists():
        extra = json.loads(extra_path.read_text(encoding='utf-8'))['companies']
        identifiers = sorted(set(identifiers) | {int(row['cik']) for row in extra})
    destination.mkdir(parents=True, exist_ok=True)
    lock, stopped = threading.Lock(), threading.Event()
    last_request = [0.0]

    def capture(cik, kind):
        filename = destination / f'CIK{cik:010d}-{kind}.json.gz'
        metadata = filename.with_suffix('.source.json')
        url = (f'https://data.sec.gov/api/xbrl/companyfacts/CIK{cik:010d}.json' if kind == 'facts'
               else f'https://data.sec.gov/submissions/CIK{cik:010d}.json')
        if filename.exists() and metadata.exists():
            raw = gzip.decompress(filename.read_bytes())
            source = json.loads(metadata.read_text(encoding='utf-8'))
            if source['url'] == url and hashlib.sha256(raw).hexdigest() == source['sha256']:
                return 'cached'
            raise ValueError('Copia local alterada: ' + filename.name)
        if stopped.is_set():
            return 'stopped'
        # One shared throttle across all workers: at most two requests per second.
        with lock:
            delay = .5 - (time.monotonic() - last_request[0])
            if delay > 0:
                time.sleep(delay)
            last_request[0] = time.monotonic()
        if stopped.is_set():
            return 'stopped'
        try:
            response = requests.get(url, headers=headers, timeout=(12, 60))
            if response.status_code in (403, 429):
                stopped.set()
                return 'access_' + str(response.status_code)
            if response.status_code != 200:
                return 'http_' + str(response.status_code)
            raw = response.content
            data = json.loads(raw)
            if int(data['cik']) != cik:
                return 'identity_mismatch'
            filename.write_bytes(gzip.compress(raw, mtime=0))
            metadata.write_text(json.dumps({'url': url, 'sha256': hashlib.sha256(raw).hexdigest(),
                'retrieved_at': datetime.now(timezone.utc).isoformat()}, indent=2), encoding='utf-8')
            return 'downloaded'
        except (requests.RequestException, ValueError, KeyError):
            return 'request_failed'

    results = {}
    with ThreadPoolExecutor(max_workers=workers) as pool:
        pending = {pool.submit(capture, cik, kind): (cik, kind)
                   for cik in identifiers for kind in ('facts', 'identity')}
        for index, future in enumerate(as_completed(pending), 1):
            cik, kind = pending[future]
            results[f'{cik:010d}-{kind}'] = future.result()
            if index % 25 == 0 or stopped.is_set():
                successful = sum(value in ('downloaded', 'cached') for value in results.values())
                print(f'SEC: {index}/{len(pending)} consultas resueltas; {successful} copias válidas.', flush=True)
    report = {'completed_at': datetime.now(timezone.utc).isoformat(), 'companies': len(identifiers),
              'stopped_for_access': stopped.is_set(), 'results': results}
    (destination / 'capture-report.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--catalog', type=Path, default=ROOT/'docs/datos/catalogo_sp500.csv')
    parser.add_argument('--destination', type=Path, default=ROOT.parent/'data_sources/altman_sec/catalog')
    parser.add_argument('--contact-file', type=Path, required=True)
    args = parser.parse_args()
    result = collect(args.catalog, args.destination, args.contact_file)
    print('Captura terminada. Acceso interrumpido:', result['stopped_for_access'])
