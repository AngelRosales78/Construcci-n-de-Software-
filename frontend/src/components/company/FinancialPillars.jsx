import { useFinancialPillarsViewModel } from '../../viewmodels/useCompanyDashboardViewModel'
import PillarRadar from './PillarRadar'
import OverallScore from './OverallScore'
import FinancialAssistant from './FinancialAssistant'
import { useEffect, useState } from 'react'

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
    {ready && policy && <p className={`pillar-band pillar-band-${policy.band}`}>{BANDS[policy.band]} según regla académica</p>}
    <details className="pillar-details">
      <summary>Ver fórmula, componentes y fuente</summary>
      {!!metric.missing_fields?.length && <p className="pillar-muted">Componentes pendientes: {metric.missing_fields.join(', ')}</p>}
      <p>{metric.period_basis === 'provider_normalized' ? `Referencia anual del proveedor: ${metric.period?.end}` : `Periodo: ${metric.period?.start || 'Sin datos'} a ${metric.period?.end || 'Sin datos'}`}</p>
      <p>Fórmula: <code>{metric.formula_display || metric.formula}</code></p>
      {metric.applicability_source && <a href={metric.applicability_source.url} target="_blank" rel="noreferrer">Consultar procedencia de la clasificación</a>}
      <ul>{metric.components?.map(component => <li key={component.id}>
        <strong>{component.label || component.id}:</strong> {component.value} {component.source.currency}
        <span> · {component.source.period_basis === 'provider_normalized' ? 'Referencia anual' : 'Cierre'}: {component.period.end}</span>
        <span> · {component.source.source_kind === 'yahoo' ? 'Campo del proveedor' : component.source.snapshot_kind === 'extracted_financial_facts' ? 'Referencia del informe' : 'Etiqueta SEC'}: {component.source.tag}</span>
        {component.source.locator && <span> · {component.source.locator}</span>}
        <span>{component.source.filed ? ` · Publicado: ${component.source.filed}` : ' · Publicación no proporcionada por el proveedor'}{component.source.accession ? ` · Informe: ${component.source.accession}` : ''}</span>
        {component.source.is_demo && <strong> · Fuente pendiente de verificar</strong>}
        <a href={component.source.url} target="_blank" rel="noreferrer">Consultar fuente</a>
      </li>)}</ul>
      {policy && <p>Umbrales {policy.version}: {policy.lower} y {policy.upper} {UNITS[metric.unit]}. {policy.direction === 'higher' ? 'Atención por debajo del primer límite; intermedio hasta el segundo; favorable desde el segundo.' : 'Favorable hasta el primer límite; intermedio hasta el segundo; atención por encima del segundo.'} {policy.explanation}</p>}
    </details>
  </article>
}

