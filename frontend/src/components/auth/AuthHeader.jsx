import { Link } from 'react-router-dom'

export default function AuthHeader({ mode }) {
  return <>
    <div className="auth-header">
      <span className="auth-logo" aria-hidden="true"><svg width="32" height="32" viewBox="0 0 36 36"><path d="m5 26 10-11 7 6 9-13m-9 0h9v9" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" /></svg></span>
      <h1>S&amp;P 500 Finance</h1>
      <p>Exploración de empresas e información financiera</p>
    </div>
    <nav className="auth-tabs" aria-label="Acceso a la plataforma">
      <Link to="/login" aria-current={mode === 'login' ? 'page' : undefined}>Iniciar sesión</Link>
      <Link to="/register" aria-current={mode === 'register' ? 'page' : undefined}>Crear cuenta</Link>
    </nav>
  </>
}
