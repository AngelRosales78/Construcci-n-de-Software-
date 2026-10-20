"""Validaciones de componentes monetarios anuales; los nulos se conservan."""
import re
from decimal import Decimal, InvalidOperation
from django.core.exceptions import ValidationError
from django.utils import timezone

COMPONENTS = {
    'current_assets', 'current_liabilities', 'cash', 'short_term_investments',
    'receivables', 'net_income', 'total_assets', 'book_equity', 'revenue',
    'total_liabilities', 'income_before_tax', 'interest_expense', 'inventory', 'cost_of_sales',
}
SIGNED = {'net_income', 'book_equity', 'income_before_tax'}


def validate_statement(statement):
    if not 1900 <= statement.fiscal_year <= timezone.localdate().year:
        raise ValidationError('Ejercicio fiscal fuera del rango admitido.')
    days = (statement.period_end - statement.period_start).days + 1
    if not 350 <= days <= 380 or statement.period_end.year != statement.fiscal_year:
        raise ValidationError('Se requiere un ejercicio anual completo identificado por el año de cierre.')
    if statement.period_end > timezone.localdate() or statement.filed > timezone.localdate():
        raise ValidationError('No se admiten fechas futuras.')
    if statement.filed < statement.period_end:
        raise ValidationError('La publicación no puede preceder al cierre.')
    if statement.currency != 'USD':
        raise ValidationError('Esta importación SEC admite solo componentes en USD.')
    if not statement.source_url.startswith('https://'):
        raise ValidationError('La procedencia requiere una URL HTTPS.')
    if not re.fullmatch(r'[0-9a-f]{64}', statement.snapshot_sha256):
        raise ValidationError('Falta un hash SHA256 válido del snapshot.')
    if not re.fullmatch(r'\d{10}-\d{2}-\d{6}', statement.accession):
        raise ValidationError('Identificador SEC de publicación inválido.')
    if not isinstance(statement.components, dict) or set(statement.components) - COMPONENTS:
        raise ValidationError('Componentes financieros no admitidos.')
    for name, component in statement.components.items():
        if not isinstance(component, dict) or not isinstance(component.get('tag'), str) or not component['tag'].strip():
            raise ValidationError(f'Falta la etiqueta de procedencia de {name}.')
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
