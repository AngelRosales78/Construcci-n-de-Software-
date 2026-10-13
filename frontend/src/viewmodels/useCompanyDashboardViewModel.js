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
