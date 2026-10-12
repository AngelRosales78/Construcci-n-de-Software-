import { useState, useEffect } from 'react'
import api from '../../services/api'

function accountKey() {
  try {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    return user?.id != null ? `sp500:history:${user.id}` : null
  } catch { return null }
}
export default function SearchHistory({ onSelectHistory, refreshKey = 0 }) {
  const key = accountKey()
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [version, setVersion] = useState(0)
  const [busy, setBusy] = useState(false)
  const persist = rows => {
    if (!key) return
    try { localStorage.setItem(key, JSON.stringify(rows.slice(0, 10))) } catch { /* Almacenamiento deshabilitado. */ }
  }
  useEffect(() => {
    let active = true
    setError('')
    setLoading(true)
    setHistory([])
    api.get('/companies/history/').then(({ data }) => {
      if (!active) return
      const rows = (data.results || []).slice(0, 10)
      setHistory(rows)
      persist(rows)
    }).catch(() => {
      if (!active) return
      try {
        const rows = key ? JSON.parse(localStorage.getItem(key) || '[]') : []
        setHistory(Array.isArray(rows) ? rows.slice(0, 10) : [])
      } catch { setHistory([]) }
      setError('Sin conexión. Se muestra el historial guardado de esta cuenta.')
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [key, refreshKey, version])

  const remove = async id => {
    setBusy(true)
    setError('')
    try {
      await api.delete(id == null ? '/companies/history/clear/' : `/companies/history/${id}/`)
      const rows = id == null ? [] : history.filter(item => item.id !== id)
      setHistory(rows)
      persist(rows)
      setVersion(value => value + 1)
    } catch { setError('No se pudo borrar el historial. Intenta nuevamente.') }
    finally { setBusy(false) }
  }
  if (loading) return <p>Cargando historial...</p>
  return <section className="search-history" aria-label="Búsquedas recientes">
    <h3>Búsquedas recientes</h3>
    {error && <p role="status">{error}</p>}
    {!history.length && <p>No hay búsquedas recientes.</p>}
    <ul>{history.map(item => <li key={item.id}>
      <button type="button" onClick={() => onSelectHistory(item.query)}>{item.query}</button>
      <button type="button" disabled={busy} aria-label={`Eliminar ${item.query}`} onClick={() => remove(item.id)}>×</button>
    </li>)}</ul>
    {history.length > 0 && <button disabled={busy} onClick={() => remove(null)}>Limpiar historial</button>}
  </section>
}
