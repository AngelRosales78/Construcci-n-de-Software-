import { useNavigate } from 'react-router-dom'
import WorkspaceLayout from './layout/WorkspaceLayout'

export default function Dashboard() {
  const navigate = useNavigate()
  return <WorkspaceLayout>
    <section className="welcome-dashboard">
      <p className="section-eyebrow">TU ESPACIO DE ANÁLISIS</p>
      <h1>Conoce la empresa.<br />Entiende sus números.</h1>
      <p className="section-description">Explora el S&amp;P 500 y consulta indicadores financieros y precios históricos en un solo lugar.</p>
      <button className="primary-action" onClick={() => navigate('/search')}>Buscar empresas y consultar fichas <span aria-hidden="true">→</span></button>
      <div className="welcome-chart" aria-hidden="true"><svg viewBox="0 0 520 210"><path d="M0 185H520M0 135H520M0 85H520M0 35H520" stroke="#d9e5df" strokeWidth="1" /><path d="M20 170L65 148L110 155L155 122L200 137L245 96L290 109L335 67L380 78L425 42L485 20" fill="none" stroke="#158569" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round" /></svg><span>Una lectura más clara de los datos</span></div>
    </section>
    <div className="onboarding-grid">
      <article><span className="step-number">01</span><h2>Encuentra una empresa</h2><p>Busca por nombre o símbolo, filtra por sector y ordena los resultados.</p></article>
      <article><span className="step-number">02</span><h2>Lee sus indicadores</h2><p>Consulta PER, ROE, capitalización, beta y dividendo, junto con su fuente y fecha.</p></article>
      <article><span className="step-number">03</span><h2>Explora su evolución</h2><p>Revisa el perfil Altman-Z y amplía el gráfico de precios históricos.</p></article>
    </div>
  </WorkspaceLayout>
}
