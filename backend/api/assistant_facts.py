"""Evidence cards compiled from existing calculations; the model never supplies numbers."""
from decimal import Decimal
from urllib.parse import urlparse
from .pillar_views import CompanyPillarsView
from .comparison_views import comparison_rows
from .pillar_scoring import display

PILLARS = {'liquidity': 'Liquidez', 'profitability': 'Rentabilidad', 'solvency': 'Deudas y solvencia',
           'efficiency': 'Uso de recursos', 'growth': 'Crecimiento de ingresos'}
UNITS = {'times': 'veces', 'percent': '%', 'days': 'días'}
LIMIT = 'Describe datos históricos; no anticipa ganancias ni constituye una recomendación de compra.'
INTERPRETATIONS = {
    'market_cap':('Es el valor conjunto de las acciones en bolsa publicado en la ficha: {v}.','No es el efectivo de la empresa ni el importe que pagarías por una sola acción.'),
    'per':('El precio equivale a {v} veces el beneficio por acción usado por el proveedor.','Por sí solo no determina si una acción está cara o barata. Su fecha y la metodología del proveedor pueden diferir del balance anual seleccionado.'),
    'beta':('La beta publicada en la ficha es {v}: resume cómo se movió la acción frente al mercado en la referencia del proveedor.','No es una probabilidad ni una predicción del próximo movimiento; depende del periodo y del índice de referencia.'),
    'dividend_yield':('El rendimiento por dividendo publicado es {v} %: relaciona dividendos registrados con el precio usado en esa referencia.','No es una ganancia garantizada; las empresas pueden modificar o suspender dividendos.'),
    'overview_roe':('El ROE publicado en la ficha es {v} %. Relaciona el resultado con el patrimonio según la referencia del proveedor.','No es el retorno de quien compra la acción. Conserva su fecha propia y no sustituye al ROE calculado con los balances del ejercicio seleccionado.'),
    'current_ratio': ('Por cada unidad de obligaciones de corto plazo, hay {v} unidades de recursos de corto plazo.', 'Estos recursos incluyen partidas que no son efectivo; el ratio no garantiza que se cobre o pague a tiempo.'),
    'quick_ratio': ('Compara efectivo, inversiones temporales y cuentas por cobrar con las obligaciones de corto plazo: {v} veces.', 'Excluye el inventario, pero las cuentas por cobrar tampoco son efectivo disponible de inmediato.'),
    'roe': ('El resultado neto equivale al {v} % del patrimonio contable promedio de los propietarios.', 'Es rentabilidad contable de la empresa, no la rentabilidad que recibiría quien compra la acción.'),
    'roa': ('El resultado neto equivale al {v} % de los activos promedio utilizados por la empresa.', 'Depende de la estructura del negocio y de sus activos; no permite comparar cualquier sector sin contexto.'),
    'net_margin': ('Por cada cien unidades de ingresos, el resultado neto equivale a {v} unidades; un valor negativo representa pérdida.', 'El resultado puede incluir partidas extraordinarias; este dato por sí solo no explica sus causas.'),
    'liabilities_to_equity': ('Por cada unidad de patrimonio contable, se registran {v} unidades de obligaciones totales.', 'Incluye pasivos que no son préstamos. Requiere patrimonio positivo y no resume todos los vencimientos.'),
    'interest_coverage': ('El resultado antes de intereses e impuestos usado en la fórmula equivale a {v} veces el gasto por intereses.', 'El beneficio contable no equivale al efectivo disponible para pagar; revisa los componentes de la fórmula.'),
    'asset_turnover': ('Los ingresos equivalen a {v} veces el valor de los activos promedio utilizados.', 'No es un margen de beneficio; sectores con estructuras distintas pueden tener ratios muy diferentes.'),
    'inventory_days': ('La fórmula estima {v} días de inventario a partir del inventario promedio y el costo de ventas.', 'Es una aproximación contable, no el tiempo observado de cada producto. Menos días no siempre significa una mejor operación.'),
    'revenue_growth': ('Los ingresos variaron {v} % respecto al ejercicio anterior comparable.', 'Mide ventas, no beneficio ni retorno de la acción. No explica por sí solo por qué cambiaron los ingresos.'),
    'revenue_cagr_3y': ('El cambio de ingresos equivale a una tasa anual compuesta de {v} % en el intervalo de tres años.', 'Resume los extremos del intervalo; no significa que cada año creciera a esa tasa ni predice el siguiente.'),
}


