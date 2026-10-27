"""Vocabulario financiero controlado; describe las fórmulas reales de pillars.py."""

DESCRIPTIONS = {
    'current_ratio': (
        'La liquidez corriente compara los recursos y las obligaciones de corto plazo.',
        'La razón corriente divide el activo corriente entre el pasivo corriente.'),
    'quick_ratio': (
        'La prueba ácida compara efectivo, inversiones de corto plazo y cuentas por cobrar con obligaciones corrientes.',
        'La prueba ácida relaciona efectivo, inversiones temporales y cuentas por cobrar con el pasivo corriente.'),
    'roe': (
        'El ROE relaciona los beneficios con el patrimonio promedio de los propietarios.',
        'El ROE expresa el beneficio neto como porcentaje del patrimonio contable promedio.'),
    'roa': (
        'El ROA relaciona los beneficios con los activos promedio de la empresa.',
        'El ROA expresa el beneficio neto como porcentaje del activo total promedio.'),
    'net_margin': (
        'El margen neto muestra qué parte de las ventas queda como beneficio neto.',
        'El margen neto relaciona el resultado neto con los ingresos del ejercicio.'),
    'liabilities_to_equity': (
        'Los pasivos sobre patrimonio comparan las obligaciones totales con el patrimonio de los propietarios.',
        'La razón de pasivos sobre patrimonio relaciona el pasivo total con el patrimonio contable.'),
    'interest_coverage': (
        'La cobertura de intereses compara el resultado antes de intereses e impuestos con los gastos financieros.',
        'La cobertura divide el EBIT aproximado, calculado como resultado antes de impuestos más intereses, entre gastos financieros.'),
    'asset_turnover': (
        'La rotación de activos relaciona las ventas con los activos promedio utilizados.',
        'La rotación de activos divide los ingresos entre el activo total promedio.'),
    'inventory_days': (
        'Los días de inventario estiman cuánto tiempo permanece el inventario según el costo de ventas.',
        'Los días de inventario convierten la relación entre inventario promedio y costo de ventas anual en días.'),
    'revenue_growth': (
        'La variación anual compara los ingresos con los del ejercicio anterior comparable.',
        'El crecimiento interanual expresa la variación relativa de ingresos entre ejercicios consecutivos comparables.'),
    'revenue_cagr_3y': (
        'El crecimiento compuesto resume el cambio anual equivalente de ingresos durante tres años.',
        'El crecimiento compuesto de ingresos anualiza geométricamente la variación entre los extremos de tres años.'),
}


def explanation_options(key, ids, metrics, level):
    """Solo opciones compatibles con las métricas disponibles y su clasificación."""
    if key == 'summary':
        return [
            'Los indicadores disponibles permiten examinar la situación financiera histórica; deben leerse junto a sus componentes y periodo.',
            'El resumen reúne métricas financieras del ejercicio seleccionado; sus referencias muestran los valores calculados y las fuentes originales.',
        ] if level == 'simple' else [
            'La lectura conjunta de ratios históricos exige considerar componentes, ejercicio fiscal y metodología; no constituye una previsión de resultados.',
            'El análisis utiliza indicadores del ejercicio fiscal seleccionado, con trazabilidad de componentes y reglas académicas de clasificación.',
        ]
    if key in ('strength', 'risk'):
        metric = metrics[ids[0]]
        band = {'high': 'favorable', 'middle': 'intermedio', 'low': 'de atención'}[metric['band']]
        label = metric['label']
        return [
            f'{label}: el indicador pertenece al tramo {band} de esta metodología; conviene revisar sus componentes antes de concluir.',
            f'{label}: la clasificación académica sitúa esta métrica en el tramo {band}; no equivale a una garantía sobre resultados futuros.',
        ] if level == 'simple' else [
            f'{label}: la métrica se ubica en el tramo {band} según los umbrales versionados, sin constituir una referencia sectorial.',
            f'{label}: el tramo {band} procede de reglas académicas; su interpretación requiere analizar componentes, periodo y límites metodológicos.',
        ]
    index = 0 if level == 'simple' else 1
    sentences = [DESCRIPTIONS[metric_id][index] for metric_id in ids]
    return list(dict.fromkeys([' '.join(sentences), sentences[0]]))
