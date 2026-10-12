import { useState, useEffect } from 'react'
import api from '../../services/api'

export default function SearchHistory({ onSelectHistory, refreshKey = 0 }) {
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    let active = true
    setLoading(true)
    api.get('/companies/history/').then(({ data }) => {
      if (active) setHistory(data.results || [])
    }).catch(() => {
      if (active) setHistory([])
    }).finally(() => {
      if (active) setLoading(false)
    })
    return () => { active = false }
  }, [refreshKey])

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
            className="search-history-item"
          >
            <button type="button" onClick={() => handleSelect(item.query)}>{item.query}</button>
          </li>
        ))}
      </ul>
    </div>
  )
}
