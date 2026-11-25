import { Link, useLocation } from 'react-router-dom'
import { useEffect, useRef, useState } from 'react'
import { useSessionViewModel } from '../../viewmodels/useSessionViewModel'
import NotificationBell from '../notifications/NotificationBell'
import BrandMark from './BrandMark'
import AssistantLauncher from '../company/AssistantLauncher'
import Icon from '../shared/Icon'
import {openAssistant} from '../../utils/assistantEvents'

export default function WorkspaceLayout({ children }) {
  const { pathname } = useLocation()
  const { name, role, avatarUrl, leaving, logout } = useSessionViewModel()
  const [menuOpen, setMenuOpen] = useState(false)
  const sidebar = useRef(null)
  useEffect(() => { setMenuOpen(false) }, [pathname])
  useEffect(() => {
    if (!menuOpen) return
    const previousFocus = document.activeElement
    const previousOverflow = document.body.style.overflow
    const links = () => [...sidebar.current.querySelectorAll('a[href],button:not(:disabled)')]
    document.body.style.overflow = 'hidden'
    links()[0]?.focus()
    const keyboard = event => {
      if (event.key === 'Escape') { event.preventDefault(); setMenuOpen(false) }
      if (event.key !== 'Tab') return
      const items = links(), first = items[0], last = items.at(-1)
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
    }
    const desktop = window.matchMedia('(min-width: 761px)')
    const resize = () => { if (desktop.matches) setMenuOpen(false) }
    document.addEventListener('keydown', keyboard)
    desktop.addEventListener('change', resize)
    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', keyboard)
      desktop.removeEventListener('change', resize)
      previousFocus?.focus()
    }
  }, [menuOpen])
  return <div className="workspace-shell">
    <a className="skip-link" href="#workspace-content">Ir al contenido</a>
    {menuOpen && <button className="navigation-backdrop" aria-label="Cerrar navegación" onClick={() => setMenuOpen(false)} />}
    <aside ref={sidebar} className={`workspace-sidebar ${menuOpen ? 'is-open' : ''}`} id="primary-sidebar">
      <Link to="/search" className="workspace-brand" aria-label="S&P 500 · Explorar empresas">
        <BrandMark />
        <span><strong>S&amp;P 500</strong><small>EMPRESAS EN PALABRAS CLARAS</small></span>
      </Link>
      <nav aria-label="Navegación principal">
        <Link to="/dashboard" aria-label="Inicio" aria-current={pathname === '/dashboard' ? 'page' : undefined}><Icon name="overview"/>Inicio</Link>
        <Link to="/search" aria-label="Explorar empresas" aria-current={pathname === '/search' || pathname.startsWith('/company/') ? 'page' : undefined}><Icon name="search"/>Explorar empresas</Link>
        <Link to="/favorites" aria-label="Favoritos" aria-current={pathname === '/favorites' ? 'page' : undefined}><Icon name="bookmark"/>Favoritos</Link>
        <Link to="/compare" aria-label="Comparar" aria-current={pathname === '/compare' ? 'page' : undefined}><Icon name="simulation"/>Comparar</Link>
        <Link to="/notifications" aria-label="Alertas de tus empresas" aria-current={pathname === '/notifications' ? 'page' : undefined}><Icon name="bell"/>Alertas de empresas</Link>
        <button type="button" className="sidebar-assistant" aria-haspopup="dialog" onClick={()=>{setMenuOpen(false);openAssistant()}}><Icon name="chat"/>Consultar a la IA<span>IA</span></button>
        <span className="navigation-caption">TU CUENTA</span>
        <Link to="/profile/subscription" aria-label="Mi suscripción" aria-current={pathname === '/profile/subscription' ? 'page' : undefined}>Mi suscripción</Link>
        <Link to="/profile" aria-label="Mi perfil" aria-current={pathname === '/profile' ? 'page' : undefined}>Mi perfil</Link>
        {role === 'admin' && <Link to="/admin-panel" aria-label="Administración" aria-current={pathname.startsWith('/admin-panel') ? 'page' : undefined}>Administración</Link>}
      </nav>
      <div className="sidebar-note"><Icon name="pillars"/><p>Entender viene primero.<small>Cada resultado conserva su fecha y su fuente.</small></p></div>
    </aside>
    <header className="workspace-header">
      <div className="workspace-heading"><button className="navigation-toggle" aria-label="Abrir navegación" aria-expanded={menuOpen} aria-controls="primary-sidebar" onClick={() => setMenuOpen(!menuOpen)}>☰</button><span>S&amp;P 500 <b>/</b> Finance</span><small>{pathname.startsWith('/admin-panel') ? 'Administración' : pathname.startsWith('/company/') ? 'Ficha de empresa' : 'Exploración y análisis'}</small></div>
      <div className="workspace-account"><button type="button" className="mobile-ai-trigger" aria-label="Abrir asistente IA" aria-haspopup="dialog" onClick={()=>openAssistant()}><Icon name="chat" size={17}/><span>IA</span></button><Link className="account-identity" to="/profile"><span className="account-avatar" aria-hidden="true">{avatarUrl ? <img src={avatarUrl} alt="" style={{width:36,height:36,borderRadius:'50%',objectFit:'cover'}} /> : name.slice(0, 1).toUpperCase()}</span><span className="account-name">{name}</span></Link>
        <NotificationBell />
        <button className="logout-button" aria-label={leaving ? 'Cerrando…' : 'Cerrar Sesión'} onClick={logout} disabled={leaving}>
          <span className="logout-label">{leaving ? 'Cerrando…' : 'Cerrar Sesión'}</span>
          <svg className="logout-icon" width="17" height="17" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 4H4v16h5m6-12 4 4-4 4m-6-4h10" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" /></svg>
        </button>
      </div>
    </header>
    <main id="workspace-content" className="workspace-content">{children}</main>
    <AssistantLauncher />
    <footer className="workspace-footer"><span>S&amp;P 500 FINANCE <b> / </b> CUADERNO DE ANÁLISIS</span><span>La información empieza en sus fuentes.</span></footer>
  </div>
}
