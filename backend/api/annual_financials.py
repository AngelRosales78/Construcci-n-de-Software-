"""Validaciones de componentes monetarios anuales; los nulos se conservan."""
import re
from decimal import Decimal, InvalidOperation
from django.core.exceptions import ValidationError
from django.utils import timezone
from django.core.validators import URLValidator
from datetime import date

COMPONENTS = {
    'current_assets', 'current_liabilities', 'cash', 'short_term_investments',
    'receivables', 'net_income', 'total_assets', 'book_equity', 'revenue',
    'total_liabilities', 'income_before_tax', 'interest_expense', 'inventory', 'cost_of_sales',
}
SIGNED = {'net_income', 'book_equity', 'income_before_tax'}


def validate_statement(statement):
    if statement.inventory_applicable is False and not statement.inventory_applicability_note.strip():
        raise ValidationError('Indique el motivo documentado por el que el inventario no aplica.')
    if not 1900 <= statement.fiscal_year <= timezone.localdate().year:
        raise ValidationError('Ejercicio fiscal fuera del rango admitido.')
    if statement.period_end.year != statement.fiscal_year:
        raise ValidationError('El año debe coincidir con la referencia anual.')
    if statement.period_end > timezone.localdate() or (statement.filed and statement.filed > timezone.localdate()):
        raise ValidationError('No se admiten fechas futuras.')
    if statement.filed and statement.filed < statement.period_end:
        raise ValidationError('La publicación no puede preceder al cierre.')
    if statement.currency != 'USD':
        raise ValidationError('Esta importación admite solo componentes en USD.')
    if not statement.source_url.startswith('https://'):
        raise ValidationError('La procedencia requiere una URL HTTPS.')
    if not re.fullmatch(r'[0-9a-f]{64}', statement.snapshot_sha256):
        raise ValidationError('Falta un hash SHA256 válido del snapshot.')
    if statement.source_kind == 'sec':
        if not statement.period_start or not statement.filed:
            raise ValidationError('El informe SEC requiere período completo y fecha de publicación.')
        days = (statement.period_end - statement.period_start).days + 1
        if not 350 <= days <= 380:
            raise ValidationError('Se requiere un ejercicio anual completo identificado por el año de cierre.')
        if not re.fullmatch(r'\d{10}-\d{2}-\d{6}', statement.accession):
            raise ValidationError('Identificador SEC de publicación inválido.')
    elif statement.source_kind == 'yahoo':
        if statement.period_start is not None or statement.filed is not None or statement.accession:
            raise ValidationError('No atribuya fechas contables o identificadores SEC a una referencia normalizada.')
        evidence = statement.provenance
        if (not isinstance(evidence, dict) or evidence.get('period_type') != '12M'
                or evidence.get('period_basis') != 'provider_normalized'
                or evidence.get('as_of') != statement.period_end.isoformat()
                or not re.fullmatch(r'https://finance\.yahoo\.com/quote/[A-Z0-9.\-]+/financials/', statement.source_url)):
            raise ValidationError('Falta la procedencia de la referencia anual del proveedor.')
        try:
            from datetime import datetime
            from urllib.parse import urlsplit
            retrieved = datetime.fromisoformat(evidence['retrieved_at'])
            request = urlsplit(evidence['request_url'])
            if (retrieved.tzinfo is None or retrieved > timezone.now() or request.scheme != 'https'
                    or request.netloc != 'query1.finance.yahoo.com'
                    or not request.path.startswith('/ws/fundamentals-timeseries/v1/finance/timeseries/')):
                raise ValueError('Procedencia inválida.')
        except (ValueError, TypeError, KeyError) as error:
            raise ValidationError('Consulta y fecha de captura del proveedor no válidas.') from error
    else:
        raise ValidationError('Origen de estado financiero no admitido.')
    if not isinstance(statement.components, dict) or set(statement.components) - COMPONENTS:
        raise ValidationError('Componentes financieros no admitidos.')
    for name, component in statement.components.items():
        if not isinstance(component, dict) or not isinstance(component.get('tag'), str) or not component['tag'].strip():
            raise ValidationError(f'Falta la etiqueta de procedencia de {name}.')
        if 'source' in component:
            evidence = component['source']
            if not isinstance(evidence, dict) or set(evidence) != {'url', 'accession', 'filed', 'evidence_sha256', 'locator'}:
                raise ValidationError('La procedencia complementaria está incompleta.')
            try:
                URLValidator(schemes=['https'])(evidence['url'])
                published = date.fromisoformat(evidence['filed'])
                if (not statement.period_end <= published <= timezone.localdate()
                        or not re.fullmatch(r'\d{10}-\d{2}-\d{6}', evidence['accession'])
                        or not re.fullmatch(r'[0-9a-f]{64}', evidence['evidence_sha256'])
                        or not isinstance(evidence['locator'], str) or not evidence['locator'].strip()):
                    raise ValueError('Procedencia complementaria inválida.')
            except (ValueError, TypeError) as error:
                raise ValidationError('La procedencia complementaria no es válida.') from error
        raw = component.get('value')
        if raw is None:
            continue
        try:
            if isinstance(raw, bool):
                raise ValueError('Booleano no monetario')
            value = Decimal(str(raw))
            if not value.is_finite() or abs(value) >= Decimal('1e25'):
                raise ValueError('Importe no finito o fuera de rango')
            if name not in SIGNED and value < 0:
                raise ValueError('Importe negativo no admitido')
        except (InvalidOperation, ValueError, TypeError) as exc:
            raise ValidationError(f'Componente inválido: {name}.') from exc
