import { Link, useLocation } from 'react-router-dom'
import { useSessionViewModel } from '../../viewmodels/useSessionViewModel'

export default function WorkspaceLayout({ children }) {
  const { pathname } = useLocation()
  const { name, leaving, logout } = useSessionViewModel()
  return <div className="workspace-shell">
    <a className="skip-link" href="#workspace-content">Ir al contenido</a>
    <header className="workspace-header">
      <Link to="/search" className="workspace-brand" aria-label="S&P 500 · Explorar empresas">
        <svg viewBox="0 0 36 36" width="36" height="36" aria-hidden="true"><rect width="36" height="36" rx="10" fill="#262a31" /><path d="m9 24 7-8 5 4 7-10m-7 0h7v7" fill="none" stroke="#75ff9e" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" /></svg>
        <span><strong>S&amp;P 500 Finance</strong><small>EXPLORACIÓN Y ANÁLISIS</small></span>
      </Link>
      <nav aria-label="Navegación principal">
        <Link to="/dashboard" aria-current={pathname === '/dashboard' ? 'page' : undefined}>Inicio</Link>
        <Link to="/search" aria-current={pathname === '/search' ? 'page' : undefined}>Explorar empresas</Link>
        {pathname.startsWith('/company/') && <span className="workspace-current">Ficha financiera</span>}
      </nav>
      <div className="workspace-account"><span className="terminal-status">Datos importados</span><span className="account-avatar" aria-hidden="true">{name.slice(0, 1).toUpperCase()}</span><span className="account-name">{name}</span>
        <button className="logout-button" onClick={logout} disabled={leaving}>{leaving ? 'Cerrando…' : 'Cerrar Sesión'}</button>
      </div>
    </header>
    <main id="workspace-content" className="workspace-content">{children}</main>
    <footer className="workspace-footer"><span>S&amp;P 500 · Exploración y análisis de empresas</span><span>Fuentes y fechas visibles en cada ficha</span></footer>
  </div>
}
