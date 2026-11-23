import { useState } from 'react'
import { adminRepository } from '../models/adminRepository'
import { currentOwner, errorMessage } from '../models/sprint3Repository'
import { useRemoteResource } from './useRemoteResource'

export function useMonitoringViewModel() {
  const today = new Date().toLocaleDateString('en-CA')
  const [section, setSection] = useState('ai')
  const [start, setStart] = useState(today.slice(0, 7) + '-01')
  const [end, setEnd] = useState(today)
  const [month, setMonth] = useState(today.slice(0, 7))
  const [level, setLevel] = useState('all')
  const [page, setPage] = useState(1)
  const [pending, setPending] = useState(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [logId, setLogId] = useState(null)
  const log = useRemoteResource(logId ? `/admin/monitoring/errors/${logId}/` : null, Boolean(logId))
  const suffix = section === 'ai' ? `?${new URLSearchParams({ start, end })}`
    : section === 'subscriptions' ? `?${new URLSearchParams({ month })}`
      : section === 'errors' ? `?${new URLSearchParams({ level, page })}` : ''
  const resource = useRemoteResource(`/admin/monitoring/${section}/${suffix}`)
  const saveBudget = async event => {
    event.preventDefault()
    if (!pending || busy) return
    const owner = currentOwner()
    setBusy(true); setError(''); setMessage('')
    try {
      await adminRepository.updateBudget(pending.provider, { version: pending.version, daily_limit: Number(pending.limit), reset_timezone: pending.timezone })
      if (owner !== currentOwner()) return
      setPending(null); setMessage('Presupuesto actualizado.'); resource.reload()
    } catch (failure) { if (owner === currentOwner()) setError(errorMessage(failure)) }
    finally { if (owner === currentOwner()) setBusy(false) }
  }
  return { section, choose: id => { setSection(id); setPage(1); setPending(null); setLogId(null); setError(''); setMessage('') },
    log, logId, setLogId,
    start, setStart, end, setEnd, month, setMonth, level, setLevel: value => { setLevel(value); setPage(1) },
    page, setPage, pending, setPending, busy, message, error, saveBudget, resource }
}
