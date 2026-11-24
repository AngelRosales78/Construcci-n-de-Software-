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
from .billing import settle, PRO_PRICE, CURRENCY


def access_info(user):
    subscription = settle(UserSubscription.objects.filter(user=user).first())
    local = bool(getattr(settings, 'LOCAL_DEVELOPMENT_PRO', False)) and subscription is None
    now = timezone.now()
    plan = subscription.plan if subscription else 'pro' if local else 'free'
    paid = subscription is not None and plan == 'pro'
    active = (subscription.active and subscription.valid_until > now) if paid else True
    # valid_until is how long this permission answer may be trusted; period_end is the billing date.
    expiry = subscription.valid_until if paid and subscription.valid_until > now else now + timedelta(minutes=5)
    if local:
        expiry = now + timedelta(days=1)
    return {'schema_version': 'subscription-v1', 'owner_id': str(user.pk), 'plan': plan,
            'active': bool(active), 'features': ['compare', 'comparison_pdf'] if plan == 'pro' and active else [],
            'valid_until': expiry.isoformat(), 'grant_origin': 'local-development' if local else 'subscription',
            'notice': 'Acceso Pro académico local, sin compra ni pago.' if local else None,
            'period_start': subscription.period_start.isoformat() if paid and subscription.period_start else None,
            'period_end': subscription.valid_until.isoformat() if paid else None,
            'auto_renew': bool(subscription.auto_renew) if paid else False,
            'cancelled_at': subscription.cancelled_at.isoformat() if paid and subscription.cancelled_at else None,
            'price': str(PRO_PRICE) if paid else None, 'currency': CURRENCY if paid else None}


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


# ─── Sugerencias de empresas comparables ─────────────────────────────

def _period_scores(year, start, end):
    """Available pillar scores of every company whose fiscal year has exactly these dates (cached)."""
    from collections import defaultdict
    from django.core.cache import cache
    from django.db.models import Count, Max
    from .models import AnnualFinancialStatement
    from .pillars import METHODOLOGY_VERSION
    stamp = AnnualFinancialStatement.objects.aggregate(n=Count('id'), last=Max('id'))
    key = f'compare-candidates:{METHODOLOGY_VERSION}:{year}:{start}:{end}:{stamp["n"]}:{stamp["last"]}'
    cached = cache.get(key)
    if cached is not None:
        return cached
    ids = list(AnnualFinancialStatement.objects.filter(fiscal_year=year, is_demo=False, period_start=start,
                                                        period_end=end).values_list('company_id', flat=True))
    statements = defaultdict(dict)
    for row in AnnualFinancialStatement.objects.filter(company_id__in=ids, is_demo=False):
        statements[row.company_id][row.fiscal_year] = row
    result = {}
    for company in Company.objects.filter(id__in=ids).only('id', 'ticker', 'name', 'sector', 'market_cap'):
        rows = statements[company.id]
        data = build_pillars(company, rows.get(year), rows.get(year - 1), year, sorted(rows, reverse=True), rows)
        result[company.ticker] = {
            'name': company.name, 'sector': company.sector, 'sector_display': company.get_sector_display(),
            'market_cap': str(company.market_cap) if company.market_cap is not None else None,
            'scores': {p['id']: p['score']['version'] for p in data['pillars'] if p['score']['status'] == 'available'},
            'scoring_policy': data['scoring_policy']['version'], 'methodology': data['methodology_version']}
    cache.set(key, result, 600)
    return result


def candidates_data(ticker, year, limit=500):
    """Companies that can be compared with `ticker` in `year`: same fiscal dates and methodology,
    ranked by how many of the five pillars both have scored, then same sector, then market cap."""
    from .models import AnnualFinancialStatement
    company = get_object_or_404(Company, ticker=str(ticker or '').strip().upper())
    statement = AnnualFinancialStatement.objects.filter(company=company, fiscal_year=year, is_demo=False).first()
    base = {'schema_version': 'compare-candidates-v1', 'a': company.ticker, 'fiscal_year': year,
            'period': None, 'a_pillars': [], 'total_same_period': 0, 'results': []}
    if statement is None or not statement.period_start or not statement.period_end:
        available = sorted(AnnualFinancialStatement.objects.filter(company=company, is_demo=False)
                           .values_list('fiscal_year', flat=True), reverse=True)
        return base | {'reason': f'{company.ticker} no tiene balance anual para FY{year}.', 'available_years': available[:6]}
    scores = _period_scores(year, statement.period_start, statement.period_end)
    own = scores.get(company.ticker, {})
    results = []
    for other, info in scores.items():
        if other == company.ticker or info['methodology'] != own.get('methodology') or info['scoring_policy'] != own.get('scoring_policy'):
            continue
        shared = [p for p, version in own.get('scores', {}).items() if info['scores'].get(p) == version]
        if shared:
            results.append({'ticker': other, 'name': info['name'], 'sector': info['sector'], 'sector_display': info['sector_display'],
                            'market_cap': info['market_cap'], 'comparable_pillars': len(shared), 'pillars': shared,
                            'same_sector': info['sector'] == company.sector})
    results.sort(key=lambda r: (-r['comparable_pillars'], not r['same_sector'], -Decimal(r['market_cap'] or 0), r['ticker']))
    reason = None
    if not own.get('scores'):
        reason = f'{company.ticker} no tiene notas de pilares para FY{year}; no se puede comparar ese ejercicio.'
    elif not results:
        reason = f'Ninguna otra empresa cierra su ejercicio en las mismas fechas que {company.ticker} con pilares disponibles.'
    return base | {'period': {'start': statement.period_start.isoformat(), 'end': statement.period_end.isoformat()},
                   'a_pillars': list(own.get('scores', {})), 'total_same_period': len(scores) - 1,
                   'results': results[:limit], 'reason': reason}


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def compare_candidates(request):
    require_pro(request.user)
    return Response(candidates_data(request.query_params.get('a'), year_value(request.query_params.get('fiscal_year'))))
