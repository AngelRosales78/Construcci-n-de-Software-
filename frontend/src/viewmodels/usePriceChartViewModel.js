import { useState, useEffect } from 'react'
import { useLocation, useParams, useSearchParams } from 'react-router-dom'
import { companyRepository } from '../models/companyRepository'

export function usePriceChartViewModel() {
  const { ticker } = useParams()
  const location = useLocation()
  const [params, setParams] = useSearchParams()
  const period = params.get('range') || '1M'
  const setPeriod = value => setParams(previous => { const next = new URLSearchParams(previous); next.set('range', value); return next }, { replace: true, state: location.state })
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    setData(null)
    companyRepository.prices(ticker, period)
      .then(response => { if (active) setData(response) })
      .catch(() => { if (active) setError('No se pudo cargar el historial de precios.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [ticker, period])
  return { ticker, period, setPeriod, data, loading, error }
}