def source_link(url, label=None, period=None):
    try:
        parsed = urlparse(url)
        if parsed.scheme not in ('http', 'https') or not parsed.netloc:
            return None
        return {'url': url, 'label': label or parsed.hostname, 'period': period}
    except (TypeError, ValueError):
        return None


def sources_for(metric):
    result = []
    for component in metric.get('components', []):
        source = component.get('source', {})
        item = source_link(source.get('url'), 'Yahoo Finance' if source.get('source_kind') == 'yahoo' else 'Fuente del balance', component.get('period', {}).get('end'))
        if item and item not in result:
            result.append(item)
    if metric.get('applicability_source'):
        item=source_link(metric['applicability_source'].get('url'),'Procedencia de la clasificación',metric.get('period',{}).get('end'))
        if item and item not in result:
            result.append(item)
    return result


def metric_facts(pillars, prefix=''):
    facts = []
    for pillar in pillars['pillars']:
        for metric in pillar['metrics']:
            ready = metric['status'] == 'available'
            band = (metric.get('thresholds') or {}).get('band')
            kind = 'strength' if band == 'high' else 'attention' if band in ('low', 'middle') else 'neutral'
            definition, limit = INTERPRETATIONS[metric['id']]
            meaning = definition.format(v=metric.get('display_value')) if ready else metric.get('explanation', 'Faltan componentes para calcular este indicador.')
            if ready and band:
                meaning += ' Tramo ' + {'high': 'favorable', 'middle': 'intermedio', 'low': 'de atención'}[band] + ' según la regla académica del proyecto, sin referencia sectorial.'
            if metric.get('period_basis') == 'provider_normalized':
                limit += ' La fecha es una referencia anual normalizada del proveedor; no acredita el cierre contable exacto.'
            if any(c.get('source', {}).get('is_demo') for c in metric.get('components', [])):
                limit += ' La procedencia de estos componentes está pendiente de verificar.'
            facts.append({'id': prefix + metric['id'], 'topic': metric['id'], 'pillar': pillar['id'], 'title': metric['label'],
                'value': f"{metric['display_value']} {UNITS[metric['unit']]}" if ready else 'No aplica' if metric['status'] == 'not_applicable' else 'No disponible',
                'meaning': meaning, 'limit': limit, 'kind': kind if ready else 'neutral' if metric['status']=='not_applicable' else 'missing', 'status': metric['status'],
                'period': metric['period'], 'sources': sources_for(metric)})
    return facts


