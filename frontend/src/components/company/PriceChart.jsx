import { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Area,
  AreaChart,
} from 'recharts'
import api from '../../services/api'

export default function PriceChart() {
  const { ticker } = useParams()
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    const fetchHistory = async () => {
      setLoading(true)
      try {
        const response = await api.get(`/companies/${ticker}/history/`)
        setHistory(response.data.history || [])
      } catch (err) {
        setError('Error al cargar el historial de precios')
      } finally {
        setLoading(false)
      }
    }
    fetchHistory()
  }, [ticker])

  if (loading) return <div className="price-chart-loading">Cargando historial...</div>
  if (error) return <div className="price-chart-error">{error}</div>
  if (history.length === 0) return null

  return (
    <div className="price-chart">
      <h3>Historial de Precios (30 días)</h3>
      <ResponsiveContainer width="100%" height={300}>
        <AreaChart data={history}>
          <defs>
            <linearGradient id="colorPrice" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3} />
              <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="date" tick={{ fontSize: 12 }} />
          <YAxis tick={{ fontSize: 12 }} />
          <Tooltip />
          <Area
            type="monotone"
            dataKey="price"
            stroke="#3b82f6"
            fillOpacity={1}
            fill="url(#colorPrice)"
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
