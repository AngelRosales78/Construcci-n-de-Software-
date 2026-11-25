export default function ScoreComparison({ score, side, comparable }) {
  const value = Number(score?.value)
  const hasValue = typeof score?.value === 'number' || (typeof score?.value === 'string' && score.value.trim() !== '')
  const ready = score?.status === 'available'
  const showBar = comparable && ready && hasValue && Number.isFinite(value) && value >= 0 && value <= 100
  return <div className={`comparison-score comparison-score-${side}`}>
    <span>{ready ? `${score.display_value} / 100` : 'No disponible'}</span>
    {showBar && <span className="comparison-score-track" aria-hidden="true"><span style={{ width: `${value}%` }} /></span>}
  </div>
}
