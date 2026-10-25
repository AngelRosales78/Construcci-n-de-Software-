"""
Management command to seed S&P 500 companies data.
"""
from django.core.management.base import BaseCommand
from api.models import Company


class Command(BaseCommand):
    help = 'Seed S&P 500 companies data'

    COMPANIES = [
        {
            'ticker': 'AAPL', 'name': 'Apple Inc.', 'sector': 'technology',
            'market_cap': 3000000000000, 'per': 29.5, 'roe': 147.9,
            'working_capital': 50000000000, 'total_assets': 350000000000,
            'retained_earnings': 5000000000, 'ebit': 120000000000,
            'total_liabilities': 280000000000, 'sales': 380000000000,
            'equity_value': 70000000000,
        },
        {
            'ticker': 'MSFT', 'name': 'Microsoft Corporation', 'sector': 'technology',
            'market_cap': 2800000000000, 'per': 35.2, 'roe': 38.6,
            'working_capital': 80000000000, 'total_assets': 450000000000,
            'retained_earnings': 100000000000, 'ebit': 90000000000,
            'total_liabilities': 250000000000, 'sales': 220000000000,
            'equity_value': 200000000000,
        },
        {
            'ticker': 'GOOGL', 'name': 'Alphabet Inc.', 'sector': 'technology',
            'market_cap': 1800000000000, 'per': 25.8, 'roe': 25.3,
            'working_capital': 120000000000, 'total_assets': 380000000000,
            'retained_earnings': 150000000000, 'ebit': 70000000000,
            'total_liabilities': 130000000000, 'sales': 300000000000,
            'equity_value': 250000000000,
        },
        {
            'ticker': 'AMZN', 'name': 'Amazon.com Inc.', 'sector': 'consumer_discretionary',
            'market_cap': 1600000000000, 'per': 60.3, 'roe': 15.2,
            'working_capital': 30000000000, 'total_assets': 500000000000,
            'retained_earnings': 80000000000, 'ebit': 40000000000,
            'total_liabilities': 350000000000, 'sales': 550000000000,
            'equity_value': 150000000000,
        },
        {
            'ticker': 'NVDA', 'name': 'NVIDIA Corporation', 'sector': 'technology',
            'market_cap': 1200000000000, 'per': 65.4, 'roe': 69.2,
            'working_capital': 25000000000, 'total_assets': 60000000000,
            'retained_earnings': 20000000000, 'ebit': 30000000000,
            'total_liabilities': 30000000000, 'sales': 60000000000,
            'equity_value': 30000000000,
        },
        {
            'ticker': 'META', 'name': 'Meta Platforms Inc.', 'sector': 'communication_services',
            'market_cap': 900000000000, 'per': 28.7, 'roe': 28.4,
            'working_capital': 40000000000, 'total_assets': 180000000000,
            'retained_earnings': 60000000000, 'ebit': 45000000000,
            'total_liabilities': 60000000000, 'sales': 130000000000,
            'equity_value': 120000000000,
        },
        {
            'ticker': 'TSLA', 'name': 'Tesla Inc.', 'sector': 'consumer_discretionary',
            'market_cap': 800000000000, 'per': 70.5, 'roe': 19.8,
            'working_capital': 20000000000, 'total_assets': 90000000000,
            'retained_earnings': 15000000000, 'ebit': 12000000000,
            'total_liabilities': 50000000000, 'sales': 95000000000,
            'equity_value': 40000000000,
        },
        {
            'ticker': 'BRK.B', 'name': 'Berkshire Hathaway Inc.', 'sector': 'financials',
            'market_cap': 750000000000, 'per': 8.5, 'roe': 12.3,
            'working_capital': 150000000000, 'total_assets': 950000000000,
            'retained_earnings': 400000000000, 'ebit': 80000000000,
            'total_liabilities': 500000000000, 'sales': 350000000000,
            'equity_value': 450000000000,
        },
        {
            'ticker': 'JPM', 'name': 'JPMorgan Chase & Co.', 'sector': 'financials',
            'market_cap': 500000000000, 'per': 11.2, 'roe': 15.6,
            'working_capital': 200000000000, 'total_assets': 3800000000000,
            'retained_earnings': 280000000000, 'ebit': 50000000000,
            'total_liabilities': 3500000000000, 'sales': 140000000000,
            'equity_value': 300000000000,
        },
        {
            'ticker': 'V', 'name': 'Visa Inc.', 'sector': 'financials',
            'market_cap': 480000000000, 'per': 30.1, 'roe': 44.2,
            'working_capital': 15000000000, 'total_assets': 90000000000,
            'retained_earnings': 35000000000, 'ebit': 20000000000,
            'total_liabilities': 55000000000, 'sales': 32000000000,
            'equity_value': 35000000000,
        },
        {
            'ticker': 'JNJ', 'name': 'Johnson & Johnson', 'sector': 'healthcare',
            'market_cap': 380000000000, 'per': 15.8, 'roe': 22.1,
            'working_capital': 25000000000, 'total_assets': 180000000000,
            'retained_earnings': 120000000000, 'ebit': 22000000000,
            'total_liabilities': 100000000000, 'sales': 95000000000,
            'equity_value': 80000000000,
        },
        {
            'ticker': 'WMT', 'name': 'Walmart Inc.', 'sector': 'consumer_staples',
            'market_cap': 420000000000, 'per': 25.3, 'roe': 18.5,
            'working_capital': 15000000000, 'total_assets': 250000000000,
            'retained_earnings': 80000000000, 'ebit': 25000000000,
            'total_liabilities': 160000000000, 'sales': 650000000000,
            'equity_value': 90000000000,
        },
        {
            'ticker': 'PG', 'name': 'Procter & Gamble Co.', 'sector': 'consumer_staples',
            'market_cap': 350000000000, 'per': 24.7, 'roe': 31.2,
            'working_capital': 10000000000, 'total_assets': 120000000000,
            'retained_earnings': 90000000000, 'ebit': 18000000000,
            'total_liabilities': 70000000000, 'sales': 80000000000,
            'equity_value': 50000000000,
        },
        {
            'ticker': 'MA', 'name': 'Mastercard Inc.', 'sector': 'financials',
            'market_cap': 400000000000, 'per': 35.6, 'roe': 165.8,
            'working_capital': 8000000000, 'total_assets': 45000000000,
            'retained_earnings': 25000000000, 'ebit': 15000000000,
            'total_liabilities': 35000000000, 'sales': 28000000000,
            'equity_value': 10000000000,
        },
        {
            'ticker': 'UNH', 'name': 'UnitedHealth Group Inc.', 'sector': 'healthcare',
            'market_cap': 450000000000, 'per': 20.5, 'roe': 25.8,
            'working_capital': 30000000000, 'total_assets': 250000000000,
            'retained_earnings': 80000000000, 'ebit': 30000000000,
            'total_liabilities': 160000000000, 'sales': 350000000000,
            'equity_value': 90000000000,
        },
        {
            'ticker': 'HD', 'name': 'Home Depot Inc.', 'sector': 'consumer_discretionary',
            'market_cap': 320000000000, 'per': 22.4, 'roe': 145.5,
            'working_capital': 5000000000, 'total_assets': 75000000000,
            'retained_earnings': 40000000000, 'ebit': 20000000000,
            'total_liabilities': 65000000000, 'sales': 150000000000,
            'equity_value': 10000000000,
        },
        {
            'ticker': 'BAC', 'name': 'Bank of America Corp.', 'sector': 'financials',
            'market_cap': 280000000000, 'per': 10.8, 'roe': 9.2,
            'working_capital': 100000000000, 'total_assets': 3200000000000,
            'retained_earnings': 200000000000, 'ebit': 35000000000,
            'total_liabilities': 3000000000000, 'sales': 100000000000,
            'equity_value': 200000000000,
        },
        {
            'ticker': 'PFE', 'name': 'Pfizer Inc.', 'sector': 'healthcare',
            'market_cap': 150000000000, 'per': 12.3, 'roe': 8.5,
            'working_capital': 20000000000, 'total_assets': 220000000000,
            'retained_earnings': 60000000000, 'ebit': 15000000000,
            'total_liabilities': 140000000000, 'sales': 100000000000,
            'equity_value': 80000000000,
        },
        {
            'ticker': 'KO', 'name': 'Coca-Cola Co.', 'sector': 'consumer_staples',
            'market_cap': 260000000000, 'per': 23.5, 'roe': 40.2,
            'working_capital': 8000000000, 'total_assets': 95000000000,
            'retained_earnings': 65000000000, 'ebit': 12000000000,
            'total_liabilities': 70000000000, 'sales': 45000000000,
            'equity_value': 25000000000,
        },
        {
            'ticker': 'PEP', 'name': 'PepsiCo Inc.', 'sector': 'consumer_staples',
            'market_cap': 230000000000, 'per': 25.8, 'roe': 52.1,
            'working_capital': 5000000000, 'total_assets': 95000000000,
            'retained_earnings': 55000000000, 'ebit': 12000000000,
            'total_liabilities': 75000000000, 'sales': 90000000000,
            'equity_value': 20000000000,
        },
        {
            'ticker': 'XOM', 'name': 'Exxon Mobil Corp.', 'sector': 'energy',
            'market_cap': 450000000000, 'per': 11.5, 'roe': 18.9,
            'working_capital': 30000000000, 'total_assets': 400000000000,
            'retained_earnings': 180000000000, 'ebit': 55000000000,
            'total_liabilities': 220000000000, 'sales': 350000000000,
            'equity_value': 180000000000,
        },
        {
            'ticker': 'CVX', 'name': 'Chevron Corporation', 'sector': 'energy',
            'market_cap': 280000000000, 'per': 10.2, 'roe': 15.3,
            'working_capital': 25000000000, 'total_assets': 260000000000,
            'retained_earnings': 150000000000, 'ebit': 40000000000,
            'total_liabilities': 150000000000, 'sales': 200000000000,
            'equity_value': 110000000000,
        },
        {
            'ticker': 'LLY', 'name': 'Eli Lilly and Co.', 'sector': 'healthcare',
            'market_cap': 550000000000, 'per': 45.2, 'roe': 58.7,
            'working_capital': 12000000000, 'total_assets': 55000000000,
            'retained_earnings': 20000000000, 'ebit': 15000000000,
            'total_liabilities': 35000000000, 'sales': 35000000000,
            'equity_value': 20000000000,
        },
        {
            'ticker': 'AVGO', 'name': 'Broadcom Inc.', 'sector': 'technology',
            'market_cap': 500000000000, 'per': 28.5, 'roe': 42.3,
            'working_capital': 15000000000, 'total_assets': 75000000000,
            'retained_earnings': 25000000000, 'ebit': 18000000000,
            'total_liabilities': 45000000000, 'sales': 35000000000,
            'equity_value': 30000000000,
        },
        {
            'ticker': 'COST', 'name': 'Costco Wholesale Corp.', 'sector': 'consumer_staples',
            'market_cap': 300000000000, 'per': 38.5, 'roe': 28.9,
            'working_capital': 8000000000, 'total_assets': 65000000000,
            'retained_earnings': 25000000000, 'ebit': 10000000000,
            'total_liabilities': 45000000000, 'sales': 240000000000,
            'equity_value': 20000000000,
        },
        {
            'ticker': 'MRK', 'name': 'Merck & Co. Inc.', 'sector': 'healthcare',
            'market_cap': 280000000000, 'per': 18.5, 'roe': 25.6,
            'working_capital': 18000000000, 'total_assets': 110000000000,
            'retained_earnings': 50000000000, 'ebit': 20000000000,
            'total_liabilities': 70000000000, 'sales': 60000000000,
            'equity_value': 40000000000,
        },
        {
            'ticker': 'ADBE', 'name': 'Adobe Inc.', 'sector': 'technology',
            'market_cap': 220000000000, 'per': 32.5, 'roe': 35.8,
            'working_capital': 10000000000, 'total_assets': 30000000000,
            'retained_earnings': 18000000000, 'ebit': 8000000000,
            'total_liabilities': 15000000000, 'sales': 20000000000,
            'equity_value': 15000000000,
        },
        {
            'ticker': 'CRM', 'name': 'Salesforce Inc.', 'sector': 'technology',
            'market_cap': 250000000000, 'per': 55.2, 'roe': 12.5,
            'working_capital': 5000000000, 'total_assets': 100000000000,
            'retained_earnings': 15000000000, 'ebit': 8000000000,
            'total_liabilities': 60000000000, 'sales': 35000000000,
            'equity_value': 40000000000,
        },
        {
            'ticker': 'NFLX', 'name': 'Netflix Inc.', 'sector': 'communication_services',
            'market_cap': 200000000000, 'per': 40.5, 'roe': 28.9,
            'working_capital': 3000000000, 'total_assets': 50000000000,
            'retained_earnings': 20000000000, 'ebit': 8000000000,
            'total_liabilities': 30000000000, 'sales': 35000000000,
            'equity_value': 20000000000,
        },
        {
            'ticker': 'DIS', 'name': 'Walt Disney Co.', 'sector': 'communication_services',
            'market_cap': 180000000000, 'per': 35.8, 'roe': 3.2,
            'working_capital': 8000000000, 'total_assets': 200000000000,
            'retained_earnings': 80000000000, 'ebit': 12000000000,
            'total_liabilities': 120000000000, 'sales': 90000000000,
            'equity_value': 80000000000,
        },
    ]

    def handle(self, *args, **options):
        self.stdout.write('Seeding S&P 500 companies...')

        for company_data in self.COMPANIES:
            Company.objects.get_or_create(
                ticker=company_data['ticker'],
                defaults=company_data
            )

        self.stdout.write(self.style.SUCCESS(f'Successfully seeded {len(self.COMPANIES)} companies'))
