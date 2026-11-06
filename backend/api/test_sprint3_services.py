from django.test import TestCase, override_settings
from rest_framework.test import APIClient
from .models import User, Company, Favorite, CompanyVisit
from .models import AnnualFinancialStatement, UserSubscription, AnalysisSnapshot
from datetime import date, timedelta
from django.utils import timezone
from .models import ReportJob, PriceHistory
from io import BytesIO
import hashlib
from pypdf import PdfReader


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


@override_settings(LOCAL_DEVELOPMENT_PRO=True)
class ComparisonServicesTests(PersonalServicesTests):
    def setUp(self):
        super().setUp()
        for company in (self.a, self.b):
            for year in range(2021, 2025):
                AnnualFinancialStatement.objects.create(company=company, fiscal_year=year,
                    period_start=date(year, 1, 1), period_end=date(year, 12, 31),
                    filed=date(year+1, 2, 1), source_url=f'https://www.sec.gov/{company.ticker}/{year}',
                    accession=f'test-{company.ticker}-{year}', snapshot_sha256='a'*64,
                    components={k: {'value': str(v), 'tag': k} for k, v in {
                        'current_assets': 200, 'current_liabilities': 100, 'cash': 50,
                        'short_term_investments': 20, 'receivables': 40, 'net_income': 20,
                        'book_equity': 100, 'total_assets': 200, 'revenue': 200+year-2021,
                        'total_liabilities': 100, 'income_before_tax': 25, 'interest_expense': 5,
                        'inventory': 20, 'cost_of_sales': 100}.items()})

    def test_local_pro_is_explicit_and_production_defaults_free(self):
        info = self.call('get', 'user/subscription/').data
        self.assertEqual(info['plan'], 'pro')
        self.assertEqual(info['grant_origin'], 'local-development')
        with override_settings(LOCAL_DEVELOPMENT_PRO=False):
            self.assertEqual(self.call('get', 'user/subscription/').data['plan'], 'free')
            self.assertEqual(self.call('get', 'compare/?a=AAPL&b=MSFT&fiscal_year=2024').status_code, 403)
        UserSubscription.objects.create(user=self.user, plan='pro', active=True, valid_until=timezone.now()-timedelta(seconds=1))
        self.assertEqual(self.call('get', 'compare/?a=AAPL&b=MSFT&fiscal_year=2024').status_code, 403)

    def test_equal_periods_compare_five_real_scores_and_reject_invalid_pairs(self):
        result = self.call('get', 'compare/?a=AAPL&b=MSFT&fiscal_year=2024')
        self.assertEqual(result.status_code, 200)
        self.assertEqual(len(result.data['comparisons']), 5)
        self.assertTrue(all(r['result'] == 'tie' and float(r['delta']) == 0 for r in result.data['comparisons']))
        self.assertEqual(self.call('get', 'compare/?a=AAPL&b=AAPL&fiscal_year=2024').status_code, 400)
        self.assertEqual(self.call('get', 'compare/?a=AAPL&b=MSFT&fiscal_year=9999').status_code, 400)

    def test_different_periods_never_invent_winners(self):
        AnnualFinancialStatement.objects.filter(company=self.b, fiscal_year=2024).update(period_start=date(2024, 2, 1))
        result = self.call('get', 'compare/?a=AAPL&b=MSFT&fiscal_year=2024')
        self.assertTrue(all(r['result'] == 'not_comparable' and r['delta'] is None for r in result.data['comparisons']))

    def test_conclusions_and_company_analysis_preserve_report_snapshot(self):
        for path, method, data in [
            ('compare/conclusion/?a=AAPL&b=MSFT&fiscal_year=2024&level=technical', 'get', None),
            ('companies/AAPL/analysis/', 'post', {'fiscal_year': 2024, 'level': 'simple'})]:
            result = self.call(method, path, data)
            self.assertEqual(result.status_code, 200)
            self.assertEqual(result.data['generation_mode'], 'rules')
        self.assertEqual(AnalysisSnapshot.objects.filter(user=self.user).count(), 2)


