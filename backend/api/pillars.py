"""Cálculos anuales para web y Android, separados de Altman-Z y de sus notas."""
from datetime import timedelta
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP, localcontext

METHODOLOGY_VERSION = 'annual-ratios-v1'
THRESHOLD_VERSION = 'academic-thresholds-v1'
THRESHOLDS = {
    'current_ratio': {'lower': '1', 'upper': '2', 'direction': 'higher'},
    'quick_ratio': {'lower': '0.5', 'upper': '1', 'direction': 'higher'},
}
THRESHOLD_EXPLANATION = ('Reglas de visualización académica de esta versión; no son un óptimo '
                         'universal ni una referencia sectorial. Consulte los límites y la fórmula.')
REASONS = {
    'missing_components': 'Faltan componentes del balance anual.',
    'insufficient_history': 'Falta el cierre anterior comparable para calcular el promedio.',
    'zero_denominator': 'El divisor es cero; no se puede calcular el indicador.',
    'invalid_denominator': 'El divisor debe ser positivo.',
    'nonpositive_equity': 'El patrimonio contable no es positivo en ambos cierres.',
    'invalid_components': 'Hay componentes no válidos para el cálculo.',
    'unconfigured_scoring': 'Las notas y ponderaciones del puntaje aún no están configuradas.',
    'not_implemented': 'Este pilar corresponde al siguiente avance del sprint.',
    'insufficient_sector_sample': 'No hay una muestra sectorial comparable suficiente con fecha y metodología comunes.',
}
LABELS = {'current_assets': 'Activos corrientes', 'current_liabilities': 'Pasivos corrientes',
          'cash': 'Efectivo', 'short_term_investments': 'Inversiones a corto plazo',
          'receivables': 'Cuentas por cobrar', 'net_income': 'Beneficio neto',
          'book_equity': 'Patrimonio contable', 'total_assets': 'Activos totales',
          'revenue': 'Ingresos anuales', 'total_liabilities': 'Pasivos totales',
          'income_before_tax': 'Beneficio antes de impuestos', 'interest_expense': 'Gasto por intereses'}


def field_label(name):
    field = name.removeprefix('previous.')
    return LABELS.get(field, field) + (' del cierre anterior' if name.startswith('previous.') else '')


def formula_label(formula):
    for name in sorted(LABELS, key=len, reverse=True):
        formula = formula.replace('previous.' + name, field_label('previous.' + name))
        formula = formula.replace(name, field_label(name))
    return formula


def source(statement, tag):
    return {'url': statement.source_url, 'tag': tag, 'accession': statement.accession,
            'filed': statement.filed.isoformat(), 'currency': statement.currency,
            'snapshot_sha256': statement.snapshot_sha256, 'is_demo': statement.is_demo}


def period(statement):
    return {'start': statement.period_start.isoformat(), 'end': statement.period_end.isoformat()} if statement else {'start': None, 'end': None}


def unavailable(reason):
    return {'status': 'unavailable', 'value': None, 'display_value': 'No disponible',
            'reason': reason, 'explanation': REASONS[reason]}


def metric(current, previous, ident, label, unit, formula, fields, calculate, denominator,
           equity_check=False):
    result = {'id': ident, 'label': label, 'unit': unit, 'formula': formula, 'formula_display': formula_label(formula),
              'period': period(current), 'components': [], 'missing_fields': [], 'thresholds': None}
    values = {}
    invalid = False
    for name in fields:
        statement = previous if name.startswith('previous.') else current
        field = name.removeprefix('previous.')
        item = statement.components.get(field, {}) if statement else {}
        value = item.get('value')
        if value is None:
            result['missing_fields'].append(name)
            continue
        try:
            decimal = Decimal(str(value))
            if not decimal.is_finite():
                raise ValueError('No finito')
            values[name] = decimal
        except (InvalidOperation, ValueError, TypeError):
            invalid = True
            continue
        result['components'].append({'id': name, 'label': field_label(name), 'value': str(decimal),
                                     'period': period(statement), 'source': source(statement, item.get('tag'))})
    if invalid:
        return result | unavailable('invalid_components')
    if result['missing_fields']:
        reason = 'insufficient_history' if any(n.startswith('previous.') for n in result['missing_fields']) else 'missing_components'
        return result | unavailable(reason)
    with localcontext() as context:
        context.prec = 40
        if equity_check and (values['book_equity'] <= 0 or values['previous.book_equity'] <= 0):
            return result | unavailable('nonpositive_equity')
        divisor = denominator(values)
        if divisor <= 0:
            return result | unavailable('zero_denominator' if divisor == 0 else 'invalid_denominator')
        value = calculate(values) / divisor
        display = format(value.quantize(Decimal('.01'), rounding=ROUND_HALF_UP), '.2f').replace('.', ',')
        result.update(status='available', value=str(value), display_value=display, reason=None)
    policy = THRESHOLDS.get(ident)
    if policy:
        lower, upper = Decimal(policy['lower']), Decimal(policy['upper'])
        if policy['direction'] == 'higher':
            band = 'low' if value < lower else 'middle' if value < upper else 'high'
        else:
            band = 'high' if value <= lower else 'middle' if value <= upper else 'low'
        result['thresholds'] = dict(policy, version=THRESHOLD_VERSION, band=band,
                                    explanation=THRESHOLD_EXPLANATION)
    return result


