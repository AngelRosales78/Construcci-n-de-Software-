import { useAnalysisViewModel } from '../../viewmodels/useAnalysisViewModel'

function EvidenceClaim({ claim, metrics }) {
  const first = metrics[claim.metric_ids[0]]
  const units = { percent: '%', times: 'veces', days: 'días' }
  return <div className="analysis-claim"><p>{claim.text}</p>{first && <p className="analysis-key-value"><strong>{first.display_value} {units[first.unit] || first.unit}</strong><span>{first.label}</span></p>}<details className="analysis-evidence"><summary>Ver cifras utilizadas</summary><small>Referencias: {claim.metric_ids.length ? claim.metric_ids.map(id => {
    const metric = metrics[id]
    return `${metric.label}: ${metric.display_value} ${{ percent: '%', times: 'veces', days: 'días' }[metric.unit] || metric.unit}`
  }).join(' · ') : 'Datos no disponibles para este pilar.'}</small></details></div>
}

export default function FinancialAnalysis({ fiscalYear }) {
  const { data, loading, error, generate, level, changeLevel } = useAnalysisViewModel(fiscalYear)
  return <section className="ai-panel" aria-labelledby="analysis-title">
    <p className="section-eyebrow">IA LOCAL · SIN CUENTAS EXTERNAS</p><h2 id="analysis-title">Tus números, explicados</h2>
    <p>Explicaciones revisadas para entender el balance. Elige el nivel y genera el resumen.</p>
    <div className="analysis-level"><label htmlFor="analysis-level">Nivel de explicación</label><select id="analysis-level" value={level} onChange={event => changeLevel(event.target.value)} disabled={loading}><option value="simple">Sencillo</option><option value="technical">Técnico</option></select></div>
    <button className="primary-action" onClick={() => generate()} disabled={loading}>{loading ? 'Generando análisis…' : 'Generar explicación de IA'}</button>
    {loading && <p role="status">El modelo local está redactando y validando la respuesta. La primera consulta puede tardar.</p>}
    {error && <p role="alert">{error}</p>}
    {data && <>
      <p className="analysis-validation">{data.ticker} · {data.fiscal_year} · {data.level === 'simple' ? 'Lenguaje sencillo' : 'Lenguaje técnico'} · Referencias verificadas</p>
      <section aria-label="Resumen de IA"><h3>Guía para interpretar estos indicadores</h3><EvidenceClaim claim={data.analysis.summary} metrics={data.metrics} /></section>
      <details className="financial-help"><summary>Explicación de los cinco pilares</summary><div className="help-content">{Object.entries({ liquidity: 'Liquidez', profitability: 'Rentabilidad', solvency: 'Solvencia', efficiency: 'Eficiencia', growth: 'Crecimiento' }).map(([id, name]) => <section key={id}><h3>{name}</h3><EvidenceClaim claim={data.analysis.pillars[id]} metrics={data.metrics} /></section>)}</div></details>
      <div className="analysis-arguments">{[['strengths', 'Fortalezas'], ['risks', 'Riesgos']].map(([key, title]) => <section key={key}><h3>{title}</h3>{data.analysis[key].length ? <ul>{data.analysis[key].map((claim, index) => <li key={index}><EvidenceClaim claim={claim} metrics={data.metrics} /></li>)}</ul> : <p>No hay argumentos suficientes respaldados por los datos para esta sección.</p>}</section>)}</div>
      <details className="financial-help"><summary>Fuentes y validación del análisis</summary><div className="help-content"><p>La IA local selecciona entre explicaciones financieras revisadas. Django calcula las cifras y añade sus referencias; no es una redacción financiera libre.</p><p>{data.validation_scope}</p><p>Periodo: {data.period.start} a {data.period.end} · Proveedor: {data.provider} · Generado: {new Date(data.generated_at).toLocaleString('es-PE')}.</p><p>{data.cache_hit ? 'Respuesta validada recuperada de caché.' : 'Nueva respuesta generada y validada.'} Identificador: {data.analysis_id}</p><ul>{data.sources.map(source => <li key={source}>{/^https:\/\//.test(source) ? <a href={source} target="_blank" rel="noreferrer">{source}</a> : source}</li>)}</ul></div></details>
    </>}
  </section>
}
