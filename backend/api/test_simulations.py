from datetime import date, timedelta
from django.test import TestCase
from rest_framework.test import APIClient
from .models import User, Company, PriceHistory
from .simulation_views import month_after

class SimulationTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.client.force_authenticate(User.objects.create_user(username='simulation-fixture'))
        self.company = Company.objects.create(ticker='TEST',name='Fixture',sector='technology')
        PriceHistory.objects.bulk_create([PriceHistory(company=self.company,date=date(2024,1,1)+timedelta(days=i),
            adjusted_close=str(100+i),currency='USD',source='https://example.org/prices') for i in range(70)])
        self.parameters = {'annual_return':.1,'annual_volatility':.2,'seed':42,'paths':100,'horizon_months':12}
    def test_defaults_derive_from_company_history(self):
        response=self.client.get('/api/v1/companies/TEST/simulations/defaults/')
        self.assertEqual(response.status_code,200)
        self.assertEqual(response.data['observations'],70)
        self.assertEqual(response.data['ticker'],'TEST')
    def test_common_percentiles_reproducible_for_seed(self):
        url='/api/v1/companies/TEST/simulations/'
        first=self.client.post(url,self.parameters,format='json')
        second=self.client.post(url,self.parameters,format='json')
        self.assertEqual(first.status_code,200)
        self.assertEqual(first.data['points'],second.data['points'])
        self.assertEqual(len(first.data['points']),13)
        self.assertNotEqual(first.data['simulation_id'],second.data['simulation_id'])
        for row in first.data['points']:
            self.assertLessEqual(row['p10'],row['p50']);self.assertLessEqual(row['p50'],row['p90'])
        self.assertEqual(first.data['parameters'],self.parameters)
    def test_zero_volatility_has_identical_quantiles(self):
        response=self.client.post('/api/v1/companies/TEST/simulations/',self.parameters|{'annual_volatility':0},format='json')
        for row in response.data['points']: self.assertEqual(row['p10'],row['p90'])
    def test_reject_expensive_or_invalid_parameters(self):
        for change in ({'paths':10001},{'annual_volatility':-1},{'horizon_months':6},{'seed':-1}):
            response=self.client.post('/api/v1/companies/TEST/simulations/',self.parameters|change,format='json')
            self.assertEqual(response.status_code,400)
    def test_no_prices_or_unknown_company_are_not_fabricated(self):
        self.company.prices.all().delete()
        self.assertEqual(self.client.get('/api/v1/companies/TEST/simulations/defaults/').status_code,400)
        self.assertEqual(self.client.get('/api/v1/companies/UNKNOWN/simulations/defaults/').status_code,404)
    def test_authentication_required(self):
        self.client.force_authenticate(None)
        self.assertEqual(self.client.get('/api/v1/companies/TEST/simulations/defaults/').status_code,401)
    def test_month_end_matches_twelve_month_contract(self):
        self.assertEqual(month_after(date(2024,2,29),12),date(2025,2,28))
        self.assertEqual(month_after(date(2024,1,31),1),date(2024,2,29))
