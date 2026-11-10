from datetime import timedelta
from decimal import Decimal
from io import BytesIO
import hashlib
import pytest
from django.contrib.auth import get_user_model
from django.utils import timezone
from pypdf import PdfReader
from rest_framework.test import APIClient
from .models import Company, Favorite, FavoriteTag, PriceHistory, ReportJob
from .test_sprint3_comparison import case
from .report_views import process_report

pytestmark = pytest.mark.django_db


def post(client, url, data):
    return client.post('/api/v1/' + url, data, format='json')


def test_tags_normalize_names_are_private_and_detect_concurrent_changes(case):
    client, _ = case
    first = post(client, 'favorites/', {'ticker': 'AAA'}).data['favorite']['id']
    data = post(client, 'tags/', {'name': '  Largo   plazo ', 'color': '#123ABC'}).data
    assert data['tags'][0]['name'] == 'Largo plazo'
    assert post(client, 'tags/', {'name': 'LARGO PLAZO', 'color': '#112233'}).status_code == 400
    ident, version = data['tags'][0]['id'], data['version']
    response = client.put(f'/api/v1/favorites/{first}/tags/', {'tag_ids': [ident], 'version': version}, format='json')
    assert response.status_code == 200
    assert response.data['assignments'] == [{'favorite_id': first, 'tag_ids': [ident]}]
    assert client.patch(f'/api/v1/tags/{ident}/', {'name': 'Otro', 'color': '#ffffff', 'version': version}, format='json').status_code == 409
    assert client.delete(f'/api/v1/tags/{ident}/?version={response.data["version"]}').status_code == 200
    assert Favorite.objects.filter(pk=first).exists()
    assert not FavoriteTag.objects.exists()


def test_tag_assignment_rejects_foreign_objects_and_preserves_original(case):
    client, _ = case
    favorite = post(client, 'favorites/', {'ticker': 'AAA'}).data['favorite']['id']
    user = get_user_model().objects.create_user(username='other')
    tag = FavoriteTag.objects.create(user=user, name='Privada', normalized_name='privada', color='#112233')
    version = client.get('/api/v1/tags/').data['version']
    assert client.put(f'/api/v1/favorites/{favorite}/tags/', {'tag_ids': [tag.pk], 'version': version}, format='json').status_code == 400
    assert client.delete(f'/api/v1/tags/{tag.pk}/?version={version}').status_code == 404
    assert not Favorite.objects.get(pk=favorite).tags.exists()


def test_recent_ten_unique_revisit_and_event_replay_do_not_reorder(case):
    client, _ = case
    for i in range(12):
        Company.objects.create(ticker=f'R{i}', name=f'Empresa {i}', sector='technology')
        assert post(client, f'companies/R{i}/visits/', {'event_id': f'event-{i}'}).status_code == 200
    rows = client.get('/api/v1/companies/recent/').data['results']
    assert len(rows) == 10 and rows[0]['ticker'] == 'R11' and rows[-1]['ticker'] == 'R2'
    post(client, 'companies/R11/visits/', {'event_id': 'event-11'})
    assert client.get('/api/v1/companies/recent/').data['results'] == rows
    post(client, 'companies/R2/visits/', {'event_id': 'new-visit'})
    assert client.get('/api/v1/companies/recent/').data['results'][0]['ticker'] == 'R2'
    assert post(client, 'companies/R3/visits/', {'event_id': 'new-visit'}).status_code == 409
    assert post(client, 'companies/UNKNOWN/visits/', {'event_id': 'none'}).status_code == 404


def test_histories_use_common_dates_and_known_percentage(case):
    client, _ = case
    today = timezone.localdate()
    for ticker, values in [('AAA', [100, 110, 121]), ('BBB', [50, 50, 55])]:
        company = Company.objects.get(ticker=ticker)
        for i, value in enumerate(values):
            PriceHistory.objects.create(company=company, date=today - timedelta(days=3-i), adjusted_close=Decimal(value), source='Fixture', currency='USD')
    response = client.get('/api/v1/compare/history/?a=AAA&b=BBB&fiscal_year=2024&range=1M')
    points = response.data['points']
    assert response.status_code == 200 and points[0]['a'] == points[0]['b'] == 0
    assert points[1]['a'] == pytest.approx(10) and points[-1]['a'] == pytest.approx(21)
    assert points[-1]['b'] == pytest.approx(10)
    PriceHistory.objects.filter(company__ticker='BBB', date=today-timedelta(days=2)).delete()
    assert len(client.get('/api/v1/compare/history/?a=AAA&b=BBB&fiscal_year=2024&range=1M').data['points']) == 2


