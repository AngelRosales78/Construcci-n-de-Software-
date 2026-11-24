import { hasIndicatorValue, formatMarketValue, formatFinancialNumber } from '../../utils/financialPresentation'

const indicators = [
  ['market_cap', 'Valor en bolsa', 'Valor de todas sus acciones en el mercado; no es el dinero que tiene en caja.'],
  ['roe', 'Rentabilidad del patrimonio · ROE', 'Relaciona el beneficio con el patrimonio de la empresa. No representa tu ganancia como inversionista.'],
  ['per', 'Precio / beneficio · PER', 'Compara el precio con el beneficio por acción. Por sí solo no indica si está barata o cara.'],
  ['beta', 'Movimiento frente al mercado · Beta', 'Describe cómo se ha movido la acción frente al mercado; no predice su próximo movimiento.'],
  ['dividend_yield', 'Rendimiento por dividendo', 'Relaciona los dividendos registrados con el precio. Los pagos futuros no están garantizados.'],
]
export default function MetricCards({ company }) {
  const format = (key, value) => {
    if (!hasIndicatorValue(value)) return 'Sin datos'
    if (key === 'market_cap') {
      return formatMarketValue(value)
    }
    return `${formatFinancialNumber(value)}${['roe', 'dividend_yield'].includes(key) ? '%' : ''}`
  }
  return <section className="dashboard-metrics beginner-metrics" aria-label="Ficha técnica">
    {indicators.map(([key, label, explanation]) => <article className="metric-card" key={key}>
      <span className="metric-label">{label}</span>
      <span className={`metric-value${hasIndicatorValue(company[key]) ? '' : ' metric-unavailable'}`}>{hasIndicatorValue(company[key]) ? format(key, company[key]) : company.indicator_availability?.[key]?.label || 'Sin datos'}</span>
      <p className="metric-meaning">{hasIndicatorValue(company[key]) ? explanation : company.indicator_availability?.[key]?.explanation || 'No hay información suficiente para mostrar esta cifra. No equivale a cero.'}</p>
      {company.field_sources?.[key]?.as_of ? <small className="metric-reference">Referencia · {company.field_sources[key].as_of}</small>
        : <small className="metric-reference">Fecha de referencia no informada.</small>}
    </article>)}
  </section>
}
