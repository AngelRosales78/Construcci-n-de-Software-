import { useCallback, useEffect, useRef, useState } from 'react'
import { subscriptionRepository } from '../models/subscriptionRepository'
import { subscribeSession } from '../services/sessionStore'

export function useSubscriptionViewModel() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [receipt, setReceipt] = useState(null)
  const [checkoutOpen, setCheckoutOpen] = useState(false)
  const [cancelOpen, setCancelOpen] = useState(false)
  const [retryOutcome, setRetryOutcome] = useState(null)
  const generation = useRef(0)
  const pending = useRef(null)
  const working = useRef(false)

  const refresh = useCallback(async () => {
    const id = ++generation.current
    setLoading(true)
    setError('')
    try {
      const result = await subscriptionRepository.load()
      if (id === generation.current) setData(result)
    } catch {
      if (id === generation.current) setError('No se pudo consultar tu suscripción. Reintenta.')
    } finally {
      if (id === generation.current) setLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
    const unsubscribe = subscribeSession(() => {
      setData(null); setReceipt(null); setNotice(''); pending.current = null
      setRetryOutcome(null)
      setCheckoutOpen(false); setCancelOpen(false)
      refresh()
    })
    window.addEventListener('focus', refresh)
    return () => { generation.current++; unsubscribe(); window.removeEventListener('focus', refresh) }
  }, [refresh])

  useEffect(() => {
    const end = Date.parse(data?.subscription.expires_at)
    if (!data?.subscription.active || !Number.isFinite(end) || end <= Date.now()) return
    const timer = setTimeout(refresh, Math.min(end - Date.now() + 100, 2147483647))
    return () => clearTimeout(timer)
  }, [data, refresh])

  const perform = async (outcome) => {
    if (working.current) return
    working.current = true
    setBusy(true); setError(''); setNotice('')
    const id = generation.current
    try {
      if (outcome) {
        // Un reintento de red conserva el identificador y resultado originales.
        pending.current ??= { request_id: crypto.randomUUID(), outcome }
        const result = await subscriptionRepository.checkout(pending.current)
        if (id !== generation.current) return
        pending.current = null
        setRetryOutcome(null)
        setReceipt(result.receipt)
        setCheckoutOpen(false)
        setNotice(result.receipt.outcome === 'approved'
          ? 'Simulación aprobada. Tu plan Pro está activo.' : 'Simulación rechazada. Puedes intentar otra operación ficticia.')
      } else {
        await subscriptionRepository.cancel()
        if (id !== generation.current) return
        setCancelOpen(false)
        setNotice('Renovación cancelada. Conservas Pro hasta el vencimiento indicado.')
      }
      await refresh()
    } catch (failure) {
      if (id !== generation.current) return
      const detail = failure.response?.data?.detail
      if (failure.response && pending.current) pending.current = null
      setRetryOutcome(pending.current?.outcome || null)
      setError(detail || 'No se confirmó la operación. Reintenta para verificar el mismo intento.')
    } finally {
      working.current = false
      setBusy(false)
    }
  }

  const pro = data?.subscription.effective_plan === 'pro'
  return { data, pro, loading, busy, error, notice, receipt, setReceipt,
    checkoutOpen, setCheckoutOpen, cancelOpen, setCancelOpen, retryOutcome, refresh,
    checkout: outcome => perform(outcome), cancel: () => perform(null) }
}
