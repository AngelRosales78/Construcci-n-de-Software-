import { useState, useEffect, useRef, useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { companyRepository } from '../models/companyRepository'
import { historyRepository } from '../models/historyRepository'

export function useCompanySearchViewModel() {
  const [companies, setCompanies] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [count, setCount] = useState(0)
  const [hasNext, setHasNext] = useState(false)
  const [historyVersion, setHistoryVersion] = useState(0)
  const [urlParams, setUrlParams] = useSearchParams()
  const signature = urlParams.toString()
  const params = useMemo(() => {
    const values = new URLSearchParams(signature)
    const page = Number(values.get('page') || 1)
    return { q: values.get('q') || '', sector: values.get('sector') || '',
      ordering: ['market_cap', '-market_cap'].includes(values.get('ordering')) ? values.get('ordering') : '-market_cap',
      page: Number.isSafeInteger(page) && page > 0 ? page : 1,
      withData: values.get('with_data') === 'true' }
  }, [signature])
  const setParams = useCallback(update => setUrlParams(previous => {
    const currentPage = Number(previous.get('page') || 1)
    const current = { q: previous.get('q') || '', sector: previous.get('sector') || '',
      ordering: ['market_cap', '-market_cap'].includes(previous.get('ordering')) ? previous.get('ordering') : '-market_cap',
      page: Number.isSafeInteger(currentPage) && currentPage > 0 ? currentPage : 1,
      withData: previous.get('with_data') === 'true' }
    const next = update(current)
    const values = new URLSearchParams()
    if (next.q) values.set('q', next.q)
    if (next.sector) values.set('sector', next.sector)
    values.set('ordering', next.ordering)
    values.set('page', String(next.page))
    if (next.withData) values.set('with_data', 'true')
    return values
  }, { replace: true }), [setUrlParams])
  const [attempt, setAttempt] = useState(0)
  const retry = useCallback(() => setAttempt(value => value + 1), [])
  const requestId = useRef(0)

  useEffect(() => {
    const id = ++requestId.current
    setLoading(true)
    setError('')
    const query = new URLSearchParams({ ordering: params.ordering, page: String(params.page) })
    if (params.q) query.set('q', params.q)
    if (params.sector) query.set('sector', params.sector)
    if (params.withData) query.set('with_data', 'true')
    companyRepository.search(query).then((data) => {
      if (id !== requestId.current) return
      setCompanies(data.results || [])
      setCount(data.count || 0)
      setHasNext(Boolean(data.next))
    }).catch(() => {
      if (id !== requestId.current) return
      setCompanies([])
      setCount(0)
      setHasNext(false)
      setError('No se pudieron cargar las empresas. Intenta nuevamente.')
    }).finally(() => {
      if (id === requestId.current) setLoading(false)
    })
    return () => { requestId.current++ }
  }, [params, attempt])

  const handleSearch = useCallback((query) => {
    const q = query.trim()
    setParams(previous => previous.q === q && previous.page === 1
      ? previous : { ...previous, q, page: 1 })
  }, [setParams])

  const saveHistory = useCallback(async (query) => {
    if (query.trim() === params.q && params.page === 1) retry()
    handleSearch(query)
    if (!query.trim()) return
    try {
      await historyRepository.save(query.trim())
      setHistoryVersion(version => version + 1)
    } catch {
      // Una falla del historial no impide consultar empresas.
    }
  }, [handleSearch, params.q, params.page, retry])

  const handleFilterChange = useCallback(({ sector, ordering }) => {
    setParams(previous => ({ ...previous, sector, ordering, page: 1 }))
  }, [setParams])

  const previousPage = () => setParams(previous => ({ ...previous, page: previous.page - 1 }))
  const nextPage = () => setParams(previous => ({ ...previous, page: previous.page + 1 }))
  const setWithData = value => setParams(previous => ({ ...previous, withData: value, page: 1 }))
  return { companies, loading, error, count, hasNext, historyVersion, params, handleSearch, saveHistory, handleFilterChange, previousPage, nextPage, retry, setWithData, searchPath: '/search' + (signature ? '?' + signature : '') }
}
