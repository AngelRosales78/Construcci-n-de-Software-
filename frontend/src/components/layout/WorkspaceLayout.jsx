import { Link, useLocation } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useSessionViewModel } from '../../viewmodels/useSessionViewModel'
import NotificationBell from '../notifications/NotificationBell'

export default function WorkspaceLayout({ children }) {
  const { pathname } = useLocation()
  const { name, role, avatarUrl, leaving, logout } = useSessionViewModel()
  const [menuOpen, setMenuOpen] = useState(false)
  useEffect(() => { setMenuOpen(false) }, [pathname])
  return <div className="workspace-shell">
    <a className="skip-link" href="#workspace-content">Ir al contenido</a>
    {menuOpen && <button className="navigation-backdrop" aria-label="Cerrar navegación" onClick={() => setMenuOpen(false)} />}
    <aside className={`workspace-sidebar ${menuOpen ? 'is-open' : ''}`} id="primary-sidebar">
      <Link to="/search" className="workspace-brand" aria-label="S&P 500 · Explorar empresas">
        <svg viewBox="0 0 36 36" width="36" height="36" aria-hidden="true"><rect width="36" height="36" rx="10" fill="#262a31" /><path d="m9 24 7-8 5 4 7-10m-7 0h7v7" fill="none" stroke="#75ff9e" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" /></svg>
        <span><strong>S&amp;P 500 Finance</strong><small>INVESTIGACIÓN FINANCIERA</small></span>
      </Link>
      <nav aria-label="Navegación principal">
        <Link to="/dashboard" aria-current={pathname === '/dashboard' ? 'page' : undefined}>Inicio</Link>
        <Link to="/search" aria-current={pathname === '/search' || pathname.startsWith('/company/') ? 'page' : undefined}>Explorar empresas</Link>
        <Link to="/favorites" aria-current={pathname === '/favorites' ? 'page' : undefined}>Favoritos</Link>
        <Link to="/compare" aria-current={pathname === '/compare' ? 'page' : undefined}>Comparar</Link>
        <Link to="/notifications" aria-current={pathname === '/notifications' ? 'page' : undefined}>Mis avisos</Link>
        <span className="navigation-caption">TU CUENTA</span>
        <Link to="/profile/subscription" aria-current={pathname === '/profile/subscription' ? 'page' : undefined}>Mi suscripción</Link>
        <Link to="/profile" aria-current={pathname === '/profile' ? 'page' : undefined}>Mi perfil</Link>
        {role === 'admin' && <Link to="/admin-panel" aria-current={pathname.startsWith('/admin-panel') ? 'page' : undefined}>Administración</Link>}
      </nav>
      <div className="sidebar-note"><span className="sidebar-indicator" /><p>Información con contexto<small>Fuentes y fechas en cada ficha.</small></p></div>
    </aside>
    <header className="workspace-header">
      <div className="workspace-heading"><button className="navigation-toggle" aria-label="Abrir navegación" aria-expanded={menuOpen} aria-controls="primary-sidebar" onClick={() => setMenuOpen(!menuOpen)}>☰</button><span>Tu espacio financiero</span><small>{pathname.startsWith('/admin-panel') ? 'Administración' : pathname.startsWith('/company/') ? 'Ficha de empresa' : 'Exploración y análisis'}</small></div>
      <div className="workspace-account"><Link className="account-identity" to="/profile"><span className="account-avatar" aria-hidden="true">{avatarUrl ? <img src={avatarUrl} alt="" style={{width:36,height:36,borderRadius:'50%',objectFit:'cover'}} /> : name.slice(0, 1).toUpperCase()}</span><span className="account-name">{name}</span></Link>
        <NotificationBell />
        <button className="logout-button" aria-label={leaving ? 'Cerrando…' : 'Cerrar Sesión'} onClick={logout} disabled={leaving}>
          <span className="logout-label">{leaving ? 'Cerrando…' : 'Cerrar Sesión'}</span>
          <svg className="logout-icon" width="17" height="17" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 4H4v16h5m6-12 4 4-4 4m-6-4h10" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" /></svg>
        </button>
      </div>
    </header>
    <main id="workspace-content" className="workspace-content">{children}</main>
    <footer className="workspace-footer"><span>S&amp;P 500 · Exploración y análisis de empresas</span><span>Fuentes y fechas visibles en cada ficha</span></footer>
  </div>
}
