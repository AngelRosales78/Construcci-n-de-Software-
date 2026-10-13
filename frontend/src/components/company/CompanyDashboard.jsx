import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../../services/api'
import AltmanZGauge from './AltmanZGauge'
import PriceChart from './PriceChart'
import MetricCards from './MetricCards'
import FinancialHealthIndicator from './FinancialHealthIndicator'
import WorkspaceLayout from '../layout/WorkspaceLayout'

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

  if (loading) return <WorkspaceLayout><div className="dashboard-loading" role="status">Cargando ficha financiera...</div></WorkspaceLayout>
  if (error) return <WorkspaceLayout><div className="dashboard-error" role="alert">{error}<button onClick={() => navigate('/search')}>Volver al explorador</button></div></WorkspaceLayout>
  if (!company) return null

  return (
    <WorkspaceLayout><div className="company-dashboard">
      <div className="dashboard-header">
        <button onClick={() => navigate('/search')} className="back-button">
          ← Volver
        </button>
        <div className="dashboard-title">
          <p className="section-eyebrow">FICHA FINANCIERA · {company.ticker}</p>
          <h1>{company.name}</h1>
          <span className="dashboard-sector">{company.sector_display}</span>
          <p className="section-description">Una lectura del balance, sus indicadores y la evolución del precio.</p>
        </div>
      </div>

      <div className="dashboard-content">
        <div className="data-provenance"><span><small>Balance de referencia</small><strong>Fecha: {company.as_of || 'Sin fecha'}</strong></span><p>Fuente: {company.source || 'Sin fuente'}</p>{company.quote && <span><small>Último cierre importado</small><strong>{company.quote.date} · {company.quote.currency}</strong></span>}</div>
        {company.is_demo && <p role="status" className="data-notice">Datos de demostración o sin verificar.</p>}
        <div className="section-title"><h2>Indicadores clave</h2><span>Consulta la explicación de cada métrica</span></div>
        <MetricCards company={company} />
        <div className="dashboard-charts">
          <AltmanZGauge score={company.altman_z.score} zone={company.altman_z.zone} />
          <FinancialHealthIndicator altmanZ={company.altman_z} profile={company.profile} />
        </div>

        <PriceChart />
      </div>
    </div></WorkspaceLayout>
  )
}
