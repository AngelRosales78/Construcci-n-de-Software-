"""Trabajos privados, reanudables e idempotentes de exportación."""
from datetime import timedelta
from types import SimpleNamespace
from time import perf_counter
import json
from django.db import transaction
from django.http import HttpResponse
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import serializers
from rest_framework.exceptions import PermissionDenied, ValidationError
from rest_framework.response import Response
from rest_framework.views import APIView
from .access_views import subscription
from .analysis_validation import context_from_pillars
from .comparison_views import ComparisonView
from .comparison_services import aligned_history, prices, digest, conclusion_key
from .models import AnalysisRecord, Company, ComparisonAnalysis, ReportJob
from .personal_views import Conflict, lock_owner
from .pillar_views import CompanyPillarsView
from .views import company_financial_data


class ReportInput(serializers.Serializer):
    request_id = serializers.RegexField(r'^[A-Za-z0-9-]{1,80}$')
    kind = serializers.ChoiceField(choices=('company', 'comparison'), default='company')
    a = serializers.RegexField(r'^[A-Z0-9.\-]{1,10}$')
    b = serializers.RegexField(r'^[A-Z0-9.\-]{1,10}$', required=False, allow_null=True, default=None)
    fiscal_year = serializers.IntegerField(min_value=1900, max_value=timezone.localdate().year)
    range = serializers.ChoiceField(choices=('1M', '6M', '1Y', '5Y'), default='1Y')
    include_charts = serializers.BooleanField(default=False)
    include_analysis = serializers.BooleanField(default=False)
    analysis_level = serializers.ChoiceField(choices=('simple', 'technical'), default='simple')
    analysis_version = serializers.CharField(max_length=300, allow_null=True, required=False, default=None)
    analysis_origin = serializers.ChoiceField(choices=('ai', 'rules', 'mixed'), allow_null=True, required=False, default=None)

    def to_internal_value(self, data):
        if not isinstance(data, dict) or set(data) - set(self.fields):
            raise ValidationError({'non_field_errors': ['Hay opciones de reporte no admitidas.']})
        if isinstance(data.get('fiscal_year'), bool):
            raise ValidationError({'fiscal_year': ['El ejercicio debe ser un año.']})
        return super().to_internal_value(data)

    def validate(self, data):
        if data['kind'] == 'comparison' and (not data['b'] or data['a'] == data['b']):
            raise ValidationError('Seleccione dos empresas distintas.')
        if data['kind'] == 'company' and data['b'] is not None:
            raise ValidationError('Una ficha individual no admite segunda empresa.')
        if data['include_analysis'] and (not data['analysis_version'] or not data['analysis_origin']):
            raise ValidationError('Prepare el análisis y seleccione su versión y origen antes de exportarlo.')
        return data


def authorize(user, payload):
    if payload['kind'] == 'comparison' and 'compare' not in subscription(user)['features']:
        raise PermissionDenied('El reporte comparativo requiere un plan Pro activo.')


def individual_analysis(pillars, level, version):
    context = context_from_pillars(pillars)
    records = AnalysisRecord.objects.filter(company__ticker=pillars['ticker'], validated=True,
        created_at__gte=timezone.now() - timedelta(hours=24)).order_by('-created_at')[:10]
    for record in records:
        data = record.response
        if data.get('level') != level or record.context != json.loads(json.dumps(context, default=str)):
            continue
        current = f"{context['methodology_version']}/{context['period']['end']}/{data['generated_at']}"
        if current == version:
            paragraphs = [data['analysis']['summary']['text']]
            paragraphs += [f"{context['pillars'][key]}: {claim['text']}" for key, claim in data['analysis']['pillars'].items()]
            paragraphs += ['Fortaleza: ' + claim['text'] for claim in data['analysis']['strengths']]
            paragraphs += ['Riesgo: ' + claim['text'] for claim in data['analysis']['risks']]
            return {'origin': 'ai', 'version': current, 'paragraphs': paragraphs}
    raise Conflict('El análisis no está disponible para esta versión de los datos. Vuelva a prepararlo.')