def test_real_generation_is_labelled_and_cached_rules_are_not_ai(case, monkeypatch):
    client, _ = case
    route = '/api/v1/compare/conclusion/?a=AAA&b=BBB&fiscal_year=2024'
    monkeypatch.setattr('api.comparison_services.generate_comparison', lambda options, level: options[0])
    first = client.get(route).data
    assert first['generation_mode'] == 'ai' and first['strengths_a']
    monkeypatch.setattr('api.comparison_services.generate_comparison', lambda *args: (_ for _ in ()).throw(OSError('offline')))
    assert client.get(route).data['cache_hit'] is True
    second = client.get(route + '&level=technical').data
    assert second['generation_mode'] == 'rules' and second['model'] is None
    assert second['data_version'] != first['data_version']


def report_request(**changes):
    return dict(request_id='fixture-job', a='AAA', fiscal_year=2024, kind='company', range='1Y',
                include_charts=False, include_analysis=False, analysis_level='simple',
                analysis_version=None, analysis_origin=None, b=None) | changes


def test_report_job_idempotency_pdf_integrity_and_private_access(case):
    client, _ = case
    req = report_request()
    response = post(client, 'reports/jobs/', req)
    assert response.status_code == 202 and response.data['status'] == 'queued'
    ident = response.data['id']
    assert post(client, 'reports/jobs/', req).data['id'] == ident
    assert post(client, 'reports/jobs/', req | {'range': '5Y'}).status_code == 409
    assert client.get(f'/api/v1/reports/jobs/{ident}/file/').status_code == 409
    assert process_report()
    result = client.get(f'/api/v1/reports/jobs/{ident}/').data
    assert result['status'] == 'ready', result
    response = client.get(f'/api/v1/reports/jobs/{ident}/file/')
    assert response['Content-Type'] == 'application/pdf'
    assert hashlib.sha256(response.content).hexdigest() == result['content_sha256']
    assert len(response.content) == result['size_bytes']
    assert 'AAA' in ''.join(p.extract_text() for p in PdfReader(BytesIO(response.content)).pages)
    other = APIClient(); other.force_authenticate(get_user_model().objects.create_user(username='outsider'))
    assert other.get(f'/api/v1/reports/jobs/{ident}/file/').status_code == 404


def test_cancelled_job_is_terminal_and_cannot_be_downloaded(case):
    client, _ = case
    ident = post(client, 'reports/jobs/', report_request()).data['id']
    assert post(client, f'reports/jobs/{ident}/cancel/', {}).data['status'] == 'cancelled'
    assert not process_report()
    assert client.get(f'/api/v1/reports/jobs/{ident}/file/').status_code == 409


def test_comparison_pdf_is_vector_landscape_two_pages_and_rechecks_pro(case):
    client, plan = case
    req = report_request(kind='comparison', b='BBB', include_charts=True)
    ident = post(client, 'reports/jobs/', req).data['id']
    process_report()
    result = client.get(f'/api/v1/reports/jobs/{ident}/').data
    assert result['status'] == 'ready', result
    pdf = client.get(f'/api/v1/reports/jobs/{ident}/file/').content
    reader = PdfReader(BytesIO(pdf))
    assert 1 <= len(reader.pages) <= 2 and all(p.mediabox.width > p.mediabox.height for p in reader.pages)
    assert all(not p.images for p in reader.pages)
    assert result['chart_format'] == 'vector' and len(pdf) < 2_000_000
    plan.active = False; plan.save()
    assert client.get(f'/api/v1/reports/jobs/{ident}/file/').status_code == 403
    assert post(client, 'reports/jobs/', report_request(request_id='other', kind='comparison', b='BBB')).status_code == 403


def test_stale_or_unavailable_analysis_is_not_silently_exported(case):
    client, _ = case
    response = post(client, 'reports/jobs/', report_request(include_analysis=True, analysis_version='stale', analysis_origin='ai'))
    assert response.status_code == 409 and not ReportJob.objects.exists()


@pytest.mark.parametrize('path', ['reports/jobs/', 'tags/', 'companies/recent/', 'compare/conclusion/', 'compare/history/'])
def test_personal_endpoints_require_authentication(path):
    client = APIClient()
    response = client.post('/api/v1/' + path, {}) if path == 'reports/jobs/' else client.get('/api/v1/' + path)
    assert response.status_code == 401
