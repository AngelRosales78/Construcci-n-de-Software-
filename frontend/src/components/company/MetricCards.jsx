const indicators = [
  ['per', 'PER', 'Precio de la acción dividido entre beneficio por acción.'],
  ['roe', 'ROE', 'Rentabilidad del patrimonio, expresada en porcentaje.'],
  ['market_cap', 'Capitalización', 'Valor de mercado total de las acciones de la empresa.'],
  ['beta', 'Beta', 'Sensibilidad de la acción frente a los movimientos del mercado.'],
  ['dividend_yield', 'Rendimiento por dividendo', 'Dividendo anual dividido entre el precio, expresado en porcentaje.'],
]
export default function MetricCards({ company }) {
  const format = (key, value) => {
    if (value == null || !Number.isFinite(Number(value))) return 'Sin datos'
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
      <abbr title={explanation} className="metric-label">{label}</abbr>
      <span className="metric-value">{format(key, company[key])}</span>
      <p className="metric-explanation">{explanation}</p>
      {key === 'market_cap' && <small className="metric-unit-note">B: mil millones · T: un billón</small>}
    </article>)}
  </section>
}