def company_pack(company, year):
    from .views import company_financial_data
    data = CompanyPillarsView().for_year(company.ticker, year).data
    facts = metric_facts(data)
    overview = company_financial_data(company)
    summary=getattr(company,'financial_metrics',None) or company
    for topic,field,label,unit in [('market_cap','market_cap','Valor en bolsa','USD'),('per','per','Precio / beneficio · PER','veces'),
                                  ('beta','beta','Movimiento frente al mercado · Beta',''),('dividend_yield','dividend_yield','Rendimiento por dividendo','%'),
                                  ('overview_roe','roe','ROE publicado en la ficha','%')]:
        raw=getattr(summary,field,None)
        available=raw is not None and Decimal(str(raw)).is_finite()
        amount=Decimal(str(raw)) if available else None
        rendered=(format(amount.normalize(),'f').replace('.',',') if amount and abs(amount)<Decimal('.01') else display(amount)) if available else None
        if available and field=='market_cap':
            value=Decimal(str(raw))
            divisor,suffix=next(((d,s) for d,s in [(Decimal('1e12'),'billones'),(Decimal('1e9'),'mil millones'),(Decimal('1e6'),'millones')] if abs(value)>=d),(Decimal(1),''))
            rendered=f"USD {display(value/divisor)} {suffix}".strip()
        source=overview.get('field_sources',{}).get(field,{})
        date=source.get('as_of')
        definition,limit=INTERPRETATIONS[topic]
        link=source_link(source.get('source'),'Fuente de la ficha',date)
        diagnostic=overview.get('indicator_availability',{}).get(field,{})
        facts.append({'id':topic,'topic':topic,'title':label,'status':'available' if available else 'unavailable','kind':'neutral' if available else 'missing',
            'value':f"{rendered}{' '+unit if unit and field!='market_cap' else ''}" if available else diagnostic.get('label','No disponible'),
            'meaning':definition.format(v=rendered) if available else diagnostic.get('explanation','No hay un valor publicado disponible para esta cifra.'),
            'limit':limit+' Referencia independiente del ejercicio anual seleccionado.'+(' Fecha de referencia no informada.' if not date else '')+(' Procedencia pendiente de verificar.' if overview.get('is_demo') else ''),
            'period':{'start':None,'end':date},'sources':[link] if link else []})
    altman = overview['altman_z']
    reference = altman.get('reference', {})
    closing = reference.get('period_end') or str(overview.get('as_of') or '') or None
    ready = altman.get('score') is not None
    zone = {'safe': 'favorable', 'grey': 'intermedia', 'distress': 'de atención'}.get(altman.get('zone'))
    altman_sources = [source_link(reference.get(key)) for key in ('report_url', 'price_url')]
    if reference.get('sec_evidence'):
        altman_sources.append(source_link(reference['sec_evidence'].get('facts_url'), 'Hechos XBRL de la SEC'))
    if not ready:
        altman_sources.append(source_link(altman.get('availability', {}).get('source_url'), 'Referencia para completar Altman-Z'))
    facts.append({'id':'altman', 'topic':'altman', 'title':'Puntaje Altman-Z',
        'value':str(altman['score']) if ready else 'No disponible', 'kind':'neutral' if ready else 'missing', 'status':'available' if ready else 'unavailable',
        'meaning': f'El puntaje queda en la zona {zone} de la fórmula original.' if ready else altman.get('availability', {}).get('explanation', 'Faltan componentes y su valoración compatible al cierre.'),
        'limit':'No es un porcentaje ni una nota sobre diez o cien. Es una clasificación académica, no una garantía de solvencia. Corresponde al cierre indicado en esta tarjeta, que puede ser distinto del año seleccionado.',
        'period':{'start':reference.get('period_start'), 'end':closing}, 'sources':[s for s in altman_sources if s]})
    score = data['overall_score']
    facts.append({'id':'scores', 'topic':'scores', 'title':'Nota de los cinco pilares', 'status':score['status'],
        'value':f"{score['display_value']} / 100" if score['status']=='available' else 'No disponible', 'kind':'neutral' if score['status']=='available' else 'missing',
        'meaning':'Promedio de las cinco notas académicas con el mismo peso. Cada nota depende de sus indicadores aplicables.' if score['status']=='available' else score['explanation'],
        'limit':'Es distinta de Altman-Z. No se promedian solamente los pilares disponibles ni se interpreta como probabilidad de ganancia. ' + data['scoring_policy']['explanation'],
        'period':data['period'], 'sources':unique_sources(facts)})
    return {'scope':'company', 'tickers':[company.ticker], 'company_name':company.name, 'description':company.description,
        'fiscal_year':data['fiscal_year'], 'available_years':data['available_years'], 'period':data['period'],
        'period_note':data.get('period_note'), 'methodology_version':data['methodology_version'], 'facts':facts}


def unique_sources(facts):
    result = []
    for fact in facts:
        for item in fact['sources']:
            if item not in result:
                result.append(item)
    return result


