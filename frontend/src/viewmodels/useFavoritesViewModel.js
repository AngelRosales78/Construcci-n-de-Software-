import { useCallback, useEffect, useRef, useState } from 'react'
import { favoritesRepository } from '../models/favoritesRepository'

export function useFavoritesViewModel() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [ready, setReady] = useState(false)
  const [pending, setPending] = useState([])
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const alive = useRef(false)
  const revision = useRef(0)
  const saving = useRef(new Set())

  const reload = useCallback(async () => {
    const id = ++revision.current
    setLoading(true)
    setError('')
    try {
      const rows = await favoritesRepository.list()
      if (alive.current && id === revision.current) {
        setItems(rows); setReady(true)
      }
    } catch {
      if (alive.current && id === revision.current) {
        setReady(false)
        setError('No se pudieron consultar tus favoritos. Reintenta antes de guardar cambios.')
      }
    } finally {
      if (alive.current && id === revision.current) setLoading(false)
    }
  }, [])

  useEffect(() => {
    alive.current = true
    reload()
    return () => { alive.current = false; revision.current++ }
  }, [reload])

  const add = async ticker => {
    if (!ready || saving.current.has(ticker) || items.some(item => item.ticker === ticker)) return
    saving.current.add(ticker)
    revision.current++
    setLoading(false)
    setPending([...saving.current]); setError(''); setMessage('')
    try {
      const favorite = await favoritesRepository.add(ticker)
      if (alive.current) {
        revision.current++
        setItems(rows => [favorite, ...rows.filter(row => row.ticker !== ticker)])
        setMessage(`${ticker} guardado en tus favoritos.`)
      }
    } catch {
      if (alive.current) setError(`No se pudo confirmar el guardado de ${ticker}. Reintenta; no se crearán duplicados.`)
    } finally {
      saving.current.delete(ticker)
      if (alive.current) setPending([...saving.current])
    }
  }
  return { items, loading, ready, pending, error, message, reload, add,
    isSaved: ticker => items.some(item => item.ticker === ticker) }
}
