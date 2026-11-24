import { Link, useNavigate } from 'react-router-dom'
import WorkspaceLayout from './layout/WorkspaceLayout'
import RecentCompanies from './RecentCompanies'

export default function Dashboard() {
  const navigate = useNavigate()
  return <WorkspaceLayout>
    <div className="desk-masthead"><span>EL CUADERNO</span><p>Empresas. Balances. Perspectiva.</p><span>INVESTIGACIÓN FINANCIERA</span></div>
    <section className="welcome-dashboard editorial-cover">
      <div className="cover-story">
        <p className="section-eyebrow">EL PUNTO DE PARTIDA</p>
        <h1>El criterio empieza<br /><em>por los datos.</em></h1>
        <p className="section-description">Un espacio para leer empresas con profundidad. Consulta sus indicadores, encuentra sus fuentes y pon cada cifra en contexto.</p>
        <button className="primary-action" onClick={() => navigate('/search')}>Buscar empresas y consultar fichas <span aria-hidden="true">↗</span></button>
        <p className="cover-footnote">Datos históricos. Fuentes identificadas. Lecturas propias.</p>
      </div>
      <aside className="cover-index" aria-label="Rutas de análisis">
        <div className="cover-index-heading"><span>EN TU MESA</span><span aria-hidden="true">↙</span></div>
        <Link to="/search"><span className="index-number">01</span><span><strong>El universo de empresas</strong><small>Explora por nombre, símbolo o sector.</small></span><span aria-hidden="true">↗</span></Link>
        <Link to="/company/MSFT?section=pillars"><span className="index-number">02</span><span><strong>Una lectura del balance</strong><small>Abre los cinco pilares de Microsoft.</small></span><span aria-hidden="true">↗</span></Link>
        <Link to="/compare"><span className="index-number">03</span><span><strong>Dos perspectivas</strong><small>Prepara una comparación. Herramienta Pro.</small></span><span aria-hidden="true">↗</span></Link>
        <p>Una cifra cobra sentido cuando conoces de dónde viene.</p>
      </aside>
    </section>
    <div className="reading-route-heading"><p className="section-eyebrow">UN MÉTODO PARA EXPLORAR</p><span>Del símbolo al contexto</span></div>
    <div className="onboarding-grid">
      <article><span className="step-number">01</span><h2>Encuentra una empresa</h2><p>Busca por nombre o símbolo, filtra por sector y ordena los resultados.</p></article>
      <article><span className="step-number">02</span><h2>Lee sus indicadores</h2><p>Consulta PER, ROE, capitalización, beta y dividendo, junto con su fuente y fecha.</p></article>
      <article><span className="step-number">03</span><h2>Explora su evolución</h2><p>Revisa el perfil Altman-Z y amplía el gráfico de precios históricos.</p></article>
    </div>
    <RecentCompanies />
  </WorkspaceLayout>
}
