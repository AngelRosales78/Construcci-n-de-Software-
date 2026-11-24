import BrandMark from '../layout/BrandMark'

export default function AuthLayout({ children, large = false }) {
  return <div className="auth-container">
    <aside className="auth-editorial" aria-label="S&P 500 Finance">
      <div className="auth-wordmark"><BrandMark /><span>S&amp;P 500 <small>FINANCE</small></span></div>
      <div className="auth-editorial-story">
        <p className="editorial-kicker">CUADERNO DE ANÁLISIS FINANCIERO</p>
        <h2>Una empresa.<br />Muchas cifras.<br /><em>Tu propia lectura.</em></h2>
        <p>Encuentra el contexto detrás de los indicadores, explora balances y construye una lectura más informada de cada empresa.</p>
      </div>
      <div className="auth-editorial-footer"><span>Fuentes y fechas a la vista.</span><span>01 — EL PUNTO DE PARTIDA</span></div>
    </aside>
    <div className="auth-form-region">
      <p className="auth-edition">S&amp;P 500 FINANCE <span>ACCESO PERSONAL</span></p>
      <div className={`auth-card${large ? ' auth-card--large' : ''}`}>{children}</div>
      <p className="auth-footnote">Información financiera con contexto, en un solo lugar.</p>
    </div>
  </div>
}
