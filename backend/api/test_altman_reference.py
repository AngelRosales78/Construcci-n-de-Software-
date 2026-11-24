"""Fiscal-date provenance and original Altman calculation stay independently reproducible."""
import copy
import json
from decimal import Decimal
from pathlib import Path
from types import SimpleNamespace
import pytest
from django.core.management import call_command
from django.core.management.base import CommandError
from api.altman_reference import validate_reference
from api.views import calculate_altman_z, company_financial_data
from api.models import Company, FinancialMetrics

ROOT = Path(__file__).resolve().parents[2]
DATA = json.loads((ROOT/'docs/datos/altman_references.json').read_text(encoding='utf-8'))

@pytest.mark.parametrize('row', DATA['companies'], ids=lambda row:row['ticker'])
def test_primary_reference_score_uses_fiscal_price_not_current_summary(row):
    data = row['reference']
    values = validate_reference(data)
    summary = SimpleNamespace(altman_reference=data, total_assets=1, equity_value=999999999999)
    score = calculate_altman_z(summary)
    a,l = values['total_assets'],values['total_liabilities']
    expected = (Decimal('1.2')*values['working_capital']/a + Decimal('1.4')*values['retained_earnings']/a
                + Decimal('3.3')*values['ebit']/a + Decimal('.6')*values['equity_value']/l + values['sales']/a)
    assert score['score'] == float(round(expected,2))
    assert score['zone'] == 'safe'

@pytest.mark.parametrize('change', [
    {'price_date':'2026-11-20'}, {'price_date':'2026-09-27'},
    {'period_start':'2026-01-01'}, {'currency':'EUR'}, {'report_url':'http://example.com'},
    {'shares_outstanding':'0'}, {'unadjusted_close':'NaN'}, {'inputs':{}},
])
def test_invalid_evidence_does_not_fall_back_to_unrelated_summary(change):
    reference = copy.deepcopy(DATA['companies'][0]['reference']); reference.update(change)
    result = calculate_altman_z(SimpleNamespace(altman_reference=reference))
    assert result['score'] is None and result['zone'] == 'unknown'

@pytest.mark.django_db
def test_import_is_repeatable_and_preserves_summary(monkeypatch):
    from io import StringIO
    for row in DATA['companies']:
        company=Company.objects.create(ticker=row['ticker'],name=row['ticker'],sector='technology')
        FinancialMetrics.objects.create(company=company,as_of=row['reference']['period_end'],per=123,
            source='Existing',reference_basis='provider_normalized',market_cap=777)
    args = [str(ROOT/'docs/datos/altman_references.json')]
    options = dict(prices=str(ROOT/'docs/datos/catalog_prices.json.gz'),stdout=StringIO())
    call_command('import_altman_references',*args,**options)
    call_command('import_altman_references',*args,**options)
    for summary in FinancialMetrics.objects.all():
        assert summary.per == 123 and summary.market_cap == 777 and summary.altman_reference
        assert company_financial_data(summary.company)['altman_z']['reference']['period_end']
    # A mismatched quote rejects the entire batch before any database writes.
    payload=copy.deepcopy(DATA); payload['companies'][-1]['reference']['price_date']='2026-01-29'
    import tempfile
    with tempfile.TemporaryDirectory() as temp:
        path=Path(temp)/'bad.json'; path.write_text(json.dumps(payload),encoding='utf-8')
        FinancialMetrics.objects.update(altman_reference={})
        with pytest.raises(CommandError):
            call_command('import_altman_references',str(path),**options)
        assert not any(s.altman_reference for s in FinancialMetrics.objects.all())
