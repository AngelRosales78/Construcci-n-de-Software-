import { Link } from 'react-router-dom'
import { useEffect, useRef, useState } from 'react'
import { useNotificationsViewModel } from '../../viewmodels/useNotificationsViewModel'
import NotificationItems from './NotificationItems'
import './notifications.css'

export default function NotificationBell() {
  const vm=useNotificationsViewModel()
  const [open,setOpen]=useState(false)
  const count=vm.resource.data?.unread_count
  const seen=useRef(null)
  const [arrival,setArrival]=useState(0)
  useEffect(()=>{
    const data=vm.resource.data, owner=vm.resource.owner
    if(!data)return
    const latest=Number(data.read_through)
    if(!Number.isFinite(latest))return
    if(!seen.current || seen.current.owner!==owner){
      seen.current={owner,latest};setArrival(0);return
    }
    if(latest>seen.current.latest && data.results?.some(item=>item.id>seen.current.latest && !item.read_at && !item.is_demo))setArrival(value=>value+1)
    seen.current={owner,latest:Math.max(latest,seen.current.latest)}
  },[vm.resource.data,vm.resource.owner])
  return <details className="notification-bell" onToggle={event=>{setOpen(event.currentTarget.open);if(event.currentTarget.open)vm.refresh()}}>
    <summary aria-label={count===undefined?'Notificaciones':`Notificaciones: ${count} sin leer`}>
      <svg key={arrival} className={arrival ? 'notification-arrived' : undefined} onAnimationEnd={()=>setArrival(0)} width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/></svg>
      {count>0&&<span>{count>99?'99+':count}</span>}
    </summary>
    {open&&<div className="notification-dropdown"><strong>Alertas de tus empresas</strong><p className="notification-purpose">Cambios de clasificación Altman-Z en tus favoritas.</p>
      {vm.resource.loading&&<p role="status">Consultando avisos…</p>}
      {(vm.error||vm.resource.error)&&<p role="alert">{vm.error||vm.resource.error} <button onClick={vm.refresh}>Reintentar avisos</button></p>}
      {vm.resource.data&&<NotificationItems vm={vm} compact/>}
      <div className="notification-links"><Link to="/notifications">Ver todos los avisos</Link><Link to="/profile/notifications">Preferencias</Link></div>
    </div>}
  </details>
}
