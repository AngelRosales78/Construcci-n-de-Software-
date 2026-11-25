const zones = [
  { id: 'distress', label: 'Atención', range: 'Menor que 1,81', color: 'red', title: 'Zona de atención según Altman-Z', meaning: 'La fórmula señala una zona de mayor riesgo financiero. Conviene revisar las deudas, la liquidez y el contexto del negocio.' },
  { id: 'grey', label: 'Intermedia', range: 'De 1,81 a 2,99', color: 'orange', title: 'Zona intermedia según Altman-Z', meaning: 'La fórmula no da una señal clara. Revisa los demás indicadores y el contexto del negocio antes de sacar conclusiones.' },
  { id: 'safe', label: 'Favorable', range: 'Mayor que 2,99', color: 'green', title: 'Zona favorable según Altman-Z', meaning: 'Las cifras usadas en esta fórmula quedan en su zona de menor riesgo financiero. Esto no evalúa si la acción está cara o barata.' },
]
export default function FinancialHealthIndicator({ altmanZ, asOf, onReviewIndicators }) {
  const known = Number.isFinite(altmanZ?.score) && zones.some(zone => zone.id === altmanZ?.zone)
  const selected = known ? zones.find(zone => zone.id === altmanZ.zone) : null
  const date = altmanZ?.reference?.period_end || asOf
  return <section className={`financial-health altman-reading health-${selected?.color || 'grey'}`} aria-label="Perfil financiero">
    <div className="altman-reading-heading"><span className="section-eyebrow">LECTURA DEL BALANCE</span><span className="altman-status">{known ? 'Puntaje disponible' : 'Sin clasificación'}</span></div>
    <h3>¿Qué dice el balance?</h3>
    <strong>{selected?.title || altmanZ?.availability?.label || 'Sin datos suficientes'}</strong>
    {known ? <>
      <div className="altman-result"><span className="altman-score">{altmanZ.score}</span><p>Puntaje Altman-Z<br /><span>No es un porcentaje ni una nota sobre 10.</span></p></div>
      <p className="health-explanation">{selected.meaning}</p>
    </> : <p className="health-explanation">{altmanZ?.availability?.explanation || 'Faltan componentes compatibles del balance y su valoración al cierre. Sin ellos no podemos asignar una zona.'}</p>}
    {known && <ol className="altman-zones" aria-label="Zonas de Altman-Z">
      {zones.map(zone => <li key={zone.id} className={`altman-zone zone-${zone.id}`} aria-current={selected?.id === zone.id ? 'true' : undefined}>
        <span className="zone-name">{zone.label}{selected?.id === zone.id && <b aria-label="Resultado de esta empresa"> ← Aquí</b>}</span><span>{zone.range}</span>
      </li>)}
    </ol>}
    <p className="altman-limits">{known ? 'Clasificación académica; no indica cuánto ganarás ni si conviene comprar. No garantiza solvencia.' : 'Sin puntaje no podemos decir que la empresa sea segura o esté en riesgo.'}</p>
    {!known && altmanZ?.availability?.source_url && <a className="altman-review-source" href={altmanZ.availability.source_url} target="_blank" rel="noreferrer">Consultar referencia SEC</a>}
    {!known && onReviewIndicators && <button className="altman-review" onClick={onReviewIndicators}>Ver cifras disponibles <span aria-hidden="true">↓</span></button>}
    {known && date && <p className="altman-date">Cierre contable usado para Altman-Z · <time>{date}</time></p>}
  </section>
}
