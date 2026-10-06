import { useState, useEffect } from 'react'
import api from '../../services/api'

export default function SearchHistory({ onSelectHistory }) {
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchHistory()
  }, [])

  const fetchHistory = async () => {
    setLoading(true)
    try {
      const response = await api.get('/companies/history/')
      setHistory(response.data || [])
    } catch (err) {
      console.error('Error fetching history:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleSelect = (query) => {
    onSelectHistory(query)
  }

  if (loading) return <div className="search-history-loading">Cargando historial...</div>

  if (history.length === 0) return null

  return (
    <div className="search-history">
      <h3>Búsquedas recientes</h3>
      <ul className="search-history-list">
        {history.map((item) => (
          <li
            key={item.id}
            onClick={() => handleSelect(item.query)}
            className="search-history-item"
          >
            {item.query}
          </li>
        ))}
      </ul>
    </div>
  )
}
