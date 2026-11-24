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
        <h1>Explora empresas.<br /><em>Entiende sus cifras.</em></h1>
        <p className="section-description">Conoce el negocio, entiende sus números y compara empresas con datos históricos y fuentes identificadas.</p>
        <button className="primary-action" onClick={() => navigate('/search')}>Explorar empresas <span aria-hidden="true">↗</span></button>
        <p className="cover-footnote">Datos históricos. Fuentes identificadas. Lecturas propias.</p>
      </div>
      <aside className="cover-index" aria-label="Rutas de análisis">
        <div className="cover-index-heading"><span>EN TU MESA</span><span aria-hidden="true">↙</span></div>
        <Link to="/search"><span className="index-number">01</span><span><strong>Buscar una empresa</strong><small>Explora por nombre, símbolo o sector.</small></span><span aria-hidden="true">↗</span></Link>
        <Link to="/company/MSFT"><span className="index-number">02</span><span><strong>Empezar con Microsoft</strong><small>Conoce el negocio y sus principales cifras.</small></span><span aria-hidden="true">↗</span></Link>
        <Link to="/compare"><span className="index-number">03</span><span><strong>Comparar dos empresas</strong><small>Revisa sus cifras del mismo año. Herramienta Pro.</small></span><span aria-hidden="true">↗</span></Link>
        <p>Una cifra cobra sentido cuando conoces de dónde viene.</p>
      </aside>
    </section>
    <div className="reading-route-heading"><p className="section-eyebrow">UN MÉTODO PARA EXPLORAR</p><span>Del símbolo al contexto</span></div>
    <div className="onboarding-grid">
      <article><span className="step-number">01</span><h2>Encuentra una empresa</h2><p>Busca por nombre o símbolo, filtra por sector y ordena los resultados.</p></article>
      <article><span className="step-number">02</span><h2>Entiende las cifras</h2><p>Cada indicador explica qué mide. Su fecha y su fuente te ayudan a ponerlo en contexto.</p></article>
      <article><span className="step-number">03</span><h2>Revisa los límites</h2><p>Explora el balance y los precios históricos. Los resultados del pasado no garantizan ganancias futuras.</p></article>
    </div>
    <RecentCompanies />
  </WorkspaceLayout>
}
