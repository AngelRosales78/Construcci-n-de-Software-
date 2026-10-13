export default function FinancialHealthIndicator({ altmanZ, profile }) {
  const defaults = {
    safe: { label: 'Muy sólido', color: 'green' },
    grey: { label: 'Moderado', color: 'orange' },
    distress: { label: 'Muy débil', color: 'red' },
  }
  const data = profile || defaults[altmanZ?.zone] || { label: 'Sin datos suficientes', color: 'grey' }
  return <section className={`financial-health health-${data.color}`} aria-label="Perfil financiero">
    <span className="section-eyebrow">LECTURA DEL BALANCE</span>
    <h3>Perfil financiero</h3><strong>{data.label}</strong>
    <p>Clasificación según el puntaje Altman-Z.</p>
    {altmanZ?.score != null && <p>Puntaje: {altmanZ.score}</p>}
    <p className="health-explanation">El indicador combina liquidez, beneficios, endeudamiento y ventas del balance de referencia.</p>
  </section>
}
