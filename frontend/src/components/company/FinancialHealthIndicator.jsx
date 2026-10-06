export default function FinancialHealthIndicator({ altmanZ }) {
  if (!altmanZ || !altmanZ.zone) {
    return <div className="health-indicator unknown">Sin datos</div>
  }

  const { zone, risk_level, score } = altmanZ

  const zoneConfig = {
    safe: { color: '#22c55e', label: 'Zona Segura', icon: '🟢' },
    grey: { color: '#eab308', label: 'Zona Gris', icon: '🟡' },
    distress: { color: '#ef4444', label: 'Zona de Riesgo', icon: '🔴' },
    unknown: { color: '#6b7280', label: 'Desconocido', icon: '⚪' },
  }

  const config = zoneConfig[zone] || zoneConfig.unknown

  return (
    <div className="health-indicator" style={{ borderColor: config.color }}>
      <div className="health-header">
        <span className="health-icon">{config.icon}</span>
        <span className="health-label" style={{ color: config.color }}>
          {config.label}
        </span>
      </div>
      <div className="health-score">
        <span className="score-value">{score}</span>
        <span className="score-label">Altman-Z Score</span>
      </div>
      <div className="health-risk">
        <span className="risk-label">Riesgo: </span>
        <span className="risk-value" style={{ color: config.color }}>
          {risk_level === 'low' ? 'Bajo' : risk_level === 'medium' ? 'Medio' : risk_level === 'high' ? 'Alto' : 'Desconocido'}
        </span>
      </div>
    </div>
  )
}
