"""Proveedor local llama.cpp. No hay respuesta financiera simulada de respaldo."""
import json
import urllib.request

MODEL = 'sp500-local-qwen3'
MODEL_SHA256 = '061b54daade076b5d3362dac252678d17da8c68f07560be70818cace6590cb1a'


def generate(context, level):
    concepts = {'liquidity': 'Capacidad de cubrir obligaciones de corto plazo.',
        'profitability': 'Generación de beneficios respecto a patrimonio, activos e ingresos.',
        'solvency': 'Pasivos respecto a patrimonio y capacidad del EBIT de cubrir intereses.',
        'efficiency': 'Uso de activos e inventario para generar ventas.',
        'growth': 'Evolución histórica de ingresos entre ejercicios comparables.'}
    available = {key: metric for key, metric in context['metrics'].items() if metric['status'] == 'available'}
    references = {'summary': list(available)[:3]}
    references.update({key: [m for m in available if available[m]['pillar'] == key][:3] for key in context['pillars']})
    references['strength'] = [key for key, metric in available.items() if metric['band'] == 'high'][:1]
    references['risk'] = [key for key, metric in available.items() if metric['band'] in ('low', 'middle')][:1]
    questions = {}
    for key, ids in references.items():
        if not ids:
            continue
        facts = '; '.join(f"{available[m]['label']}: {available[m]['display_value']} ({available[m]['band'] or 'sin clasificación'})" for m in ids)
        purpose = concepts.get(key, {'summary': 'Resume el significado conjunto de estos indicadores.',
            'strength': 'Explica una fortaleza en el tramo favorable de esta métrica.',
            'risk': 'Explica una precaución ante el tramo intermedio o de atención de esta métrica.'}.get(key, ''))
        if key in ('strength', 'risk'):
            purpose += ' Concepto que debes explicar: ' + concepts[available[ids[0]]['pillar']]
        questions[key] = purpose + ' Datos históricos: ' + facts
    instruction = ('Responde en español una oración completa por pregunta, sin listas internas ni campos adicionales. '
        'Cada respuesta debe explicar, no repetir una etiqueta. Usa entre ocho y veinte palabras. '
        'No inventes cifras; si incluyes una, cópiala exactamente de los datos de esa pregunta. '
        'No hagas pronósticos ni recomendaciones de compra. No agregues fechas ni fuentes nuevas. '
        'Los tramos son convenciones académicas, no garantías. No confundas liquidez con generación de beneficios. No rellenes con espacios. '
        + ('Usa palabras sencillas.' if level == 'simple' else 'Usa vocabulario financiero técnico y preciso.'))
    schema = {'type': 'object', 'additionalProperties': False, 'required': list(questions),
        'properties': {key: {'type': 'string', 'minLength': 40, 'maxLength': 220,
            'pattern': '^[A-Za-z0-9áéíóúÁÉÍÓÚñÑüÜ ,;:()%-]{39,209}[.]$'} for key in questions}}
    prompt = '<|im_start|>system\n' + instruction + ' /no_think<|im_end|>\n<|im_start|>user\nPREGUNTAS: ' + json.dumps(questions, ensure_ascii=False) + '<|im_end|>\n<|im_start|>assistant\n<think>\n\n</think>\n\n'
    payload = {'temperature': 0, 'seed': 2026, 'n_predict': 1200, 'prompt': prompt,
               'json_schema': schema, 'stop': ['<|im_end|>'], 'cache_prompt': True}
    # URL fijada a loopback: no admite destinos enviados por el usuario ni expone credenciales.
    request = urllib.request.Request('http://127.0.0.1:58110/completion',
        data=json.dumps(payload).encode(), headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=150) as response:
        raw = response.read(100_001)
    if len(raw) > 100_000:
        raise ValueError('provider_response_too_large')
    content = json.loads(raw)['content']
    texts = json.loads(content)
    # Las referencias las fija el servidor según la pregunta; nunca las inventa el modelo.
    def claim(key):
        return {'text': texts[key], 'metric_ids': references[key]} if references[key] else {
            'text': 'No disponible por falta de métricas verificables para este pilar.', 'metric_ids': []}
    return {'summary': claim('summary'), 'pillars': {key: claim(key) for key in context['pillars']},
            'strengths': [claim('strength')] if references['strength'] else [],
            'risks': [claim('risk')] if references['risk'] else []}
