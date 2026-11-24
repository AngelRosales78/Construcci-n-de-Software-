"""Prepare public company profiles, joined by SEC CIK rather than similar names.

The local catalog remains a dated reference. Wikidata fields retain their own
source and retrieval time; this does not verify current index membership.
"""
import argparse
import csv
import hashlib
import json
import time
import re
import unicodedata
from concurrent.futures import ThreadPoolExecutor
from urllib.error import HTTPError
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlencode, urlsplit
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]


def https_url(value):
    parsed = urlsplit(value)
    return parsed.scheme == 'https' and bool(parsed.hostname) and not parsed.username and not parsed.password


def fetch_profiles(identifiers, listing=False):
    values = ' '.join('"' + value + '"' for value in identifiers)
    identity = '''VALUES ?ticker { %s }
      ?company p:P414 ?listing. ?listing ps:P414 ?exchange; pq:P249 ?ticker.
      VALUES ?exchange { wd:Q13677 wd:Q82059 }
      FILTER NOT EXISTS { ?listing pq:P582 ?end }
      OPTIONAL { ?company wdt:P5531 ?cik }
      OPTIONAL { ?company rdfs:label ?name. FILTER(LANG(?name)="en") }
      OPTIONAL { ?company skos:altLabel ?alias. FILTER(LANG(?alias)="en") }
    ''' % values if listing else '''VALUES ?cik { %s } ?company wdt:P5531 ?cik.''' % values
    query = '''SELECT DISTINCT ?company ?cik ?ticker ?name ?alias ?description ?website WHERE {
      %s
      OPTIONAL { ?company schema:description ?description. FILTER(LANG(?description)="es") }
      OPTIONAL { ?company p:P856 ?statement. ?statement a wikibase:BestRank; ps:P856 ?website.
        FILTER NOT EXISTS { ?statement pq:P582 ?end }
        FILTER(STRSTARTS(STR(?website),"https://") || STRSTARTS(STR(?website),"http://")) }
    }''' % identity
    url = 'https://query.wikidata.org/sparql?' + urlencode({'query': query, 'format': 'json'})
    request = Request(url, headers={'User-Agent': 'SP500Academic/1.0 (https://github.com/AngelRosales78/Construcci-n-de-Software-/issues)', 'Accept': 'application/sparql-results+json'})
    with urlopen(request, timeout=45) as response:
        raw = response.read(8_000_001)
        if len(raw) > 8_000_000:
            raise ValueError('Profile response exceeds the permitted size.')
        return json.loads(raw)['results']['bindings']


def normalized_name(value):
    value = re.sub(r'\(class [ab]\)', '', value, flags=re.I)
    value = ''.join(char for char in unicodedata.normalize('NFKD', value.lower()) if not unicodedata.combining(char))
    words = re.findall(r'[a-z0-9]+', value)
    return ''.join(word for word in words if word not in {'the', 'inc', 'incorporated', 'corp', 'corporation', 'co', 'company', 'plc', 'ltd', 'limited'})


def upgrade_website(value):
    if https_url(value): return value
    if not value.startswith('http://'): return ''
    candidate = 'https://' + value[7:]
    try:
        request = Request(candidate, method='HEAD', headers={'User-Agent': 'SP500Academic/1.0'})
        with urlopen(request, timeout=6) as response:
            return candidate if https_url(response.url) else ''
    except HTTPError as error:
        # A valid TLS connection was established, even if automated reading is denied.
        return candidate if error.code in (401, 403, 405) else ''
    except (OSError, ValueError): return ''


