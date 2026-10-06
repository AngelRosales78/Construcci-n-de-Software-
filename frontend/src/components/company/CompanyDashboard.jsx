import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../../services/api'
import AltmanZGauge from './AltmanZGauge'
import PriceChart from './PriceChart'
import FinancialHealthIndicator from './FinancialHealthIndicator'

export default function CompanyDashboard() {
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

  if (loading) return <div className="dashboard-loading">Cargando dashboard...</div>
  if (error) return <div className="dashboard-error">{error}</div>
  if (!company) return null

  const formatMarketCap = (value) => {
    if (value >= 1e12) return `$${(value / 1e12).toFixed(2)}T`
    if (value >= 1e9) return `$${(value / 1e9).toFixed(2)}B`
    if (value >= 1e6) return `$${(value / 1e6).toFixed(2)}M`
    return `$${value.toFixed(2)}`
  }

  return (
    <div className="company-dashboard">
      <div className="dashboard-header">
        <button onClick={() => navigate('/search')} className="back-button">
          ← Volver
        </button>
        <div className="dashboard-title">
          <h1>{company.ticker}</h1>
          <h2>{company.name}</h2>
          <span className="dashboard-sector">{company.sector_display}</span>
        </div>
      </div>

      <div className="dashboard-content">
        <div className="dashboard-metrics">
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

        <div className="dashboard-charts">
          <AltmanZGauge score={company.altman_z.score} zone={company.altman_z.zone} />
          <FinancialHealthIndicator altmanZ={company.altman_z} />
        </div>

        <PriceChart />
      </div>
    </div>
  )
}
