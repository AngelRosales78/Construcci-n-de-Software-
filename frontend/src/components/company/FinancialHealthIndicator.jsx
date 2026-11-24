export default function FinancialHealthIndicator({ altmanZ, asOf }) {
  const defaults = {
    safe: { label: 'Zona favorable según Altman-Z', color: 'green' },
    grey: { label: 'Zona intermedia según Altman-Z', color: 'orange' },
    distress: { label: 'Zona de atención según Altman-Z', color: 'red' },
  }
  const data = defaults[altmanZ?.zone] || { label: altmanZ?.availability?.label || 'Sin datos suficientes', color: 'grey' }
  return <section className={`financial-health health-${data.color}`} aria-label="Perfil financiero">
    <span className="section-eyebrow">LECTURA DEL BALANCE</span>
    <h3>Perfil financiero</h3><strong>{data.label}</strong>
    <p>Clasificación académica; no es una garantía de solvencia.</p>
    {altmanZ?.score != null && <p>Puntaje: {altmanZ.score} · No es un porcentaje.</p>}
    {altmanZ?.score != null && (altmanZ?.reference?.period_end || asOf) && <p>Balance del {altmanZ.reference?.period_end || asOf}</p>}
    <p className="health-explanation">{altmanZ?.score != null ? 'Altman-Z resume parte de la situación del balance. No indica si conviene comprar la acción.' : altmanZ?.availability?.explanation || 'Faltan componentes compatibles del balance y su valoración al cierre. Sin ellos no podemos asignar una zona.'}</p>
  </section>
}
