import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../../services/api'
import FinancialHealthIndicator from './FinancialHealthIndicator'

export default function CompanyProfile() {
  const { ticker } = useParams()
  const navigate = useNavigate()
  const [company, setCompany] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    const fetchCompany = async () => {
      setLoading(true)
      try {
        const response = await api.get(`/companies/${ticker}/overview/`)
        setCompany(response.data)
      } catch (err) {
        setError(err.response?.data?.error || 'Error al cargar la empresa')
      } finally {
        setLoading(false)
      }
    }
    fetchCompany()
  }, [ticker])

  if (loading) return <div className="company-profile-loading">Cargando perfil...</div>
  if (error) return <div className="company-profile-error">{error}</div>
  if (!company) return null

  const formatMarketCap = (value) => {
    if (value >= 1e12) return `$${(value / 1e12).toFixed(2)}T`
    if (value >= 1e9) return `$${(value / 1e9).toFixed(2)}B`
    if (value >= 1e6) return `$${(value / 1e6).toFixed(2)}M`
    return `$${value.toFixed(2)}`
  }

  return (
    <div className="company-profile">
      <div className="profile-header">
        <button onClick={() => navigate('/search')} className="back-button">
          ← Volver
        </button>
        <div className="profile-title">
          <h1>{company.ticker}</h1>
          <h2>{company.name}</h2>
          <span className="profile-sector">{company.sector_display}</span>
        </div>
      </div>

      <div className="profile-content">
        <div className="profile-metrics">
          <div className="metric-card">
            <span className="metric-label">Market Cap</span>
            <span className="metric-value">{formatMarketCap(company.market_cap)}</span>
          </div>
          <div className="metric-card">
            <span className="metric-label">PER</span>
            <span className="metric-value">{company.per || 'N/A'}</span>
          </div>
          <div className="metric-card">
            <span className="metric-label">ROE</span>
            <span className="metric-value">{company.roe ? `${company.roe}%` : 'N/A'}</span>
          </div>
        </div>

        <FinancialHealthIndicator altmanZ={company.altman_z} />
      </div>
    </div>
  )
}
