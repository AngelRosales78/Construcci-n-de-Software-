"""Inventariar datos anuales SEC locales sin modificar la base ni descargar datos.

Uso desde la raíz: python tools/audit_sprint2_data.py ../data_sources/MSFT_sec.json
Salida: docs/datos/sprint2_disponibilidad_MSFT.json
No calcula notas de pilares ni implementa endpoints del sprint 2.
"""
import argparse
import csv
import hashlib
import json
from datetime import date
from pathlib import Path

FIELDS = {
    'current_assets': ('AssetsCurrent', False),
    'current_liabilities': ('LiabilitiesCurrent', False),
    'cash': ('CashAndCashEquivalentsAtCarryingValue', False),
    'short_term_investments': ('ShortTermInvestments', False),
    'receivables': ('AccountsReceivableNetCurrent', False),
    'net_income': ('NetIncomeLoss', True),
    'total_assets': ('Assets', False),
    'book_equity': ('StockholdersEquity', False),
    'revenue': ('RevenueFromContractWithCustomerExcludingAssessedTax', True),
    'total_liabilities': ('Liabilities', False),
    'income_before_tax': ('IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest', True),
    'interest_expense': ('InterestExpense', True),
    'inventory': ('InventoryNet', False),
    'cost_of_sales': ('CostOfGoodsAndServicesSold', True),
}


def inventory(snapshot, root):
    raw = snapshot.read_bytes()
    entity = json.loads(raw)
    if entity.get('cik') != 789019:
        raise ValueError('Se esperaba el snapshot SEC de Microsoft CIK 789019.')
    facts = entity['facts']['us-gaap']
    years = []
    # FY2020 provides the opening balances needed for FY2021 averages.
    for year in range(2020, 2025):
        end = f'{year}-06-30'
        start = f'{year-1}-07-01'
        filings = [r for r in facts['Assets']['units']['USD']
                   if r['end'] == end and r.get('form') == '10-K'
                   and r.get('fy') == year and r.get('filed', '9999') <= date.today().isoformat()]
        if not filings:
            raise ValueError(f'Falta el balance anual original de {year}.')
        filing = min(filings, key=lambda r: (r['filed'], r['accn']))
        record = {'fiscal_year': year, 'start': start, 'end': end,
                  'accession': filing['accn'], 'filed': filing['filed'],
                  'unit': 'USD', 'values': {}}
        for name, (tag, duration) in FIELDS.items():
            rows = [r for r in facts.get(tag, {}).get('units', {}).get('USD', [])
                    if r['end'] == end and r.get('accn') == filing['accn']
                    and r.get('form') == '10-K'
                    and (r.get('start') == start if duration else 'start' not in r)]
            unique = {r['val'] for r in rows}
            if len(unique) > 1:
                raise ValueError(f'Valores ambiguos para {name} en {year}.')
            record['values'][name] = {'value': next(iter(unique)) if unique else None,
                                      'tag': tag, 'available': bool(unique)}
        years.append(record)
    paths = [root/'docs/datos/catalogo_sp500.csv', root/'docs/datos/MSFT_balance_2024.csv',
             root/'docs/datos/MSFT_cierres_5y.csv']
    packaged = {}
    for path in paths:
        with path.open(encoding='utf-8-sig', newline='') as handle:
            rows = list(csv.DictReader(handle))
        packaged[path.name] = {'rows': len(rows), 'columns': list(rows[0]) if rows else [],
                               'sha256': hashlib.sha256(path.read_bytes()).hexdigest()}
    return {'entity': entity['entityName'], 'ticker': 'MSFT',
            'scope': 'Auditoría del snapshot local y CSV, no de la base activa ni de datos actuales.',
            'source': 'https://data.sec.gov/api/xbrl/companyfacts/CIK0000789019.json',
            'snapshot_sha256': hashlib.sha256(raw).hexdigest(),
            'selection': 'Un mismo 10-K original por ejercicio; no mezclar cifras reexpresadas de otros filings.',
            'packaged_files': packaged, 'annual_records': years}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('snapshot', type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    result = inventory(args.snapshot, root)
    output = root/'docs/datos/sprint2_disponibilidad_MSFT.json'
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    for row in result['annual_records']:
        missing = [k for k, v in row['values'].items() if not v['available']]
        print(f"FY{row['fiscal_year']}: {len(FIELDS)-len(missing)}/{len(FIELDS)} campos; faltantes: {', '.join(missing) or 'ninguno'}")
    print(output)
