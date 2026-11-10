"""Históricos y conclusión compartidos por web, Android y reportes."""
from datetime import date, timedelta
from decimal import Decimal
import hashlib
import json
import urllib.request
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response
from rest_framework.views import APIView
from .comparison_views import ComparisonView
from .models import Company, ComparisonAnalysis
from .simulations import month_after
from .analysis_provider import MODEL, MODEL_SHA256

RANGES = {'1M': 1, '6M': 6, '1Y': 12, '5Y': 60}


def digest(data):
    return hashlib.sha256(json.dumps(data, sort_keys=True, default=str).encode()).hexdigest()


def prices(ticker, period):
    if period not in RANGES:
        raise ValidationError({'range': 'Use 1M, 6M, 1Y o 5Y.'})
    company = get_object_or_404(Company, ticker=ticker)
    end = timezone.localdate()
    return list(company.prices.filter(date__gte=month_after(end, -RANGES[period]), date__lte=end)
                .order_by('date').values('date', 'adjusted_close', 'currency', 'source', 'is_demo'))


def aligned_history(a, b, period):
    left, right = prices(a, period), prices(b, period)
    def series(rows):
        if any(not r['adjusted_close'].is_finite() or r['adjusted_close'] <= 0 for r in rows):
            raise ValidationError('El histórico contiene precios no válidos.')
        return {str(r['date']): r for r in rows}
    x, y = series(left), series(right)
    common = sorted(x.keys() & y.keys())
    base = {'schema_version': 'comparison-history-v1', 'a': a, 'b': b, 'range': period,
            'sources': sorted({r['source'] for r in left + right}),
            'is_demo': any(r['is_demo'] for r in left + right), 'points': []}
    if len(common) < 2:
        return base | {'status': 'unavailable', 'reason': 'No hay al menos dos fechas comunes con precios ajustados.'}
    if len({r['currency'] for r in left + right}) != 1:
        return base | {'status': 'unavailable', 'reason': 'Las monedas de los históricos no coinciden.'}
    p, q = x[common[0]]['adjusted_close'], y[common[0]]['adjusted_close']
    points = [{'date': d, 'a': float((x[d]['adjusted_close'] / p - 1) * 100),
               'b': float((y[d]['adjusted_close'] / q - 1) * 100),
               'price_a': str(x[d]['adjusted_close']), 'price_b': str(y[d]['adjusted_close'])} for d in common]
    return base | {'status': 'available', 'base_date': common[0], 'points': points,
                   'unit': 'percent', 'source_sha256': digest([left, right])}


class ComparisonHistoryView(ComparisonView):
    def get(self, request):
        comparison = super().get(request).data
        result = aligned_history(comparison['a']['ticker'], comparison['b']['ticker'], request.query_params.get('range', '1Y'))
        return Response(result | {'owner_id': str(request.user.pk)}, headers={'Cache-Control': 'private, no-store'})


def generate_comparison(options, level):
    """El modelo elige un resumen permitido; no crea cifras o ganadores."""
    prompt = ('<|im_start|>system\nElige un resumen de las opciones verificadas. Devuelve solo JSON. /no_think<|im_end|>\n'
              '<|im_start|>user\nNivel: ' + level + '\n' + json.dumps(options, ensure_ascii=False) +
              '<|im_end|>\n<|im_start|>assistant\n<think>\n\n</think>\n\n')
    payload = {'prompt': prompt, 'temperature': 0, 'seed': 2026, 'n_predict': 350,
               'json_schema': {'type': 'object', 'additionalProperties': False, 'required': ['summary'],
                               'properties': {'summary': {'type': 'string', 'enum': options}}}, 'stop': ['<|im_end|>']}
    req = urllib.request.Request('http://127.0.0.1:58110/completion', data=json.dumps(payload).encode(), headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(req, timeout=150) as response:
        raw = response.read(20001)
    if len(raw) > 20000:
        raise ValueError('Respuesta excesiva')
    result = json.loads(json.loads(raw)['content'])
    if set(result) != {'summary'} or result['summary'] not in options:
        raise ValueError('Conclusión no respaldada')
    return result['summary']


def conclusion_key(comparison, level):
    return digest({'comparison': comparison, 'level': level, 'model': MODEL_SHA256, 'version': 'controlled-comparison-v1'})


def conclusion(comparison, level):
    if level not in ('simple', 'technical'):
        raise ValidationError({'level': 'Use simple o technical.'})
    ident = conclusion_key(comparison, level)
    cached = ComparisonAnalysis.objects.filter(key=ident, created_at__gte=timezone.now() - timedelta(hours=24)).first()
    if cached and cached.response.get('generation_mode') == 'ai':
        return cached.response | {'cache_hit': True}
    a, b = comparison['a']['ticker'], comparison['b']['ticker']
    labels = {p['id']: p['label'] for p in comparison['a']['pillars']}
    strengths = {a: [], b: []}
    for row in comparison['comparisons']:
        if row['status'] == 'available' and row['result'] in ('a', 'b'):
            winner = a if row['result'] == 'a' else b
            strengths[winner].append(f"{labels[row['id']]}: mayor nota académica en el periodo comparable; diferencia A menos B: {row['delta']} puntos.")
    available = sum(r['status'] == 'available' for r in comparison['comparisons'])
    summary = (f'{a} y {b}: se pudieron comparar {available} de cinco pilares. '
               'Las notas describen el periodo histórico y no garantizan resultados futuros.')
    if available == 5:
        scores = [Decimal(comparison[k]['overall_score']['value']) for k in ('a', 'b')]
        summary += (' Los puntajes generales empatan.' if scores[0] == scores[1] else
                    f' {a if scores[0] > scores[1] else b} tiene mayor puntaje académico general.')
    else:
        summary += ' No hay base comparable completa para declarar una empresa globalmente más sólida.'
    options = [summary, summary + (' Revise las fuentes y fechas antes de interpretar las diferencias.' if level == 'simple' else
                                   ' La inferencia está limitada por la cobertura y la equivalencia temporal de los componentes.')]
    sources = sorted({c['source']['url'] for side in ('a', 'b') for p in comparison[side]['pillars']
                      for m in p['metrics'] for c in m.get('components', [])})
    mode = 'rules'
    if sources and available:
        try:
            summary = generate_comparison(options, level)
            mode = 'ai'
        except (OSError, ValueError, KeyError, TypeError):
            pass
    data = {'schema_version': 'comparison-conclusion-v1', 'a': a, 'b': b,
            'fiscal_year': comparison['fiscal_year'], 'level': level, 'generation_mode': mode,
            'model': MODEL if mode == 'ai' else None, 'generated_at': timezone.now().isoformat(),
            'data_version': ident, 'paragraphs': [summary], 'strengths_a': strengths[a][:4],
            'strengths_b': strengths[b][:4], 'sources': [{'label': 'Componentes financieros', 'url': s} for s in sources],
            'validation_scope': 'Texto restringido a conclusiones verificadas. Las fortalezas provienen de las notas del servidor.',
            'cache_hit': False}
    # Un fallo temporal del proveedor no bloquea una generación posterior real.
    ComparisonAnalysis.objects.update_or_create(key=ident, defaults={'response': data, 'created_at': timezone.now()})
    return data


class ComparisonConclusionView(ComparisonView):
    def get(self, request):
        comparison = super().get(request).data
        result = conclusion(comparison, request.query_params.get('level', 'simple'))
        return Response(result | {'owner_id': str(request.user.pk)}, headers={'Cache-Control': 'private, no-store'})
