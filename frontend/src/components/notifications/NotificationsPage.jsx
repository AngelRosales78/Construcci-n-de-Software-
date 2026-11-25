import { Link } from 'react-router-dom'
import WorkspaceLayout from '../layout/WorkspaceLayout'
import { useNotificationsViewModel } from '../../viewmodels/useNotificationsViewModel'
import NotificationItems from './NotificationItems'
import Icon from '../shared/Icon'
import './notifications.css'

export default function NotificationsPage() {
  const vm=useNotificationsViewModel()
  return <WorkspaceLayout><section className="notification-page">
    <p className="section-eyebrow">SIGUE LO QUE TE INTERESA</p><h1>Alertas de tus empresas</h1><p className="section-description">Entérate cuando cambie la clasificación del balance de una empresa que guardaste en favoritos.</p>
    <section className="alerts-explainer" aria-label="Cómo funcionan las alertas"><span className="alerts-illustration"><Icon name="bell" size={42}/></span><div><h2>Una alerta te dice qué cambió.</h2><p>Por ejemplo, si una empresa pasa de zona favorable a intermedia al actualizar su balance, aquí podrás revisar el cambio y su procedencia.</p><strong>Son cambios de Altman-Z, no avisos de subida o bajada del precio.</strong></div></section>
    <ol className="alerts-steps"><li><span>1</span><strong>Guarda una empresa</strong><p>Usa la estrella de su ficha o del explorador.</p></li><li><span>2</span><strong>Elige tus avisos</strong><p>Activa los canales disponibles en preferencias.</p></li><li><span>3</span><strong>Revisa los cambios</strong><p>Recibirás alertas de cambios de clasificación observados después de seguirla.</p></li></ol>
    <div className="notification-links"><Link to="/profile/notifications">Preferencias de avisos</Link>
      <button onClick={vm.refresh}>Actualizar lista</button>
      <button disabled={vm.busy||vm.resource.loading||!vm.resource.data?.unread_count} onClick={()=>vm.mark(true)}>Marcar todos como leídos</button></div>
    {vm.resource.loading&&<p role="status">Consultando avisos…</p>}
    {(vm.error||vm.resource.error)&&<p role="alert">{vm.error||vm.resource.error}</p>}
    {vm.resource.data&&<><p>{vm.resource.data.unread_count} sin leer</p><NotificationItems vm={vm}/>
      {!vm.resource.data.results?.length && <div className="alerts-empty-actions"><p>Una lista vacía significa que no tienes cambios de clasificación para mostrar; no significa que tus empresas no tengan riesgos.</p><Link className="primary-action" to="/search">Buscar empresas para seguir →</Link><Link to="/favorites">Ver mis favoritos</Link></div>}
      <div className="notification-links"><button disabled={vm.resource.loading||!vm.resource.data.previous} onClick={()=>vm.go(-1)}>Anterior</button>
        <span>Página {vm.page}</span><button disabled={vm.resource.loading||!vm.resource.data.next} onClick={()=>vm.go(1)}>Siguiente</button></div></>}
  </section></WorkspaceLayout>
}
