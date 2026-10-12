"""
Locust load testing for S&P 500 Platform API.
Tests search, autocomplete, overview, and history endpoints.
"""
from locust import HttpUser, task, between
import random
import os
from locust.exception import StopUser


class SPP500User(HttpUser):
    wait_time = between(1, 3)

    def on_start(self):
        """Login and get access token."""
        self.access_token = os.environ.get('LOCUST_ACCESS_TOKEN')
        if not self.access_token:
            username = os.environ.get('LOCUST_USERNAME')
            password = os.environ.get('LOCUST_PASSWORD')
            if not username or not password:
                raise StopUser('Se requiere un token o credenciales de prueba.')
            response = self.client.post(os.environ.get('LOCUST_LOGIN_PATH', '/api/v1/token/'),
                                        json={'username': username, 'password': password})
            if response.status_code != 200:
                raise StopUser('No se pudo autenticar el usuario de carga.')
            self.access_token = response.json().get('access')
            if not self.access_token:
                raise StopUser('La respuesta no contiene un token de acceso.')
        self.client.headers['Authorization'] = f'Bearer {self.access_token}'

    @task(3)
    def search_companies(self):
        """Test company search endpoint."""
        if not self.access_token:
            return
        queries = ['AAPL', 'MSFT', 'GOOGL', 'AMZN', 'Apple', 'Microsoft', 'Google']
        query = random.choice(queries)
        with self.client.get('/api/v1/companies/search/', params={'q': query},
                             name='HU03_01 búsqueda', catch_response=True) as response:
            if response.status_code != 200:
                response.failure(f'Estado inesperado: {response.status_code}')
            elif response.elapsed.total_seconds() >= 0.2:
                response.failure('La búsqueda excedió 200 ms')
            else:
                try:
                    if not isinstance(response.json().get('results'), list):
                        response.failure('Falta la lista paginada de empresas')
                except (ValueError, AttributeError):
                    response.failure('La respuesta no contiene un JSON válido')

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
