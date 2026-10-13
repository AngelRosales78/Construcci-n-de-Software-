export default function DataSources({ company }) {
  return <details className="financial-help">
    <summary>Ver fuentes y cómo se obtuvieron los indicadores</summary>
    <div className="help-content">
      <p><strong>Balance:</strong> {company.as_of || 'No disponible'} · {company.source || 'Sin fuente registrada'}.</p>
      {company.quote && <p><strong>Precios:</strong> {company.quote.source} · último cierre importado: {company.quote.date} ({company.quote.currency}).</p>}
      {company.ticker === 'MSFT' && company.as_of && <p>En esta demostración se guardaron respuestas JSON de SEC/EDGAR y Yahoo Finance. Un script Python seleccionó el ejercicio 2024, calculó las métricas y generó archivos CSV; comandos Django los cargaron en PostgreSQL. Cada consulta utiliza esos datos persistidos.</p>}
      <dl>
        <dt>PER</dt><dd>Precio de referencia / beneficio por acción. No representa años garantizados para recuperar una inversión.</dd>
        <dt>ROE</dt><dd>Beneficio anual / patrimonio promedio × 100.</dd>
        <dt>Capitalización</dt><dd>Precio de referencia × acciones en circulación.</dd>
        <dt>Beta</dt><dd>Sensibilidad histórica frente a la referencia. En la carga de Microsoft se utilizaron retornos diarios ajustados frente a SPY durante los dos años anteriores al balance.</dd>
        <dt>Rendimiento por dividendo</dt><dd>Dividendos de los doce meses anteriores / precio de referencia × 100.</dd>
      </dl>
      <p>Las fechas y métodos pueden diferir entre proveedores. Estos datos históricos no se actualizan automáticamente al abrir la pantalla.</p>
    </div>
  </details>
}
