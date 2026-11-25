"""Prepare reproducible SEC-backed references from captured files; no network or database writes."""
import argparse
import csv
import gzip
import hashlib
import json
import sys
from collections import Counter
from datetime import date, datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'backend'))
from api.sec_altman import prepare_reference, IncompleteReference
from api.altman_reference import validate_reference


def read_capture(cache, cik, kind):
    path = cache / f'CIK{cik:010d}-{kind}.json.gz'
    raw = gzip.decompress(path.read_bytes())
    source = json.loads(path.with_suffix('.source.json').read_text(encoding='utf-8'))
    expected = (f'https://data.sec.gov/api/xbrl/companyfacts/CIK{cik:010d}.json' if kind == 'facts'
                else f'https://data.sec.gov/submissions/CIK{cik:010d}.json')
    if source['url'] != expected or source['sha256'] != hashlib.sha256(raw).hexdigest():
        raise ValueError('Origen o huella SEC no correspondientes.')
    return json.loads(raw), source


def prepare(inventory_file, cache, output, report_path):
    inventory = json.loads(inventory_file.read_text(encoding='utf-8'))
    catalog = {row['Symbol']: row for row in csv.DictReader((ROOT/'docs/datos/catalogo_sp500.csv').open(encoding='utf-8-sig'))}
    extra_path = ROOT/'docs/datos/altman_extra_identities.json'
    if extra_path.exists():
        for item in json.loads(extra_path.read_text(encoding='utf-8'))['companies']:
            if item['ticker'] in catalog and int(catalog[item['ticker']]['CIK']) != item['cik']:
                raise ValueError('Una identidad adicional contradice el catálogo.')
            catalog[item['ticker']] = {'CIK':item['cik']}
    prices_path = ROOT/'docs/datos/catalog_prices.json.gz'
    raw_prices = prices_path.read_bytes()
    price_hash = hashlib.sha256(raw_prices).hexdigest()
    prices = {row['ticker']: row for row in json.loads(gzip.decompress(raw_prices))['companies']}
    base = json.loads((ROOT/'docs/datos/altman_references.json').read_text(encoding='utf-8'))
    if base['price_snapshot_sha256'] != price_hash:
        raise ValueError('Revisar las referencias previas: cambió la copia de precios.')
    references = {row['ticker']: row['reference'] for row in base['companies']}
    results = []
    for row in inventory:
        ticker = row['company__ticker']
        if ticker in references or ticker == 'MSFT':
            results.append({'ticker': ticker, 'status': 'existing', 'reason': 'Cálculo anterior conservado.'})
            continue
        if ticker not in catalog or ticker not in prices:
            results.append({'ticker': ticker, 'status': 'pending', 'reason': 'Conciliar identidad con el catálogo SEC.'})
            continue
        cik = int(catalog[ticker]['CIK'])
        identity_metadata = {}
        try:
            facts, source = read_capture(cache, cik, 'facts')
            identity, identity_source = read_capture(cache, cik, 'identity')
            identity_metadata = {'cik': cik, 'sic': identity.get('sic'), 'identity_source': identity_source['url'],
                                 'identity_tickers': [value.replace('-', '.') for value in identity.get('tickers', [])]}
            reference = prepare_reference(ticker, cik, facts, identity, source, prices[ticker],
                                          date.fromisoformat(row['as_of']).year)
            validate_reference(reference)
            references[ticker] = reference
            results.append({'ticker': ticker, 'status': 'prepared', 'reason': 'Referencia SEC validada.',
                            'period_end': reference['period_end'], 'report_url': reference['report_url'], **identity_metadata})
        except FileNotFoundError:
            results.append({'ticker': ticker, 'status': 'pending', 'reason': 'Captura SEC pendiente de descargar.'})
        except IncompleteReference as error:
            results.append({'ticker': ticker, 'status': 'review', 'reason': str(error), **identity_metadata})
        except (ValueError, KeyError, TypeError, ArithmeticError) as error:
            results.append({'ticker': ticker, 'status': 'rejected', 'reason': str(error)})
    payload = {'version': 1, 'price_snapshot_sha256': price_hash,
               'companies': [{'ticker': ticker, 'reference': reference} for ticker, reference in sorted(references.items())]}
    packaged = (json.dumps(payload, ensure_ascii=False, indent=2)+'\n').encode('utf-8')
    output.write_bytes(gzip.compress(packaged, mtime=0) if output.suffix == '.gz' else packaged)
    report = {'prepared_at': datetime.now(timezone.utc).isoformat(),
              'counts': dict(Counter(row['status'] for row in results)), 'companies': results}
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    with report_path.with_suffix('.csv').open('w', encoding='utf-8-sig', newline='') as stream:
        writer = csv.writer(stream)
        writer.writerow(['Ticker', 'Estado de preparación', 'Motivo', 'Cierre', 'CIK', 'SIC', 'Fuente'])
        for row in results:
            writer.writerow([row['ticker'], row['status'], row['reason'], row.get('period_end', ''),
                row.get('cik', ''), row.get('sic', ''), row.get('report_url') or row.get('identity_source', '')])
    print(json.dumps(report['counts'], ensure_ascii=False))
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--inventory', type=Path, required=True)
    parser.add_argument('--cache', type=Path, default=ROOT.parent/'data_sources/altman_sec/catalog')
    parser.add_argument('--output', type=Path, default=ROOT.parent/'output/altman_sec_prepared.json')
    parser.add_argument('--report', type=Path, default=ROOT.parent/'output/altman_sec_preparation_report.json')
    args = parser.parse_args()
    prepare(args.inventory, args.cache, args.output, args.report)
