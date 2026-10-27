export default function FinancialHealthIndicator({ altmanZ }) {
  const defaults = {
    safe: { label: 'Zona favorable según Altman-Z', color: 'green' },
    grey: { label: 'Zona intermedia según Altman-Z', color: 'orange' },
    distress: { label: 'Zona de atención según Altman-Z', color: 'red' },
  }
  const data = defaults[altmanZ?.zone] || { label: 'Sin datos suficientes', color: 'grey' }
  return <section className={`financial-health health-${data.color}`} aria-label="Perfil financiero">
    <span className="section-eyebrow">LECTURA DEL BALANCE</span>
    <h3>Perfil financiero</h3><strong>{data.label}</strong>
    <p>Clasificación académica; no es una garantía de solvencia.</p>
    {altmanZ?.score != null && <p>Puntaje: {altmanZ.score}</p>}
    <p className="health-explanation">El indicador combina liquidez, beneficios, endeudamiento y ventas del balance de referencia.</p>
  </section>
}
