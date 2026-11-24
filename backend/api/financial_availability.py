"""Explain unavailable values without treating an unknown value as zero."""
import hashlib
import json
from datetime import date, datetime, timedelta
from decimal import Decimal
from zoneinfo import ZoneInfo

INDICATORS = ('market_cap','per','roe','beta','dividend_yield')
LABELS = {'working_capital':'capital de trabajo','total_assets':'activos totales','retained_earnings':'beneficios retenidos',
          'ebit':'beneficio antes de intereses e impuestos','equity_value':'valor bursátil del cierre',
          'total_liabilities':'pasivos totales','sales':'ventas'}

def reason(code, label, explanation, next_step):
    return dict(status='unavailable',code=code,label=label,explanation=explanation,next_step=next_step)

def fingerprint(summary):
    fields = (*INDICATORS,*LABELS,'as_of','reference_basis','source_snapshot_sha256','altman_reference')
    return hashlib.sha256(json.dumps({k:getattr(summary,k,None) for k in fields},sort_keys=True,default=str).encode()).hexdigest()

def available():
    return dict(status='available',code='available',label='Disponible')

def diagnose(summary, records, chart, raw_chart, benchmark):
    result = {}
    latest = max(records,key=lambda row:row['period_end']) if records else None
    previous = next((row for row in records if latest and row['fiscal_year']==latest['fiscal_year']-1
                     and row['period_end'].month==latest['period_end'].month
                     and row['snapshot_sha256']==latest['snapshot_sha256']),None)
    for field in INDICATORS:
        if getattr(summary,field) is not None: result[field]=available(); continue
        item = reason('source_missing','No publicado en la fuente','La captura consultada no proporciona un valor válido para este indicador.','Revisar otra fuente con fecha y metodología identificadas.')
        if field=='roe':
            if not latest or not previous:
                item=reason('annual_history_missing','Falta un ejercicio comparable','El ROE necesita beneficio y patrimonio de dos referencias anuales consecutivas del mismo origen.','Completar el ejercicio anual que falta.')
            else:
                equity=[row['components']['book_equity']['value'] for row in (previous,latest)]
                if any(v is None for v in equity):
                    item=reason('equity_missing','Patrimonio no publicado','Falta el patrimonio de una de las dos referencias anuales.','Consultar el patrimonio en los informes del mismo período.')
                elif any(Decimal(v)<=0 for v in equity):
                    item=reason('nonpositive_equity','Patrimonio no positivo','El patrimonio es cero o negativo en una de las referencias. La metodología de este resumen no presenta ROE en ese caso.','Consultar el balance; completar datos no cambia esta condición.')
                elif latest['components']['net_income']['value'] is None:
                    item=reason('net_income_missing','Beneficio no publicado','La referencia anual no proporciona el beneficio neto necesario.','Consultar el beneficio neto del mismo ejercicio.')
                else:
                    item=reason('calculation_unverified','Cálculo por revisar','Los componentes necesitan conciliarse con la versión del resumen cargado.','Revisar los importes y límites antes de publicar un resultado.')
        if field=='beta':
            n=len(set(chart)&set(benchmark))
            sessions = '1 sesión común' if n == 1 else f'{n} sesiones comunes'
            item=reason('price_history_short','Histórico insuficiente',f'Hay {sessions}; esta beta requiere 253 para obtener 252 retornos diarios.','Completar sesiones comunes con el índice, sin interpolar precios.') if n<253 else reason('benchmark_unusable','Referencia de mercado por revisar','El histórico no produjo una beta válida con la metodología actual.','Revisar la variación del índice y los límites del cálculo.')
        if field=='dividend_yield':
            if not chart or (max(chart)-min(chart)).days<365:
                item=reason('dividend_history_short','Histórico inferior a un año','No hay cobertura de precios suficiente para la ventana de dividendos de 365 días.','Completar un año de historia compatible.')
            else:
                end=max(chart); events=raw_chart.get('events',{})
                day=lambda entry:datetime.fromtimestamp(entry['date'],ZoneInfo('America/New_York')).date()
                if any(end-timedelta(days=365)<day(e)<=end for e in events.get('splits',{}).values()):
                    item=reason('split_basis_unverified','Ajuste por desdoblamiento pendiente','Hubo un desdoblamiento reciente y la copia no certifica una base por acción común para los dividendos.','Conciliar las acciones y los pagos antes de calcular el rendimiento.')
                elif not any(end-timedelta(days=365)<day(e)<=end for e in events.get('dividends',{}).values()):
                    item=reason('no_dividend_events','Sin dividendos registrados','La copia no contiene pagos en la ventana consultada. Esto no confirma que la empresa nunca pague dividendos.','Verificar los pagos del período con la empresa antes de mostrar cero.')
                else:
                    item=reason('dividend_calculation_unverified','Dividendos por conciliar','Los eventos registrados no produjeron un rendimiento publicable.','Revisar sus importes y la base por acción.')
        result[field]=item
    return dict(version=1,fingerprint=fingerprint(summary),indicators=result)

def public_indicators(summary):
    saved=getattr(summary,'data_diagnostics',{}) or {}
    current=saved.get('fingerprint')==fingerprint(summary)
    return {field:available() if getattr(summary,field,None) is not None else
            saved.get('indicators',{}).get(field) if current and saved.get('indicators',{}).get(field) else
            reason('not_reviewed','Dato pendiente de verificar','No hay un diagnóstico vigente para esta cifra.','Revisar la fuente del resumen actualizado.')
            for field in INDICATORS}

def altman_status(summary, score):
    if score.get('score') is not None: return available()
    if getattr(summary,'altman_reference',None):
        return reason('invalid_reference','Referencia de cierre por revisar','La referencia Altman guardada no supera las validaciones.','Verificar fechas, importes y precio de la referencia.')
    missing=[key for key in LABELS if getattr(summary,key,None) is None]
    if missing:
        item=reason('missing_components','Faltan componentes del cierre','Falta verificar: '+', '.join(LABELS[key] for key in missing)+'.','Completar estos componentes en el mismo cierre contable.')
        if missing==['equity_value']:
            item.update(code='fiscal_valuation_missing',label='Valoración del cierre pendiente')
        item['missing_fields']=missing
        return item
    return reason('invalid_inputs','Importes no compatibles','Los importes no permiten aplicar la fórmula: revise denominadores positivos y valores finitos.','Conciliar los componentes antes de calcular la zona.')
