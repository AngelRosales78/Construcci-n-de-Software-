import { useFinancialPillarsViewModel } from '../../viewmodels/useCompanyDashboardViewModel'

const UNITS = { times: 'veces', percent: '%', days: 'días' }
const BANDS = { low: 'Bajo', middle: 'Intermedio', high: 'Alto' }
const VISIBLE_PILLARS = ['liquidity']
const CONCEPTS = {
  liquidity: 'Capacidad de cubrir obligaciones de corto plazo con los recursos disponibles.',
}

export function PillarMetric({ metric }) {
  const ready = metric.status === 'available'
  const policy = metric.thresholds
  return <article className="pillar-metric" aria-label={metric.label}>
    <h4>{metric.label}</h4>
    <p className="pillar-value">{ready ? metric.display_value : 'No disponible'}{ready && <small> {UNITS[metric.unit] || metric.unit}</small>}</p>
    {!ready && <p>{metric.explanation || 'Faltan datos para calcular este indicador.'}</p>}
    {!!metric.missing_fields?.length && <p className="pillar-muted">Componentes pendientes: {metric.missing_fields.join(', ')}</p>}
    {ready && policy && <p className={`pillar-band pillar-band-${policy.band}`}>{BANDS[policy.band]} según regla académica</p>}
    <details className="pillar-details">
      <summary>Ver fórmula, componentes y fuente</summary>
      <p>Periodo: {metric.period?.start || 'Sin datos'} a {metric.period?.end || 'Sin datos'}</p>
      <p>Fórmula: <code>{metric.formula}</code></p>
      <ul>{metric.components?.map(component => <li key={component.id}>
        <strong>{component.id}:</strong> {component.value} {component.source.currency}
        <span> · Cierre: {component.period.end}</span>
        <span> · Etiqueta SEC: {component.source.tag}</span>
        <span> · Publicado: {component.source.filed} · Informe: {component.source.accession}</span>
        {component.source.is_demo && <strong> · Datos de prueba</strong>}
        <a href={component.source.url} target="_blank" rel="noreferrer">Consultar fuente</a>
      </li>)}</ul>
      {policy && <p>Umbrales {policy.version}: {policy.lower} y {policy.upper} {UNITS[metric.unit]}. {policy.direction === 'higher' ? 'Bajo por debajo del primer límite; intermedio hasta el segundo; alto desde el segundo.' : 'Alto hasta el primer límite; intermedio hasta el segundo; bajo por encima del segundo.'} {policy.explanation}</p>}
    </details>
  </article>
}

export default function FinancialPillars() {
  const { data, year, setYear, loading, error, retry } = useFinancialPillarsViewModel()
  const years = data?.available_years || []
  return <section className="financial-pillars" aria-labelledby="financial-pillars-title">
    <div className="section-title"><h2 id="financial-pillars-title">Pilares financieros anuales</h2></div>
    <p className="pillar-muted">Estos indicadores corresponden al ejercicio fiscal elegido. Cambiar el rango de precios no cambia el balance anual.</p>
    {loading && <p role="status">Cargando pilares financieros...</p>}
    {error && <div role="alert"><p>{error}</p><button onClick={retry}>Reintentar pilares</button></div>}
    {!loading && data && <>
      <label className="pillar-year">Ejercicio fiscal
        <select value={year} onChange={event => setYear(event.target.value)}>
          <option value="">Último disponible{years.length ? ` (${years[0]})` : ''}</option>
          {year && !years.includes(Number(year)) && <option value={year}>{year} · Sin datos</option>}
          {years.map(value => <option key={value} value={value}>{value}</option>)}
        </select>
      </label>
      <p className="pillar-muted">Ejercicio: {data.fiscal_year || 'Sin datos'} · {data.period.start || 'Sin fecha inicial'} a {data.period.end || 'Sin fecha final'}</p>
      {!years.length && <p>No hay balances anuales importados para esta empresa.</p>}
      {data.pillars.filter(pillar => VISIBLE_PILLARS.includes(pillar.id)).map(pillar => <section className="pillar-section" key={pillar.id} aria-label={`Pilar de ${pillar.label}`}>
        <h3>{pillar.label}</h3><p className="pillar-muted">{CONCEPTS[pillar.id]}</p>
        <div className="pillar-metrics">{pillar.metrics.map(metric => <PillarMetric key={metric.id} metric={metric} />)}</div>
      </section>)}
    </>}
  </section>
}
