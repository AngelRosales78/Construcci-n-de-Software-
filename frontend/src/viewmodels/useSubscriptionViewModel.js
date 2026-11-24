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
  const loadSequence = useRef(0)
  const pending = useRef(null)
  const working = useRef(false)
  const external = data?.plans?.payment_provider === 'mercadopago_test'
  const currentPayment = data?.payment_orders?.[0] || null

  const refresh = useCallback(async () => {
    const session = generation.current
    const id = ++loadSequence.current
    setLoading(true)
    setError('')
    try {
      const result = await subscriptionRepository.load()
      if (session === generation.current && id === loadSequence.current) setData(result)
    } catch {
      if (session === generation.current && id === loadSequence.current) setError('No se pudo consultar tu suscripción. Reintenta.')
    } finally {
      if (session === generation.current && id === loadSequence.current) setLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
    const unsubscribe = subscribeSession(() => {
      generation.current++
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
        pending.current ??= external ? { request_id: currentPayment?.status === 'new' ? currentPayment.request_id : crypto.randomUUID() } : { request_id: crypto.randomUUID(), outcome }
        const result = await subscriptionRepository.checkout(pending.current)
        if (id !== generation.current) return
        pending.current = null
        setRetryOutcome(null)
        if (result.payment) {
          setCheckoutOpen(false)
          setNotice('Tu operación está preparada. Continúa en Mercado Pago para completar el pago de prueba.')
          await refresh()
          return
        }
        setReceipt(result.receipt)
        setCheckoutOpen(false)
        setNotice(result.receipt.outcome === 'approved'
          ? 'Tu acceso Pro está activo.' : 'No se confirmó la activación. Puedes volver a intentarlo.')
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
      if (failure.response?.status >= 400 && failure.response.status < 500) pending.current = null
      setRetryOutcome(pending.current ? external ? 'mercadopago' : pending.current.outcome : null)
      setError(detail || 'No se confirmó la operación. Reintenta para verificar el mismo intento.')
    } finally {
      working.current = false
      setBusy(false)
    }
  }

  const pro = data?.subscription.effective_plan === 'pro'
  const checkPayment = async () => {
    if (working.current || !currentPayment) return
    working.current=true;setBusy(true);setError('')
    const id=generation.current
    try {
      const result=await subscriptionRepository.refreshPayment(currentPayment.id)
      if(id!==generation.current)return
      setNotice(result.payment.status === 'approved' ? result.subscription?.effective_plan === 'pro' ? 'Tu pago de prueba fue confirmado. Pro está activo.' : 'El pago ya fue confirmado. Consulta la vigencia de tu plan.' : result.payment.status_label)
      await refresh()
    } catch(failure) {
      if(id===generation.current)setError(failure.response?.data?.detail || 'No se pudo verificar el pago. Tu acceso no cambia hasta confirmar el resultado.')
    } finally {working.current=false;setBusy(false)}
  }
  return { data, pro, loading, busy, error, notice, receipt, setReceipt,
    external, currentPayment, checkPayment,
    checkoutOpen, setCheckoutOpen, cancelOpen, setCancelOpen, retryOutcome, refresh,
    checkout: outcome => perform(outcome), cancel: () => perform(null) }
}