def overview_cards(pack):
    facts = pack['facts']
    if pack['scope']=='comparison':
        count=sum(f['status']=='available' for f in facts)
        return [{'id':'differences','title':'Qué se puede comparar','facts':[f for f in facts if f['status']=='available'][:1],
                 'message':f'{count} de {len(facts)} pilares tienen periodos y notas compatibles.' if count else 'No hay pilares con periodos y notas compatibles.'},
                {'id':'missing','title':'Qué impide comparar','facts':[f for f in facts if f['status']!='available'][:1],
                 'message':None if any(f['status']!='available' for f in facts) else 'Los cinco pilares tienen notas y periodos compatibles.'}]
    groups = [('strength','Qué destaca','No hay indicadores en tramo favorable respaldados por las reglas de clasificación disponibles.'),
              ('attention','Qué merece atención','No hay señales de atención clasificadas con los datos disponibles; esto no demuestra ausencia de riesgo.'),
              ('missing','Qué falta conocer','No faltan indicadores en esta lectura; sigue siendo necesario revisar el contexto y las limitaciones.')]
    return [{'id':kind,'title':title,'facts':[f for f in facts if f['kind']==kind][:1],
             'message':message if not any(f['kind']==kind for f in facts) else None} for kind,title,message in groups]


def comparison_pack(a, b, year):
    left, right = [CompanyPillarsView().for_year(t,year).data for t in (a,b)]
    rows = comparison_rows(left,right)
    facts=[]
    for row in rows:
        x,y=[next(p for p in data['pillars'] if p['id']==row['id']) for data in (left,right)]
        ready=row['status']=='available'
        meaning=(f"La diferencia de notas {a} menos {b} es {display(Decimal(row['delta']))} puntos. " +
                 ('Las notas coinciden a la precisión presentada.' if row['result']=='tie' else f"{a if row['result']=='a' else b} tiene la nota académica mayor en este pilar.")) if ready else row['reason']
        sources=[]
        for ticker,pillar in [(a,x),(b,y)]:
            sources.extend({**item,'label':f"{ticker} · {item['label']}"} for item in unique_sources(metric_facts({'pillars':[pillar]})))
        facts.append({'id':row['id'],'topic':row['id'],'title':PILLARS[row['id']], 'kind':'neutral' if ready else 'missing',
            'status':'available' if ready else 'not_comparable',
            'value':f"{a}: {x['score']['display_value']} / 100 · {b}: {y['score']['display_value']} / 100" if ready else 'No comparable',
            'comparison_values':[{'ticker':a,'value':f"{x['score']['display_value']} / 100"},{'ticker':b,'value':f"{y['score']['display_value']} / 100"}] if ready else [],
            'meaning':meaning, 'limit':'Una nota mayor no determina qué empresa conviene comprar. Solo se comparan las fechas y las metodologías compatibles.',
            'period':left['period'] if ready else {'start':None,'end':None},
            'periods':{a:left['period'],b:right['period']},
            'sources':sources})
    return {'scope':'comparison','tickers':[a,b], 'company_name':f'{a} y {b}', 'fiscal_year':year,
        'available_years':sorted(set(left['available_years']) & set(right['available_years']),reverse=True),
        'period':left['period'] if left['period']==right['period'] else {'start':None,'end':None},
        'period_note':' '.join(dict.fromkeys(p for p in [left.get('period_note'),right.get('period_note')] if p)),
        'methodology_version':left['methodology_version'], 'facts':facts}


