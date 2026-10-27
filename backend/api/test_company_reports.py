from datetime import date, timedelta
import os
from unittest.mock import patch
from django.test import TestCase
from rest_framework.test import APIClient
from .models import User,Company,AnnualFinancialStatement,PriceHistory
from .management.commands.sync_company_data import annual_inventory

@patch.dict(os.environ, {'LOCAL_AI_MODEL':''})
class CompanyReportTests(TestCase):
    def setUp(self):
        self.client=APIClient()
        self.client.force_authenticate(User.objects.create_user(username='report-fixture'))
        self.company=Company.objects.create(ticker='TEST',name='Empresa elegida',sector='technology')
        AnnualFinancialStatement.objects.create(company=self.company,fiscal_year=2024,
            period_start=date(2024,1,1),period_end=date(2024,12,31),filed=date(2025,2,1),
            source_url='https://data.sec.gov/api/xbrl/companyfacts/CIK0000320193.json',
            accession='0000320193-25-000001',snapshot_sha256='a'*64,
            components={'current_assets':{'value':'200','tag':'AssetsCurrent'},'current_liabilities':{'value':'100','tag':'LiabilitiesCurrent'}})
    def test_real_report_is_explicitly_not_ai(self):
        response=self.client.post('/api/v1/companies/TEST/analysis/',{'fiscal_year':2024,'level':'simple'},format='json')
        self.assertEqual(response.status_code,200)
        self.assertEqual(response.data['ticker'],'TEST')
        self.assertEqual(response.data['generation_mode'],'rules')
        self.assertIn('No fue generado por IA',response.data['generation_notice'])
        self.assertEqual(len(response.data['pillars']),5)
        self.assertEqual(response.data['validation']['status'],'passed')
        self.assertIn('2,00',response.data['pillars'][0]['text'])
        self.assertTrue(response.data['risks'])
    def test_no_annual_data_has_actionable_error(self):
        response=self.client.post('/api/v1/companies/TEST/analysis/',{'fiscal_year':2020,'level':'simple'},format='json')
        self.assertEqual(response.status_code,400)
        self.assertEqual([int(y) for y in response.data['available_years']],[2024])
    def test_unknown_language_rejected(self):
        response=self.client.post('/api/v1/companies/TEST/analysis/',{'fiscal_year':2024,'level':'other'},format='json')
        self.assertEqual(response.status_code,400)
    def test_technical_report_includes_original_formula(self):
        response=self.client.post('/api/v1/companies/TEST/analysis/',{'fiscal_year':2024,'level':'technical'},format='json')
        self.assertIn('Fórmula:',response.data['pillars'][0]['text'])
    def test_sec_import_uses_one_original_filing_and_keeps_missing_values(self):
        original={'form':'10-K','fp':'FY','fy':2024,'start':'2024-01-01','end':'2024-12-31','filed':'2025-02-01','accn':'0000320193-25-000001','val':100}
        restated=original|{'filed':'2026-02-01','accn':'0000320193-26-000002','val':999}
        payload={'entityName':'Fixture','facts':{'us-gaap':{'Revenues':{'units':{'USD':[original,restated]}}}}}
        result=annual_inventory(payload,'TEST',320193,b'raw snapshot')
        row=result['annual_records'][0]
        self.assertEqual(row['values']['revenue']['value'],100)
        self.assertIsNone(row['values']['inventory']['value'])
        self.assertEqual(row['accession'],original['accn'])

    def test_ready_catalog_requires_prices_and_annual_data(self):
        PriceHistory.objects.bulk_create([PriceHistory(company=self.company,date=date(2024,1,1)+timedelta(days=i),adjusted_close='100',source='https://example.org/prices') for i in range(60)])
        result=self.client.get('/api/v1/companies/search/?ready=true')
        self.assertEqual(result.status_code,200)
        self.assertEqual(result.data['count'],1)
        self.assertEqual(result.data['results'][0]['ticker'],'TEST')
