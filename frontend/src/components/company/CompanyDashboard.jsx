import { useCompanyDashboardViewModel } from '../../viewmodels/useCompanyDashboardViewModel'
import AltmanZGauge from './AltmanZGauge'
import PriceChart from './PriceChart'
import MetricCards from './MetricCards'
import FinancialHealthIndicator from './FinancialHealthIndicator'
import WorkspaceLayout from '../layout/WorkspaceLayout'


export default function CompanyDashboard() {
  const { company, loading, error, navigate } = useCompanyDashboardViewModel()
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
        {company.quote && <div className="terminal-quote"><small>Último cierre ajustado importado</small><strong>{Number(company.quote.adjusted_close).toLocaleString('es-PE', { maximumFractionDigits: 2, minimumFractionDigits: 2 })} <span>{company.quote.currency}</span></strong><small>{company.quote.date}</small></div>}
      </div>

      <div className="dashboard-content">
        <div className="data-provenance"><span><small>Balance de referencia</small><strong>Fecha: {company.as_of || 'Sin fecha'}</strong></span><p>Fuente: {company.source || 'Sin fuente'}</p>{company.quote && <span><small>Último cierre importado</small><strong>{company.quote.date} · {company.quote.currency}</strong></span>}</div>
        {company.is_demo && <p role="status" className="data-notice">Datos de demostración o sin verificar.</p>}
        <div className="terminal-financial-grid">
          <PriceChart />
          <section className="terminal-fundamentals"><div className="section-title"><h2>Indicadores clave</h2><span>Balance de referencia</span></div><MetricCards company={company} /></section>
          <section className="terminal-analysis"><p className="section-eyebrow">LECTURA FINANCIERA</p><h2>Cómo interpretar esta ficha</h2><p>Las métricas describen el periodo indicado en el balance. El gráfico permite explorar cierres históricos mediante sus cuatro rangos y el zoom.</p><p>Altman-Z resume relaciones del balance. Es un indicador académico; su zona no garantiza resultados futuros.</p><div className="terminal-tag">Datos ausentes se muestran como «Sin datos»</div></section>
          <div className="dashboard-charts">
            <AltmanZGauge score={company.altman_z.score} zone={company.altman_z.zone} />
            <FinancialHealthIndicator altmanZ={company.altman_z} profile={company.profile} />
          </div>
        </div>
      </div>
    </div></WorkspaceLayout>
  )
}
