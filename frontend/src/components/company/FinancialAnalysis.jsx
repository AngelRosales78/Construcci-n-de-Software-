import { useAnalysisViewModel } from '../../viewmodels/useAnalysisViewModel'

function EvidenceClaim({ claim, metrics }) {
  return <div className="analysis-claim"><p>{claim.text}</p><small>Referencias: {claim.metric_ids.length ? claim.metric_ids.map(id => {
    const metric = metrics[id]
    return `${metric.label}: ${metric.display_value} ${{ percent: '%', times: 'veces', days: 'días' }[metric.unit] || metric.unit}`
  }).join(' · ') : 'Datos no disponibles para este pilar.'}</small></div>
}

export default function FinancialAnalysis({ fiscalYear }) {
  const { data, loading, error, generate } = useAnalysisViewModel(fiscalYear)
  return <section className="ai-panel" aria-labelledby="analysis-title">
    <p className="section-eyebrow">EXPLICACIÓN CON INTELIGENCIA ARTIFICIAL LOCAL</p><h2 id="analysis-title">Entender los resultados</h2>
    <p>La IA explica las métricas del ejercicio {fiscalYear || 'seleccionado'}. Las cifras las calcula el servidor; las interpretaciones pueden requerir revisión.</p>
    <button className="primary-action" onClick={() => generate()} disabled={loading}>{loading ? 'Generando análisis…' : 'Generar explicación de IA'}</button>
    {loading && <p role="status">El modelo local está redactando y validando la respuesta. La primera consulta puede tardar.</p>}
    {error && <p role="alert">{error}</p>}
    {data && <>
      <p className="analysis-validation">Estructura, referencias y cifras verificadas · {data.ticker} · ejercicio {data.fiscal_year} · {data.level === 'simple' ? 'Lenguaje sencillo' : 'Lenguaje técnico'}</p>
      <section aria-label="Resumen de IA"><h3>Resumen de la empresa</h3><EvidenceClaim claim={data.analysis.summary} metrics={data.metrics} /></section>
      <details className="financial-help"><summary>Explicación de los cinco pilares</summary><div className="help-content">{Object.entries({ liquidity: 'Liquidez', profitability: 'Rentabilidad', solvency: 'Solvencia', efficiency: 'Eficiencia', growth: 'Crecimiento' }).map(([id, name]) => <section key={id}><h3>{name}</h3><EvidenceClaim claim={data.analysis.pillars[id]} metrics={data.metrics} /></section>)}</div></details>
      <div className="analysis-arguments">{[['strengths', 'Fortalezas'], ['risks', 'Riesgos']].map(([key, title]) => <section key={key}><h3>{title}</h3>{data.analysis[key].length ? <ul>{data.analysis[key].map((claim, index) => <li key={index}><EvidenceClaim claim={claim} metrics={data.metrics} /></li>)}</ul> : <p>No hay argumentos suficientes respaldados por los datos para esta sección.</p>}</section>)}</div>
      <details className="financial-help"><summary>Fuentes y validación del análisis</summary><div className="help-content"><p>{data.validation_scope}</p><p>Periodo: {data.period.start} a {data.period.end} · Proveedor: {data.provider} · Generado: {new Date(data.generated_at).toLocaleString('es-PE')}.</p><p>{data.cache_hit ? 'Respuesta validada recuperada de caché.' : 'Nueva respuesta generada y validada.'} Identificador: {data.analysis_id}</p><ul>{data.sources.map(source => <li key={source}>{/^https:\/\//.test(source) ? <a href={source} target="_blank" rel="noreferrer">{source}</a> : source}</li>)}</ul></div></details>
    </>}
  </section>
}
