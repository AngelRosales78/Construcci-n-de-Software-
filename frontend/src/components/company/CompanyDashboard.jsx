import { useCompanyDashboardViewModel } from '../../viewmodels/useCompanyDashboardViewModel'
import AltmanZGauge from './AltmanZGauge'
import PriceChart from './PriceChart'
import MetricCards from './MetricCards'
import FinancialHealthIndicator from './FinancialHealthIndicator'
import WorkspaceLayout from '../layout/WorkspaceLayout'
import DataSources from './DataSources'
import FinancialPillars from './FinancialPillars'
import SimulationScenarios from './SimulationScenarios'
import { countAvailableIndicators, hasIndicators, sectorLabel } from '../../utils/financialPresentation'
import { useFavoritesViewModel } from '../../viewmodels/useFavoritesViewModel'
import FavoriteButton from '../favorites/FavoriteButton'
import ReportPanel from '../reports/ReportPanel'
import CompanyAbout from './CompanyAbout'
import './company-reading.css'

const sections = [
  { id: 'overview', label: 'Resumen', description: 'Consulta el precio histórico y los cinco indicadores del resumen. Cada dato conserva su fecha de referencia.' },
  { id: 'pillars', label: 'Salud financiera', description: 'Revisa los cinco pilares del balance anual. Elige un ejercicio y abre cada pilar para consultar sus indicadores, fórmulas y fuentes.' },
  { id: 'simulation', label: 'Simulaciones', description: 'Explora escenarios cambiando sus supuestos. Los resultados de un escenario no modifican los datos históricos de la empresa.' },
  { id: 'analysis', label: 'Explicación de IA', description: 'Consulta una explicación de los indicadores del ejercicio seleccionado, junto con sus referencias y fecha de generación.' },
]

export default function CompanyDashboard() {
  const favorites = useFavoritesViewModel()
  const { company, loading, error, navigate, returnTo, section, setSection, visitError, retryVisit, retryCompany } = useCompanyDashboardViewModel()
  if (loading) return <WorkspaceLayout><div className="dashboard-loading" role="status">Cargando ficha financiera...</div></WorkspaceLayout>
  if (error) return <WorkspaceLayout><div className="dashboard-error" role="alert"><h1>No se pudo abrir la ficha</h1><p>{error}</p><button onClick={retryCompany}>Reintentar ficha</button><button onClick={() => navigate(returnTo, { replace: true })}>Volver al explorador</button></div></WorkspaceLayout>
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
          <FavoriteButton ticker={company.ticker} favorites={favorites} />
          <span className="dashboard-sector">{sectorLabel(company.sector, company.sector_display)}</span>
        </div>
        {company.quote && <div className="terminal-quote"><small>Último cierre ajustado importado</small><strong>{Number(company.quote.adjusted_close).toLocaleString('es-PE', { maximumFractionDigits: 2, minimumFractionDigits: 2 })} <span>{company.quote.currency}</span></strong><small>{company.quote.date}</small></div>}
      </div>

      <div className="dashboard-content">
        <CompanyAbout company={company} />
        <ReportPanel a={company.ticker} />
        {visitError && <p role="status">{visitError} <button onClick={retryVisit}>Reintentar registro de visita</button></p>}
        {favorites.error && <div role="alert"><p>{favorites.error}</p><button onClick={favorites.reload}>Reintentar favoritos</button></div>}
        {favorites.message && <p role="status">{favorites.message}</p>}
        <div className="data-provenance" aria-label="Disponibilidad y fechas de los datos" hidden={section !== 'overview'}>
          <span><small>Indicadores del resumen</small><strong>{countAvailableIndicators(company)} de 5 disponibles</strong></span>
          <span><small>Balance de referencia</small><strong>{company.as_of || 'Sin balance cargado'}</strong></span>
          <span><small>Último cierre importado</small><strong>{company.quote ? `${company.quote.date} · ${company.quote.currency}` : 'Sin precios cargados'}</strong></span>
          <p>Las fechas del balance, los precios y cada indicador pueden ser diferentes.</p>
        </div>
        <nav className="company-sections" aria-label="Secciones de la empresa">
          {sections.map(({ id, label }) =>
            <button key={id} type="button" aria-pressed={section === id} aria-controls={id === 'analysis' || id === 'pillars' ? 'company-financial' : `company-${id}`} onClick={() => setSection(id)}>{label}</button>)}
        </nav>
        <p className="company-section-guide">{sections.find(item => item.id === section).description}</p>
        {!hasIndicators(company) && section === 'overview' && <section className="empty-financial-notice"><h2>Indicadores del resumen pendientes</h2><p>Revisa los balances disponibles en Salud financiera. Microsoft también tiene el resumen cargado.</p><button onClick={() => navigate('/company/MSFT', { state: { searchReturn: returnTo } })}>Ver Microsoft</button></section>}
        {company.is_demo && hasIndicators(company) && <p role="status" className="data-notice">Resumen financiero pendiente de verificar. Consulta la procedencia de los precios y de los balances anuales.</p>}
        <div id="company-financial" hidden={!['pillars', 'analysis'].includes(section)}><FinancialPillars key={company.ticker} view={section === 'analysis' ? 'analysis' : 'pillars'} /></div>
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
