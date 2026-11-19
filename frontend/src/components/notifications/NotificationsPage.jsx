import { Link } from 'react-router-dom'
import WorkspaceLayout from '../layout/WorkspaceLayout'
import { useNotificationsViewModel } from '../../viewmodels/useNotificationsViewModel'
import NotificationItems from './NotificationItems'
import './notifications.css'

export default function NotificationsPage() {
  const vm=useNotificationsViewModel()
  return <WorkspaceLayout><section className="notification-page">
    <h1>Mis avisos</h1><p>Cambios del perfil Altman-Z de tus empresas favoritas.</p>
    <div className="notification-links"><Link to="/profile/notifications">Preferencias de avisos</Link>
      <button onClick={vm.refresh}>Actualizar lista</button>
      <button disabled={vm.busy||vm.resource.loading||!vm.resource.data?.unread_count} onClick={()=>vm.mark(true)}>Marcar todos como leídos</button></div>
    {vm.resource.loading&&<p role="status">Consultando avisos…</p>}
    {(vm.error||vm.resource.error)&&<p role="alert">{vm.error||vm.resource.error}</p>}
    {vm.resource.data&&<><p>{vm.resource.data.unread_count} sin leer</p><NotificationItems vm={vm}/>
      <div className="notification-links"><button disabled={vm.resource.loading||!vm.resource.data.previous} onClick={()=>vm.go(-1)}>Anterior</button>
        <span>Página {vm.page}</span><button disabled={vm.resource.loading||!vm.resource.data.next} onClick={()=>vm.go(1)}>Siguiente</button></div></>}
  </section></WorkspaceLayout>
}