export default function FinancialPillars({ view = 'pillars' }) {
  const { data, year, setYear, loading, error, retry } = useFinancialPillarsViewModel()
  const years = data?.available_years || []
  const [activePillar, setActivePillar] = useState('liquidity')
  useEffect(() => {
    const selectHash = () => {
      const id = window.location.hash.replace('#pillar-', '')
      if (VISIBLE_PILLARS.includes(id)) setActivePillar(id)
    }
    selectHash()
    window.addEventListener('hashchange', selectHash)
    return () => window.removeEventListener('hashchange', selectHash)
  }, [])
  const openLinkedPillar = event => {
    const target = event.target.closest?.('a')?.getAttribute('href')?.replace('#pillar-', '')
    if (VISIBLE_PILLARS.includes(target)) setActivePillar(target)
  }
  return <section className="financial-pillars" aria-labelledby="financial-pillars-title">
    <div className="section-title"><h2 id="financial-pillars-title" tabIndex={-1}>{view === 'analysis' ? 'Entiende los resultados' : 'Salud financiera'}</h2><span>Balance anual</span></div>
    {loading && <p role="status">Cargando pilares financieros...</p>}
    {error && <div role="alert"><p>{error}</p><button onClick={retry}>Reintentar pilares</button></div>}
    {!loading && data && <>
      <div className="pillar-year"><label htmlFor="fiscal-year">Ejercicio fiscal</label>
        <select id="fiscal-year" value={year} onChange={event => setYear(event.target.value)}>
          <option value="">Último disponible{years.length ? ` (${years[0]})` : ''}</option>
          {year && !years.includes(Number(year)) && <option value={year}>{year} · Sin datos</option>}
          {years.map(value => <option key={value} value={value}>{value}</option>)}
        </select>
      </div>
      <p className="pillar-muted">{data.period_basis === 'provider_normalized' ? `Referencia anual: ${data.period.end} · ${data.fiscal_year}` : `Ejercicio: ${data.fiscal_year || 'Sin datos'} · ${data.period.start || 'Sin fecha inicial'} a ${data.period.end || 'Sin fecha final'}`}</p>
      {data.period_note && <div className="provider-period-note" role="note"><strong>Referencia anual normalizada</strong><span>El cierre contable exacto no está verificado.</span><details><summary>Qué implica esta fecha</summary><p>{data.period_note}</p></details></div>}
      {!!data.annual_components?.length && <details className="financial-help annual-statement"><summary>Ver estados anuales importados</summary><div className="help-content">
        <p>Importes completos en {data.currency}; no están expresados en millones. Los valores ausentes permanecen sin dato.</p>
        <div className="scenario-table"><table><caption>Componentes anuales · {data.fiscal_year} · {data.currency}</caption><thead><tr><th scope="col">Concepto</th><th scope="col">Importe</th><th scope="col">Procedencia</th></tr></thead>
          <tbody>{data.annual_components.map(component => <tr key={component.id}><th scope="row">{component.label}</th><td>{component.value == null ? 'Sin dato' : Number(component.value).toLocaleString('es-PE', { maximumFractionDigits: 2 })}</td><td>{component.value != null && <a href={component.source.url} target="_blank" rel="noreferrer">Consultar fuente</a>}</td></tr>)}</tbody>
        </table></div>
      </div></details>}
      {!years.length && <p>No hay balances anuales importados para esta empresa.</p>}
      <div hidden={view === 'analysis'} onClick={openLinkedPillar}>
      {data.overall_score && <OverallScore score={data.overall_score} fiscalYear={data.fiscal_year} periodBasis={data.period_basis} />}
      {data.pillars.length === 5 && <PillarRadar data={data} />}
      {data.pillars.filter(pillar => VISIBLE_PILLARS.includes(pillar.id)).map(pillar => <section className="pillar-section" id={`pillar-${pillar.id}`} tabIndex={-1} key={pillar.id} aria-label={`Pilar de ${pillar.label}`}>
        <h3><button className="pillar-toggle" type="button" aria-label={`Mostrar ${pillar.label}`} aria-expanded={activePillar === pillar.id} aria-controls={`detail-${pillar.id}`} onClick={() => setActivePillar(previous => previous === pillar.id ? null : pillar.id)}><span>{pillar.label}</span><small>{pillar.score?.status === 'available' ? `${pillar.score.display_value} / 100` : 'Ver indicadores'} · {activePillar === pillar.id ? '−' : '+'}</small></button></h3>
        <div id={`detail-${pillar.id}`} hidden={activePillar !== pillar.id}>
        <p className="pillar-concept pillar-meaning">{CONCEPTS[pillar.id]}</p>
        {pillar.score && <p className="pillar-note">Nota académica: {pillar.score.status === 'available' ? `${pillar.score.display_value} / 100` : 'No disponible'}</p>}
        {pillar.sector_reference?.status === 'unavailable' && <details className="pillar-concept"><summary>Referencia sectorial no disponible</summary><p className="pillar-muted">{pillar.sector_reference.explanation}</p></details>}
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
        </div>
      </section>)}
      </div>
      {view === 'analysis' && <FinancialAssistant key={`${data.ticker}:${data.fiscal_year}`} a={data.ticker} year={data.fiscal_year} />}
    </>}
  </section>
}
