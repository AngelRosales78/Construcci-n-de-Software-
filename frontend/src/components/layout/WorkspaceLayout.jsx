import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { authService } from '../../services/authService'

export default function WorkspaceLayout({ children }) {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const [leaving, setLeaving] = useState(false)
  let user = {}
  try { user = authService.getCurrentUser() || {} } catch { /* Sesión local inválida. */ }
  const name = user.first_name || user.username || 'Mi cuenta'
  const logout = async () => {
    setLeaving(true)
    try { await authService.logout(authService.getRefreshToken()) }
    catch { /* El servicio también elimina la sesión local cuando falla la red. */ }
    finally { navigate('/login') }
  }
  return <div className="workspace-shell">
    <a className="skip-link" href="#workspace-content">Ir al contenido</a>
    <header className="workspace-header">
      <Link to="/search" className="workspace-brand" aria-label="S&P 500 · Explorar empresas">
        <svg viewBox="0 0 36 36" width="36" height="36" aria-hidden="true"><rect width="36" height="36" rx="10" fill="#b8e8cd" /><path d="M9 25V19M17 25V13M25 25V8" stroke="#173a32" strokeWidth="4" strokeLinecap="round" /></svg>
        <span><strong>S&amp;P 500</strong><small>Análisis financiero</small></span>
      </Link>
      <nav aria-label="Navegación principal">
        <Link to="/dashboard" aria-current={pathname === '/dashboard' ? 'page' : undefined}>Inicio</Link>
        <Link to="/search" aria-current={pathname === '/search' ? 'page' : undefined}>Explorar empresas</Link>
        {pathname.startsWith('/company/') && <span className="workspace-current">Ficha financiera</span>}
      </nav>
      <div className="workspace-account"><span className="account-avatar" aria-hidden="true">{name.slice(0, 1).toUpperCase()}</span><span className="account-name">{name}</span>
        <button className="logout-button" onClick={logout} disabled={leaving}>{leaving ? 'Cerrando…' : 'Cerrar Sesión'}</button>
      </div>
    </header>
    <main id="workspace-content" className="workspace-content">{children}</main>
    <footer className="workspace-footer"><span>S&amp;P 500 · Exploración y análisis de empresas</span><span>Fuentes y fechas visibles en cada ficha</span></footer>
  </div>
}
