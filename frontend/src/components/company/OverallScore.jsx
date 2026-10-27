export default function OverallScore({ score, fiscalYear }) {
  const ready = score.status === 'available'
  return <section className="overall-score" aria-labelledby="overall-score-title">
    <div><h3 id="overall-score-title">Puntaje general de los cinco pilares</h3>
      <p className="pillar-muted">Ejercicio {fiscalYear || 'sin datos'} · {score.period?.start || 'Sin fecha inicial'} a {score.period?.end || 'Sin fecha final'}</p>
      <p className="overall-score-value">{ready ? score.display_value : 'No disponible'}{ready && <small> / 100</small>}</p>
      <p className="pillar-muted">Promedio de cinco notas académicas. No es una probabilidad de ganancia ni el puntaje Altman-Z.</p>
      {!ready && <p>{score.explanation}</p>}
      {!!score.missing_pillars?.length && <p>Notas pendientes: {score.contributions.filter(p => score.missing_pillars.includes(p.id)).map(p => p.label).join(', ')}.</p>}
    </div>
    <details className="pillar-details"><summary>Ver pesos y cálculo del puntaje</summary>
      <p>Promedio académico de cinco notas con igual peso. Es distinto de Altman Z y no expresa una probabilidad de ganancia.</p>
      <p>{score.aggregation}</p><p>Versión: {score.version}</p>
      <ul>{score.contributions?.map(p => <li key={p.id}><a href={`#pillar-${p.id}`}>{p.label}</a>Nota {p.display_value} · Peso {p.weight_display}</li>)}</ul>
    </details>
  </section>
}
