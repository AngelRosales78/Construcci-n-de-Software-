import BrandMark from '../layout/BrandMark'
import Icon from '../shared/Icon'

export default function AuthLayout({ children, large = false }) {
  return <div className={`auth-container auth-layout${large ? ' auth-layout-register' : ''}`}>
    <aside className="auth-editorial" aria-label="FINDSCORE">
      <div className="auth-wordmark"><BrandMark /><span>FINDSCORE <small>EMPRESAS EN PALABRAS CLARAS</small></span></div>
      <div className="auth-editorial-story">
        <p className="editorial-kicker">TU PUNTO DE PARTIDA</p>
        <h2>Las empresas,<br /><em>en palabras claras.</em></h2>
        <p>Conoce sus finanzas y pregunta lo que no entiendas.</p>
        <ul className="auth-highlights"><li><Icon name="search"/><span>Explora empresas que conoces</span></li><li><Icon name="pillars"/><span>Entiende qué dicen sus cifras</span></li><li><Icon name="chat"/><span>Resuelve tus dudas con IA</span></li></ul>
      </div>
      <div className="auth-editorial-footer"><span>Entender viene primero.</span><span>Fuentes y fechas a la vista.</span></div>
    </aside>
    <div className="auth-form-region">
      <div className={`auth-card${large ? ' auth-card--large' : ''}`}>{children}</div>
      <p className="auth-footnote">FINDSCORE · Información financiera con contexto.</p>
    </div>
  </div>
}
