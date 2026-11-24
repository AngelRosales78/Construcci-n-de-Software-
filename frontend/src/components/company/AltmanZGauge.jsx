import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts'

function RiskLegend() {
  return <details className="risk-legend"><summary>Ver límites de las zonas</summary><p>Zona segura: mayor que 2,99 · Zona gris: de 1,81 a 2,99 · Peligro: menor que 1,81</p></details>
}
function RiskExplanation() {
  return <details className="financial-help"><summary>¿Cómo se calcula Altman-Z?</summary><div className="help-content"><p>Z = 1,2X₁ + 1,4X₂ + 3,3X₃ + 0,6X₄ + X₅</p><ul><li>X₁: capital de trabajo / activos totales.</li><li>X₂: beneficios retenidos / activos totales.</li><li>X₃: beneficio antes de intereses e impuestos / activos totales.</li><li>X₄: valor bursátil del patrimonio / pasivos totales.</li><li>X₅: ventas / activos totales.</li></ul><p>La fórmula original se diseñó para manufactureras cotizadas. Su uso aquí es académico y no se ha validado como predictor para esta empresa. El puntaje no es un porcentaje ni garantiza solvencia.</p></div></details>
}
export default function AltmanZGauge({ score, zone, reference }) {
  if (score == null || !Number.isFinite(score) || zone === 'unknown') {
    return <div className="altmanz-gauge"><h3>Altman-Z Score</h3>
      <p>No hay datos suficientes para calcular el indicador.</p><RiskLegend /><RiskExplanation /></div>
  }
  const displayedValue = Math.min(5, Math.max(0, score))
  const getZoneColor = (z) => {
    if (z === 'safe') return '#8ec19d'
    if (z === 'grey') return '#e4c480'
    if (z === 'distress') return '#e29a80'
    return '#6b7280'
  }

  const color = getZoneColor(zone)

  const data = [
    { name: 'Peligro', value: 1.81, color: '#c47a61' },
    { name: 'Zona gris', value: 1.18, color: '#d5b575' },
    { name: 'Zona segura', value: 2.01, color: '#72a986' },
  ]
  const angle = Math.PI * (1 - displayedValue / 5)
  const needle = { x: 100 + 68 * Math.cos(angle), y: 100 - 68 * Math.sin(angle) }

  return (
    <div className="altmanz-gauge">
      <h3>Altman-Z Score</h3>
      {reference && <p className="altman-period-badge">Cierre contable · {reference.period_end}</p>}
      <div className="gauge-container">
        <ResponsiveContainer width="100%" height={200}>
          <PieChart>
            <Tooltip formatter={() => [score, "Puntaje Altman-Z"]} />
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              startAngle={180}
              endAngle={0}
              innerRadius={60}
              outerRadius={80}
              paddingAngle={0}
              isAnimationActive={false}
              dataKey="value"
            >
              {data.map(segment => <Cell key={segment.name} fill={segment.color} stroke="none" />)}
            </Pie>
          </PieChart>
        </ResponsiveContainer>
        <svg className="gauge-needle" width="200" height="200" viewBox="0 0 200 200" aria-hidden="true"><path d={`M100 100 L${needle.x} ${needle.y}`} stroke="#dfe2eb" strokeWidth="3" strokeLinecap="round" /><circle cx="100" cy="100" r="6" fill="#dfe2eb" /></svg>
        <div className="gauge-value">
          <span className="gauge-score" style={{ color }}>{score}</span>
        </div>
      </div>
      <RiskLegend />
      <div className="gauge-labels">
        <span className="gauge-label">0</span>
        <span className="gauge-label">1.81</span>
        <span className="gauge-label">2.99</span>
        <span className="gauge-label">5</span>
      </div>
      {(score > 5 || score < 0) && <p className="gauge-note">La escala visual va de 0 a 5; el puntaje calculado es {score}.</p>}
      <RiskExplanation />
      {reference && <details className="financial-help"><summary>Ver datos y fuentes de Altman-Z</summary><div className="help-content">
        <p>Período contable: {reference.period_start} a {reference.period_end}. Moneda: {reference.currency}.</p>
        <p>Precio sin ajustar: {Number(reference.unadjusted_close).toLocaleString('es-PE', {maximumFractionDigits: 2})} USD · Sesión: {reference.price_date}.</p>
        <p>Acciones al cierre: {Number(reference.shares_outstanding).toLocaleString('es-PE')}.</p>
        <ul>{Object.entries(reference.inputs).map(([key, value]) => <li key={key}>{({working_capital:'Capital de trabajo',total_assets:'Activos totales',retained_earnings:'Beneficios retenidos',ebit:'EBIT',equity_value:'Valor bursátil del patrimonio',total_liabilities:'Pasivos totales',sales:'Ventas'})[key]}: {Number(value).toLocaleString('es-PE', {maximumFractionDigits:2})} USD</li>)}</ul>
        <p>{reference.method}</p><p>{reference.locator}</p>
        <a href={reference.report_url} target="_blank" rel="noreferrer">Consultar informe oficial</a>{' · '}<a href={reference.price_url} target="_blank" rel="noreferrer">Consultar histórico de precios</a>
      </div></details>}
    </div>
  )
}
