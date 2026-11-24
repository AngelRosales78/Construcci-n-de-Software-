"""Package captured public responses without substituting dates or generating figures."""
import argparse
import gzip
import hashlib
import json
import re
from pathlib import Path
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]


def package(source, output):
    manifest = json.loads((source / 'manifest.json').read_text(encoding='utf-8'))
    companies = []
    for ticker, evidence in sorted(manifest['companies'].items()):
        if evidence.get('status') != 'downloaded': continue
        if not re.fullmatch(r'[A-Z][A-Z0-9.-]{0,9}', ticker): raise ValueError('Ticker inválido.')
        raw = (source / (ticker + '.json')).read_bytes()
        if hashlib.sha256(raw).hexdigest() != evidence['sha256']: raise ValueError('Huella financiera inválida.')
        companies.append({'ticker': ticker, 'evidence': evidence, 'raw_response': raw.decode('utf-8')})
    benchmark = (source / 'benchmark.json').read_bytes()
    benchmark_evidence = json.loads((source / 'benchmark-source.json').read_text(encoding='utf-8'))
    if hashlib.sha256(benchmark).hexdigest() != benchmark_evidence['sha256']: raise ValueError('Huella del índice inválida.')
    prices = ROOT / 'docs/datos/catalog_prices.json.gz'
    payload = {'version': 1, 'calculation_version': 'provider-financials-v1', 'prepared_at': datetime.now(timezone.utc).isoformat(),
        'price_snapshot_sha256': hashlib.sha256(prices.read_bytes()).hexdigest(),
        'benchmark': {'evidence': benchmark_evidence, 'raw_response': benchmark.decode('utf-8')}, 'companies': companies}
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(gzip.compress(json.dumps(payload, ensure_ascii=False).encode('utf-8'), mtime=0))
    print(f'Copia financiera guardada: {len(companies)} empresas; {output.stat().st_size} bytes.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, default=ROOT.parent/'data_sources/fundamentals/catalog')
    parser.add_argument('--output', type=Path, default=ROOT/'docs/datos/catalog_financials.json.gz')
    args = parser.parse_args()
    package(args.source, args.output)
