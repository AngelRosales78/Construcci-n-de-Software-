import { useEffect, useId, useRef, useState } from 'react'
import { companyRepository } from '../models/companyRepository'

export function useCompanyPickerViewModel({ selected, onSelect }) {
  const [query, setQuery] = useState('')
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(-1)
  const [attempt, setAttempt] = useState(0)
  const id = useId()
  const requestId = useRef(0)
  useEffect(() => { setQuery(''); setRows([]); setError(''); setOpen(false) }, [selected])
  useEffect(() => {
    const request = ++requestId.current
    const value = query.trim()
    setRows([]); setError(''); setActive(-1)
    if (!value) { setLoading(false); return }
    setLoading(true)
    const timer = setTimeout(() => {
      companyRepository.search(new URLSearchParams({ q: value, ordering: 'name', page: '1' }))
        .then(data => { if (request === requestId.current) setRows((data.results || []).slice(0, 6)) })
        .catch(() => { if (request === requestId.current) setError('No se pudo buscar. Reintenta.') })
        .finally(() => { if (request === requestId.current) setLoading(false) })
    }, 250)
    return () => { clearTimeout(timer); requestId.current++ }
  }, [query, attempt])
  const choose = row => {
    onSelect(row.ticker)
    setOpen(false); setQuery(''); setRows([]); setActive(-1)
  }
  const keyDown = event => {
    if (event.key === 'Escape') { setOpen(false); setActive(-1) }
    if (event.key === 'ArrowDown' && rows.length) {
      event.preventDefault(); setOpen(true); setActive(index => (index + 1) % rows.length)
    }
    if (event.key === 'ArrowUp' && rows.length) {
      event.preventDefault(); setOpen(true); setActive(index => index <= 0 ? rows.length - 1 : index - 1)
    }
    if (event.key === 'Enter' && open && active >= 0 && rows[active]) {
      event.preventDefault(); choose(rows[active])
    }
  }
  return { query, change: value => { setQuery(value.slice(0, 200)); setOpen(true) }, rows, loading, error,
    open, setOpen, active, id, choose, keyDown, retry: () => setAttempt(value => value + 1) }
}
