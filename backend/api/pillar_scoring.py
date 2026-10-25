"""Convención académica explicable; no es un ranking sectorial ni un modelo validado."""
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP, localcontext

VERSION = 'academic-pillar-scores-v1'
EXPLANATION = ('Escala académica definida por el proyecto, pendiente de revisión del equipo. '
               'No es un ranking sectorial ni una probabilidad o garantía de inversión. '
               'Los valores entre puntos se interpolan linealmente y los extremos se limitan a 0 y 100.')
POINTS = {
    'current_ratio': [('0', '0'), ('1', '50'), ('2', '100')],
    'quick_ratio': [('0', '0'), ('0.5', '50'), ('1', '100')],
    'roe': [('0', '0'), ('10', '50'), ('20', '100')],
    'roa': [('0', '0'), ('5', '50'), ('10', '100')],
    'net_margin': [('0', '0'), ('10', '50'), ('20', '100')],
    'liabilities_to_equity': [('0', '100'), ('1', '75'), ('2', '50'), ('4', '0')],
    'interest_coverage': [('0', '0'), ('1', '50'), ('3', '100')],
    'asset_turnover': [('0', '0'), ('0.5', '50'), ('1', '100')],
    'inventory_days': [('0', '100'), ('90', '50'), ('180', '0')],
    'revenue_growth': [('0', '0'), ('5', '50'), ('20', '100')],
    'revenue_cagr_3y': [('0', '0'), ('5', '50'), ('20', '100')],
}
METRICS = {
    'liquidity': ['current_ratio', 'quick_ratio'],
    'profitability': ['roe', 'roa', 'net_margin'],
    'solvency': ['liabilities_to_equity', 'interest_coverage'],
    'efficiency': ['asset_turnover', 'inventory_days'],
    'growth': ['revenue_growth', 'revenue_cagr_3y'],
}
WEIGHTS = {ident: '0.20' for ident in METRICS}


def display(value):
    return format(value.quantize(Decimal('.01'), rounding=ROUND_HALF_UP), '.2f').replace('.', ',')


def normalize(ident, value):
    """Interpolar los anclajes declarados sin utilizar valores ya redondeados."""
    points = [(Decimal(x), Decimal(y)) for x, y in POINTS[ident]]
    value = Decimal(str(value))
    if not value.is_finite():
        raise ValueError('No se puede puntuar un dato no finito.')
    if value <= points[0][0]:
        return points[0][1]
    if value >= points[-1][0]:
        return points[-1][1]
    with localcontext() as context:
        context.prec = 40
        for (x0, y0), (x1, y1) in zip(points, points[1:]):
            if x0 <= value <= x1:
                return y0 + (value - x0) * (y1 - y0) / (x1 - x0)
    raise ValueError('No se encontró un intervalo de puntuación.')


def score_pillar(pillar):
    metrics = {m['id']: m for m in pillar['metrics']}
    missing, parts, excluded = [], [], []
    for ident in METRICS[pillar['id']]:
        metric = metrics.get(ident)
        if (ident == 'inventory_days' and metric and metric['status'] == 'not_applicable'
                and metric.get('reason') == 'inventory_not_applicable' and metric.get('explanation')):
            excluded.append({'id': ident, 'reason': metric['reason'], 'explanation': metric['explanation']})
            continue
        if not metric or metric['status'] != 'available' or metric.get('value') is None:
            missing.append(ident)
            continue
        try:
            value = normalize(ident, metric['value'])
        except (ArithmeticError, InvalidOperation, ValueError, TypeError):
            missing.append(ident)
            continue
        parts.append({'id': ident, 'label': metric['label'], 'input_value': metric['value'],
                      'value': str(value), 'display_value': display(value),
                      'anchors': [{'input': x, 'score': y} for x, y in POINTS[ident]]})
    result = {'version': VERSION, 'parts': parts, 'excluded_metrics': excluded, 'missing_metrics': missing,
              'aggregation': 'Media aritmética de las notas de los indicadores aplicables, sin redondeo previo.'}
    if missing or not parts:
        return result | {'status': 'unavailable', 'value': None, 'display_value': 'No disponible',
                         'reason': 'missing_metrics', 'explanation': 'Faltan indicadores requeridos para obtener la nota del pilar.'}
    with localcontext() as context:
        context.prec = 40
        value = sum(Decimal(part['value']) for part in parts) / len(parts)
        return result | {'status': 'available', 'value': str(value), 'display_value': display(value), 'reason': None}


def policy():
    return {'version': VERSION, 'status': 'academic_convention_pending_team_review',
            'explanation': EXPLANATION, 'rounding': 'HALF_UP a dos decimales solo al presentar.',
            'anchors': {ident: [{'input': x, 'score': y} for x, y in points] for ident, points in POINTS.items()},
            'pillar_aggregation': 'Media aritmética de indicadores requeridos. Solo se excluye inventario no aplicable con motivo documentado.',
            'overall_weights': WEIGHTS}


def overall_score(pillars):
    """Exigir cinco notas compatibles; nunca reponderar las que sí existen."""
    counts = {ident: sum(p['id'] == ident for p in pillars) for ident in WEIGHTS}
    missing, contributions = [], []
    for ident, weight in WEIGHTS.items():
        p = next((p for p in pillars if p['id'] == ident), None)
        note = p.get('score', {}) if p else {}
        value = None
        if counts[ident] == 1 and note.get('status') == 'available' and note.get('version') == VERSION:
            try:
                candidate = Decimal(str(note['value']))
                if candidate.is_finite() and 0 <= candidate <= 100:
                    value = candidate
            except (ArithmeticError, ValueError, TypeError, KeyError):
                pass
        if value is None:
            missing.append(ident)
        contributions.append({'id': ident, 'label': p['label'] if p else ident, 'weight': weight,
                              'weight_display': '20 %', 'value': str(value) if value is not None else None,
                              'display_value': display(value) if value is not None else 'No disponible'})
    result = {'version': VERSION, 'contributions': contributions, 'missing_pillars': missing,
              'formula': '0.20 * (liquidity + profitability + solvency + efficiency + growth)',
              'aggregation': 'Cada pilar aporta un 20 %. Se utilizan las notas sin redondear y se redondea solo el resultado presentado.'}
    if missing:
        return result | {'status': 'unavailable', 'value': None, 'display_value': 'No disponible',
                         'reason': 'incomplete_pillars',
                         'explanation': 'Faltan cinco notas válidas de la misma versión; no se calcula un promedio parcial.'}
    with localcontext() as context:
        context.prec = 40
        value = sum(Decimal(row['value']) * Decimal(row['weight']) for row in contributions)
        return result | {'status': 'available', 'value': str(value), 'display_value': display(value), 'reason': None}
