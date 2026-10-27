"""Adaptador opcional a Ollama local. Las cifras proceden del snapshot, no del modelo."""
import json
import logging
import hashlib
from django.core.cache import cache
import os
import re
from decouple import config
from urllib.request import Request, urlopen

logger = logging.getLogger(__name__)

DEFINITIONS = {
    'liquidity': 'capacidad de atender obligaciones a corto plazo con activos corrientes y activos líquidos',
    'profitability': 'beneficios respecto al patrimonio, los activos y los ingresos',
    'solvency': 'pasivos respecto al patrimonio y capacidad de cubrir gastos por intereses',
    'efficiency': 'ventas generadas con los activos y tiempo de permanencia del inventario',
    'growth': 'cambio de los ingresos anuales y su evolución compuesta entre ejercicios comparables',
}

IDS = {
    'liquidity': {'current_ratio', 'quick_ratio'},
    'profitability': {'roe', 'roa', 'net_margin'},
    'solvency': {'liabilities_to_equity', 'interest_coverage'},
    'efficiency': {'asset_turnover', 'inventory_days'},
    'growth': {'revenue_growth', 'revenue_cagr_3y'},
}

def optional_local_ai(report):
    model = config('LOCAL_AI_MODEL', default='').strip()
    if not model:
        return report
    # Only validated facts and public company identifiers leave this server. No user/session data.
    context = {'ticker': report['ticker'], 'level': report['level'],
               'sections': [{'pilar': section['pillar_id'], 'definicion': DEFINITIONS[section['pillar_id']], 'nota_academica': section.get('academic_signal'), 'disponibilidad': list(section.get('availability', {}).values())} for section in report['pillars']]}
    cache_key = 'local-ai-v4:' + hashlib.sha256(json.dumps([model, context, report['fiscal_year'], report.get('snapshot_sha256'), report.get('metrics_version')], sort_keys=True).encode()).hexdigest()
    labels = {'liquidity':'liquidez','profitability':'rentabilidad','solvency':'solvencia','efficiency':'eficiencia','growth':'crecimiento'}
    schema = {'type':'object','properties':{'explanation':{'type':'string'}},'required':['explanation'],'additionalProperties':False}
    url = config('LOCAL_AI_URL', default='http://127.0.0.1:11434/api/chat')
    try:
        explanations = cache.get(cache_key)
        if explanations is None:
            explanations = {}
            for section in report['pillars']:
                ident = section['pillar_id']
                signal = section.get('academic_signal') or 'sin nota'
                canonical = f"El pilar de {labels[ident]} examina {DEFINITIONS[ident]}. Se interpreta con los indicadores disponibles del ejercicio seleccionado."
                prompt = (f"Tema: {labels[ident]}. Reescribe el texto siguiente en español claro en una o dos oraciones completas. "
                          f"Conserva literalmente la palabra {labels[ident]}. No añadas conceptos, notas, resultados ni conclusiones sobre la empresa. "
                          "No uses cifras ni menciones otros pilares. No confundas ausencia de nota con un resultado negativo. "
                          "Devuelve JSON con una sola clave explanation. Texto: " + canonical)
                body = json.dumps({'model':model,'stream':False,'format':schema,
                    'options':{'temperature':0,'num_predict':180,'num_ctx':2048},
                    'messages':[{'role':'system','content':'Eres un editor en español. Parafrasea fielmente sin agregar información.'}, {'role':'user','content':prompt}]}).encode()
                with urlopen(Request(url, data=body, headers={'Content-Type':'application/json'}),timeout=120) as response:
                    result = json.loads(response.read(100_000))
                if result.get('done') is not True or result.get('done_reason') == 'length':
                    raise ValueError('Incomplete response')
                value = json.loads(result['message']['content'])['explanation']
                if not isinstance(value,str) or labels[ident] not in value.lower() or not value.rstrip().endswith(('.', '!', '?')):
                    raise ValueError('Topic or signal changed')
                if any(other in value.lower() for key,other in labels.items() if key != ident):
                    raise ValueError('Mixed pillars')
                explanations[ident] = value
        if set(explanations) != set(IDS):
            raise ValueError('Missing pillar')
        for value in explanations.values():
            if not isinstance(value, str) or not 40 <= len(value) <= 900 or re.search(r'\d|https?://', value):
                raise ValueError('Unverified numerical claim or invalid format')
        cache.set(cache_key, explanations, timeout=1800)
        report['pillars'] = [section | {'text': explanations[section['pillar_id']] + '\n\nIndicadores verificados: ' + section['text']}
                             for section in report['pillars']]
        report['generation_mode'] = 'ai'
        report['provider'] = 'ollama-local:' + model
        report['generation_notice'] = ('Explicaciones conceptuales generadas con IA local. Los indicadores numéricos se adjuntan desde el servidor. '
                                       'La comprobación automática de estructura y cifras no certifica la interpretación; requiere revisión humana.')
        report['validation']['scope'] = 'estructura, referencias y cifras del bloque de indicadores; no interpretación semántica'
    except (OSError, ValueError, KeyError, TypeError) as exc:
        logger.warning('IA local no disponible o respuesta rechazada: %s', str(exc) if isinstance(exc, ValueError) else type(exc).__name__)
        report['generation_notice'] = ('No se obtuvo una respuesta de IA local compatible. Se muestra el informe basado en reglas, sin IA.')
    return report
