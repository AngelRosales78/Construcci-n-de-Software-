import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../../services/api'
import AltmanZGauge from './AltmanZGauge'
import PriceChart from './PriceChart'
import MetricCards from './MetricCards'
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
      setError(null)
      setCompany(null)
      try {
        const response = await api.get(`/companies/${ticker}/summary/`)
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
        <MetricCards company={company} />
        <p>Fuente: {company.source || 'Sin fuente'} · Fecha: {company.as_of || 'Sin fecha'}</p>
        {company.is_demo && <p role="status">Datos de demostración o sin verificar.</p>}
        <div className="dashboard-charts">
          <AltmanZGauge score={company.altman_z.score} zone={company.altman_z.zone} />
          <FinancialHealthIndicator altmanZ={company.altman_z} profile={company.profile} />
        </div>

        <PriceChart />
      </div>
    </div>
  )
}