def create_snapshot(user, payload):
    symbols = [payload['a']] + ([payload['b']] if payload['kind'] == 'comparison' else [])
    companies, chart_sources = [], set()
    for ticker in symbols:
        company = get_object_or_404(Company.objects.select_related('financial_metrics'), ticker=ticker)
        pillars = CompanyPillarsView().for_year(ticker, payload['fiscal_year']).data
        # Los precios solo se consultan cuando se pidieron gráficos.
        history = prices(ticker, payload['range']) if payload['include_charts'] else []
        chart_sources.update(r['source'] for r in history)
        companies.append({'ticker': ticker, 'name': company.name, 'pillars': pillars,
                          'summary': company_financial_data(company),
                          'history': [{'date': str(r['date']), 'price': float(r['adjusted_close'])} for r in history]})
    snapshot = {'companies': companies, 'template_version': 'sprint3-pdf-v2'}
    if payload['kind'] == 'comparison':
        request = SimpleNamespace(user=user, query_params={'a': payload['a'], 'b': payload['b'], 'fiscal_year': str(payload['fiscal_year'])})
        snapshot['comparison'] = ComparisonView().get(request).data
        if payload['include_charts']:
            snapshot['history'] = aligned_history(payload['a'], payload['b'], payload['range'])
    if payload['include_analysis']:
        if payload['kind'] == 'company':
            snapshot['analysis'] = individual_analysis(companies[0]['pillars'], payload['analysis_level'], payload['analysis_version'])
        else:
            parts = payload['analysis_version'].split('/', 1)
            record = ComparisonAnalysis.objects.filter(key=parts[0], created_at__gte=timezone.now() - timedelta(hours=24)).first()
            if not record:
                raise Conflict('Prepare una conclusión comparativa vigente antes de exportar.')
            data = record.response
            current_key = conclusion_key(snapshot['comparison'], payload['analysis_level'])
            if (data['a'] != payload['a'] or data['b'] != payload['b'] or data['fiscal_year'] != payload['fiscal_year']
                    or data['level'] != payload['analysis_level'] or data['data_version'] != current_key
                    or payload['analysis_version'] != f"{data['data_version']}/{data['generated_at']}"):
                raise Conflict('La conclusión ya no corresponde al par, ejercicio o versión actual.')
            snapshot['analysis'] = {'origin': data['generation_mode'], 'version': payload['analysis_version'],
                'paragraphs': data['paragraphs'] + [payload['a'] + ': ' + text for text in data['strengths_a']]
                    + [payload['b'] + ': ' + text for text in data['strengths_b']]}
        if snapshot['analysis']['origin'] != payload['analysis_origin']:
            raise Conflict('El origen solicitado no coincide con el análisis almacenado.')
    snapshot['sources'] = sorted({component['source']['url'] for row in companies for p in row['pillars']['pillars']
                                  for m in p['metrics'] for component in m.get('components', [])})
    if payload['include_charts']:
        snapshot['sources'] = sorted(set(snapshot['sources']) | chart_sources)
    snapshot = json.loads(json.dumps(snapshot, default=str))
    options = {k: v for k, v in payload.items() if k != 'request_id'}
    version = digest([snapshot, options])
    snapshot['generated_at'] = timezone.now().isoformat()
    return snapshot, version


def job_data(job):
    return {'schema_version': 'report-job-v1', 'owner_id': str(job.user_id), 'id': str(job.pk),
            'status': job.status, 'request': job.request, 'progress': job.progress,
            'message': job.message, **job.metadata}


