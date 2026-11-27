"""Resolve known context mismatches before asking a model to explain figures."""
import re
from decimal import Decimal, InvalidOperation
from .assistant_context import normalized


def reply(messages, topics, cards=None):
    return {'messages':messages,'topics':topics,'cards':cards or [],'examples':[],'period_note':None}


def context_reply(pack, question):
    text=normalized(question)
    facts=pack['facts']
    altman=next((f for f in facts if f['id']=='altman'),None)
    if re.search(r'\bcero\b|\b0\b',text) and re.search(r'no aparece|no hay|falta|ausen|sin dato|no disponible',text):
        return reply(['No. Un dato ausente no significa que valga cero. Significa que no tenemos una cifra verificada para ese indicador y ese periodo. No lo sustituimos por cero ni lo usamos para calcular una nota.'],['missing'])
    years=set(re.findall(r'\b(?:19|20|21)\d{2}\b',question))
    if len(years)==1 and re.search(r'liquid|altman|\bro[ea]\b|benefici|ventas|ingres|balance|pasivo|deuda|ejercicio|datos',text):
        requested=next(iter(years))
        own_altman_year=str((altman or {}).get('period',{}).get('end') or '')[:4]
        only_altman='altman' in text and not re.search(r'liquid|\bro[ea]\b|benefici|ventas|ingres|pasivo|deuda',text)
        if requested!=str(pack['fiscal_year']) and not (only_altman and requested==own_altman_year):
            message=f"Esta conversación está en el ejercicio {pack['fiscal_year']}. "
            message+=(f'Para consultar {requested}, cambia el año en la ficha.' if int(requested) in pack.get('available_years',[]) else f'No hay un balance cargado para {requested} en este contexto.')
            return reply([message+' No atribuiré las cifras actuales a otro ejercicio.'],['limits'])
    if ('sector' in text or 'competidores' in text) and re.search(r'promedio|media\b|mediana|benchmark',text):
        return reply(['Esta ficha no incluye un promedio sectorial calculado. Puedo explicar las cifras de la empresa, pero no presentarlas como si fueran las de su sector.'],['limits'])
    if pack['scope']=='comparison' and not any(f['status']=='available' for f in facts) and re.search(r'compar|gana|mejor|diferenc',text):
        reason=next((f['meaning'] for f in facts if f.get('meaning')), 'Faltan periodos y notas compatibles.')
        return reply(['No se puede determinar qué empresa tiene una nota mayor con esta selección: no hay pilares con datos comparables.',reason],['comparison'])
    if altman and 'altman' in text:
        if altman['status']!='available':
            return reply([f"Altman-Z: {altman['meaning']}",altman['limit']],['altman'],[altman])
        premise=re.search(r'(?:\bsu|\bel)\s+(?:puntaje\s+)?altman(?:[-\s]*z)?\s+(?:es(?: de)?|de(?!\s+(?:19|20|21)\d{2}\b)|vale|=)\s*(-?\d+(?:[.,]\d+)?)',question,re.I)
        if premise:
            try:
                differs=Decimal(premise.group(1).replace(',','.'))!=Decimal(altman['value'].replace(',','.'))
            except InvalidOperation:
                differs=False
            if differs:
                return reply([f"La cifra indicada en esta ficha para Altman-Z es {altman['value']}; no coincide con el puntaje mencionado en tu pregunta. {altman['meaning']}",altman['limit']],['altman'],[altman])
    return None
