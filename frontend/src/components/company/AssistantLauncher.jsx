import {lazy,Suspense,useEffect,useId,useRef,useState} from 'react'
import {useLocation} from 'react-router-dom'
import {ASSISTANT_EVENT} from '../../utils/assistantEvents'
import Icon from '../shared/Icon'
const FinancialAssistant=lazy(()=>import('./FinancialAssistant'))
const CompanyPicker=lazy(()=>import('../compare/CompanyPicker'))

export default function AssistantLauncher() {
  const location=useLocation(),id=useId(),dialog=useRef(null),opener=useRef(null),serial=useRef(0)
  const [open,setOpen]=useState(false),[session,setSession]=useState(null),[selected,setSelected]=useState('')
  const context=()=>{
    const params=new URLSearchParams(location.search),company=location.pathname.match(/^\/company\/([A-Za-z0-9.\-]+)$/)
    const year=/^\d{4}$/.test(params.get('fiscal_year') || '') ? Number(params.get('fiscal_year')) : undefined
    if(company)return {a:company[1].toUpperCase(),year}
    if(location.pathname==='/compare' && ['a','b'].every(key=>/^[A-Z0-9.\-]{1,10}$/.test(params.get(key) || '')) && params.get('a')!==params.get('b'))return {a:params.get('a'),b:params.get('b'),year:year || 2024}
    return {}
  }
  const launch=(detail={})=>{
    const route=context()
    const ticker=typeof detail.ticker==='string' && /^[A-Z0-9.\-]{1,10}$/.test(detail.ticker) ? detail.ticker : undefined
    opener.current=document.activeElement
    const requestedYear=/^\d{4}$/.test(String(detail.year || '')) ? Number(detail.year) : undefined
    const next={...route,...(ticker ? {a:ticker,b:undefined} : {}),year:requestedYear || (ticker && ticker!==route.a ? undefined : route.year),question:typeof detail.question==='string' ? detail.question.slice(0,600) : '',mode:detail.mode==='learn' ? 'learn' : 'question',id:++serial.current}
    setSession(previous=>!detail.ticker && !detail.question && !detail.mode && previous && (!route.a || (previous.a===route.a && previous.b===route.b && previous.year===route.year)) ? previous : next)
    setSelected('');setOpen(true)
  }
  useEffect(()=>{
    const handle=event=>launch(event.detail || {})
    window.addEventListener(ASSISTANT_EVENT,handle)
    return ()=>window.removeEventListener(ASSISTANT_EVENT,handle)
  },[location.pathname,location.search])
  useEffect(()=>{setOpen(false);setSession(null)},[location.pathname,location.search])
  useEffect(()=>{
    if(open && !dialog.current.open)dialog.current.showModal()
    if(!open && dialog.current.open){
      dialog.current.close()
      const target=opener.current?.isConnected && opener.current.getClientRects().length ? opener.current : [...document.querySelectorAll('.mobile-ai-trigger,.assistant-launcher')].find(element=>element.getClientRects().length)
      target?.focus()
    }
    if(open){const previousOverflow=document.documentElement.style.overflow;document.documentElement.style.overflow='hidden';return ()=>{document.documentElement.style.overflow=previousOverflow}}
  },[open])
  const close=()=>setOpen(false)
  return <>
    <button type="button" className="assistant-launcher" aria-haspopup="dialog" aria-expanded={open} aria-controls={id} onClick={()=>launch()}><Icon name="chat" size={22}/><span>Asistente IA<small>Te ayudo a entender</small></span></button>
    <dialog ref={dialog} id={id} className="assistant-drawer" aria-labelledby={`${id}-title`} onCancel={event=>{event.preventDefault();close()}} onClose={()=>setOpen(false)}>
      <header className="assistant-drawer-header"><div><span className="assistant-drawer-symbol"><Icon name="chat" size={21}/></span><div><h2 id={`${id}-title`}>Entiende las cifras con IA</h2><p>{session?.a ? `${session.a}${session.b ? ` y ${session.b}` : ''} · ${session.year || 'Último ejercicio disponible'}` : 'Primero elige una empresa'}</p></div></div><button type="button" onClick={close} aria-label="Cerrar asistente">✕</button></header>
      <div className="workspace-content assistant-drawer-content"><Suspense fallback={<p role="status">Abriendo el asistente…</p>}>
        {session?.a ? <FinancialAssistant key={session.id} a={session.a} b={session.b} year={session.year} initialMode={session.mode} initialQuestion={session.question}/> : <section className="assistant-welcome"><span className="section-eyebrow">PREGUNTA SIN PERDERTE</span><h3>¿Qué empresa quieres entender?</h3><p>Puedes preguntar qué significa su balance, sus deudas o sus puntajes. Las respuestas conservan las cifras y sus fuentes.</p><CompanyPicker side="para el asistente" selected={selected} onSelect={setSelected} onRemove={()=>setSelected('')}/><button className="primary-action" disabled={!selected} onClick={()=>setSession(old=>({...old,a:selected,year:undefined,id:++serial.current}))}>Consultar esta empresa →</button><p className="assistant-welcome-note">También puedes abrir una ficha y pulsar «Explícame este resultado».</p></section>}
      </Suspense></div>
    </dialog>
  </>
}