class ReportsView(APIView):
    def post(self, request):
        form = ReportInput(data=request.data)
        form.is_valid(raise_exception=True)
        payload = form.validated_data
        authorize(request.user, payload)
        existing = ReportJob.objects.defer('content', 'snapshot').filter(user=request.user, request_id=payload['request_id']).first()
        if existing:
            if existing.request != payload:
                raise Conflict('Este identificador ya pertenece a otras opciones de reporte.')
            return Response(job_data(existing), headers={'Cache-Control': 'private, no-store'})
        snapshot, version = create_snapshot(request.user, payload)
        with transaction.atomic():
            lock_owner(request.user)
            job, created = ReportJob.objects.get_or_create(user=request.user, request_id=payload['request_id'],
                defaults={'request': payload, 'snapshot': snapshot, 'metadata': {'data_version': version}})
            if job.request != payload:
                raise Conflict('Ese identificador ya identifica otro reporte.')
        return Response(job_data(job), status=202 if created else 200, headers={'Cache-Control': 'private, no-store'})


def own_job(request, job_id, content=False):
    rows = ReportJob.objects.all() if content else ReportJob.objects.defer('content', 'snapshot')
    job = get_object_or_404(rows, pk=job_id, user=request.user)
    authorize(request.user, job.request)
    return job


class ReportStatusView(APIView):
    def get(self, request, job_id):
        return Response(job_data(own_job(request, job_id)), headers={'Cache-Control': 'private, no-store'})


class ReportCancelView(APIView):
    def post(self, request, job_id):
        job = own_job(request, job_id)
        ReportJob.objects.filter(pk=job.pk, status__in=('queued', 'running')).update(status='cancelled', progress=0,
                                                                                  message='Cancelación confirmada.', updated_at=timezone.now())
        job.refresh_from_db()
        return Response(job_data(job))


class ReportFileView(APIView):
    def get(self, request, job_id):
        job = own_job(request, job_id, content=True)
        if job.status != 'ready' or not job.content:
            raise Conflict('El archivo todavía no está listo.')
        name = f"informe_{job.request['a']}{'_' + job.request['b'] if job.request.get('b') else ''}_{job.request['fiscal_year']}.pdf"
        return HttpResponse(bytes(job.content), content_type='application/pdf', headers={
            'Content-Disposition': f'attachment; filename="{name}"', 'Content-Length': str(len(job.content)),
            'X-Content-SHA256': job.metadata['content_sha256'], 'Cache-Control': 'private, no-store'})


def process_report():
    """Una cola persistente en PostgreSQL; el proceso dedicado se puede reiniciar."""
    with transaction.atomic():
        rows = ReportJob.objects.select_for_update(skip_locked=True).filter(status='queued').order_by('created_at')
        job = rows.first()
        if job is None:
            return False
        job.status = 'running'; job.progress = 20; job.save(update_fields=['status', 'progress', 'updated_at'])
    start = perf_counter()
    try:
        authorize(job.user, job.request)
        cached = ReportJob.objects.filter(user=job.user, status='ready', metadata__data_version=job.metadata['data_version']).exclude(pk=job.pk).first()
        if cached:
            content, metadata = bytes(cached.content), dict(cached.metadata)
        else:
            from .reporting import render_report
            content, metadata = render_report(job.snapshot, job.request)
        metadata |= {'data_version': job.metadata['data_version'], 'size_bytes': len(content),
                     'content_sha256': __import__('hashlib').sha256(content).hexdigest(),
                     'generation_ms': round((perf_counter() - start) * 1000, 2), 'cache_hit': bool(cached),
                     'chart_format': 'vector' if job.request['include_charts'] else None,
                     'chart_range': job.request['range'] if job.request['include_charts'] else None,
                     'analysis_version': job.request['analysis_version'] if job.request['include_analysis'] else None,
                     'analysis_origin': job.request['analysis_origin'] if job.request['include_analysis'] else None}
        ReportJob.objects.filter(pk=job.pk, status='running').update(status='ready', content=content, metadata=metadata,
                                                                  progress=100, updated_at=timezone.now())
    except Exception as exc:
        message = str(exc) if isinstance(exc, (ValueError, PermissionDenied)) else 'No se pudo generar el PDF. Revise los datos y reintente.'
        ReportJob.objects.filter(pk=job.pk, status='running').update(status='error', message=message[:400], updated_at=timezone.now())
    return True
