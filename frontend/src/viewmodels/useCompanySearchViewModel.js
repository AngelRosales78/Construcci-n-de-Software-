import { useState, useEffect, useRef, useCallback } from 'react'
import { companyRepository } from '../models/companyRepository'
import { historyRepository } from '../models/historyRepository'

export function useCompanySearchViewModel() {
  const [companies, setCompanies] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [count, setCount] = useState(0)
  const [hasNext, setHasNext] = useState(false)
  const [historyVersion, setHistoryVersion] = useState(0)
  const [params, setParams] = useState({ q: '', sector: '', ordering: '-market_cap', page: 1 })
  const requestId = useRef(0)

  useEffect(() => {
    const id = ++requestId.current
    setLoading(true)
    setError('')
    const query = new URLSearchParams({ ordering: params.ordering, page: String(params.page) })
    if (params.q) query.set('q', params.q)
    if (params.sector) query.set('sector', params.sector)
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
  }, [params])

  const handleSearch = useCallback((query) => {
    const q = query.trim()
    setParams(previous => previous.q === q && previous.page === 1
      ? previous : { ...previous, q, page: 1 })
  }, [])

  const saveHistory = useCallback(async (query) => {
    handleSearch(query)
    if (!query.trim()) return
    try {
      await historyRepository.save(query.trim())
      setHistoryVersion(version => version + 1)
    } catch {
      // Una falla del historial no impide consultar empresas.
    }
  }, [handleSearch])

  const handleFilterChange = useCallback(({ sector, ordering }) => {
    setParams(previous => ({ ...previous, sector, ordering, page: 1 }))
  }, [])

  const previousPage = () => setParams(previous => ({ ...previous, page: previous.page - 1 }))
  const nextPage = () => setParams(previous => ({ ...previous, page: previous.page + 1 }))
  return { companies, loading, error, count, hasNext, historyVersion, params, handleSearch, saveHistory, handleFilterChange, previousPage, nextPage }
}
