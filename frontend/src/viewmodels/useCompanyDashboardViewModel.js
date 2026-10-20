import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { companyRepository } from '../models/companyRepository'

export function useCompanyDashboardViewModel() {
  const { ticker } = useParams()
  const navigate = useNavigate()
  const [company, setCompany] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    const fetchCompany = async () => {
      setLoading(true)
      setError(null)
      setCompany(null)
      try {
        const response = await companyRepository.summary(ticker)
        if (active) setCompany(response)
      } catch (err) {
        if (active) setError(err.response?.data?.error || 'Error al cargar la empresa')
      } finally {
        if (active) setLoading(false)
      }
    }
    fetchCompany()
    return () => { active = false }
  }, [ticker])

  return { company, loading, error, navigate }
}

export function useFinancialPillarsViewModel() {
  const { ticker } = useParams()
  const [data, setData] = useState(null)
  const [year, setYear] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => { setYear('') }, [ticker])
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
