import BrandMark from '../layout/BrandMark'

export default function AuthLayout({ children, large = false }) {
  return <div className="auth-container">
    <aside className="auth-editorial" aria-label="FINDSCORE">
      <div className="auth-wordmark"><BrandMark /><span>FINDSCORE <small>EMPRESAS EN PALABRAS CLARAS</small></span></div>
      <div className="auth-editorial-story">
        <p className="editorial-kicker">EMPRESAS EN PALABRAS CLARAS</p>
        <h2>Entiende una empresa.<br /><em>Pregunta lo que<br />no entiendas.</em></h2>
        <p>Explora sus finanzas, reconoce qué dice su balance y resuelve tus dudas con ayuda de la IA.</p>
      </div>
      <div className="auth-editorial-footer"><span>Fuentes y fechas a la vista.</span><span>INFORMACIÓN PARA ENTENDER</span></div>
    </aside>
    <div className="auth-form-region">
      <p className="auth-edition">FINDSCORE <span>ACCESO PERSONAL</span></p>
      <div className={`auth-card${large ? ' auth-card--large' : ''}`}>{children}</div>
      <p className="auth-footnote">Información financiera con contexto, en un solo lugar.</p>
    </div>
  </div>
}