def changes_pack(company, earlier, later):
    a,b=[CompanyPillarsView().for_year(company.ticker,y).data for y in (earlier,later)]
    statements={s.fiscal_year:s for s in company.annual_statements.filter(fiscal_year__in=[earlier,later])}
    x,y=statements.get(earlier),statements.get(later)
    reason=None
    if not x or not y:
        reason='Falta un balance para uno de los ejercicios elegidos.'
    elif x.source_kind=='yahoo' or y.source_kind=='yahoo' or not x.period_start or not y.period_start:
        reason='Las referencias normalizadas no acreditan periodos contables completos comparables. No se calcula una diferencia.'
    elif x.currency!=y.currency:
        reason='Las monedas de los balances no coinciden.'
    elif (not all(330 <= (s.period_end-s.period_start).days <= 380 for s in (x,y)) or
          abs((x.period_end-x.period_start).days-(y.period_end-y.period_start).days)>15 or
          x.period_end.month!=y.period_end.month or abs(x.period_end.day-y.period_end.day)>14):
        reason='Los periodos deben tener duración anual y cierres equivalentes; no se comparan ejercicios recortados o cambios de cierre.'
    elif a['methodology_version']!=b['methodology_version']:
        reason='Las versiones de cálculo no coinciden.'
    before={m['id']:m for p in a['pillars'] for m in p['metrics']}
    after={m['id']:m for p in b['pillars'] for m in p['metrics']}
    facts=[]
    for ident, old in before.items():
        new=after[ident]
        unavailable=reason or (None if old['status']==new['status']=='available' and old['unit']==new['unit'] and old['formula']==new['formula'] else 'Falta un indicador compatible en alguno de los ejercicios.')
        delta=Decimal(new['value'])-Decimal(old['value']) if unavailable is None else None
        unit='puntos porcentuales' if old['unit']=='percent' else UNITS[old['unit']]
        shown=('menos de 0,01' if delta and abs(delta)<Decimal('.01') else display(abs(delta))) if delta is not None else None
        facts.append({'id':ident,'topic':ident,'title':old['label'], 'kind':'neutral' if delta is not None else 'missing',
            'status':'available' if delta is not None else 'not_comparable', 'delta':str(delta) if delta is not None else None,
            'value':f"{old['display_value']} → {new['display_value']} {UNITS[old['unit']]}" if delta is not None else 'No comparable',
            'meaning':('Sin cambio a la precisión del cálculo.' if delta==0 else f"{'Aumentó' if delta>0 else 'Disminuyó'} {shown} {unit} entre {earlier} y {later}.") if delta is not None else unavailable,
            'limit':'El cambio no explica su causa. Los balances disponibles no permiten atribuirlo a decisiones, noticias o acontecimientos concretos.',
            'period':{'start':x.period_end.isoformat() if x else None,'end':y.period_end.isoformat() if y else None},
            'sources':unique_sources([{'sources':sources_for(old)},{'sources':sources_for(new)}])})
    return {'scope':'changes','tickers':[company.ticker],'company_name':company.name,'fiscal_year':later,
        'from_year':earlier,'to_year':later,'available_years':b['available_years'], 'period':{'start':a['period']['end'],'end':b['period']['end']},
        'period_note':reason,'methodology_version':b['methodology_version'],'facts':facts}


def answer_for(pack, topics):
    facts=pack['facts']
    selected=[]
    messages=[]
    for topic in topics:
        if topic in ('advice','out_of_scope','clarify'):
            messages.append({'advice':'Puedo explicar cifras y sus límites, pero no indicar qué comprar ni prometer resultados.',
                'out_of_scope':'Esta conversación usa solamente los datos de la empresa o comparación abierta. No dispone de noticias, cotizaciones en vivo ni información de otras empresas.',
                'clarify':'¿Qué quieres entender: liquidez, deudas, beneficios, crecimiento o el significado de una nota?'}[topic])
            continue
        if topic=='causes':
            messages.append('Estos datos permiten describir lo que cambió, pero no demostrar por qué. Para explicar causas haría falta documentación adicional que las respalde.')
        elif topic=='changes' and pack['scope']!='changes':
            messages.append('Para calcular cambios selecciona dos ejercicios en «Cambios entre años» dentro de la ficha. No se infieren variaciones a partir de un solo balance.')
        elif topic=='limits':
            messages.append(LIMIT)
        elif topic=='missing':
            missing=[f for f in facts if f['kind']=='missing']
            selected.extend(missing)
            if not missing:
                messages.append('No se detectan indicadores ausentes en esta lectura. Esto no significa que conozcamos toda la empresa: siguen faltando contexto y documentación para explicar causas o anticipar resultados.')
        elif topic in ('overview','comparison','changes'):
            selected.extend(f for group in ('strength','attention','neutral','missing') for f in facts if f['kind']==group)
        else:
            selected.extend(f for f in facts if f['topic']==topic or f.get('pillar')==topic)
    unique={f['id']:f for f in selected}
    if not unique and not messages:
        messages.append('No hay datos de ese tema en el contexto abierto. Puedes consultar los datos faltantes o elegir otra pregunta.')
    cards=list(unique.values())[:5]
    independent={'altman','market_cap','per','beta','dividend_yield','overview_roe'}
    return {'cards':cards, 'messages':list(dict.fromkeys(messages)), 'topics':topics,
            'period_note':pack.get('period_note') if any(card['topic'] not in independent for card in cards) else None}