def prepare(destination, cache):
    catalog = ROOT / 'docs/datos/catalogo_sp500.csv'
    rows = list(csv.DictReader(catalog.open(encoding='utf-8-sig', newline='')))
    # This company was added through the verified administrative flow.
    rows.append({'Symbol': 'HIMS', 'Security': 'Hims & Hers Health, Inc.', 'CIK': '1773751',
                 'GICS Sector': 'Health Care', 'GICS Sub-Industry': '',
                 'Headquarters Location': '', 'Founded': ''})
    industry_labels = json.loads((ROOT / 'tools/company_industries_es.json').read_text(encoding='utf-8'))
    website_overrides = json.loads((ROOT / 'tools/company_website_overrides.json').read_text(encoding='utf-8'))
    ciks = sorted({str(int(row['CIK'])).zfill(10) for row in rows})
    cache.mkdir(parents=True, exist_ok=True)
    records = []
    for start in range(0, len(ciks), 40):
        batch = ciks[start:start + 40]
        key = hashlib.sha256(('websites-v2\n' + '\n'.join(batch)).encode()).hexdigest()[:16]
        cached = cache / (key + '.json')
        if cached.exists():
            result = json.loads(cached.read_text(encoding='utf-8'))
        else:
            result = {'retrieved_at': datetime.now(timezone.utc).isoformat(), 'rows': fetch_profiles(batch)}
            cached.write_text(json.dumps(result, ensure_ascii=False), encoding='utf-8')
            time.sleep(1)
        records.append(result)
        print(f'Perfiles consultados: {min(start + 40, len(ciks))}/{len(ciks)} CIK', flush=True)
    by_cik = {}
    for result in records:
        for item in result['rows']:
            cik = item['cik']['value']
            group = by_cik.setdefault(cik, {'entities': set(), 'descriptions': set(), 'websites': set(), 'retrieved_at': result['retrieved_at']})
            group['entities'].add(item['company']['value'].replace('http://', 'https://'))
            if item.get('description'): group['descriptions'].add(item['description']['value'])
            if item.get('website'): group['websites'].add(item['website']['value'])
    missing = [row for row in rows if len(by_cik.get(str(int(row['CIK'])).zfill(10), {}).get('entities', [])) != 1]
    for start in range(0, len(missing), 30):
        batch = missing[start:start + 30]
        tickers = [row['Symbol'] for row in batch]
        key = hashlib.sha256(('listing-v2\n' + '\n'.join(tickers)).encode()).hexdigest()[:16]
        cached = cache / (key + '.json')
        if cached.exists():
            result = json.loads(cached.read_text(encoding='utf-8'))
        else:
            result = {'retrieved_at': datetime.now(timezone.utc).isoformat(), 'rows': fetch_profiles(tickers, listing=True)}
            cached.write_text(json.dumps(result, ensure_ascii=False), encoding='utf-8')
            time.sleep(1)
        for row in batch:
            candidates = {}
            cik = str(int(row['CIK'])).zfill(10)
            for item in result['rows']:
                if item.get('ticker', {}).get('value') != row['Symbol']:
                    continue
                entity = item['company']['value'].replace('http://', 'https://')
                group = candidates.setdefault(entity, {'ciks': set(), 'names': set(), 'descriptions': set(), 'websites': set()})
                for field, target in [('cik','ciks'), ('name','names'), ('alias','names'), ('description','descriptions'), ('website','websites')]:
                    if item.get(field): group[target].add(item[field]['value'])
            valid = [(entity, group) for entity, group in candidates.items()
                     if (group['ciks'] and cik in {value.zfill(10) for value in group['ciks']})
                     or (not group['ciks'] and normalized_name(row['Security']) in {normalized_name(name) for name in group['names']})]
            if len(valid) == 1:
                entity, group = valid[0]
                by_cik[cik] = {'entities': {entity}, 'descriptions': group['descriptions'],
                    'websites': group['websites'],
                    'retrieved_at': result['retrieved_at'], 'identity_match': 'exchange_ticker_name'}
        print(f'Identidades adicionales revisadas: {min(start + 30, len(missing))}/{len(missing)}', flush=True)
    http_websites = sorted({value for profile in by_cik.values() for value in profile['websites'] if value.startswith('http://')})
    upgrades_path = cache / 'website_https_checks.json'
    upgrades = json.loads(upgrades_path.read_text(encoding='utf-8')) if upgrades_path.exists() else {}
    to_check = [value for value in http_websites if value not in upgrades]
    with ThreadPoolExecutor(max_workers=4) as pool:
        for value, checked in zip(to_check, pool.map(upgrade_website, to_check)):
            upgrades[value] = checked
    upgrades_path.write_text(json.dumps(upgrades, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'Sitios HTTPS revisados: {len(http_websites)}', flush=True)
    companies = []
    for row in rows:
        cik = str(int(row['CIK'])).zfill(10)
        profile = by_cik.get(cik, {})
        unambiguous = len(profile.get('entities', [])) == 1
        website = ''
        description = ''
        profile_source = ''
        if unambiguous:
            # Prefer a canonical root over regional, translated and deep URLs.
            candidates = {value if https_url(value) else upgrades.get(value, '') for value in profile['websites']}
            websites = sorted(filter(None, candidates), key=lambda value: (len(urlsplit(value).path.strip('/')) + len(urlsplit(value).query), len(value), value))
            website = websites[0] if websites else ''
            description = sorted(profile['descriptions'], key=lambda value: (-len(value), value))[0] if profile['descriptions'] else ''
            profile_source = next(iter(profile['entities']))
        industry_label = industry_labels.get(row['GICS Sub-Industry'], row['GICS Sub-Industry'])
        if row['Symbol'] in website_overrides:
            website = website_overrides[row['Symbol']]
        if row['Symbol'] == 'HIMS':
            description = 'Plataforma de salud y bienestar que facilita el acceso a atención personalizada.'
            description_origin = 'company_website'
            description_source = 'https://investors.hims.com/overview/default.aspx'
            website = 'https://investors.hims.com/'
        else:
            description_source = ''
        if not description and industry_label:
            description = 'Actividad principal en el catálogo: ' + industry_label.lower() + '.'
            description_origin = 'catalog_industry'
        else:
            description_origin = 'company_website' if description_source else 'wikidata' if description else ''
            if description: description = description[0].upper() + description[1:].rstrip('.') + '.'
        companies.append({'ticker': row['Symbol'], 'name': row['Security'], 'cik': cik,
            'description': description[:3000], 'website_url': website,
            'details': {'industry': row['GICS Sub-Industry'], 'industry_label': industry_label, 'headquarters': row['Headquarters Location'],
                'founded': row['Founded'], 'cik': cik,
                'catalog_source_url': 'https://github.com/datasets/s-and-p-500-companies/blob/main/data/constituents.csv' if row['Symbol'] != 'HIMS' else '',
                'catalog_snapshot_date': '2026-10-12' if row['Symbol'] != 'HIMS' else '',
                'profile_source_url': profile_source, 'retrieved_at': profile.get('retrieved_at', ''),
                'regulatory_url': 'https://www.sec.gov/edgar/browse/?CIK=' + str(int(cik)),
                'description_origin': description_origin,
                'description_source_url': description_source,
                'website_source_url': website_overrides.get(row['Symbol'], profile_source),
                'identity_match': profile.get('identity_match', 'cik') if unambiguous else 'catalog_only'}})
    payload = {'version': 1, 'prepared_at': datetime.now(timezone.utc).isoformat(),
               'catalog_sha256': hashlib.sha256(catalog.read_bytes()).hexdigest(),
               'sources': ['https://www.wikidata.org/wiki/Property:P5531', 'https://www.wikidata.org/wiki/Property:P856'],
               'companies': companies}
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'companies': len(companies), 'descriptions': sum(bool(c['description']) for c in companies),
                      'websites': sum(bool(c['website_url']) for c in companies)}, ensure_ascii=False), flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, default=ROOT / 'docs/datos/company_profiles.json')
    parser.add_argument('--cache', type=Path, default=ROOT.parent / 'data_sources/company_profiles')
    arguments = parser.parse_args()
    prepare(arguments.output, arguments.cache)
