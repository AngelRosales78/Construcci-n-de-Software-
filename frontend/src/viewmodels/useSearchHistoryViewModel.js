import { useState, useEffect } from 'react'
import { historyRepository } from '../models/historyRepository'

export function useSearchHistoryViewModel({ onSelectHistory, refreshKey = 0 }) {
  const key = historyRepository.accountKey()
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [version, setVersion] = useState(0)
  const [busy, setBusy] = useState(false)
  const persist = rows => historyRepository.persist(key, rows)
  useEffect(() => {
    let active = true
    setError('')
    setLoading(true)
    setHistory([])
    historyRepository.list().then((data) => {
      if (!active) return
      const rows = (data.results || []).slice(0, 10)
      setHistory(rows)
      persist(rows)
    }).catch(() => {
      if (!active) return
      setHistory(historyRepository.read(key))
      setError('Sin conexión. Se muestra el historial guardado de esta cuenta.')
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [key, refreshKey, version])

  const remove = async id => {
    setBusy(true)
    setError('')
    try {
      await historyRepository.remove(id)
      const rows = id == null ? [] : history.filter(item => item.id !== id)
      setHistory(rows)
      persist(rows)
      setVersion(value => value + 1)
    } catch { setError('No se pudo borrar el historial. Intenta nuevamente.') }
    finally { setBusy(false) }
  }
  return { history, loading, error, busy, remove }
}
