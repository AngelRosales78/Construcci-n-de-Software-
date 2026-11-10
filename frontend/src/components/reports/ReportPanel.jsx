import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useRemoteResource } from '../../viewmodels/useRemoteResource'
import { useReportViewModel } from '../../viewmodels/useReportViewModel'

export default function ReportPanel({ a, b, year }) {
  const [params] = useSearchParams()
  const [open, setOpen] = useState(false)
  const [level, setLevel] = useState('simple')
  const [charts, setCharts] = useState(true)
  const [includeAnalysis, setIncludeAnalysis] = useState(false)
  const [requested, setRequested] = useState(false)
  const chosenYear = params.get('fiscal_year')
  const period = useRemoteResource(`/companies/${encodeURIComponent(a)}/pillars/${chosenYear ? '?fiscal_year=' + encodeURIComponent(chosenYear) : ''}`, open && !year)
  const fiscalYear = year || period.data?.fiscal_year
  const range = params.get('range') || (b ? '1Y' : '1M')
  const query = new URLSearchParams({ fiscal_year: fiscalYear || '', level })
  const analysis = useRemoteResource(b ? `/compare/conclusion/?${new URLSearchParams({ a, b, fiscal_year: fiscalYear || '', level })}`
    : `/companies/${encodeURIComponent(a)}/analysis/?${query}`, open && requested && Boolean(fiscalYear))
  const data = analysis.data
  const matches = data && data.fiscal_year === Number(fiscalYear) && data.level === level &&
    (b ? data.a === a && data.b === b : data.ticker === a && data.validated)
  const origin = matches ? (data.generation_mode || 'ai') : null
  const version = matches ? b ? `${data.data_version}/${data.generated_at}` : `${data.metrics_version}/${data.period.end}/${data.generated_at}` : null
  const paragraphs = matches ? b ? data.paragraphs : [data.analysis.summary.text] : []
  const reports = useReportViewModel({ a, b, fiscal_year: fiscalYear || 0 })
  const options = { range, include_charts: charts, include_analysis: includeAnalysis,
    analysis_level: level, analysis_origin: includeAnalysis ? origin : null, analysis_version: includeAnalysis ? version : null }
  return <details className="sprint3-panel" onToggle={e => setOpen(e.currentTarget.open)}>
    <summary>{b ? 'Conclusión comparativa y reporte PDF' : 'Exportar reporte PDF de esta empresa'}</summary>
    {open && <>
      <p>{a}{b ? ` y ${b}` : ''} · FY{fiscalYear || 'Sin ejercicio disponible'} · Gráficos: {range}</p>
      {!year && period.error && <p role="alert">{period.error}</p>}
      <label>Nivel del análisis<select value={level} onChange={e => setLevel(e.target.value)}><option value="simple">Sencillo</option><option value="technical">Técnico</option></select></label>
      <button disabled={!fiscalYear || analysis.loading} onClick={() => { setRequested(true); analysis.reload() }}>{analysis.loading ? 'Preparando análisis…' : b ? 'Generar conclusión comparativa' : 'Preparar análisis para exportar'}</button>
      {analysis.error && <p role="alert">{analysis.error}</p>}
      {data?.status === 'unavailable' && <p role="status">No hay datos suficientes para preparar el análisis.</p>}
      {matches && <section aria-label="Análisis exportable">
        <p>Origen: {origin === 'ai' ? 'IA local con texto validado' : origin === 'mixed' ? 'Mixto: IA y reglas' : 'Reglas, sin generación de IA'}</p>
        <p className="pillar-muted">Generado: {new Date(data.generated_at).toLocaleString('es-PE')} · {level === 'simple' ? 'Sencillo' : 'Técnico'}</p>
        {paragraphs.map((text, i) => <p key={i}>{text}</p>)}
        {b && <><h3>Fortalezas relativas de {a}</h3>{!data.strengths_a.length && <p>Sin ventaja relativa en los pilares comparables.</p>}<ul>{data.strengths_a.map((text, i) => <li key={i}>{text}</li>)}</ul>
          <h3>Fortalezas relativas de {b}</h3>{!data.strengths_b.length && <p>Sin ventaja relativa en los pilares comparables.</p>}<ul>{data.strengths_b.map((text, i) => <li key={i}>{text}</li>)}</ul></>}
        <details><summary>Fuentes del análisis</summary><ul>{data.sources.map((source, i) => {
          const url = typeof source === 'string' ? source : source.url
          return /^https?:\/\//.test(url) ? <li key={i}><a href={url} target="_blank" rel="noreferrer">{typeof source === 'string' ? source : source.label || url}</a></li> : null
        })}</ul></details>
      </section>}
      <fieldset><legend>Contenido del PDF</legend>
        <label><input type="checkbox" checked={charts} onChange={e => setCharts(e.target.checked)} />Incluir gráficos vectoriales del rango activo</label>
        <label><input type="checkbox" checked={includeAnalysis} disabled={!matches} onChange={e => setIncludeAnalysis(e.target.checked)} />Incluir el análisis preparado, con su origen y fuentes</label>
      </fieldset>
      <p>El informe conserva empresa, ejercicio, fuentes y fecha. Preparar IA y generar el PDF son operaciones distintas.</p>
      <button disabled={!fiscalYear || reports.busy || (includeAnalysis && !matches) || ['queued', 'running'].includes(reports.job?.status)} onClick={() => reports.submit(options)}>Solicitar PDF</button>
      {reports.record?.request && !reports.record.id && <button disabled={reports.busy} onClick={() => reports.submit(options, true)}>Reintentar misma solicitud</button>}
      {reports.error && <p role="alert">{reports.error}</p>}
      {reports.message && <p role="status">{reports.message}</p>}
      {reports.job && <section aria-label="Estado del reporte">
        <p role="status">{({ queued: 'En cola', running: 'Generando', ready: 'Listo para descargar', error: 'No se pudo generar', cancelled: 'Cancelado' })[reports.job.status]} · {reports.job.progress || 0} %</p>
        <p>Reporte actual: {reports.job.request.a}{reports.job.request.b ? ` y ${reports.job.request.b}` : ''} · FY{reports.job.request.fiscal_year} · {reports.job.request.range}{reports.job.request.include_charts ? ' · Con gráficos' : ' · Sin gráficos'}</p>
        {reports.job.message && <p>{reports.job.message}</p>}
        {['queued', 'running'].includes(reports.job.status) && <button disabled={reports.busy} onClick={reports.cancel}>Cancelar generación</button>}
        {reports.job.status === 'ready' && <><p>{reports.job.page_count} páginas · {(reports.job.size_bytes / 1024).toFixed(1)} KB · Generación: {reports.job.generation_ms} ms{reports.job.cache_hit ? ' (caché)' : ''}</p>
          <button disabled={reports.busy} onClick={reports.download}>Descargar PDF verificado</button></>}
      </section>}
    </>}
  </details>
}
