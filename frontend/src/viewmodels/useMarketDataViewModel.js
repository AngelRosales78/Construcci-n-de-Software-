import { useEffect, useRef, useState } from 'react'
import { adminRepository } from '../models/adminRepository'
import { currentOwner, errorMessage } from '../models/sprint3Repository'

export function useMarketDataViewModel(owner, reload) {
  const [ticker, setTicker] = useState('')
  const [sector, setSector] = useState('')
  const [verification, setVerification] = useState(null)
  const [pending, setPending] = useState(null)
  const [attempt, setAttempt] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const working = useRef(false)
  useEffect(() => {
    setTicker(''); setSector(''); setVerification(null); setPending(null)
    setAttempt(null); setError(''); setMessage(''); setBusy(false)
  }, [owner])
  const perform = async action => {
    if (working.current || action.owner !== currentOwner()) return
    working.current = true; setBusy(true); setError(''); setMessage(''); setAttempt(action)
    try {
      const result = action.kind === 'refresh' ? await adminRepository.refreshPrices(action.company.id, action.request_id)
        : action.kind === 'verify' ? await adminRepository.verifyCompany(action.data)
        : await adminRepository.createCompany(action.verification_id)
      if (action.owner !== currentOwner()) return
      if (result.operation.status === 'running') {
        setPending(null)
        setMessage('La consulta sigue en curso. Reintenta la misma operación para recuperar su resultado.')
        return
      }
      setAttempt(null); setPending(null); setMessage(result.operation.message)
      if (action.kind === 'verify') setVerification(result.operation)
      else { setVerification(null); reload() }
    } catch (failure) {
      if (action.owner !== currentOwner()) return
      setError(errorMessage(failure))
      setPending(null)
      if (failure.response?.data?.operation) reload()
      // Si no hay confirmación del servidor, se conserva el mismo identificador.
      if (failure.response?.data?.operation?.status === 'failed'
          || (failure.response?.status >= 400 && failure.response?.status < 500)) setAttempt(null)
    } finally {
      working.current = false
      if (action.owner === currentOwner()) setBusy(false)
    }
  }
  const verify = () => {
    if (!ticker.trim() || !sector) { setError('Introduce un ticker y selecciona su sector.'); return }
    setVerification(null)
    return perform({ kind: 'verify', owner: currentOwner(), data: {
      ticker: ticker.trim().toUpperCase(), sector, request_id: crypto.randomUUID() } })
  }
  const change = (setter, value) => {
    setter(value); setVerification(null); setAttempt(null); setError(''); setMessage('')
  }
  return { ticker, sector, verification, pending, busy, error, message, attempt, verify,
    setTicker: value => change(setTicker, value), setSector: value => change(setSector, value),
    chooseRefresh: company => { setPending({ kind: 'refresh', owner: currentOwner(), company, request_id: crypto.randomUUID() }); setAttempt(null); setError('') },
    chooseCreate: () => { if (verification) setPending({ kind: 'create', owner: currentOwner(), verification_id: verification.id }); setError('') },
    cancel: () => { setPending(null); setError('') },
    confirm: () => pending && perform(pending), retry: () => attempt && perform(attempt) }
}
