"""Contrato comparison-v1 compartido con el cliente Android de Rosales."""
import re
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
from django.utils import timezone
from rest_framework.exceptions import PermissionDenied, ValidationError
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from .access_views import subscription
from .pillar_views import CompanyPillarsView

PILLARS = ('liquidity', 'profitability', 'solvency', 'efficiency', 'growth')


def comparison_rows(a, b):
    periods_match = (a['period'].get('start') is not None and a['period'].get('end') is not None
                     and a['period'] == b['period'])
    methods_match = (a['methodology_version'] == b['methodology_version']
                     and a['scoring_policy']['version'] == b['scoring_policy']['version'])
    rows = []
    for ident in PILLARS:
        left = next(p for p in a['pillars'] if p['id'] == ident)['score']
        right = next(p for p in b['pillars'] if p['id'] == ident)['score']
        reason = None
        if not periods_match:
            reason = 'Los períodos completos deben coincidir; el mismo año fiscal no garantiza las mismas fechas.'
        elif not methods_match or left.get('version') != right.get('version'):
            reason = 'Las metodologías y versiones de puntuación deben coincidir.'
        elif (left.get('period') != a['period'] or right.get('period') != b['period']):
            reason = 'Las notas deben corresponder al período declarado.'
        elif left.get('status') != 'available' or right.get('status') != 'available':
            reason = 'Faltan indicadores requeridos para una de las dos notas.'
        values = []
        if reason is None:
            try:
                values = [Decimal(str(note['value'])) for note in (left, right)]
                if any(not v.is_finite() or not 0 <= v <= 100 for v in values):
                    raise ValueError('Nota fuera de rango')
            except (InvalidOperation, TypeError, ValueError, KeyError):
                reason = 'Una de las notas no es válida.'
        row = {'id': ident, 'unit': 'points'}
        if reason:
            row.update(status='not_comparable', result='not_comparable', delta=None, reason=reason)
        else:
            # La decisión usa la misma precisión de dos decimales que ve el usuario.
            left_value, right_value = [v.quantize(Decimal('.01'), rounding=ROUND_HALF_UP) for v in values]
            delta = left_value - right_value
            row.update(status='available', result='a' if delta > 0 else 'b' if delta < 0 else 'tie',
                       delta=format(delta, '.2f'), reason=None)
        rows.append(row)
    return rows


class ComparisonView(APIView):
    permission_classes = (IsAuthenticated,)

    def get(self, request):
        if 'compare' not in subscription(request.user)['features']:
            raise PermissionDenied('El comparador requiere un plan Pro activo. No se incluyen pagos en este sprint.')
        symbols = []
        for key in ('a', 'b'):
            ticker = request.query_params.get(key, '').strip().upper()
            if not re.fullmatch(r'[A-Z0-9.\-]{1,10}', ticker):
                raise ValidationError({key: 'Seleccione una empresa válida del catálogo.'})
            symbols.append(ticker)
        if symbols[0] == symbols[1]:
            raise ValidationError({'b': 'Seleccione dos empresas distintas.'})
        year = request.query_params.get('fiscal_year', '')
        if not re.fullmatch(r'[0-9]{4}', year) or not 1900 <= int(year) <= timezone.localdate().year:
            raise ValidationError({'fiscal_year': 'Indique un año de cierre válido, sin fechas futuras.'})
        a, b = [CompanyPillarsView().for_year(ticker, year).data for ticker in symbols]
        rows = comparison_rows(a, b)
        available = sum(row['status'] == 'available' for row in rows)
        response = Response({'schema_version': 'comparison-v1', 'fiscal_year': int(year),
                             'a': a, 'b': b, 'comparisons': rows,
                             'comparison_policy': {'version': 'matching-periods-v1',
                                 'rounding': 'Notas HALF_UP a dos decimales; diferencia A menos B.',
                                 'meaning': 'Mayor nota según reglas académicas; no es una recomendación de inversión.'},
                             'status': 'available' if available == 5 else 'partial' if available else 'not_comparable',
                             'reason': None if available == 5 else 'Revise las fechas y los indicadores disponibles de cada pilar.'})
        response['Cache-Control'] = 'private, no-store'
        return response
