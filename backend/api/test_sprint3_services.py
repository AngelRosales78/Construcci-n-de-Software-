from django.test import TestCase, override_settings
from rest_framework.test import APIClient
from .models import User, Company, Favorite, CompanyVisit


class PersonalServicesTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(username='alice', password='TestsOnly8!')
        self.other = User.objects.create_user(username='bob', password='TestsOnly8!')
        self.a = Company.objects.create(ticker='AAPL', name='Apple', sector='technology')
        self.b = Company.objects.create(ticker='MSFT', name='Microsoft', sector='technology')
        self.client = APIClient()
        self.client.force_authenticate(self.user)

    def call(self, method, path, data=None):
        return getattr(self.client, method)('/api/v1/' + path, data, format='json')

    def test_favorites_persist_deduplicate_and_isolate_accounts(self):
        first = self.call('post', 'favorites/', {'ticker': 'AAPL'})
        self.assertEqual(first.status_code, 201)
        duplicate = self.call('post', 'favorites/', {'ticker': 'AAPL'})
        self.assertEqual(first.data['favorite'], duplicate.data['favorite'])
        self.assertEqual(Favorite.objects.count(), 1)
        self.assertEqual(self.call('get', 'favorites/').data['owner_id'], str(self.user.pk))
        pk = first.data['favorite']['id']
        self.client.force_authenticate(self.other)
        self.assertEqual(self.call('get', 'favorites/').data['results'], [])
        self.assertEqual(self.call('delete', f'favorites/{pk}/').status_code, 404)
        self.client.force_authenticate(self.user)
        self.assertEqual(self.call('delete', f'favorites/{pk}/').status_code, 204)
        self.assertEqual(self.call('delete', f'favorites/{pk}/').status_code, 404)

    def test_tags_versions_ownership_and_delete_preserves_favorite(self):
        pk = self.call('post', 'favorites/', {'ticker': 'AAPL'}).data['favorite']['id']
        initial = self.call('get', 'tags/').data
        created = self.call('post', 'tags/', {'name': ' Seguimiento ', 'color': '#00ff87', 'version': initial['version']})
        self.assertEqual(created.status_code, 201)
        tag = created.data['tags'][0]['id']
        stale = self.call('patch', f'tags/{tag}/', {'name': 'Otra', 'color': '#000000', 'version': initial['version']})
        self.assertEqual(stale.status_code, 409)
        selected = self.call('put', f'favorites/{pk}/tags/', {'tag_ids': [tag], 'version': created.data['version']})
        self.assertEqual(selected.status_code, 200)
        self.assertEqual(selected.data['assignments'][0]['tag_ids'], [tag])
        self.client.force_authenticate(self.other)
        self.assertEqual(self.call('put', f'favorites/{pk}/tags/', {'tag_ids': [], 'version': '0'}).status_code, 404)
        self.client.force_authenticate(self.user)
        removed = self.call('delete', f'tags/{tag}/?version={selected.data["version"]}')
        self.assertEqual(removed.status_code, 200)
        self.assertEqual(removed.data['tags'], [])
        self.assertTrue(Favorite.objects.filter(pk=pk).exists())

    def test_normalized_tags_reject_duplicate_and_foreign_assignments(self):
        first = self.call('post', 'tags/', {'name': 'Mi  Lista', 'color': '#112233', 'version': '0'})
        duplicate = self.call('post', 'tags/', {'name': 'mi lista', 'color': '#112233', 'version': first.data['version']})
        self.assertEqual(duplicate.status_code, 409)
        self.assertEqual(self.call('get', 'tags/').data['version'], first.data['version'])
        tag = first.data['tags'][0]['id']
        self.client.force_authenticate(self.other)
        pk = self.call('post', 'favorites/', {'ticker': 'MSFT'}).data['favorite']['id']
        self.assertEqual(self.call('put', f'favorites/{pk}/tags/', {'tag_ids': [tag], 'version': '0'}).status_code, 400)

    def test_visits_deduplicate_events_keep_ten_and_isolate(self):
        for i in range(12):
            c = Company.objects.create(ticker=f'T{i}', name=f'Company {i}', sector='technology')
            self.assertEqual(self.call('post', f'companies/{c.ticker}/visits/', {'event_id': f'event-{i}'}).status_code, 200)
        recent = self.call('get', 'companies/recent/').data['results']
        self.assertEqual(len(recent), 10)
        self.assertEqual(recent[0]['ticker'], 'T11')
        visited = CompanyVisit.objects.get(user=self.user, company__ticker='T11').visited_at
        self.call('post', 'companies/T11/visits/', {'event_id': 'event-11'})
        self.assertEqual(CompanyVisit.objects.get(user=self.user, company__ticker='T11').visited_at, visited)
        self.assertEqual(self.call('post', 'companies/T10/visits/', {'event_id': 'event-11'}).status_code, 409)
        self.client.force_authenticate(self.other)
        self.assertEqual(self.call('get', 'companies/recent/').data['results'], [])

    def test_personal_routes_require_authentication(self):
        self.client.force_authenticate(None)
        for path in ('favorites/', 'tags/', 'companies/recent/'):
            self.assertEqual(self.call('get', path).status_code, 401)
