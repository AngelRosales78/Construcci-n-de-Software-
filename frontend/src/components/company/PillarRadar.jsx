import { Radar, RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, ResponsiveContainer } from 'recharts'

const IDS = { Liquidez: 'liquidity', Rentabilidad: 'profitability', Solvencia: 'solvency', Eficiencia: 'efficiency', Crecimiento: 'growth' }

function AxisLink({ x, y, payload }) {
  return <g transform={`translate(${x},${y})`}>
    <a href={`#pillar-${IDS[payload.value]}`} aria-label={`Abrir detalle de ${payload.value}`}>
      <text textAnchor="middle" dy={4} fill="var(--primary)" fontSize={12}>{payload.value}</text>
    </a>
  </g>
}

export default function PillarRadar({ data }) {
  const entries = data.pillars.map(p => ({ id: p.id, label: p.label,
    value: p.score?.status === 'available' && p.score.value !== null ? Number(p.score.value) : null,
    display: p.score?.display_value || 'No disponible', score: p.score }))
  const complete = entries.length === 5 && entries.every(p => p.value !== null && Number.isFinite(p.value) && p.value >= 0 && p.value <= 100)
  return <section className="pillar-radar-section" aria-labelledby="pillar-radar-title">
    <h3 id="pillar-radar-title">Los cinco pilares en una escala común</h3>
    <p className="pillar-muted">Notas académicas de 0 a 100 para el ejercicio {data.fiscal_year || 'sin datos'}. Selecciona un pilar para abrir su detalle. Las notas dependen de las reglas mostradas abajo.</p>
    <div className="pillar-radar-layout">
      <figure className="pillar-radar-figure" aria-label="Radar de los cinco pilares financieros">
        <ResponsiveContainer width="100%" height={340}>
          <RadarChart data={entries} outerRadius="60%" margin={{ top: 24, right: 22, bottom: 20, left: 22 }}>
            <PolarGrid stroke="var(--terminal-line)" />
            <PolarAngleAxis dataKey="label" tick={<AxisLink />} />
            <PolarRadiusAxis domain={[0, 100]} ticks={[0, 25, 50, 75, 100]} tick={{ fill: 'var(--terminal-muted)', fontSize: 10 }} axisLine={false} />
            {complete && <Radar name="Nota académica" dataKey="value" stroke="var(--primary)" fill="var(--primary)" fillOpacity={0.2} dot isAnimationActive={false} />}
          </RadarChart>
        </ResponsiveContainer>
        {!complete && <figcaption>El polígono se dibuja cuando existen cinco notas válidas. Los datos ausentes no se representan como cero.</figcaption>}
      </figure>
      <div className="pillar-radar-notes"><table>
        <caption>Notas de los pilares del ejercicio seleccionado</caption>
        <thead><tr><th scope="col">Pilar</th><th scope="col">Nota de 0 a 100</th></tr></thead>
        <tbody>{entries.map(p => <tr key={p.id}><th scope="row"><a href={`#pillar-${p.id}`}>{p.label}</a></th><td>{p.value === null ? 'No disponible' : p.display}</td></tr>)}</tbody>
      </table></div>
    </div>
    <details className="pillar-details"><summary>Cómo se calcula cada nota</summary>
      <p>{data.scoring_policy?.explanation}</p>
      <p>{data.scoring_policy?.pillar_aggregation}</p>
      <p>Versión: {data.scoring_policy?.version || 'No informada'} · {data.scoring_policy?.rounding}</p>
      {entries.map(p => <section key={p.id}><h4>{p.label}</h4>
        {p.score?.status !== 'available' && <p>{p.score?.explanation || 'Falta información para calcular esta nota.'}</p>}
        <ul>{p.score?.parts?.map(part => <li key={part.id}>{part.label}: valor {part.input_value}, nota {part.display_value}. Anclajes (indicador → nota): {part.anchors.map(point => `${point.input} → ${point.score}`).join('; ')}.</li>)}</ul>
        {p.score?.excluded_metrics?.map(item => <p key={item.id}>Indicador excluido por no aplicar: {item.explanation}</p>)}
      </section>)}
    </details>
  </section>
}
