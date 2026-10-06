"""
Locust load testing for S&P 500 Platform API.
Tests search, autocomplete, overview, and history endpoints.
"""
from locust import HttpUser, task, between
import random


class SPP500User(HttpUser):
    wait_time = between(1, 3)

    def on_start(self):
        """Login and get access token."""
        response = self.client.post('/api/v1/auth/token/', json={
            'username': 'testuser',
            'password': 'Str0ng!Pass#2024'
        })
        if response.status_code == 200:
            self.access_token = response.json()['access']
            self.client.headers['Authorization'] = f'Bearer {self.access_token}'
        else:
            self.access_token = None

    @task(3)
    def search_companies(self):
        """Test company search endpoint."""
        if not self.access_token:
            return
        queries = ['AAPL', 'MSFT', 'GOOGL', 'AMZN', 'Apple', 'Microsoft', 'Google']
        query = random.choice(queries)
        self.client.get(f'/api/v1/companies/search/?q={query}')

    @task(3)
    def autocomplete(self):
        """Test autocomplete endpoint."""
        if not self.access_token:
            return
        prefixes = ['A', 'M', 'G', 'T', 'AAP', 'MS', 'GO', 'AM']
        prefix = random.choice(prefixes)
        self.client.get(f'/api/v1/companies/autocomplete/?q={prefix}')

    @task(2)
    def company_overview(self):
        """Test company overview endpoint."""
        if not self.access_token:
            return
        tickers = ['AAPL', 'MSFT', 'GOOGL', 'AMZN', 'NVDA', 'META', 'TSLA']
        ticker = random.choice(tickers)
        self.client.get(f'/api/v1/companies/{ticker}/overview/')

    @task(2)
    def company_history(self):
        """Test company history endpoint."""
        if not self.access_token:
            return
        tickers = ['AAPL', 'MSFT', 'GOOGL', 'AMZN', 'NVDA', 'META', 'TSLA']
        ticker = random.choice(tickers)
        self.client.get(f'/api/v1/companies/{ticker}/history/')

    @task(1)
    def search_with_filters(self):
        """Test search with sector filter and ordering."""
        if not self.access_token:
            return
        sectors = ['technology', 'healthcare', 'financials', 'energy']
        sector = random.choice(sectors)
        ordering = random.choice(['market_cap', '-market_cap'])
        self.client.get(f'/api/v1/companies/search/?sector={sector}&ordering={ordering}')
