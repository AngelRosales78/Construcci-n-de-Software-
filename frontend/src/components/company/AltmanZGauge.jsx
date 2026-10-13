import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts'

function RiskLegend() {
  return <p className="risk-legend">Zona segura: mayor que 2,99 · Zona gris: de 1,81 a 2,99 · Peligro: menor que 1,81</p>
}
function RiskExplanation() {
  return <details className="financial-help"><summary>¿Cómo se calcula Altman-Z?</summary><div className="help-content"><p>Z = 1,2X₁ + 1,4X₂ + 3,3X₃ + 0,6X₄ + X₅</p><ul><li>X₁: capital de trabajo / activos totales.</li><li>X₂: beneficios retenidos / activos totales.</li><li>X₃: beneficio antes de intereses e impuestos / activos totales.</li><li>X₄: valor bursátil del patrimonio / pasivos totales.</li><li>X₅: ventas / activos totales.</li></ul><p>La fórmula original se diseñó para manufactureras cotizadas. Su uso aquí es académico y no se ha validado como predictor para Microsoft. El puntaje no es un porcentaje ni garantiza solvencia.</p></div></details>
}
export default function AltmanZGauge({ score, zone }) {
  if (score == null || !Number.isFinite(score) || zone === 'unknown') {
    return <div className="altmanz-gauge"><h3>Altman-Z Score</h3>
      <p>No hay datos suficientes para calcular el indicador.</p><RiskLegend /><RiskExplanation /></div>
  }
  const displayedValue = Math.min(5, Math.max(0, score))
  const getZoneColor = (z) => {
    if (z === 'safe') return '#22c55e'
    if (z === 'grey') return '#eab308'
    if (z === 'distress') return '#ef4444'
    return '#6b7280'
  }

  const color = getZoneColor(zone)

  const data = [
    { name: 'Peligro', value: 1.81, color: '#ff6262' },
    { name: 'Zona gris', value: 1.18, color: '#ffb95f' },
    { name: 'Zona segura', value: 2.01, color: '#00e676' },
  ]
  const angle = Math.PI * (1 - displayedValue / 5)
  const needle = { x: 100 + 68 * Math.cos(angle), y: 100 - 68 * Math.sin(angle) }

  return (
    <div className="altmanz-gauge">
      <h3>Altman-Z Score</h3>
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
    </div>
  )
}
