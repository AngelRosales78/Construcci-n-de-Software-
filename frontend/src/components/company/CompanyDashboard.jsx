import { useState } from 'react'
import { useCompanyDashboardViewModel } from '../../viewmodels/useCompanyDashboardViewModel'
import AltmanZGauge from './AltmanZGauge'
import PriceChart from './PriceChart'
import MetricCards from './MetricCards'
import FinancialHealthIndicator from './FinancialHealthIndicator'
import WorkspaceLayout from '../layout/WorkspaceLayout'
import DataSources from './DataSources'
import FinancialPillars from './FinancialPillars'
import SimulationScenarios from './SimulationScenarios'
import { hasIndicators, sectorLabel } from '../../utils/financialPresentation'


export default function CompanyDashboard() {
  const { company, loading, error, navigate, returnTo } = useCompanyDashboardViewModel()
  const [section, setSection] = useState('overview')
  if (loading) return <WorkspaceLayout><div className="dashboard-loading" role="status">Cargando ficha financiera...</div></WorkspaceLayout>
  if (error) return <WorkspaceLayout><div className="dashboard-error" role="alert"><p>{error}</p><button onClick={() => navigate(returnTo, { replace: true })}>Volver al explorador</button></div></WorkspaceLayout>
  if (!company) return null

  return (
    <WorkspaceLayout><div className="company-dashboard">
      <div className="dashboard-header">
        <button onClick={() => navigate(returnTo, { replace: true })} className="back-button">
          ← Volver
        </button>
        <div className="dashboard-title">
          <p className="section-eyebrow">FICHA FINANCIERA · {company.ticker}</p>
          <h1>{company.name}</h1>
          <span className="dashboard-sector">{sectorLabel(company.sector, company.sector_display)}</span>
        </div>
        {company.quote && <div className="terminal-quote"><small>Último cierre ajustado importado</small><strong>{Number(company.quote.adjusted_close).toLocaleString('es-PE', { maximumFractionDigits: 2, minimumFractionDigits: 2 })} <span>{company.quote.currency}</span></strong><small>{company.quote.date}</small></div>}
      </div>

      <div className="dashboard-content">
        <div className="data-provenance" hidden={section === 'pillars' || section === 'analysis'}><span><small>Balance del resumen</small><strong>{company.as_of || 'Sin balance cargado'}</strong></span><p>Datos históricos</p>{company.quote && <span><small>Precios hasta</small><strong>{company.quote.date} · {company.quote.currency}</strong></span>}</div>
        <nav className="company-sections" aria-label="Secciones de la empresa">
          {[['overview', 'Resumen'], ['pillars', 'Salud financiera'], ['simulation', 'Simulaciones'], ['analysis', 'Explicación de IA']].map(([id, label]) =>
            <button key={id} type="button" aria-pressed={section === id} aria-controls={id === 'analysis' || id === 'pillars' ? 'company-financial' : `company-${id}`} onClick={() => setSection(id)}>{label}</button>)}
        </nav>
        {!hasIndicators(company) && section === 'overview' && <section className="empty-financial-notice"><h2>Indicadores del resumen pendientes</h2><p>La empresa figura en el catálogo. Puedes ver un caso con datos en Microsoft.</p><button onClick={() => navigate('/company/MSFT', { state: { searchReturn: returnTo } })}>Ver Microsoft</button></section>}
        {company.is_demo && (hasIndicators(company) || company.quote) && <p role="status" className="data-notice">Datos de demostración o sin verificar.</p>}
        <div id="company-financial" hidden={!['pillars', 'analysis'].includes(section)}><FinancialPillars view={section === 'analysis' ? 'analysis' : 'pillars'} /></div>
        <div id="company-simulation" hidden={section !== 'simulation'}><SimulationScenarios /></div>
        <div id="company-overview" hidden={section !== 'overview'}>
        {(hasIndicators(company) || company.quote) && <>
        <div className="overview-intro"><div><h2>La empresa, de un vistazo</h2><p>Precio histórico e indicadores del balance.</p></div><DataSources company={company} /></div>
        <div className="terminal-financial-grid">
          <PriceChart />
          <section className="terminal-fundamentals"><div className="section-title"><h2>Indicadores clave</h2><span>{company.as_of ? `Balance: ${company.as_of}` : 'Balance pendiente'}</span></div><p className="metrics-period-note">Cambiar el rango del gráfico no modifica estos indicadores.</p><MetricCards company={company} /></section>
          <div className="dashboard-charts">
            <AltmanZGauge score={company.altman_z.score} zone={company.altman_z.zone} />
            <FinancialHealthIndicator altmanZ={company.altman_z} profile={company.profile} />
          </div>
        </div>
        </>}
        </div>
      </div>
    </div></WorkspaceLayout>
  )
}
