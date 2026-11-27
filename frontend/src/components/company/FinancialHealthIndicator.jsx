const zones = [
  { id: 'distress', label: 'Atención', heading:'Zona de atención', range: 'Menor que 1,81', color: 'red', meaning: 'El modelo señala mayor riesgo financiero. Revisa las deudas y la liquidez.' },
  { id: 'grey', label: 'Intermedia', heading:'Zona intermedia', range: 'De 1,81 a 2,99', color: 'orange', meaning: 'El modelo no da una señal clara. Revisa las deudas y la liquidez.' },
  { id: 'safe', label: 'Favorable', heading:'Zona favorable', range: 'Mayor que 2,99', color: 'green', meaning: 'Las cifras están en la zona de menor riesgo del modelo.' },
]
export default function FinancialHealthIndicator({ altmanZ, asOf, onReviewIndicators, onReviewCalculation }) {
  const known = Number.isFinite(altmanZ?.score) && zones.some(zone => zone.id === altmanZ?.zone)
  const selected = known ? zones.find(zone => zone.id === altmanZ.zone) : null
  const date = altmanZ?.reference?.period_end || asOf
  return <section className={`financial-health altman-reading health-${selected?.color || 'grey'} ${known ? 'has-altman-result' : 'has-no-altman-result'}`} data-zone={selected?.id || 'unknown'} aria-label="Perfil financiero">
    <div className="altman-summary">
    <div className="altman-reading-heading"><span className="section-eyebrow">LECTURA DEL BALANCE</span>{!known && <span className="altman-status">Sin clasificación</span>}</div>
    <div className="altman-verdict"><span className="altman-outcome-symbol" aria-hidden="true">{selected?.id==='safe' ? '✓' : selected?.id==='distress' ? '!' : '?'}</span><div>
    <h3>{selected?.heading || 'Clasificación pendiente'}</h3>
    <strong>{known ? 'Según el modelo Altman-Z' : altmanZ?.availability?.label || 'Sin datos suficientes'}</strong>
    </div></div>
    {known ? <>
      <p className="health-explanation">{selected.meaning}</p>
    </> : <p className="health-explanation">{altmanZ?.availability?.explanation || 'Faltan componentes compatibles del balance y su valoración al cierre. Sin ellos no podemos asignar una zona.'}</p>}
    </div>
    {known && <div className="altman-result"><p>Puntaje Altman-Z</p><span className="altman-score">{altmanZ.score}</span><p>No es un porcentaje ni una nota sobre 10.</p></div>}
    {known && <ol className="altman-zones" aria-label="Zonas de Altman-Z">
      {zones.map(zone => <li key={zone.id} className={`altman-zone zone-${zone.id}`} aria-current={selected?.id === zone.id ? 'true' : undefined}>
        <span className="zone-name">{zone.label}{selected?.id === zone.id && <b aria-label="Resultado de esta empresa"> ✓ Aquí</b>}</span><span className="zone-range">{zone.range}</span>
      </li>)}
    </ol>}
    <div className="altman-reading-footer"><div><p className="altman-limits">{known ? 'No indica si conviene comprar ni garantiza solvencia.' : 'Sin puntaje no podemos decir que la empresa sea segura o esté en riesgo.'}</p>{known && date && <p className="altman-date">Cierre contable usado para Altman-Z · <time>{date}</time></p>}</div>{onReviewCalculation && <button type="button" className="text-action" onClick={onReviewCalculation}>Ver cálculo y fuentes →</button>}</div>
    {!known && altmanZ?.availability?.source_url && <a className="altman-review-source" href={altmanZ.availability.source_url} target="_blank" rel="noreferrer">Consultar referencia SEC</a>}
    {!known && onReviewIndicators && <button className="altman-review" onClick={onReviewIndicators}>Ver cifras disponibles <span aria-hidden="true">↓</span></button>}
  </section>
}
