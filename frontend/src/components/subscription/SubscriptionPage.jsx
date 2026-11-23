import { Link } from 'react-router-dom'
import WorkspaceLayout from '../layout/WorkspaceLayout'
import { useSubscriptionViewModel } from '../../viewmodels/useSubscriptionViewModel'
import './subscription.css'

const date = value => value ? new Date(value).toLocaleString('es-PE') : 'Sin vencimiento asignado'
const emailStatus = { sent: 'Copia enviada por correo', local: 'Copia preparada en la salida local del servidor',
  pending: 'Copia por correo pendiente de envío', not_required: 'Sin comprobante de activación' }

export default function SubscriptionPage() {
  const vm = useSubscriptionViewModel()
  const state = vm.data?.subscription
  const plans = vm.data?.plans
  return <WorkspaceLayout><div className="subscription-page">
    <p className="section-eyebrow">MI PERFIL</p><h1>Mi suscripción</h1>
    <p>Elige el acceso que se adapta a tu forma de investigar. Sin cargos automáticos.</p>
    {vm.loading && <p role="status">Consultando suscripción…</p>}
    {vm.error && <div role="alert"><p>{vm.error}</p><button disabled={vm.busy} onClick={vm.refresh}>Actualizar estado</button></div>}
    {vm.notice && <p role="status">{vm.notice}</p>}
    {state && <section className="sprint3-panel"><h2>Plan {vm.pro ? 'Pro' : 'Free'}</h2>
      <p>{vm.pro ? state.cancelled_at ? 'Renovación cancelada · Pro vigente' : 'Activo' : 'Acceso Free'}</p>
      {vm.pro && <><p>Fin de vigencia · {date(state.expires_at)}</p>
        {state.auto_renew && <p>Fin de la vigencia actual · {date(state.expires_at)}</p>}
        <p>La renovación requiere tu confirmación al finalizar el período.</p>
        {!state.cancelled_at && <button disabled={vm.busy || vm.loading} onClick={() => vm.setCancelOpen(true)}>Cancelar renovación</button>}</>}
      <Link to="/compare">Ir al comparador</Link>
    </section>}
    {plans && <section className="sprint3-panel"><h2>Elige tu plan</h2>
      <div className="subscription-table"><table><caption>Funciones disponibles por plan</caption>
        <thead><tr><th scope="col">Función</th><th scope="col">Free</th><th scope="col">Pro</th></tr></thead>
        <tbody>{plans.features.map(row => <tr key={row.label}><th scope="row">{row.label}</th><td>{row.free ? 'Sí' : '—'}</td><td>{row.pro ? 'Sí' : '—'}</td></tr>)}</tbody>
      </table></div>
      <div className="subscription-pro"><h3>Pro</h3><p>{plans.currency} {plans.amount} por {plans.duration_days} días</p>
        <button disabled={vm.pro || vm.busy || vm.loading} onClick={() => vm.setCheckoutOpen(true)}>{vm.pro ? 'Pro vigente' : 'Elegir Pro'}</button>
      </div>
    </section>}
    {vm.checkoutOpen && <section className="sprint3-panel" aria-label="Activar acceso Pro"><h2>Activar acceso Pro</h2>
      <p>Pro durante {plans.duration_days} días. La activación de esta versión no realiza un cargo monetario.</p>
      {vm.retryOutcome ? <button disabled={vm.busy || vm.loading} onClick={() => vm.checkout(vm.retryOutcome)}>Reintentar la misma operación</button> : <>
        <button disabled={vm.busy || vm.loading} onClick={() => vm.checkout('approved')}>{vm.busy ? 'Procesando…' : 'Confirmar activación'}</button>
</>}
      <button disabled={vm.busy} onClick={() => vm.setCheckoutOpen(false)}>Volver</button>
    </section>}
    {vm.cancelOpen && <section className="sprint3-panel" aria-label="Confirmar cancelación"><h2>¿Cancelar la renovación?</h2>
      <p>{state.expires_at ? `Conservarás Pro hasta ${date(state.expires_at)}.`
        : `Tu acceso asignado sin vencimiento finalizará ${plans.duration_days} días después de confirmar.`}</p>
      <button disabled={vm.busy || vm.loading} onClick={vm.cancel}>Confirmar cancelación</button>
      <button disabled={vm.busy} onClick={() => vm.setCancelOpen(false)}>Conservar suscripción</button>
    </section>}
    {vm.data && <section className="sprint3-panel"><h2>Historial de suscripciones</h2>
      {!vm.data.receipts.length && <p>Aún no tienes operaciones.</p>}
      <ul className="subscription-history">{vm.data.receipts.map(row => <li key={row.id}>
        <span>{date(row.created_at)} · {row.currency} {row.amount} · {row.outcome === 'approved' ? 'Aprobada' : 'Rechazada'}</span>
        <button onClick={() => vm.setReceipt(row)}>Ver operación</button>
      </li>)}</ul>
    </section>}
    {vm.receipt && <section className="sprint3-panel subscription-receipt" aria-label="Comprobante de suscripción">
      <h2>Comprobante de suscripción</h2><p>Registro de activación sin cargo monetario</p>
      <dl><dt>Identificador</dt><dd>{vm.receipt.id}</dd><dt>Plan solicitado</dt><dd>Pro</dd>
        <dt>Resultado</dt><dd>{vm.receipt.outcome === 'approved' ? 'Aprobada' : 'Rechazada'}</dd>
        <dt>Valor de referencia</dt><dd>{vm.receipt.currency} {vm.receipt.amount}</dd>
        <dt>Fecha</dt><dd>{date(vm.receipt.created_at)}</dd>
        {vm.receipt.outcome === 'approved' && <><dt>Vigencia</dt><dd>{date(vm.receipt.starts_at)} hasta {date(vm.receipt.ends_at)}</dd></>}
      </dl><p>{emailStatus[vm.receipt.email_status]}</p>
      <button onClick={() => window.print()}>Imprimir o guardar como PDF</button>
      <button onClick={() => vm.setReceipt(null)}>Cerrar comprobante</button>
    </section>}
  </div></WorkspaceLayout>
}
