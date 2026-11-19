import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { adminRepository } from '../models/adminRepository'
import { currentOwner, errorMessage } from '../models/sprint3Repository'
import { useRemoteResource } from './useRemoteResource'

export function useAdminCompaniesViewModel() {
  const [params, setParams] = useSearchParams()
  const resource = useRemoteResource(`/admin/companies/?${params}`)
  const sectors = useRemoteResource('/companies/sectors/')
  const [pending, setPending] = useState(null)
  const [form, setForm] = useState({})
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const working = useRef(false)
  useEffect(() => { setPending(null); setForm({}); setError(''); setMessage('') }, [resource.owner])
  const update = values => setParams(previous => {
    const next = new URLSearchParams(previous)
    next.set('page', '1')
    for (const [key, value] of Object.entries(values)) next.set(key, value)
    return next
  })
  const choose = (company, edit = false) => {
    setPending({ company, edit }); setError(''); setMessage('')
    setForm(edit ? { description: company.description, sector: company.sector, website_url: company.website_url }
      : { is_active: !company.is_active })
  }
  const confirm = async () => {
    if (!pending || working.current) return
    const owner = currentOwner()
    working.current = true; setBusy(true); setError('')
    try {
      await adminRepository.updateCompany(pending.company.id, form)
      if (owner !== currentOwner()) return
      setPending(null); setMessage('Empresa guardada. Los buscadores usan el catálogo actualizado.'); resource.reload()
    } catch (failure) { if (owner === currentOwner()) setError(errorMessage(failure)) }
    finally { working.current = false; setBusy(false) }
  }
  return { resource, sectors, params, update, pending, form, choose, confirm, busy, error, message,
    field: (key, value) => setForm(previous => ({ ...previous, [key]: value })),
    cancel: () => { setPending(null); setForm({}); setError('') },
    go: direction => setParams(previous => {
      const next = new URLSearchParams(previous)
      next.set('page', String(Math.max(1, Number(previous.get('page') || 1) + direction)))
      return next
    }) }
}
