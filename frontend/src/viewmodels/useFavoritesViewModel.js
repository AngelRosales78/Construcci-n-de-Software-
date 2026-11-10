import { useCallback, useEffect, useRef, useState } from 'react'
import { favoritesRepository } from '../models/favoritesRepository'
import { subscribeSession } from '../services/sessionStore'

export function useFavoritesViewModel() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [ready, setReady] = useState(false)
  const [pending, setPending] = useState([])
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [removing, setRemoving] = useState(null)
  const [lastSynced, setLastSynced] = useState(null)
  const instance = useRef(Symbol('favorites'))
  const fetching = useRef(false)
  const account = () => localStorage.getItem('user') || ''
  const alive = useRef(false)
  const revision = useRef(0)
  const saving = useRef(new Set())

  const reload = useCallback(async (quiet = false) => {
    if (fetching.current || saving.current.size) return null
    const id = ++revision.current
    fetching.current = id
    if (quiet !== true) setLoading(true)
    setError('')
    try {
      const rows = await favoritesRepository.list()
      if (alive.current && id === revision.current) {
        setItems(rows); setReady(true); setLastSynced(new Date().toISOString())
      }
      return rows
    } catch (failure) {
      if (alive.current && id === revision.current) {
        setReady(false)
        if ([401, 403].includes(failure.response?.status)) setItems([])
        setError('No se pudieron consultar tus favoritos. Reintenta antes de guardar cambios.')
      }
    } finally {
      if (fetching.current === id) fetching.current = false
      if (alive.current && id === revision.current) setLoading(false)
    }
    return null
  }, [])

  useEffect(() => {
    alive.current = true
    reload()
    const refresh = () => { if (!document.hidden) reload(true) }
    const changed = event => { if (event.detail?.source !== instance.current) refresh() }
    let owner = account()
    const unsubscribe = subscribeSession(() => {
      const next = account()
      if (next === owner) return
      owner = next; revision.current++; setItems([]); setReady(false); setRemoving(null); setLastSynced(null)
      fetching.current = false; saving.current.clear(); setPending([]); setLoading(false); setError(''); setMessage('')
      if (next) refresh()
    })
    const timer = setInterval(refresh, 2000)
    window.addEventListener('focus', refresh)
    document.addEventListener('visibilitychange', refresh)
    window.addEventListener('sp500-favorites-changed', changed)
    return () => {
      alive.current = false; revision.current++; clearInterval(timer); unsubscribe()
      window.removeEventListener('focus', refresh)
      document.removeEventListener('visibilitychange', refresh)
      window.removeEventListener('sp500-favorites-changed', changed)
    }
  }, [reload])

  const add = async ticker => {
    if (!ready || saving.current.has(ticker) || items.some(item => item.ticker === ticker)) return
    saving.current.add(ticker)
    const owner = account()
    revision.current++
    setLoading(false)
    setPending([...saving.current]); setError(''); setMessage('')
    try {
      const favorite = await favoritesRepository.add(ticker)
      if (alive.current && owner === account()) {
        revision.current++
        setItems(rows => [favorite, ...rows.filter(row => row.ticker !== ticker)])
        setMessage(`${ticker} guardado en tus favoritos.`)
      }
    } catch {
      if (alive.current && owner === account()) setError(`No se pudo confirmar el guardado de ${ticker}. Reintenta; no se crearán duplicados.`)
    } finally {
      if (owner === account()) {
        saving.current.delete(ticker)
        if (alive.current) setPending([...saving.current])
      }
      window.dispatchEvent(new CustomEvent('sp500-favorites-changed', { detail: { source: instance.current } }))
    }
  }
  const confirmRemove = async () => {
    const selected = removing
    const owner = account()
    if (!ready || !selected || saving.current.has(selected.ticker)) return
    saving.current.add(selected.ticker); revision.current++
    setPending([...saving.current]); setError(''); setMessage('')
    try {
      await favoritesRepository.remove(selected.id)
      if (alive.current && owner === account()) {
        setItems(rows => rows.filter(row => row.id !== selected.id))
        setRemoving(null); setMessage(`${selected.ticker} eliminado de tus favoritos.`)
      }
    } catch (failure) {
      if (!alive.current || owner !== account()) return
      if (failure.response?.status === 404) {
        saving.current.delete(selected.ticker)
        const rows = await reload()
        if (rows) { setRemoving(null); setMessage('La lista se actualizó con el estado confirmado por el servidor.') }
      } else setError('No se pudo confirmar la eliminación. El favorito se conserva hasta verificar el servidor.')
    } finally {
      if (owner === account()) {
        saving.current.delete(selected.ticker)
        if (alive.current) setPending([...saving.current])
      }
      window.dispatchEvent(new CustomEvent('sp500-favorites-changed', { detail: { source: instance.current } }))
    }
  }
  return { items, loading, ready, pending, error, message, reload, add, removing, confirmRemove, lastSynced,
    requestRemove: ticker => setRemoving(items.find(row => row.ticker === ticker) || null),
    cancelRemove: () => setRemoving(null),
    isSaved: ticker => items.some(item => item.ticker === ticker) }
}
