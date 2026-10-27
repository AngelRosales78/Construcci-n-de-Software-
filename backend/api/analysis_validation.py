"""HU07_04 T45: contexto acotado y validación independiente del texto generado."""
from decimal import Decimal, InvalidOperation
import re

VERSION = 'grounded-analysis-v2'


def context_from_pillars(data):
    metrics = {}
    for pillar in data['pillars']:
        for metric in pillar['metrics']:
            metrics[metric['id']] = {'label': metric['label'], 'status': metric['status'],
                'value': metric.get('value'), 'display_value': metric.get('display_value'),
                'unit': metric['unit'], 'pillar': pillar['id'],
                'band': metric.get('thresholds', {}).get('band') if metric.get('thresholds') else None}
    return {'ticker': data['ticker'], 'fiscal_year': data['fiscal_year'], 'period': data['period'],
            'metrics': metrics, 'methodology_version': data['methodology_version'],
            'pillars': {pillar['id']: pillar['label'] for pillar in data['pillars']}}


def schema_for(context):
    def claim(ids, prefix=''):
        return {'type': 'object', 'additionalProperties': False, 'required': ['text', 'metric_ids'],
                'properties': {'text': {'type': 'string', 'minLength': 40, 'maxLength': 180,
                    'pattern': '^' + prefix + '[A-Za-záéíóúÁÉÍÓÚñÑüÜ ,.;:()%-]{40,150}$'},
                    'metric_ids': {'type': 'array', 'items': {'type': 'string', 'enum': ids or ['unavailable']},
                                   'minItems': 1 if ids else 0, 'maxItems': min(len(ids), 3)}}}
    available = [key for key, metric in context['metrics'].items() if metric['status'] == 'available']
    pillars = {key: claim([m for m in available if context['metrics'][m]['pillar'] == key]) for key in context['pillars']}
    positive = [key for key in available if context['metrics'][key]['band'] == 'high']
    risks = [key for key in available if context['metrics'][key]['band'] in ('low', 'middle')]
    return {'type': 'object', 'additionalProperties': False, 'required': ['summary', 'pillars', 'strengths', 'risks'],
            'properties': {'summary': claim(available),
                'pillars': {'type': 'object', 'additionalProperties': False, 'required': list(pillars), 'properties': pillars},
                'strengths': {'type': 'array', 'maxItems': 1 if positive else 0, 'items': claim(positive, 'Fortaleza: ')},
                'risks': {'type': 'array', 'maxItems': 1 if risks else 0, 'items': claim(risks, 'Atención: ')}}}


def validate_analysis(candidate, context):
    if not isinstance(candidate, dict) or set(candidate) != {'summary', 'pillars', 'strengths', 'risks'}:
        raise ValueError('invalid_structure')
    if not isinstance(candidate['pillars'], dict) or set(candidate['pillars']) != set(context['pillars']):
        raise ValueError('missing_pillars')
    for kind in ('strengths', 'risks'):
        if not isinstance(candidate[kind], list) or len(candidate[kind]) > 5:
            raise ValueError('invalid_list')
    argument_texts = [claim.get('text', '').strip().casefold() for kind in ('strengths', 'risks')
                      for claim in candidate[kind] if isinstance(claim, dict) and isinstance(claim.get('text'), str)]
    if len(argument_texts) != len(set(argument_texts)):
        raise ValueError('invalid_list')
    for kind, bands in (('strengths', {'high'}), ('risks', {'low', 'middle'})):
        for claim in candidate[kind]:
            if isinstance(claim, dict) and isinstance(claim.get('metric_ids'), list):
                if any(not isinstance(key, str) or context['metrics'].get(key, {}).get('band') not in bands
                       for key in claim['metric_ids']):
                    raise ValueError('unsupported_claim')
    claims = [(candidate['summary'], None)]
    claims += [(claim, key) for key, claim in candidate['pillars'].items()]
    claims += [(claim, None) for kind in ('strengths', 'risks') for claim in candidate[kind]]
    for claim, pillar in claims:
        if not isinstance(claim, dict) or set(claim) != {'text', 'metric_ids'}:
            raise ValueError('invalid_claim')
        text, ids = claim['text'], claim['metric_ids']
        if not isinstance(text, str) or not 10 <= len(text) <= 650 or '<' in text or '>' in text:
            raise ValueError('invalid_text')
        if not isinstance(ids, list) or any(not isinstance(key, str) for key in ids) or len(set(ids)) != len(ids):
            raise ValueError('invalid_references')
        available = {key: metric for key, metric in context['metrics'].items()
                     if metric['status'] == 'available' and (pillar is None or metric['pillar'] == pillar)}
        if (available and not ids) or any(key not in available for key in ids):
            raise ValueError('unsupported_claim')
        if not available and not re.search(r'no disponible|sin datos|faltan|insuficient', text, re.I):
            raise ValueError('unsupported_claim')
        allowed = {Decimal(str(context['fiscal_year']))}
        for key in ids:
            raw = Decimal(str(available[key]['value']))
            allowed.update((raw, raw.quantize(Decimal('.01'))))
        for number in re.findall(r'(?<![a-zA-Z_])[-+]?\d+(?:[.,]\d+)?', text):
            try:
                if Decimal(number.replace(',', '.')) not in allowed:
                    raise ValueError('unsupported_number')
            except InvalidOperation as exc:
                raise ValueError('invalid_number') from exc
    return candidate
