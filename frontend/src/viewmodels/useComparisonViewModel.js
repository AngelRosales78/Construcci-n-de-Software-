import { useCallback, useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { comparisonRepository } from '../models/comparisonRepository'

export function useComparisonViewModel() {
  const [params, setParams] = useSearchParams()
  const a = (params.get('a') || '').toUpperCase()
  const b = (params.get('b') || '').toUpperCase()
  const year = params.get('fiscal_year') ?? '2024'
  const [access, setAccess] = useState(null)
  const [accessError, setAccessError] = useState('')
  const [accessLoading, setAccessLoading] = useState(true)
  const [accessAttempt, setAccessAttempt] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [selectionError, setSelectionError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const sequence = useRef(0)
  const pro = access?.plan === 'pro' && access.active && access.features.includes('compare') && Date.parse(access.valid_until) > Date.now()
  const validYear = /^[0-9]{4}$/.test(year) && Number(year) >= 1900 && Number(year) <= new Date().getFullYear()
  const validSymbols = [a, b].every(value => /^[A-Z0-9.\-]{1,10}$/.test(value))
  const valid = validSymbols && a !== b && validYear

  useEffect(() => {
    let alive = true
    setAccessLoading(true); setAccessError('')
    comparisonRepository.subscription().then(value => { if (alive) setAccess(value) })
      .catch(() => { if (alive) { setAccess(null); setAccessError('No se pudo verificar tu plan. Reintenta para continuar.') } })
      .finally(() => { if (alive) setAccessLoading(false) })
    return () => { alive = false }
  }, [accessAttempt])

  useEffect(() => {
    const id = ++sequence.current
    setData(null); setError(''); setLoading(false)
    if (!valid || !pro || accessLoading) return
    setLoading(true)
    comparisonRepository.compare(a, b, year).then(result => { if (id === sequence.current) setData(result) })
      .catch(failure => {
        if (id !== sequence.current) return
        const status = failure.response?.status
        setError(status === 403 ? 'El servidor requiere un plan Pro activo. Verifica tu acceso.'
          : status === 404 ? 'Una empresa no está en el catálogo. Corrige la selección.'
            : 'No se pudo consultar la selección. Se conserva para que puedas reintentar.')
      })
      .finally(() => { if (id === sequence.current) setLoading(false) })
    return () => { sequence.current++ }
  }, [a, b, year, valid, pro, accessLoading, attempt])

  const update = useCallback(values => {
    sequence.current++
    setData(null); setError(''); setSelectionError('')
    setParams(previous => {
      const next = new URLSearchParams(previous)
      for (const [key, value] of Object.entries(values)) {
        if (value || key === 'fiscal_year') next.set(key, value)
        else next.delete(key)
      }
      return next
    })
  }, [setParams])
  const select = (side, ticker) => {
    if (ticker === (side === 'a' ? b : a)) { setSelectionError('Elige dos empresas distintas.'); return }
    update({ [side]: ticker, fiscal_year: year })
  }
  return { a, b, year, data, loading, error, access, accessError, accessLoading, pro, validYear,
    selectionError: selectionError || (a && b && a === b ? 'Elige dos empresas distintas.' : '')
      || (a && b && !validSymbols ? 'Selecciona símbolos válidos del catálogo.' : ''),
    select, remove: side => update({ [side]: '' }), swap: () => update({ a: b, b: a }),
    setYear: fiscal_year => update({ fiscal_year }), retry: () => setAttempt(value => value + 1),
    refreshAccess: () => setAccessAttempt(value => value + 1) }
}
