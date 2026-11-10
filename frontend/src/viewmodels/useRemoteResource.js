import { useCallback, useEffect, useState } from 'react'
import { currentOwner, errorMessage, sprint3Repository } from '../models/sprint3Repository'
import { subscribeSession } from '../services/sessionStore'

// Un solo patrón de carga, cancelación y aislamiento para los recursos del Sprint 3.
export function useRemoteResource(path, enabled = true, poll = 0) {
  const [owner, setOwner] = useState(currentOwner)
  const [attempt, setAttempt] = useState(0)
  const [state, setState] = useState({})
  const reload = useCallback(() => setAttempt(n => n + 1), [])
  useEffect(() => subscribeSession(() => setOwner(currentOwner())), [])
  useEffect(() => {
    if (!enabled || !path) return
    const controller = new AbortController()
    setState(old => ({ ...old, loading: true, error: '' }))
    sprint3Repository.get(path, controller.signal).then(data => {
      if (!controller.signal.aborted && owner === currentOwner()) setState({ path, owner, data, loading: false, error: '' })
    }).catch(error => {
      if (!controller.signal.aborted) setState({ path, owner, data: null, error: errorMessage(error), loading: false })
    })
    return () => controller.abort()
  }, [path, enabled, attempt, owner])
  useEffect(() => {
    if (!enabled || !poll) return
    const refresh = () => { if (!document.hidden) reload() }
    const id = setInterval(refresh, poll)
    window.addEventListener('focus', refresh)
    return () => { clearInterval(id); window.removeEventListener('focus', refresh) }
  }, [enabled, poll, reload])
  const data = enabled && state.path === path && state.owner === owner ? state.data : null
  return { data, loading: enabled && (state.loading || state.path !== path),
    error: enabled && state.path === path && state.owner === owner ? state.error : '', reload, owner }
}
