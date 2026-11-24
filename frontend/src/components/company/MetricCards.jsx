import { hasIndicatorValue } from '../../utils/financialPresentation'

const indicators = [
  ['per', 'PER', 'Precio de la acción dividido entre beneficio por acción.'],
  ['roe', 'ROE', 'Rentabilidad del patrimonio, expresada en porcentaje.'],
  ['market_cap', 'Capitalización', 'Valor de mercado total de las acciones de la empresa.'],
  ['beta', 'Beta', 'Sensibilidad de la acción frente a los movimientos del mercado.'],
  ['dividend_yield', 'Rendimiento por dividendo', 'Dividendo anual dividido entre el precio, expresado en porcentaje.'],
]
export default function MetricCards({ company }) {
  const format = (key, value) => {
    if (!hasIndicatorValue(value)) return 'Sin datos'
    if (key === 'market_cap') {
      const number = Number(value)
      const unit = number >= 1e12 ? 'T' : number >= 1e9 ? 'B' : ''
      const amount = number / (unit === 'T' ? 1e12 : unit === 'B' ? 1e9 : 1)
      return `USD ${amount.toLocaleString('es-PE', { maximumFractionDigits: 2 })}${unit ? ` ${unit}` : ''}`
    }
    return `${Number(value).toLocaleString('es-PE', { maximumFractionDigits: 4 })}${['roe', 'dividend_yield'].includes(key) ? '%' : ''}`
  }
  return <section className="dashboard-metrics" aria-label="Ficha técnica">
    {indicators.map(([key, label, explanation]) => <article className="metric-card" key={key}>
      <span className="metric-label">{label}</span>
      <span className="metric-value">{format(key, company[key])}</span>
      {company.field_sources?.[key]?.as_of ? <small className="metric-reference">Referencia · {company.field_sources[key].as_of}</small>
        : <small className="metric-reference">Fecha de referencia no informada.</small>}
      <details className="metric-help"><summary aria-label={`Entender ${label}`}><span aria-hidden="true">?</span> Qué significa</summary>
        <p>{explanation}</p>
        {key === 'market_cap' && <p>B: mil millones · T: un billón.</p>}
        {company.field_sources?.[key] && <><p>{company.field_sources[key].method}</p>
          <a href={company.field_sources[key].source} target="_blank" rel="noreferrer">Consultar fuente del indicador</a></>}
      </details>
    </article>)}
  </section>
}
