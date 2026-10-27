from datetime import date,timedelta
from decimal import Decimal
import pytest
from rest_framework.test import APIClient
from .models import Company,User,AnnualFinancialStatement,FinancialMetrics
from .company_snapshots import beta_value,latest_valuation,update_snapshot


def test_beta_uses_matched_returns_and_requires_history():
    assert beta_value([],[]) is None
    stock=[];index=[];a=b=100.0
    for i in range(190):
        if i:
            r=.001 if i%2 else -.0005
            a*=1+2*r;b*=1+r
        day=(date(2024,1,1)+timedelta(days=i)).isoformat()
        stock.append({'date':day,'adjusted_close':str(a)});index.append({'date':day,'adjusted_close':str(b)})
    assert float(beta_value(stock,index))==pytest.approx(2,abs=1e-8)


def test_valuation_does_not_use_future_values():
    data={'timeseries':{'result':[{'annualMarketCap':[
        {'asOfDate':'2024-12-31','reportedValue':{'raw':100}},
        {'asOfDate':'2999-12-31','reportedValue':{'raw':999}}]}]}}
    assert latest_valuation(data,'annualMarketCap')==('2024-12-31',Decimal('100'))


@pytest.mark.django_db
def test_snapshot_populates_card_and_does_not_mark_real_data_as_demo():
    company=Company.objects.create(ticker='TEST',name='Fixture',sector='technology')
    for year in (2023,2024):
        AnnualFinancialStatement.objects.create(company=company,fiscal_year=year,period_start=date(year,1,1),period_end=date(year,12,31),
            filed=date(year+1,2,1),source_url='https://data.sec.gov/api/xbrl/companyfacts/CIK0000320193.json',accession=f'0000320193-{year%100+1:02}-000001',snapshot_sha256='a'*64,
            components={'book_equity':{'value':'100','tag':'StockholdersEquity'},'net_income':{'value':'20','tag':'NetIncomeLoss'}})
    values={'timeseries':{'result':[{'trailingMarketCap':[{'asOfDate':'2025-03-01','reportedValue':{'raw':5000}}]},
        {'trailingPeRatio':[{'asOfDate':'2025-03-02','reportedValue':{'raw':25}}]}]}}
    update_snapshot(company,{}, {},[],values,[])
    card=company.financial_metrics
    assert card.market_cap==Decimal('5000')
    assert card.per==Decimal('25')
    assert card.roe==Decimal('20')
    assert card.field_sources['roe']['as_of']=='2024-12-31'
    assert card.field_sources['per']['as_of']=='2025-03-02'
    client=APIClient();client.force_authenticate(User.objects.create_user(username='snapshot-fixture'))
    response=client.get('/api/v1/companies/TEST/summary/')
    assert response.status_code==200 and response.data['is_demo'] is False
    assert response.data['market_cap']==5000
    assert response.data['field_sources']['per']['source'].startswith('https://finance.yahoo.com/')
    company.refresh_from_db();assert company.market_cap==5000


@pytest.mark.django_db
def test_no_new_data_keeps_existing_snapshot():
    company=Company.objects.create(ticker='TEST',name='Fixture',sector='technology')
    FinancialMetrics.objects.create(company=company,as_of=date(2024,1,1),source='https://example.org',market_cap=100)
    assert update_snapshot(company,{}, {},[],{},[]) is None
    assert company.financial_metrics.market_cap==100


def test_registered_seed_and_aerospace_data_are_the_audited_business_not_parent():
    import json
    from pathlib import Path
    root=Path(__file__).resolve().parents[2]/'docs/datos/empresas'
    hona=json.loads((root/'HONA-annual.json').read_text(encoding='utf-8'))
    seed=json.loads((root/'VYLR-annual.json').read_text(encoding='utf-8'))
    h=next(r for r in hona['annual_records'] if r['fiscal_year']==2025)['values']
    v=next(r for r in seed['annual_records'] if r['fiscal_year']==2025)['values']
    assert h['revenue']['value']==17404000000
    assert h['net_income']['value']==2687000000
    assert v['revenue']['value']==9898000000
    assert v['total_assets']['value']==22019000000
    assert v['total_liabilities']['value']==8781000000
    assert 'combinados auditados' in seed['reporting_note']
    assert 'exhibit991' in hona['source']


@pytest.mark.django_db
def test_packaged_snapshot_restores_values_sources_and_prices(tmp_path):
    import json,shutil
    from pathlib import Path
    from django.core.management import call_command
    import io
    folder=Path(__file__).resolve().parents[2]/'docs/datos/empresas'
    for name in ['AAPL-annual.json','AAPL-prices.csv','AAPL-metrics.json']:
        shutil.copyfile(folder/name,tmp_path/name)
    Company.objects.create(ticker='AAPL',name='Apple fixture',sector='technology')
    call_command('import_company_bundle',str(tmp_path),stdout=io.StringIO())
    payload=json.loads((folder/'AAPL-metrics.json').read_text(encoding='utf-8'))['values']
    company=Company.objects.get(ticker='AAPL')
    assert company.market_cap==Decimal(payload['market_cap'])
    assert company.financial_metrics.field_sources==payload['field_sources']
    assert company.prices.count()>1000
    count=company.annual_statements.count()
    call_command('import_company_bundle',str(tmp_path),stdout=io.StringIO())
    assert company.annual_statements.count()==count
