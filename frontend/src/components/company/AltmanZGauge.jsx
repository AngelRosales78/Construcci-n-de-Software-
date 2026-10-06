import { PieChart, Pie, Cell, ResponsiveContainer } from 'recharts'

export default function AltmanZGauge({ score, zone }) {
  const getZoneColor = (z) => {
    if (z === 'safe') return '#22c55e'
    if (z === 'grey') return '#eab308'
    if (z === 'distress') return '#ef4444'
    return '#6b7280'
  }

  const color = getZoneColor(zone)

  const data = [
    { name: 'Score', value: Math.max(0, score) },
    { name: 'Rest', value: Math.max(0, 5 - score) },
  ]

  return (
    <div className="altmanz-gauge">
      <h3>Altman-Z Score</h3>
      <div className="gauge-container">
        <ResponsiveContainer width="100%" height={200}>
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              startAngle={180}
              endAngle={0}
              innerRadius={60}
              outerRadius={80}
              paddingAngle={5}
              dataKey="value"
            >
              <Cell fill={color} />
              <Cell fill="#e5e7eb" />
            </Pie>
          </PieChart>
        </ResponsiveContainer>
        <div className="gauge-value">
          <span className="gauge-score" style={{ color }}>{score}</span>
        </div>
      </div>
      <div className="gauge-labels">
        <span className="gauge-label">0</span>
        <span className="gauge-label">1.81</span>
        <span className="gauge-label">2.99</span>
        <span className="gauge-label">5</span>
      </div>
    </div>
  )
}
