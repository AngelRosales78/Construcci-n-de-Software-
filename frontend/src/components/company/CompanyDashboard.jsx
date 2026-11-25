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
import BeginnerGuide from './BeginnerGuide'
import CompanyMark from '../shared/CompanyMark'
import Icon from '../shared/Icon'
import AskAI from '../shared/AskAI'
import './company-reading.css'

const sections = [
  { id: 'overview', label: 'Resumen', description: 'Precio, indicadores y lectura del balance, de un vistazo.' },
  { id: 'pillars', label: 'Salud financiera', description: 'Elige un año y explora los cinco pilares del balance.' },
  { id: 'simulation', label: 'Simulaciones', description: 'Explora escenarios. Los supuestos no modifican los datos históricos.' },
  { id: 'analysis', label: 'Explicación de IA', description: 'Entiende los indicadores del año seleccionado con sus referencias.' },
]

export default function CompanyDashboard() {
  const favorites = useFavoritesViewModel()
  const { company, loading, error, navigate, returnTo, section, setSection, visitError, retryVisit, retryCompany } = useCompanyDashboardViewModel()
  if (loading) return <WorkspaceLayout><div className="dashboard-loading" role="status">Cargando ficha financiera...</div></WorkspaceLayout>
  if (error) return <WorkspaceLayout><div className="dashboard-error" role="alert"><h1>No se pudo abrir la ficha</h1><p>{error}</p><button onClick={retryCompany}>Reintentar ficha</button><button onClick={() => navigate(returnTo, { replace: true })}>Volver al explorador</button></div></WorkspaceLayout>
  if (!company) return null
  const openFinancialDetails = () => {
    setSection('pillars')
    requestAnimationFrame(() => document.getElementById('financial-pillars-title')?.focus())
  }

  return (
    <WorkspaceLayout><div className="company-dashboard">
      <div className="dashboard-header">
        <button onClick={() => navigate(returnTo, { replace: true })} className="back-button">
          ← Volver
        </button>
        <CompanyMark company={company} large />
        <div className="dashboard-title">
          <p className="section-eyebrow">FICHA FINANCIERA · {company.ticker}</p>
          <h1>{company.name}</h1>
          <FavoriteButton ticker={company.ticker} favorites={favorites} />
          <span className="dashboard-sector">{sectorLabel(company.sector, company.sector_display)}</span>
        </div>
        {company.quote && <div className="terminal-quote"><small>Último cierre ajustado importado</small><strong>{Number(company.quote.adjusted_close).toLocaleString('es-PE', { maximumFractionDigits: 2, minimumFractionDigits: 2 })} <span>{company.quote.currency}</span></strong><small>{company.quote.date}</small></div>}
      </div>

      <div className="dashboard-content">
        {visitError && <p role="status">{visitError} <button onClick={retryVisit}>Reintentar registro de visita</button></p>}
        {favorites.error && <div role="alert"><p>{favorites.error}</p><button onClick={favorites.reload}>Reintentar favoritos</button></div>}
        {favorites.message && <p role="status">{favorites.message}</p>}
        <nav className="company-sections" aria-label="Secciones de la empresa">
          {sections.map(({ id, label }) =>
            <button key={id} type="button" aria-pressed={section === id} aria-controls={id === 'analysis' || id === 'pillars' ? 'company-financial' : `company-${id}`} onClick={() => setSection(id)}><Icon name={id} /> {label}</button>)}
        </nav>
        {section !== 'overview' && <p className="company-section-guide">{sections.find(item => item.id === section).description}</p>}
        {company.is_active === false && <p role="status">Empresa inactiva: ficha histórica, fuera de los buscadores.</p>}
        {company.is_index_member === false && <p role="note">Pertenencia al índice S&amp;P 500 sin verificar.</p>}
        <details className="company-tools" hidden={section === 'overview'}><summary>Información y fuentes</summary><div className="company-utilities">
          <details className="company-context"><summary>Acerca de {company.name}</summary><CompanyAbout company={company} /></details>
          <DataSources company={company} />
        </div></details>

        {!hasIndicators(company) && section === 'overview' && <section className="empty-financial-notice"><h2>Indicadores del resumen pendientes</h2><p>{company.quote ? 'Ya puedes consultar su histórico de precios. ' : ''}Los cinco indicadores del resumen todavía no están cargados. La disponibilidad de balances se muestra en Salud financiera.</p><button onClick={() => setSection('pillars')}>Ver salud financiera</button></section>}
        {company.is_demo && hasIndicators(company) && <p role="status" className="data-notice">Resumen financiero pendiente de verificar. Consulta la procedencia de los precios y de los balances anuales.</p>}
        <div id="company-financial" hidden={!['pillars', 'analysis'].includes(section)}><FinancialPillars key={company.ticker} view={section === 'analysis' ? 'analysis' : 'pillars'} /></div>
        <div id="company-simulation" hidden={section !== 'simulation'}><SimulationScenarios /></div>
        <div id="company-overview" hidden={section !== 'overview'}>
        <div className="company-overview-feature">
          <FinancialHealthIndicator ticker={company.ticker} altmanZ={company.altman_z} asOf={company.as_of} onReviewIndicators={hasIndicators(company) ? () => document.getElementById('company-metrics-title')?.focus() : undefined} />
          <div className="company-first-step"><BeginnerGuide key={company.ticker} company={company} onExplore={openFinancialDetails} /><aside className="company-ai-invitation"><span className="ai-label">IA</span><h3>Una cifra no tiene por qué dejarte con dudas.</h3><p>Pregunta sobre esta empresa y sigue la conversación con ejemplos sencillos.</p><AskAI ticker={company.ticker}>Preguntar sobre {company.ticker}</AskAI></aside></div>
        </div>
        <PriceChart />
        <div className="data-provenance" aria-label="Disponibilidad y fechas de los datos" hidden={section !== 'overview'}>
          <span><small>{company.reference_basis === 'provider_normalized' ? 'Referencia anual del proveedor' : 'Balance de referencia'}</small><strong>{company.as_of || 'Sin balance cargado'}</strong></span>
          <p>Datos históricos · Cada indicador conserva su fecha.</p>
          {company.reference_basis === 'provider_normalized' && <p className="provider-reference-inline">Fecha anual normalizada; no acredita el cierre contable exacto.</p>}
        </div>
        {(hasIndicators(company) || company.quote) && <>
        <div className="terminal-financial-grid">
          <section className="terminal-fundamentals"><div className="section-title"><h2 id="company-metrics-title" tabIndex={-1}>Las cifras, explicadas</h2><span className="coverage-badge">{countAvailableIndicators(company)} de 5 disponibles</span></div><MetricCards company={company} /><p className="metrics-period-note">El rango del gráfico no cambia estos indicadores.</p><button className="financial-detail-action" onClick={openFinancialDetails}>Ver detalle financiero y fuentes →</button></section>
        </div>
        </>}
        </div>
        <div className="altman-detail" hidden={section !== 'pillars'}><AltmanZGauge score={company.altman_z.score} zone={company.altman_z.zone} reference={company.altman_z.reference} availability={company.altman_z.availability} />
          {company.reference_basis === 'provider_normalized' && company.altman_z.score == null && <p className="altman-reference-note">Altman-Z requiere un valor bursátil correspondiente al cierre contable. Las referencias normalizadas y la capitalización actual no verifican esa coincidencia; por eso este puntaje puede permanecer sin calcular.</p>}
        </div>
        <div className="overview-report"><ReportPanel a={company.ticker} /></div>
      </div>
    </div></WorkspaceLayout>
  )
}