def liquidity(current, previous):
    return [
        metric(current, previous, 'current_ratio', 'Liquidez corriente', 'times',
               'current_assets / current_liabilities', ['current_assets', 'current_liabilities'],
               lambda v: v['current_assets'], lambda v: v['current_liabilities']),
        metric(current, previous, 'quick_ratio', 'Prueba ácida', 'times',
               '(cash + short_term_investments + receivables) / current_liabilities',
               ['cash', 'short_term_investments', 'receivables', 'current_liabilities'],
               lambda v: v['cash'] + v['short_term_investments'] + v['receivables'],
               lambda v: v['current_liabilities']),
    ]


def profitability(current, previous):
    return [
        metric(current, previous, 'roe', 'ROE', 'percent',
               'net_income / ((book_equity + previous.book_equity) / 2) * 100',
               ['net_income', 'book_equity', 'previous.book_equity'],
               lambda v: v['net_income'] * 100,
               lambda v: (v['book_equity'] + v['previous.book_equity']) / 2, equity_check=True),
        metric(current, previous, 'roa', 'ROA', 'percent',
               'net_income / ((total_assets + previous.total_assets) / 2) * 100',
               ['net_income', 'total_assets', 'previous.total_assets'],
               lambda v: v['net_income'] * 100,
               lambda v: (v['total_assets'] + v['previous.total_assets']) / 2),
        metric(current, previous, 'net_margin', 'Margen neto', 'percent',
               'net_income / revenue * 100', ['net_income', 'revenue'],
               lambda v: v['net_income'] * 100, lambda v: v['revenue']),
    ]


def build_pillars(company, current, previous, fiscal_year, available_years):
    if previous and (not current or previous.currency != current.currency
                     or previous.period_end + timedelta(days=1) != current.period_start):
        previous = None
    implementations = {'liquidity': liquidity, 'profitability': profitability}
    pillars = []
    for ident, label in [('liquidity', 'Liquidez'), ('profitability', 'Rentabilidad'),
                         ('solvency', 'Solvencia'), ('efficiency', 'Eficiencia'), ('growth', 'Crecimiento')]:
        calculate = implementations.get(ident)
        metrics = calculate(current, previous) if calculate else []
        pillars.append({'id': ident, 'label': label, 'status': 'available' if metrics and all(
            m['status'] == 'available' for m in metrics) else 'partial' if any(
            m['status'] == 'available' for m in metrics) else 'unavailable',
            'reason': None if calculate else 'not_implemented', 'metrics': metrics,
            'score': unavailable('unconfigured_scoring')})
        if ident == 'profitability':
            pillars[-1]['sector_reference'] = unavailable('insufficient_sector_sample')
    return {'schema_version': 'pillars-v1', 'methodology_version': METHODOLOGY_VERSION,
            'methodology_status': 'implementation_pending_team_review',
            'ticker': company.ticker, 'fiscal_year': fiscal_year, 'period': period(current),
            'currency': current.currency if current else None, 'available_years': available_years,
            'pillars': pillars, 'overall_score': unavailable('unconfigured_scoring')}
