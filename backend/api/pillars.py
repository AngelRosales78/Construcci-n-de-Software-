"""Cálculos anuales para web y Android, separados de Altman-Z y de sus notas."""
from datetime import timedelta
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP, localcontext
from .pillar_scoring import score_pillar, policy

METHODOLOGY_VERSION = 'annual-ratios-v2'
THRESHOLD_VERSION = 'academic-thresholds-v1'
THRESHOLDS = {
    'current_ratio': {'lower': '1', 'upper': '2', 'direction': 'higher'},
    'quick_ratio': {'lower': '0.5', 'upper': '1', 'direction': 'higher'},
    'liabilities_to_equity': {'lower': '1', 'upper': '2', 'direction': 'lower'},
    'interest_coverage': {'lower': '1', 'upper': '3', 'direction': 'higher'},
}
THRESHOLD_EXPLANATION = ('Reglas de visualización académica de esta versión; no son un óptimo '
                         'universal ni una referencia sectorial. Consulte los límites y la fórmula.')
REASONS = {
    'missing_components': 'Faltan componentes del balance anual.',
    'insufficient_history': 'Faltan ejercicios consecutivos comparables para calcular este indicador.',
    'zero_denominator': 'El divisor es cero; no se puede calcular el indicador.',
    'invalid_denominator': 'El divisor debe ser positivo.',
    'nonpositive_equity': 'El patrimonio contable no es positivo en ambos cierres.',
    'invalid_components': 'Hay componentes no válidos para el cálculo.',
    'unconfigured_scoring': 'Las notas y ponderaciones del puntaje aún no están configuradas.',
    'not_implemented': 'Este pilar corresponde al siguiente avance del sprint.',
    'insufficient_sector_sample': 'No hay una muestra sectorial comparable suficiente con fecha y metodología comunes.',
    'inventory_not_applicable': 'El inventario no aplica a este negocio según el motivo documentado.',
}
LABELS = {'current_assets': 'Activos corrientes', 'current_liabilities': 'Pasivos corrientes',
          'cash': 'Efectivo', 'short_term_investments': 'Inversiones a corto plazo',
          'receivables': 'Cuentas por cobrar', 'net_income': 'Beneficio neto',
          'book_equity': 'Patrimonio contable', 'total_assets': 'Activos totales',
          'revenue': 'Ingresos anuales', 'total_liabilities': 'Pasivos totales',
          'income_before_tax': 'Beneficio antes de impuestos', 'interest_expense': 'Gasto por intereses',
          'inventory': 'Inventario', 'cost_of_sales': 'Costo de ventas anual'}


def field_label(name):
    prefix, separator, field = name.partition('.')
    if not separator:
        return LABELS.get(name, name)
    suffix = {'previous': ' del cierre anterior', 'year2': ' de hace dos ejercicios',
              'year3': ' de hace tres ejercicios'}
    return LABELS.get(field, field) + suffix.get(prefix, '')


def formula_label(formula):
    for name in sorted(LABELS, key=len, reverse=True):
        for prefix in ('previous.', 'year2.', 'year3.'):
            formula = formula.replace(prefix + name, field_label(prefix + name))
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
           equity_check=False, history=None, postprocess=None):
    result = {'id': ident, 'label': label, 'unit': unit, 'formula': formula, 'formula_display': formula_label(formula),
              'period': period(current), 'components': [], 'missing_fields': [], 'thresholds': None}
    values = {}
    invalid = False
    for name in fields:
        prefix, separator, field = name.partition('.')
        statement = ({'previous': previous} | (history or {})).get(prefix) if separator else current
        field = field if separator else name
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
        reason = 'insufficient_history' if any('.' in n for n in result['missing_fields']) else 'missing_components'
        return result | unavailable(reason)
    with localcontext() as context:
        context.prec = 40
        if equity_check and (values['book_equity'] <= 0 or values['previous.book_equity'] <= 0):
            return result | unavailable('nonpositive_equity')
        divisor = denominator(values)
        if divisor <= 0:
            return result | unavailable('zero_denominator' if divisor == 0 else 'invalid_denominator')
        try:
            value = calculate(values) / divisor
            if postprocess:
                value = postprocess(value)
            display = format(value.quantize(Decimal('.01'), rounding=ROUND_HALF_UP), '.2f').replace('.', ',')
        except ArithmeticError:
            return result | unavailable('invalid_components')
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


def solvency(current, previous):
    return [
        metric(current, previous, 'liabilities_to_equity', 'Pasivos sobre patrimonio', 'times',
               'total_liabilities / book_equity', ['total_liabilities', 'book_equity'],
               lambda v: v['total_liabilities'], lambda v: v['book_equity']),
        metric(current, previous, 'interest_coverage', 'Cobertura de intereses', 'times',
               '(income_before_tax + interest_expense) / interest_expense',
               ['income_before_tax', 'interest_expense'],
               lambda v: v['income_before_tax'] + v['interest_expense'], lambda v: v['interest_expense']),
    ]


