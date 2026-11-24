"""Sugerencias de empresas comparables: mismas fechas de ejercicio y pilares con nota en ambas."""
from datetime import date

from django.core.cache import cache
from django.test import override_settings

from .models import AnnualFinancialStatement, Company
from .test_sprint3_services import ComparisonServicesTests


@override_settings(LOCAL_DEVELOPMENT_PRO=True)
class CompareCandidatesTests(ComparisonServicesTests):
    def setUp(self):
        super().setUp()
        cache.clear()
        # Same sector, different fiscal calendar: must never be suggested for FY2024.
        self.shifted = Company.objects.create(ticker='NVDA', name='Nvidia', sector='technology')
        # Same calendar, other sector, without liabilities: fewer comparable pillars.
        self.partial = Company.objects.create(ticker='PEP', name='PepsiCo', sector='consumer_staples')
        for year in range(2021, 2025):
            self.statement(self.shifted, year, date(year - 1, 1, 30), date(year, 1, 28))
            self.statement(self.partial, year, date(year, 1, 1), date(year, 12, 31), drop=('total_liabilities', 'interest_expense'))

    def statement(self, company, year, start, end, drop=()):
        values = {'current_assets': 200, 'current_liabilities': 100, 'cash': 50, 'short_term_investments': 20,
                  'receivables': 40, 'net_income': 20, 'book_equity': 100, 'total_assets': 200, 'revenue': 200 + year - 2021,
                  'total_liabilities': 100, 'income_before_tax': 25, 'interest_expense': 5, 'inventory': 20, 'cost_of_sales': 100}
        AnnualFinancialStatement.objects.create(company=company, fiscal_year=year, period_start=start, period_end=end,
            filed=date(year + 1, 2, 1), source_url=f'https://www.sec.gov/{company.ticker}/{year}',
            accession=f'test-{company.ticker}-{year}', snapshot_sha256='a' * 64,
            components={k: {'value': None if k in drop else str(v), 'tag': k} for k, v in values.items()})

    def test_only_same_fiscal_dates_ranked_by_comparable_pillars(self):
        data = self.call('get', 'compare/candidates/?a=AAPL&fiscal_year=2024').data
        self.assertEqual(data['period'], {'start': '2024-01-01', 'end': '2024-12-31'})
        tickers = [row['ticker'] for row in data['results']]
        self.assertEqual(tickers[0], 'MSFT')
        self.assertNotIn('NVDA', tickers)
        self.assertNotIn('AAPL', tickers)
        msft = data['results'][0]
        self.assertEqual(msft['comparable_pillars'], len(data['a_pillars']))
        self.assertTrue(msft['same_sector'])
        pep = next(row for row in data['results'] if row['ticker'] == 'PEP')
        self.assertLess(pep['comparable_pillars'], msft['comparable_pillars'])
        self.assertNotIn('solvency', pep['pillars'])

    def test_suggestions_agree_with_the_comparison_itself(self):
        data = self.call('get', 'compare/candidates/?a=AAPL&fiscal_year=2024').data
        for row in data['results']:
            comparison = self.call('get', f'compare/?a=AAPL&b={row["ticker"]}&fiscal_year=2024').data
            available = {c['id'] for c in comparison['comparisons'] if c['status'] == 'available'}
            self.assertEqual(available, set(row['pillars']), row['ticker'])

    def test_company_without_peers_or_year_explains_why(self):
        alone = self.call('get', 'compare/candidates/?a=NVDA&fiscal_year=2024').data
        self.assertEqual(alone['results'], [])
        self.assertIn('mismas fechas', alone['reason'])
        missing = self.call('get', 'compare/candidates/?a=AAPL&fiscal_year=2019').data
        self.assertIn('FY2019', missing['reason'])
        self.assertEqual(missing['available_years'][0], 2024)
        self.assertEqual(self.call('get', 'compare/candidates/?a=ZZZZ&fiscal_year=2024').status_code, 404)

    def test_requires_pro(self):
        with override_settings(LOCAL_DEVELOPMENT_PRO=False):
            self.assertEqual(self.call('get', 'compare/candidates/?a=AAPL&fiscal_year=2024').status_code, 403)
