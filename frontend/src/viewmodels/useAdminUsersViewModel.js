import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { adminRepository } from '../models/adminRepository'
import { currentOwner, errorMessage } from '../models/sprint3Repository'
import { useRemoteResource } from './useRemoteResource'

export function useAdminUsersViewModel() {
  const [params, setParams] = useSearchParams()
  const resource = useRemoteResource(`/admin/users/?${params}`)
  const [pending, setPending] = useState(null)
  const [busy, setBusy] = useState(false)
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  useEffect(() => { setPending(null); setReason(''); setError(''); setMessage('') }, [resource.owner])
  const update = values => setParams(previous => {
    const next = new URLSearchParams(previous)
    next.set('page', '1')
    for (const [key, value] of Object.entries(values)) next.set(key, value)
    return next
  })
  const choose = (user, changes) => { setPending({ user, changes }); setReason(''); setError(''); setMessage('') }
  const confirm = async () => {
    if (!pending || busy) return
    const owner = currentOwner()
    setBusy(true); setError('')
    try {
      const changes = { ...pending.changes }
      if (changes.is_active === false) changes.reason = reason
      await adminRepository.updateUser(pending.user.id, changes)
      if (owner !== currentOwner()) return
      setPending(null); setMessage('Cambio guardado y registrado.'); resource.reload()
    } catch (failure) { if (owner === currentOwner()) setError(errorMessage(failure)) }
    finally { setBusy(false) }
  }
  return { resource, params, update, pending, choose, busy, reason, setReason, error, message, confirm,
    cancel: () => { setPending(null); setReason(''); setError('') },
    go: direction => setParams(previous => {
      const next = new URLSearchParams(previous)
      next.set('page', String(Math.max(1, Number(previous.get('page') || 1) + direction)))
      return next
    }) }
}
