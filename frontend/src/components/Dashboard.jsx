import {Link,useNavigate} from 'react-router-dom'
import {useRef,useState} from 'react'
import WorkspaceLayout from './layout/WorkspaceLayout'
import RecentCompanies from './RecentCompanies'
import SectorExplorer from './search/SectorExplorer'
import CompanyMark from './shared/CompanyMark'
import Icon from './shared/Icon'
import AskAI from './shared/AskAI'

export default function Dashboard() {
  const navigate=useNavigate(),guide=useRef(null)
  const [query,setQuery]=useState(''),[showGuide,setShowGuide]=useState(false)
  const startGuide=()=>{setShowGuide(value=>!value);if(!showGuide)requestAnimationFrame(()=>guide.current?.focus())}
  return <WorkspaceLayout>
    <section className="clarity-hero">
      <div className="clarity-hero-copy"><p className="section-eyebrow">FINANZAS QUE PUEDES ENTENDER</p><h1>Detrás de cada cifra,<br/><span>hay una empresa.</span></h1><p>Busca una empresa, entiende su balance y pregunta a la IA qué significa cada resultado.</p>
        <form className="hero-search" onSubmit={event=>{event.preventDefault();navigate('/search'+(query.trim()?`?q=${encodeURIComponent(query.trim())}`:''))}}><label htmlFor="home-company-search">¿Qué empresa quieres conocer?</label><div><Icon name="search"/><input id="home-company-search" value={query} onChange={event=>setQuery(event.target.value)} placeholder="Por ejemplo: Apple o AAPL" maxLength={100}/><button className="primary-action" type="submit">Explorar empresas <span aria-hidden="true">→</span></button></div></form>
        <div className="hero-starters"><span>Empieza con una conocida</span>{[['AAPL','Apple'],['MSFT','Microsoft'],['NVDA','Nvidia']].map(([ticker,name])=><Link key={ticker} to={`/company/${ticker}`}><CompanyMark company={{ticker,name}}/>{name}</Link>)}</div>
        <div className="hero-guidance"><button type="button" aria-expanded={showGuide} aria-controls="home-start-guide" onClick={startGuide}>Soy nuevo, ¿cómo empiezo?</button><AskAI>Quiero ayuda para entender</AskAI></div>
      </div>
      <figure className="hero-company-image"><img src="/editorial-images/architecture.jpg" alt="Edificios de oficinas vistos desde la calle" width="640" height="700" fetchPriority="high"/><figcaption><span>EL NEGOCIO ANTES QUE EL NÚMERO</span><strong>Conoce qué hay<br/>detrás de las empresas.</strong><p>Datos históricos · Fuentes consultables</p></figcaption><div className="hero-image-tag"><Icon name="pillars"/><span>Entiende el balance.<br/><strong>Forma tu propia lectura.</strong></span></div></figure>
    </section>
    <section ref={guide} tabIndex={-1} id="home-start-guide" hidden={!showGuide} className="home-start-guide" aria-label="Cómo empezar en tres pasos"><div className="section-title"><h2>Tu primera visita, en tres pasos</h2><button type="button" onClick={()=>setShowGuide(false)}>Cerrar recorrido</button></div><ol><li><span>1</span><h3>Elige una empresa</h3><p>Busca una que conozcas. Su ficha explica qué hace y qué información tiene.</p><Link to="/search">Buscar una empresa →</Link></li><li><span>2</span><h3>Lee el resultado, no solo la nota</h3><p>El color y el nombre de la zona explican la clasificación del balance.</p><Link to="/company/MSFT">Ver una ficha →</Link></li><li><span>3</span><h3>Pregunta lo que no entiendas</h3><p>La IA explica las cifras y sus límites usando los datos de la empresa.</p><AskAI ticker="MSFT" question="¿Qué significa su Altman-Z?">Probar una pregunta</AskAI></li></ol></section>
    <section className="home-purpose-strip" aria-label="Qué puedes hacer"><article><Icon name="pillars"/><div><h2>Entender un balance</h2><p>Clasificaciones con significado y contexto.</p></div></article><article><Icon name="chat"/><div><h2>Resolver tus dudas</h2><p>Ayuda con IA junto a los resultados.</p></div></article><article><Icon name="bell"/><div><h2>Seguir tus empresas</h2><p>Favoritos y alertas de clasificación.</p></div></article></section>
    <SectorExplorer/>
    <section className="home-alert-invitation"><span className="home-alert-icon"><Icon name="bell" size={28}/></span><div><h2>Sigue las empresas que te interesan.</h2><p>Guárdalas en favoritos y recibe una alerta cuando cambie su clasificación Altman-Z tras actualizar un balance.</p></div><Link to="/notifications">Cómo funcionan las alertas →</Link></section>
    <RecentCompanies/>
  </WorkspaceLayout>
}
