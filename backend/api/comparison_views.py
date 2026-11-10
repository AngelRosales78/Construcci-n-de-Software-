import hashlib
import json
from datetime import timedelta
from decimal import Decimal
from django.conf import settings
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.exceptions import PermissionDenied, ValidationError
from rest_framework.response import Response
from .models import Company, UserSubscription, AnalysisSnapshot
from .pillars import build_pillars


def access_info(user):
    subscription = UserSubscription.objects.filter(user=user).first()
    local = bool(getattr(settings, 'LOCAL_DEVELOPMENT_PRO', False)) and subscription is None
    now = timezone.now()
    plan = subscription.plan if subscription else 'pro' if local else 'free'
    active = subscription.active and subscription.valid_until > now if subscription else True
    expiry = subscription.valid_until if subscription and subscription.valid_until > now else now + timedelta(minutes=5)
    if local:
        expiry = now + timedelta(days=1)
    return {'schema_version': 'subscription-v1', 'owner_id': str(user.pk), 'plan': plan,
            'active': bool(active), 'features': ['compare', 'comparison_pdf'] if plan == 'pro' and active else [],
            'valid_until': expiry.isoformat(), 'grant_origin': 'local-development' if local else 'subscription',
            'notice': 'Acceso Pro académico local, sin compra ni pago.' if local else None}


def require_pro(user):
    access = access_info(user)
    if access['plan'] != 'pro' or not access['active'] or 'compare' not in access['features']:
        raise PermissionDenied('Esta operación requiere acceso Pro activo.')


def year_value(value):
    if isinstance(value, bool):
        raise ValidationError({'fiscal_year': 'Ejercicio no válido.'})
    try:
        year = int(str(value))
    except (TypeError, ValueError):
        raise ValidationError({'fiscal_year': 'Indica un ejercicio entero.'})
    if not 1900 <= year <= timezone.localdate().year:
        raise ValidationError({'fiscal_year': 'Ejercicio no válido o futuro.'})
    return year


def pillars_for(company, year):
    statements = {r.fiscal_year: r for r in company.annual_statements.filter(is_demo=False)}
    return build_pillars(company, statements.get(year), statements.get(year-1), year, sorted(statements, reverse=True), statements)


def comparison_data(a, b, year):
    if not isinstance(a, str) or not isinstance(b, str) or a == b:
        raise ValidationError('Selecciona dos empresas distintas.')
    left = pillars_for(get_object_or_404(Company, ticker=a), year)
    right = pillars_for(get_object_or_404(Company, ticker=b), year)
    shared_reasons = []
    left_period, right_period = left['period'], right['period']
    if not all(period and period.get('start') and period.get('end') for period in (left_period, right_period)):
        shared_reasons.append('Faltan fechas del ejercicio.')
    elif left_period != right_period:
        shared_reasons.append('Los ejercicios tienen fechas de inicio o cierre distintas.')
    if (left['methodology_version'] != right['methodology_version']
            or left['scoring_policy']['version'] != right['scoring_policy']['version']):
        shared_reasons.append('Las metodologías de puntuación son distintas.')

    decisions = []
    for p, q in zip(left['pillars'], right['pillars']):
        score_a, score_b = p['score'], q['score']
        reasons = shared_reasons.copy()
        missing = [ticker for ticker, score in ((a, score_a), (b, score_b)) if score['status'] != 'available']
        if missing:
            reasons.append(f'Falta la nota en {" y ".join(missing)}.')
        elif score_a['version'] != score_b['version']:
            reasons.append('Las versiones de las notas son distintas.')
        valid = not reasons
        delta = Decimal(score_a['value']) - Decimal(score_b['value']) if valid else None
        decisions.append({'id': p['id'], 'status': 'available' if valid else 'not_comparable',
                          'result': ('a' if delta > 0 else 'b' if delta < 0 else 'tie') if valid else 'not_comparable',
                          'delta': str(delta) if valid else None, 'unit': 'points',
                          'reason': 'Diferencia de notas académicas con igual periodo y metodología.' if valid else ' '.join(reasons)})
    return {'schema_version': 'comparison-v1', 'fiscal_year': year, 'status': 'available',
            'a': left, 'b': right, 'comparisons': decisions}


def conclusion_data(user, a, b, year, level):
    if level not in ('simple', 'technical'):
        raise ValidationError({'level': 'Usa simple o technical.'})
    data = comparison_data(a, b, year)
    paragraphs, strengths_a, strengths_b, sources = [], [], [], []
    for left, right, decision in zip(data['a']['pillars'], data['b']['pillars'], data['comparisons']):
        label = left['label']
        a_note, b_note = left['score']['display_value'], right['score']['display_value']
        text = f'{label}: {a} {a_note} / 100; {b} {b_note} / 100. {decision["reason"]}'
        if decision['result'] in ('a', 'b'):
            text += f' Mayor nota: {a if decision["result"] == "a" else b}.'
            target = strengths_a if decision['result'] == 'a' else strengths_b
            target.append(text)
        if level == 'technical':
            text += f' Normalización: {data["a"]["scoring_policy"]["version"]}; delta A menos B: {decision["delta"] or "no comparable"}.'
        paragraphs.append(text)
    for ticker in (a, b):
        statement = Company.objects.get(ticker=ticker).annual_statements.filter(fiscal_year=year, is_demo=False).first()
        if statement:
            sources.append({'label': f'{ticker} · informe anual {year}', 'url': statement.source_url})
    if not sources:
        raise ValidationError('No hay informes anuales reales para el ejercicio seleccionado.')
    paragraphs.append('Las notas siguen una convención académica. No son recomendaciones de compra ni garantizan rendimientos.')
    return {'schema_version': 'comparison-conclusion-v1', 'owner_id': str(user.pk), 'a': a, 'b': b,
            'fiscal_year': year, 'level': level, 'generation_mode': 'rules', 'generated_at': timezone.now().isoformat(),
            'data_version': hashlib.sha256(json.dumps(data, sort_keys=True).encode()).hexdigest(),
            'paragraphs': paragraphs, 'strengths_a': strengths_a[:4], 'strengths_b': strengths_b[:4], 'sources': sources}


def save_analysis(user, report, b=''):
    comparison = bool(b)
    version = (f'{report["data_version"]}/{report["generated_at"]}' if comparison else
               f'{report["metrics_version"]}/{report["data_as_of"]}/{report["generated_at"]}')
    origin = report['generation_mode'] if comparison else 'mixed' if report['generation_mode'] == 'ai' else 'rules'
    AnalysisSnapshot.objects.update_or_create(user=user, version=version, defaults={
        'a': report['a'] if comparison else report['ticker'], 'b': b,
        'fiscal_year': report['fiscal_year'], 'level': report['level'], 'origin': origin, 'payload': report})


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def subscription(request):
    return Response(access_info(request.user))


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def compare(request):
    require_pro(request.user)
    return Response(comparison_data(request.query_params.get('a'), request.query_params.get('b'), year_value(request.query_params.get('fiscal_year'))))


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def conclusion(request):
    require_pro(request.user)
    report = conclusion_data(request.user, request.query_params.get('a'), request.query_params.get('b'),
                             year_value(request.query_params.get('fiscal_year')), request.query_params.get('level', 'simple'))
    save_analysis(request.user, report, report['b'])
    return Response(report)
