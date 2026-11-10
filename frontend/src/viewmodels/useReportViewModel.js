import { useCallback, useEffect, useRef, useState } from 'react'
import { currentOwner, errorMessage, sprint3Repository } from '../models/sprint3Repository'
import { subscribeSession } from '../services/sessionStore'

export function useReportViewModel(selection) {
  const [owner, setOwner] = useState(currentOwner)
  const key = `sp500-report:${owner}:${selection.a}:${selection.b || ''}:${selection.fiscal_year}`
  const [record, setRecord] = useState(null)
  const [job, setJob] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const epoch = useRef(0)
  const inFlight = useRef(false)
  const save = useCallback(value => {
    sessionStorage.setItem(key, JSON.stringify(value)); setRecord(value)
  }, [key])
  useEffect(() => subscribeSession(() => setOwner(currentOwner())), [])
  useEffect(() => {
    epoch.current++; setJob(null); setError(''); setMessage(''); setBusy(false)
    try { setRecord(JSON.parse(sessionStorage.getItem(key))) } catch { setRecord(null) }
    return () => { epoch.current++ }
  }, [key])
  const valid = useCallback((data, request, id) => {
    if (data.schema_version !== 'report-job-v1' || data.owner_id !== currentOwner() || (id && data.id !== id)
      || Object.keys(request).some(k => data.request?.[k] !== request[k])) throw new Error('El trabajo pertenece a otra selección o cuenta.')
    return data
  }, [])
  const poll = useCallback(async () => {
    if (!record?.id || inFlight.current || document.hidden) return
    inFlight.current = true
    const sequence = epoch.current
    try {
      const data = valid(await sprint3Repository.get(`/reports/jobs/${encodeURIComponent(record.id)}/`), record.request, record.id)
      if (sequence === epoch.current) { setJob(data); setError('') }
    } catch (failure) { if (sequence === epoch.current) { setError(errorMessage(failure)); setJob(null) } }
    finally { inFlight.current = false }
  }, [record, valid])
  useEffect(() => {
    if (!record?.id) return
    poll()
    const timer = setInterval(() => { if (!['ready', 'error', 'cancelled'].includes(job?.status)) poll() }, 500)
    window.addEventListener('focus', poll)
    return () => { clearInterval(timer); window.removeEventListener('focus', poll) }
  }, [record, job?.status, poll])
  async function submit(options, retry = false) {
    if (busy) return
    if (!retry && record?.request && !['ready', 'error', 'cancelled'].includes(job?.status)) {
      setError('Hay una solicitud pendiente de confirmar. Usa Reintentar misma solicitud para evitar duplicados.')
      return
    }
    const sequence = epoch.current
    const request = retry && record?.request ? record.request : {
      request_id: crypto.randomUUID(), kind: selection.b ? 'comparison' : 'company', a: selection.a,
      b: selection.b || null, fiscal_year: selection.fiscal_year, ...options,
    }
    if (!retry) { save({ request }); setJob(null) }
    setBusy(true); setError(''); setMessage('')
    try {
      const data = valid(await sprint3Repository.send('post', '/reports/jobs/', request), request)
      if (sequence === epoch.current) { save({ id: data.id, request }); setJob(data) }
    } catch (failure) { if (sequence === epoch.current) {
      if ([400, 401, 403, 404, 422].includes(failure.response?.status)) {
        save(null); setError(errorMessage(failure))
      } else setError(errorMessage(failure) + ' Reintentar conserva el identificador de la solicitud.')
    } }
    finally { if (sequence === epoch.current) setBusy(false) }
  }
  async function cancel() {
    if (!job || busy) return
    const sequence = epoch.current
    setBusy(true)
    try {
      const data = valid(await sprint3Repository.send('post', `/reports/jobs/${job.id}/cancel/`, {}), record.request, job.id)
      if (sequence === epoch.current) { setJob(data); setMessage(data.status === 'cancelled' ? 'Cancelación confirmada.' : 'El trabajo ya había terminado.') }
    } catch (failure) { if (sequence === epoch.current) setError(errorMessage(failure)) }
    finally { if (sequence === epoch.current) setBusy(false) }
  }
  async function download() {
    if (job?.status !== 'ready' || busy) return
    const sequence = epoch.current
    setBusy(true); setError('')
    try {
      const blob = await sprint3Repository.pdf(job)
      if (sequence !== epoch.current || currentOwner() !== owner) return
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url; anchor.download = `informe_${job.request.a}${job.request.b ? '_' + job.request.b : ''}_${job.request.fiscal_year}.pdf`
      anchor.click(); setTimeout(() => URL.revokeObjectURL(url), 1500)
      setMessage('Descarga iniciada con integridad verificada. Revisa las descargas de tu navegador.')
    } catch (failure) { if (sequence === epoch.current) setError(errorMessage(failure)) }
    finally { if (sequence === epoch.current) setBusy(false) }
  }
  const sameSelection = value => value?.request?.a === selection.a && value.request.b === (selection.b || null) && value.request.fiscal_year === selection.fiscal_year
  return { job: sameSelection(job) && job.owner_id === owner ? job : null,
    record: sameSelection(record) ? record : null, busy, error, message, submit, cancel, download, poll }
}
