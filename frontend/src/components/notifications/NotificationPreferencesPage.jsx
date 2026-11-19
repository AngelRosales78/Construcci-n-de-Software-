import { Link } from 'react-router-dom'
import WorkspaceLayout from '../layout/WorkspaceLayout'
import { useNotificationPreferencesViewModel } from '../../viewmodels/useNotificationPreferencesViewModel'
import './notifications.css'

export default function NotificationPreferencesPage() {
  const vm=useNotificationPreferencesViewModel()
  return <WorkspaceLayout><section className="notification-page">
    <h1>Preferencias de avisos</h1><Link to="/notifications">Ver mis avisos</Link>
    <p>Los cambios se guardan automáticamente. Desactivarlos no borra avisos anteriores.</p>
    {vm.resource.loading&&<p role="status">Consultando preferencias…</p>}
    {(vm.error||vm.resource.error)&&<p role="alert">{vm.error||vm.resource.error}</p>}
    {vm.busy&&<p role="status">Guardando…</p>}{vm.message&&<p role="status">{vm.message}</p>}
    {vm.form&&<div className="notification-preferences">
      <h2>Tipo de aviso</h2><label><input type="checkbox" checked={vm.form.rating_changes} disabled={vm.busy} onChange={event=>vm.change('rating_changes',event.target.checked)}/> Cambios del perfil Altman-Z</label>
      <h2>Canales</h2>{[['web','En la web'],['email','Correo de alertas'],['push','Push en Android']].map(([channel,label])=><div key={channel}>
        <label><input type="checkbox" checked={vm.form.channels[channel]} disabled={vm.busy||!vm.form.available_channels[channel]} onChange={event=>vm.change(channel,event.target.checked)}/> {label}</label>
        {!vm.form.available_channels[channel]&&<p>{vm.form.channel_messages[channel]}</p>}
      </div>)}
    </div>}
    {vm.error&&<button disabled={vm.busy} onClick={vm.retry}>Reintentar guardado</button>}
    <button disabled={vm.busy} onClick={vm.reload}>Recargar preferencias</button>
  </section></WorkspaceLayout>
}
