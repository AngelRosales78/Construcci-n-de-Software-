import { useFinancialPillarsViewModel } from '../../viewmodels/useCompanyDashboardViewModel'
import PillarRadar from './PillarRadar'
import OverallScore from './OverallScore'
import FinancialAnalysis from './FinancialAnalysis'

const UNITS = { times: 'veces', percent: '%', days: 'días' }
const BANDS = { low: 'Tramo de atención', middle: 'Tramo intermedio', high: 'Tramo favorable' }
const VISIBLE_PILLARS = ['liquidity', 'profitability', 'solvency', 'efficiency', 'growth']
const CONCEPTS = {
  liquidity: 'Capacidad de cubrir obligaciones de corto plazo con los recursos disponibles.',
  profitability: 'ROE relaciona la ganancia con el patrimonio; ROA, con los activos. El margen indica qué parte de los ingresos queda como beneficio.',
  solvency: 'Relaciona los pasivos con el patrimonio contable y el beneficio antes de intereses e impuestos con los intereses anuales. Estos ratios son distintos del indicador Altman Z.',
  efficiency: 'La rotación relaciona los ingresos con los activos utilizados. Los días de inventario estiman cuánto tarda en venderse el inventario promedio; usan una convención anual de 365 días.',
  growth: 'La variación anual compara ingresos de dos ejercicios. La tasa compuesta resume tres años entre cuatro cierres consecutivos; no es el retorno de la acción.',
}

export function PillarMetric({ metric }) {
  const ready = metric.status === 'available'
  const policy = metric.thresholds
  return <article className="pillar-metric" aria-label={metric.label}>
    <h4>{metric.label}</h4>
    <p className="pillar-value">{ready ? metric.display_value : metric.status === 'not_applicable' ? 'No aplica' : 'No disponible'}{ready && <small> {UNITS[metric.unit] || metric.unit}</small>}</p>
    {!ready && <p>{metric.explanation || 'Faltan datos para calcular este indicador.'}</p>}
    {!!metric.missing_fields?.length && <p className="pillar-muted">Componentes pendientes: {metric.missing_fields.join(', ')}</p>}
    {ready && policy && <p className={`pillar-band pillar-band-${policy.band}`}>{BANDS[policy.band]} según regla académica</p>}
    <details className="pillar-details">
      <summary>Ver fórmula, componentes y fuente</summary>
      <p>Periodo: {metric.period?.start || 'Sin datos'} a {metric.period?.end || 'Sin datos'}</p>
      <p>Fórmula: <code>{metric.formula_display || metric.formula}</code></p>
      {metric.applicability_source && <a href={metric.applicability_source.url} target="_blank" rel="noreferrer">Consultar procedencia de la clasificación</a>}
      <ul>{metric.components?.map(component => <li key={component.id}>
        <strong>{component.label || component.id}:</strong> {component.value} {component.source.currency}
        <span> · Cierre: {component.period.end}</span>
        <span> · Etiqueta SEC: {component.source.tag}</span>
        <span> · Publicado: {component.source.filed} · Informe: {component.source.accession}</span>
        {component.source.is_demo && <strong> · Datos de prueba</strong>}
        <a href={component.source.url} target="_blank" rel="noreferrer">Consultar fuente</a>
      </li>)}</ul>
      {policy && <p>Umbrales {policy.version}: {policy.lower} y {policy.upper} {UNITS[metric.unit]}. {policy.direction === 'higher' ? 'Atención por debajo del primer límite; intermedio hasta el segundo; favorable desde el segundo.' : 'Favorable hasta el primer límite; intermedio hasta el segundo; atención por encima del segundo.'} {policy.explanation}</p>}
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
      {data.overall_score && <OverallScore score={data.overall_score} fiscalYear={data.fiscal_year} />}
      {data.pillars.length === 5 && <PillarRadar data={data} />}
      <FinancialAnalysis fiscalYear={data.fiscal_year} />
      {data.pillars.filter(pillar => VISIBLE_PILLARS.includes(pillar.id)).map(pillar => <section className="pillar-section" id={`pillar-${pillar.id}`} tabIndex={-1} key={pillar.id} aria-label={`Pilar de ${pillar.label}`}>
        <h3>{pillar.label}</h3><p className="pillar-muted">{CONCEPTS[pillar.id]}</p>
        {pillar.score && <p className="pillar-note">Nota académica: {pillar.score.status === 'available' ? `${pillar.score.display_value} / 100` : 'No disponible'}</p>}
        {pillar.sector_reference?.status === 'unavailable' && <p className="pillar-muted">Referencia sectorial no disponible: {pillar.sector_reference.explanation}</p>}
        <div className="pillar-metrics">{pillar.metrics.map(metric => <PillarMetric key={metric.id} metric={metric} />)}</div>
        {!!pillar.history?.length && <div className="pillar-history"><table>
          <caption>Eficiencia en hasta tres ejercicios disponibles</caption>
          <thead><tr><th scope="col">Ejercicio y cierre</th><th scope="col">Rotación de activos</th><th scope="col">Días de inventario</th></tr></thead>
          <tbody>{pillar.history.map(row => <tr key={row.fiscal_year}>
            <th scope="row">{row.fiscal_year}<small>{row.period.end}</small></th>
            {['asset_turnover', 'inventory_days'].map(id => {
              const item = row.metrics.find(value => value.id === id)
              return <td key={id}>{item?.status === 'available' ? `${item.display_value} ${UNITS[item.unit]}` : item?.status === 'not_applicable' ? 'No aplica' : 'No disponible'}</td>
            })}
          </tr>)}</tbody>
        </table></div>}
      </section>)}
    </>}
  </section>
}
