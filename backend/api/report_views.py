import hashlib
import logging
import re
from datetime import timedelta
from django.db import transaction
from django.http import HttpResponse
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import serializers
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response
from .models import ReportJob, AnalysisSnapshot, Company
from .personal_views import Conflict
from .comparison_views import require_pro, year_value, pillars_for
from .report_pdf import generate_pdf

logger = logging.getLogger(__name__)


class ReportInput(serializers.Serializer):
    request_id = serializers.RegexField(r'^[a-zA-Z0-9-]{1,80}$')
    kind = serializers.ChoiceField(choices=['company', 'comparison'], default='company')
    a = serializers.RegexField(r'^[A-Z0-9][A-Z0-9.\-]{0,9}$')
    b = serializers.RegexField(r'^[A-Z0-9][A-Z0-9.\-]{0,9}$', required=False, allow_null=True, default=None)
    fiscal_year = serializers.IntegerField()
    range = serializers.ChoiceField(choices=['1M', '6M', '1Y', '5Y'], default='1Y')
    include_charts = serializers.BooleanField(default=False)
    include_analysis = serializers.BooleanField(default=False)
    analysis_level = serializers.ChoiceField(choices=['simple', 'technical'], default='simple')
    analysis_version = serializers.CharField(max_length=500, required=False, allow_null=True, default=None)
    analysis_origin = serializers.ChoiceField(choices=['ai', 'rules', 'mixed'], required=False, allow_null=True, default=None)

    def validate_fiscal_year(self, value):
        return year_value(value)

    def validate(self, data):
        if (data['kind'] == 'company' and data['b'] is not None) or (data['kind'] == 'comparison' and (not data['b'] or data['a'] == data['b'])):
            raise serializers.ValidationError('Empresas incompatibles con el tipo de reporte.')
        if data['include_analysis'] and (not data['analysis_version'] or not data['analysis_origin']):
            raise serializers.ValidationError('Consulta primero la versión y el origen del análisis.')
        return data


def job_row(job):
    return {'schema_version': 'report-job-v1', 'owner_id': str(job.user_id), 'id': str(job.pk),
            'status': job.status, 'request': job.request, 'progress': 100 if job.status == 'ready' else 30 if job.status == 'running' else 0,
            'message': job.message or None} | job.metadata


def check_request_access(user, value):
    if value['kind'] == 'comparison':
        require_pro(user)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def submit_report(request):
    serializer = ReportInput(data=request.data)
    serializer.is_valid(raise_exception=True)
    value = dict(serializer.validated_data)
    check_request_access(request.user, value)
    with transaction.atomic():
        existing = ReportJob.objects.filter(user=request.user, request_id=value['request_id']).first()
        if existing:
            if existing.request != value:
                raise Conflict('Este request_id ya identifica otras opciones de reporte.')
            return Response(job_row(existing))
        for ticker in filter(None, [value['a'], value['b']]):
            company = get_object_or_404(Company, ticker=ticker)
            if not company.annual_statements.filter(fiscal_year=value['fiscal_year'], is_demo=False).exists():
                raise ValidationError(f'{ticker} no tiene informe anual real para ese ejercicio.')
        analysis = {}
        if value['include_analysis']:
            snapshot = AnalysisSnapshot.objects.filter(user=request.user, version=value['analysis_version'],
                a=value['a'], b=value['b'] or '', fiscal_year=value['fiscal_year'], level=value['analysis_level'], origin=value['analysis_origin']).first()
            if snapshot is None:
                raise ValidationError('La versión del análisis no corresponde a esta cuenta o selección. Vuelve a consultarlo.')
            analysis = snapshot.payload
        if ReportJob.objects.filter(user=request.user, status__in=['queued', 'running']).count() >= 3:
            raise Conflict('Ya hay tres reportes pendientes. Termina o cancela uno antes de solicitar otro.')
        job, created = ReportJob.objects.get_or_create(user=request.user, request_id=value['request_id'],
            defaults={'request': value, 'analysis_payload': analysis})
        if job.request != value:
            raise Conflict('El identificador ya se usó con otras opciones.')
        return Response(job_row(job), status=202 if created else 200)


def execute_report(job):
    # The first status poll claims the durable job; another poll only observes running.
    # A crashed process can reclaim an abandoned lease after two minutes.
    ReportJob.objects.filter(pk=job.pk, status='running', updated_at__lt=timezone.now()-timedelta(minutes=2)).update(status='queued')
    claimed = ReportJob.objects.filter(pk=job.pk, status='queued').update(status='running', updated_at=timezone.now())
    if claimed:
        try:
            content, pages, data_version = generate_pdf(job.request, job.analysis_payload)
            metadata = {'data_version': data_version,
                        'content_sha256': hashlib.sha256(content).hexdigest(), 'size_bytes': len(content),
                        'page_count': pages, 'orientation': 'landscape' if job.request['kind'] == 'comparison' else 'portrait',
                        'chart_format': 'vector' if job.request['include_charts'] else None,
                        'chart_range': job.request['range'] if job.request['include_charts'] else None,
                        'analysis_version': job.request['analysis_version'] if job.request['include_analysis'] else None,
                        'analysis_origin': job.request['analysis_origin'] if job.request['include_analysis'] else None}
            ReportJob.objects.filter(pk=job.pk, status='running').update(status='ready', content=content, metadata=metadata, updated_at=timezone.now())
        except Exception:
            logger.exception('No se pudo generar reporte %s', job.pk)
            ReportJob.objects.filter(pk=job.pk, status='running').update(status='error', message='No se pudo generar el PDF con los datos y opciones solicitados.', updated_at=timezone.now())
    job.refresh_from_db()


def owned_job(request, pk):
    job = get_object_or_404(ReportJob, user=request.user, pk=pk)
    check_request_access(request.user, job.request)
    return job


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def report_status(request, pk):
    job = owned_job(request, pk)
    if job.status in ('queued', 'running'):
        execute_report(job)
    return Response(job_row(job))


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def cancel_report(request, pk):
    job = owned_job(request, pk)
    ReportJob.objects.filter(pk=job.pk, status__in=['queued', 'running']).update(status='cancelled', updated_at=timezone.now())
    job.refresh_from_db()
    return Response(job_row(job))


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def report_file(request, pk):
    job = owned_job(request, pk)
    if job.status != 'ready' or job.content is None:
        raise Conflict('El reporte aún no está listo para descargar.')
    response = HttpResponse(bytes(job.content), content_type='application/pdf')
    response['Content-Disposition'] = f'attachment; filename="reporte-{job.pk}.pdf"'
    response['Content-Length'] = job.metadata['size_bytes']
    response['Cache-Control'] = 'private, no-store'
    return response
