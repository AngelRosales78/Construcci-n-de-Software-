from types import SimpleNamespace
from datetime import date, timedelta, datetime, timezone
from decimal import Decimal
from api.financial_availability import diagnose, public_indicators, altman_status, fingerprint, INDICATORS, LABELS

def summary(**changes):
    values=dict.fromkeys((*INDICATORS,*LABELS))
    values.update(as_of=date(2025,12,31),reference_basis='provider_normalized',source_snapshot_sha256='a'*64,altman_reference={},data_diagnostics={})
    values.update(changes)
    return SimpleNamespace(**values)

def records(equity='10',profit='2'):
    return [dict(fiscal_year=year,period_end=date(year,12,31),snapshot_sha256='a'*64,components={
        'book_equity':{'value':equity},'net_income':{'value':profit}}) for year in (2024,2025)]

def test_equity_condition_is_distinguished_from_missing_profit_and_missing_year():
    row=summary()
    assert diagnose(row,records('-2'),{},{},{})['indicators']['roe']['code']=='nonpositive_equity'
    assert diagnose(row,records(profit=None),{},{},{})['indicators']['roe']['code']=='net_income_missing'
    assert diagnose(row,records()[:1],{},{},{})['indicators']['roe']['code']=='annual_history_missing'

def test_dividend_absence_is_not_zero_and_split_is_a_separate_cause():
    row=summary(); end=date(2025,12,31); chart={end:{},end-timedelta(days=400):{}}
    data=diagnose(row,records(),chart,{},chart)
    assert data['indicators']['dividend_yield']['code']=='no_dividend_events'
    epoch=int(datetime(2025,12,1,tzinfo=timezone.utc).timestamp())
    data=diagnose(row,records(),chart,{'events':{'splits':{'a':{'date':epoch}}}},chart)
    assert data['indicators']['dividend_yield']['code']=='split_basis_unverified'
    assert diagnose(row,records(),{end:{}},{},{})['indicators']['dividend_yield']['code']=='dividend_history_short'

def test_beta_counts_shared_sessions_and_available_zero_is_preserved():
    row=summary(per=Decimal(0)); end=date(2025,12,31); chart={end:{}}
    data=diagnose(row,records(),chart,{},chart)
    assert data['indicators']['per']['status']=='available'
    assert '1 sesión común' in data['indicators']['beta']['explanation']

def test_diagnostic_is_invalidated_when_source_or_inputs_change():
    row=summary()
    row.data_diagnostics=diagnose(row,records('-2'),{},{},{})
    assert public_indicators(row)['roe']['code']=='nonpositive_equity'
    row.source_snapshot_sha256='b'*64
    assert public_indicators(row)['roe']['code']=='not_reviewed'
    row.roe=Decimal(0)
    assert public_indicators(row)['roe']['status']=='available'

def test_valuation_is_only_listed_as_only_gap_when_other_inputs_exist():
    row=summary(**{key:Decimal(1) for key in LABELS if key!='equity_value'})
    assert altman_status(row,{'score':None})['code']=='fiscal_valuation_missing'
    row.ebit=None
    result=altman_status(row,{'score':None})
    assert result['missing_fields']==['ebit','equity_value']
    assert altman_status(row,{'score':5})['status']=='available'

def test_fingerprint_ignores_json_dictionary_order():
    a=summary(altman_reference={'x':1,'y':2}); b=summary(altman_reference={'y':2,'x':1})
    assert fingerprint(a)==fingerprint(b)
