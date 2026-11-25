"""Reviewed educational content, explicitly separate from company observations and AI."""
LESSONS = {
 'liquidity': {'title':'Entender la liquidez', 'explanation':'Relaciona recursos y obligaciones de corto plazo. Tener activos no significa disponer hoy de todo ese dinero.',
   'example':'Ejemplo ilustrativo: una empresa ficticia tiene 200 unidades en recursos corrientes y debe 100 a corto plazo. Su liquidez corriente es 2 veces. Parte de esos recursos puede estar en inventario.',
   'question':'¿Una liquidez de 2 veces significa que todo ese valor está en efectivo?',
   'options':['Sí, todo está disponible hoy.','No, puede incluir inventario y cuentas por cobrar.'], 'correct':1,
   'feedback':'La razón incluye recursos corrientes que todavía pueden necesitar venderse o cobrarse.'},
 'profitability': {'title':'Entender la rentabilidad', 'explanation':'Compara beneficios con ingresos, activos o patrimonio. Cada indicador usa una base distinta.',
   'example':'Ejemplo ilustrativo: un negocio ficticio vende 100 unidades y obtiene 10 de beneficio neto. Su margen neto es 10 %. Esto no establece cuánto ganará quien compre sus acciones.',
   'question':'¿El margen neto indica la ganancia garantizada del inversionista?',
   'options':['No, describe el resultado del negocio frente a sus ingresos.','Sí, es el retorno de la acción.'], 'correct':0,
   'feedback':'El margen es contable. El retorno de una acción depende de otros factores y no está garantizado.'},
 'solvency': {'title':'Entender las deudas', 'explanation':'Los pasivos incluyen préstamos y otras obligaciones. Para interpretarlos también importan sus vencimientos y el efectivo disponible.',
   'example':'Ejemplo ilustrativo: un negocio ficticio tiene 150 unidades de pasivos y 100 de patrimonio. Su razón de pasivos sobre patrimonio es 1,5 veces; no significa que todos los pasivos sean préstamos bancarios.',
   'question':'¿Todos los pasivos de una empresa son préstamos?',
   'options':['Sí, son exactamente lo mismo.','No, también pueden incluir otras obligaciones.'], 'correct':1,
   'feedback':'Pasivo es un concepto más amplio: puede incluir cuentas por pagar y otras obligaciones.'},
 'growth': {'title':'Entender el crecimiento', 'explanation':'El crecimiento de ingresos compara ventas de periodos compatibles. No mide automáticamente beneficios ni el rendimiento de la acción.',
   'example':'Ejemplo ilustrativo: un negocio ficticio pasa de 100 a 110 unidades de ingresos en dos ejercicios comparables. Sus ingresos crecen 10 %, aunque sus gastos también podrían haber aumentado.',
   'question':'Si crecen las ventas, ¿queda demostrado que aumentaron los beneficios?',
   'options':['No, también hay que revisar gastos y resultados.','Sí, necesariamente.'], 'correct':0,
   'feedback':'Las ventas son una parte del resultado; conocerlas no basta para afirmar que creció el beneficio.'},
 'scores': {'title':'Entender los puntajes', 'explanation':'Las notas de los cinco pilares usan una escala académica sobre cien. Altman-Z utiliza otra fórmula y otra escala.',
   'example':'Ejemplo ilustrativo: una nota ficticia de 80 sobre 100 en un pilar describe las reglas de esa nota. No significa una probabilidad de ganar del 80 % ni equivale a un Altman-Z de 80.',
   'question':'¿Una nota de un pilar sobre cien es una probabilidad de ganancia?',
   'options':['Sí, representa esa probabilidad.','No, es una puntuación según una metodología académica.'], 'correct':1,
   'feedback':'Una escala de puntuación no se convierte por sí sola en probabilidad ni garantía.'},
}

METRIC_EXAMPLES = {
 'current_ratio':'Una empresa ficticia tiene 200 unidades de recursos corrientes y debe 100 a corto plazo. La razón es 2 veces; esos recursos pueden incluir inventario.',
 'quick_ratio':'Una empresa ficticia suma 120 unidades de efectivo, inversiones temporales y cuentas por cobrar, frente a 100 de obligaciones corrientes. La prueba ácida es 1,2 veces.',
 'roe':'Una empresa ficticia obtiene 10 unidades de beneficio neto y tiene un patrimonio promedio de 100. Su ROE es 10 %; no es el retorno garantizado de sus acciones.',
 'roa':'Una empresa ficticia obtiene 5 unidades de beneficio neto con activos promedio de 100. Su ROA es 5 %.',
 'net_margin':'Un negocio ficticio registra ingresos de 100 y beneficio neto de 10. Su margen neto es 10 %.',
 'liabilities_to_equity':'Una empresa ficticia registra pasivos de 150 y patrimonio de 100. La razón es 1,5 veces; los pasivos no son solamente préstamos.',
 'interest_coverage':'Una empresa ficticia tiene un resultado antes de intereses e impuestos de 30 y un gasto por intereses de 10. La cobertura es 3 veces; esto no demuestra que tenga ese importe en efectivo.',
 'asset_turnover':'Una empresa ficticia genera ingresos de 200 con activos promedio de 100. La rotación es 2 veces y no expresa un margen de ganancia.',
 'inventory_days':'Una empresa ficticia tiene inventario promedio de 10 y costo de ventas anual de 365. Con la convención de 365 días, la fórmula estima 10 días de inventario.',
 'revenue_growth':'Los ingresos de una empresa ficticia pasan de 100 a 110 en ejercicios comparables. La variación es 10 %, sin que esto demuestre un aumento del beneficio.',
 'revenue_cagr_3y':'Los ingresos de una empresa ficticia pasan de 100 a 133,1 entre los extremos de tres años. La tasa compuesta equivale a 10 % anual; los años intermedios pueden ser distintos.',
 'altman':'Un Altman-Z ficticio de 4 supera el límite de 2,99 de la zona favorable. No significa una probabilidad de ganancia ni una nota de cuatro sobre diez.',
 'scores':'Una nota ficticia de 80 sobre 100 refleja una metodología de puntuación. No significa una probabilidad de ganar del 80 %.',
}


def examples_for(topics):
    aliases={'liquidity':'current_ratio','profitability':'net_margin','solvency':'liabilities_to_equity','efficiency':'asset_turnover','growth':'revenue_growth'}
    result=[]
    for topic in topics:
        text=METRIC_EXAMPLES.get(aliases.get(topic,topic))
        if text and text not in result:
            result.append(text)
    return result[:2]


def lesson(ident, answer=None):
    item=LESSONS[ident]
    result={k:v for k,v in item.items() if k not in ('correct','feedback')}
    result.update(id=ident, generation_mode='reviewed_educational_content')
    if answer is not None:
        result['assessment']={'correct':answer==item['correct'], 'feedback':item['feedback'], 'selected':answer}
    return result