def efficiency(current, previous):
    turnover = metric(current, previous, 'asset_turnover', 'Rotación de activos', 'times',
                      'revenue / ((total_assets + previous.total_assets) / 2)',
                      ['revenue', 'total_assets', 'previous.total_assets'],
                      lambda v: v['revenue'], lambda v: (v['total_assets'] + v['previous.total_assets']) / 2)
    if current and current.inventory_applicable is False:
        inventory = {'id': 'inventory_days', 'label': 'Días de inventario', 'unit': 'days',
                     'period': period(current), 'components': [], 'thresholds': None, 'missing_fields': [],
                     'formula': '((inventory + previous.inventory) / 2) / cost_of_sales * 365',
                     'formula_display': 'Inventario promedio / Costo de ventas anual × 365',
                     'status': 'not_applicable', 'value': None, 'display_value': 'No aplica',
                     'reason': 'inventory_not_applicable',
                     'explanation': current.inventory_applicability_note,
                     'applicability_source': source(current, None)}
    else:
        inventory = metric(current, previous, 'inventory_days', 'Días de inventario', 'days',
                           '((inventory + previous.inventory) / 2) / cost_of_sales * 365',
                           ['inventory', 'previous.inventory', 'cost_of_sales'],
                           lambda v: (v['inventory'] + v['previous.inventory']) / 2 * 365,
                           lambda v: v['cost_of_sales'])
    return [turnover, inventory]


def comparable_previous(current, candidate):
    return candidate if current and candidate and current.currency == candidate.currency and (
        candidate.period_end + timedelta(days=1) == current.period_start) else None


def growth(current, previous, statements):
    year2 = comparable_previous(previous, statements.get(current.fiscal_year-2)) if current else None
    year3 = comparable_previous(year2, statements.get(current.fiscal_year-3)) if current else None
    return [
        metric(current, previous, 'revenue_growth', 'Variación anual de ingresos', 'percent',
               '(revenue / previous.revenue - 1) * 100', ['revenue', 'previous.revenue'],
               lambda v: v['revenue'], lambda v: v['previous.revenue'],
               postprocess=lambda ratio: (ratio - 1) * 100),
        metric(current, previous, 'revenue_cagr_3y', 'Crecimiento compuesto a tres años', 'percent',
               '((revenue / year3.revenue) ^ (1 / 3) - 1) * 100',
               ['revenue', 'previous.revenue', 'year2.revenue', 'year3.revenue'],
               lambda v: v['revenue'], lambda v: v['year3.revenue'],
               history={'year2': year2, 'year3': year3},
               postprocess=lambda ratio: (ratio ** (Decimal(1) / 3) - 1) * 100),
    ]


def build_pillars(company, current, previous, fiscal_year, available_years, statements=None):
    previous = comparable_previous(current, previous)
    statements = statements or ({current.fiscal_year: current} if current else {})
    implementations = {'liquidity': liquidity, 'profitability': profitability, 'solvency': solvency,
                       'efficiency': efficiency, 'growth': lambda c, p: growth(c, p, statements)}
    pillars = []
    for ident, label in [('liquidity', 'Liquidez'), ('profitability', 'Rentabilidad'),
                         ('solvency', 'Solvencia'), ('efficiency', 'Eficiencia'), ('growth', 'Crecimiento')]:
        calculate = implementations.get(ident)
        metrics = calculate(current, previous) if calculate else []
        pillars.append({'id': ident, 'label': label, 'status': 'available' if metrics and all(
            m['status'] in ('available', 'not_applicable') for m in metrics) else 'partial' if any(
            m['status'] == 'available' for m in metrics) else 'unavailable',
            'reason': None if calculate else 'not_implemented', 'metrics': metrics,
            'score': unavailable('unconfigured_scoring')})
        if ident == 'profitability':
            pillars[-1]['sector_reference'] = unavailable('insufficient_sector_sample')
        if ident == 'efficiency':
            years = sorted((y for y in statements if current and y <= current.fiscal_year), reverse=True)[:3]
            pillars[-1]['history'] = [
                {'fiscal_year': y, 'period': period(statements[y]),
                 'metrics': efficiency(statements[y], comparable_previous(statements[y], statements.get(y-1)))}
                for y in years]
        pillars[-1]['score'] = score_pillar(pillars[-1])
    return {'schema_version': 'pillars-v1', 'methodology_version': METHODOLOGY_VERSION,
            'methodology_status': 'implementation_pending_team_review',
            'ticker': company.ticker, 'fiscal_year': fiscal_year, 'period': period(current),
            'currency': current.currency if current else None, 'available_years': available_years,
            'pillars': pillars, 'scoring_policy': policy(),
            'overall_score': unavailable('unconfigured_scoring')}
