import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate, useLocation, useSearchParams } from 'react-router-dom'
import { companyRepository } from '../models/companyRepository'
import { sprint3Repository } from '../models/sprint3Repository'

export function useCompanyDashboardViewModel() {
  const { ticker } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const previousSearch = location.state?.searchReturn
  const returnTo = typeof previousSearch === 'string' && (previousSearch === '/favorites' || previousSearch === '/search' || previousSearch.startsWith('/search?')) ? previousSearch : '/search'
  const [company, setCompany] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [visitError, setVisitError] = useState('')
  const [visitAttempt, setVisitAttempt] = useState(0)
  const visit = useRef(null)

  useEffect(() => {
    let active = true
    const fetchCompany = async () => {
      setLoading(true)
      setError(null)
      setVisitError('')
      setCompany(null)
      try {
        const response = await companyRepository.summary(ticker)
        if (active) {
          setCompany(response)
          if (visit.current?.ticker !== ticker) visit.current = { ticker, id: crypto.randomUUID() }
          sprint3Repository.send('post', `/companies/${encodeURIComponent(ticker)}/visits/`, { event_id: visit.current.id })
            .catch(() => { if (active) setVisitError('La ficha se cargó, pero no se pudo registrar en recientes.') })
        }
      } catch (err) {
        if (active) setError(err.response?.data?.error || 'Error al cargar la empresa')
      } finally {
        if (active) setLoading(false)
      }
    }
    fetchCompany()
    return () => { active = false }
  }, [ticker, visitAttempt])

  return { company, loading, error, navigate, returnTo, visitError, retryVisit: () => setVisitAttempt(n => n + 1) }
}

export function useFinancialPillarsViewModel() {
  const { ticker } = useParams()
  const [data, setData] = useState(null)
  const [params, setParams] = useSearchParams()
  const year = params.get('fiscal_year') || ''
  const setYear = value => setParams(previous => { const next = new URLSearchParams(previous); value ? next.set('fiscal_year', value) : next.delete('fiscal_year'); return next }, { replace: true })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setData(null)
    setError(null)
    companyRepository.pillars(ticker, year).then(result => {
      if (active) setData(result)
    }).catch(() => {
      if (active) setError('No se pudieron cargar los pilares financieros. Puedes reintentar.')
    }).finally(() => {
      if (active) setLoading(false)
    })
    return () => { active = false }
  }, [ticker, year, attempt])

  return { data, year, setYear, loading, error, retry: () => setAttempt(value => value + 1) }
}
