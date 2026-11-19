import { Link } from 'react-router-dom'

export default function NotificationItems({vm,compact=false}) {
  const rows=Array.isArray(vm.resource.data?.results)?vm.resource.data.results:[]
  if(!rows.length)return <p>No tienes avisos en esta lista.</p>
  return <ul className="notification-items">{(compact?rows.slice(0,3):rows).map(item=><li key={item.id} className={item.read_at?'notification-read':'notification-unread'}>
    <Link to={item.url}>{item.title}</Link>
    <small>{new Date(item.created_at).toLocaleString('es-PE')} · {item.read_at?'Leído':'Sin leer'}</small>
    {item.is_demo&&<p className="notification-demo">Aviso de prueba. No representa un cambio real de la empresa.</p>}
    <p>{item.message}</p>
    {!compact&&<details><summary>Ver balance y procedencia</summary>
      <p>Referencia anterior: {item.before.as_of}. Referencia nueva: {item.after.as_of}.</p>
      <p>Fuente anterior: {item.before.source}</p><p>Fuente nueva: {item.after.source}</p>
      <p>Clasificación académica del balance. No es una recomendación de inversión.</p>
    </details>}
    {!item.read_at&&<button disabled={vm.busy||vm.resource.loading} onClick={()=>vm.mark(item.id)} aria-label={`Marcar aviso ${item.id} como leído`}>Marcar leído</button>}
  </li>)}</ul>
}