@override_settings(LOCAL_DEVELOPMENT_PRO=True)
class ReportServicesTests(ComparisonServicesTests):
    def setUp(self):
        super().setUp()
        for c in (self.a, self.b):
            for i in range(4):
                PriceHistory.objects.create(company=c, date=date(2024, 12, 20+i), adjusted_close=100+i,
                                            source='https://finance.yahoo.com/', currency='USD')

    def report_request(self, **options):
        return {'request_id': 'test-request', 'kind': 'company', 'a': 'AAPL', 'fiscal_year': 2024,
                'range': '1Y', 'include_charts': True, 'include_analysis': False, 'analysis_level': 'simple'} | options

    def test_company_pdf_is_durable_complete_and_matches_metadata(self):
        submitted = self.call('post', 'reports/jobs/', self.report_request())
        self.assertEqual(submitted.status_code, 202)
        self.assertEqual(submitted.data['status'], 'queued')
        pk = submitted.data['id']
        result = self.call('get', f'reports/jobs/{pk}/')
        self.assertEqual(result.data['status'], 'ready', result.data)
        downloaded = self.call('get', f'reports/jobs/{pk}/file/')
        self.assertEqual(downloaded['Content-Type'], 'application/pdf')
        self.assertEqual(len(downloaded.content), result.data['size_bytes'])
        self.assertEqual(hashlib.sha256(downloaded.content).hexdigest(), result.data['content_sha256'])
        self.assertEqual(result.data['chart_format'], 'vector')
        pdf = PdfReader(BytesIO(downloaded.content))
        self.assertEqual(len(pdf.pages), result.data['page_count'])
        self.assertIn('AAPL', pdf.pages[0].extract_text())

    def test_comparison_pdf_preserves_exact_analysis_and_two_landscape_pages(self):
        analysis = self.call('get', 'compare/conclusion/?a=AAPL&b=MSFT&fiscal_year=2024&level=technical').data
        value = self.report_request(kind='comparison', b='MSFT', include_analysis=True, analysis_level='technical',
            analysis_origin='rules', analysis_version=f'{analysis["data_version"]}/{analysis["generated_at"]}')
        submitted = self.call('post', 'reports/jobs/', value)
        pk = submitted.data['id']
        result = self.call('get', f'reports/jobs/{pk}/')
        self.assertEqual(result.data['status'], 'ready', result.data)
        self.assertEqual(result.data['analysis_version'], value['analysis_version'])
        content = self.call('get', f'reports/jobs/{pk}/file/').content
        pdf = PdfReader(BytesIO(content))
        self.assertEqual(len(pdf.pages), 2)
        self.assertTrue(all(p.mediabox.width > p.mediabox.height for p in pdf.pages))
        self.assertIn(analysis['paragraphs'][0].split(':')[0], pdf.pages[1].extract_text())

    def test_idempotency_conflict_cancellation_and_cross_account_access(self):
        value = self.report_request()
        first = self.call('post', 'reports/jobs/', value)
        repeat = self.call('post', 'reports/jobs/', value)
        self.assertEqual(first.data['id'], repeat.data['id'])
        self.assertEqual(ReportJob.objects.count(), 1)
        self.assertEqual(self.call('post', 'reports/jobs/', value | {'range': '5Y'}).status_code, 409)
        pk = first.data['id']
        self.client.force_authenticate(self.other)
        for method, path in [('get', f'reports/jobs/{pk}/'), ('get', f'reports/jobs/{pk}/file/'), ('post', f'reports/jobs/{pk}/cancel/')]:
            self.assertEqual(self.call(method, path).status_code, 404)
        self.client.force_authenticate(self.user)
        self.assertEqual(self.call('post', f'reports/jobs/{pk}/cancel/').data['status'], 'cancelled')
        self.assertEqual(self.call('get', f'reports/jobs/{pk}/').data['status'], 'cancelled')
        self.assertEqual(self.call('get', f'reports/jobs/{pk}/file/').status_code, 409)

    def test_reports_reject_unverified_analysis_and_free_comparison(self):
        invalid = self.report_request(include_analysis=True, analysis_version='missing', analysis_origin='rules')
        self.assertEqual(self.call('post', 'reports/jobs/', invalid).status_code, 400)
        with override_settings(LOCAL_DEVELOPMENT_PRO=False):
            self.assertEqual(self.call('post', 'reports/jobs/', self.report_request(kind='comparison', b='MSFT')).status_code, 403)
