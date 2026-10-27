import hashlib
import json
from datetime import timedelta
from django.utils import timezone
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response
from rest_framework.throttling import UserRateThrottle
from .pillar_views import CompanyPillarsView
from .models import AnalysisRecord, Company
from .analysis_validation import context_from_pillars, validate_analysis, VERSION
from .analysis_provider import generate, MODEL, MODEL_SHA256


class AnalysisThrottle(UserRateThrottle):
    rate = '4/min'


class CompanyAnalysisView(CompanyPillarsView):
    throttle_classes = (AnalysisThrottle,)

    def get(self, request, ticker):
        pillars = super().get(request, ticker).data
        context = context_from_pillars(pillars)
        level = request.query_params.get('level', 'simple')
        if level not in ('simple', 'technical'):
            raise ValidationError({'level': 'Use simple o technical.'})
        if not context['fiscal_year'] or not any(m['status'] == 'available' for m in context['metrics'].values()):
            return Response({'status': 'unavailable', 'reason': 'insufficient_context', 'message': 'No hay métricas verificables para generar un análisis.'})
        company = Company.objects.get(ticker=context['ticker'])
        signature = {'context': context, 'level': level, 'version': VERSION, 'model': MODEL, 'model_sha256': MODEL_SHA256}
        digest = hashlib.sha256(json.dumps(signature, sort_keys=True, default=str).encode()).hexdigest()
        cached = AnalysisRecord.objects.filter(company=company, context_hash=digest, validated=True,
            created_at__gte=timezone.now() - timedelta(hours=24)).order_by('-created_at').first()
        if cached:
            return Response(cached.response | {'cache_hit': True})
        try:
            content = generate(context, level)
            validate_analysis(content, context)
        except Exception as exc:
            reason = str(exc) if isinstance(exc, ValueError) and str(exc) in {
                'unsupported_number', 'unsupported_claim', 'missing_pillars', 'invalid_structure',
                'invalid_list', 'invalid_claim', 'invalid_text', 'invalid_references', 'invalid_number'} else 'provider_unavailable_or_invalid'
            AnalysisRecord.objects.create(company=company, context_hash=digest, context=context,
                response={}, validated=False, validation_reason=reason)
            return Response({'status': 'unavailable', 'reason': reason,
                'message': 'El análisis no está disponible: el proveedor falló o la respuesta no superó la validación. Puedes reintentar.'}, status=503)
        result = {'status': 'available', 'schema_version': VERSION, 'ticker': context['ticker'],
            'fiscal_year': context['fiscal_year'], 'period': context['period'], 'level': level,
            'model': MODEL, 'model_sha256': MODEL_SHA256, 'provider': 'llama.cpp local · Qwen3 1.7B', 'validated': True,
            'validation_scope': 'Estructura, referencias y cifras contrastadas; no certifica todas las interpretaciones del modelo.',
            'context_hash': digest, 'metrics': context['metrics'], 'analysis': content,
            'pillar_availability': {key: any(m['status'] == 'available' and m['pillar'] == key
                for m in context['metrics'].values()) for key in context['pillars']},
            'sources': sorted({c['source']['url'] for p in pillars['pillars'] for m in p['metrics'] for c in m.get('components', [])}),
            'generated_at': timezone.now().isoformat(), 'cache_hit': False}
        record = AnalysisRecord.objects.create(company=company, context_hash=digest, context=context, response=result, validated=True)
        result['analysis_id'] = str(record.pk)
        record.response = result; record.save(update_fields=['response'])
        return Response(result)
