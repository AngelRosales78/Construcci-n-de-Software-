import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import WorkspaceLayout from '../layout/WorkspaceLayout'
import ConfirmationDialog from './ConfirmationDialog'
import { useAdminUsersViewModel } from '../../viewmodels/useAdminUsersViewModel'
import './account.css'

export default function AdminUsersPage() {
  const vm = useAdminUsersViewModel()
  const [query, setQuery] = useState(vm.params.get('q') || '')
  const appliedQuery = vm.params.get('q') || ''
  useEffect(() => setQuery(appliedQuery), [appliedQuery])
  return <WorkspaceLayout><div className="account-page">
    <Link to="/admin-panel">Volver a administración</Link><h1>Usuarios</h1>
    <form className="account-filters" onSubmit={event => { event.preventDefault(); vm.update({ q: query }) }}>
      <label>Nombre o correo<input maxLength={120} value={query} onChange={event => setQuery(event.target.value)} /></label>
      <button>Buscar</button>
      <label>Plan<select value={vm.params.get('plan') || 'all'} onChange={event => vm.update({ plan: event.target.value })}>
        <option value="all">Todos los planes</option><option value="free">Free</option><option value="pro">Pro</option>
      </select></label>
      <label>Estado<select value={vm.params.get('state') || 'all'} onChange={event => vm.update({ state: event.target.value })}>
        <option value="all">Todos los estados</option><option value="active">Activos</option><option value="blocked">Bloqueados</option>
      </select></label>
    </form>
    {(vm.resource.error || vm.error) && <p role="alert">{vm.resource.error || vm.error}</p>}
    {vm.resource.loading && <p role="status">Consultando usuarios…</p>}
    {vm.message && <p role="status">{vm.message}</p>}
    {vm.resource.data && <><p>{vm.resource.data.count} usuarios encontrados</p>
      <div className="account-table"><table><thead><tr><th>ID</th><th>Usuario</th><th>Correo</th><th>Rol</th><th>Plan</th><th>Estado</th><th>Acciones</th></tr></thead>
        <tbody>{vm.resource.data.results.map(user => <tr key={user.id}>
          <td>{user.id}</td><td>{user.first_name || user.username} {user.last_name}</td><td>{user.email}</td>
          <td><select aria-label={`Rol de ${user.username}`} value={user.role} disabled={vm.busy}
            onChange={event => vm.choose(user, { role: event.target.value })}><option value="user">Usuario</option><option value="admin">Administrador</option></select></td>
          <td><select aria-label={`Plan de ${user.username}`} value={user.plan} disabled={vm.busy}
            onChange={event => vm.choose(user, { plan: event.target.value })}><option value="free">Free</option><option value="pro">Pro</option></select></td>
          <td>{user.is_active ? 'Activo' : 'Bloqueado'}{user.blocked_reason && <small>{user.blocked_reason}</small>}</td>
          <td><button disabled={vm.busy} onClick={() => vm.choose(user, { is_active: !user.is_active })}>{user.is_active ? 'Bloquear' : 'Desbloquear'}</button></td>
        </tr>)}</tbody></table></div>
      {!vm.resource.data.results.length && <p>No hay usuarios con esos filtros.</p>}
      <div className="account-actions"><button disabled={!vm.resource.data.previous || vm.resource.loading} onClick={() => vm.go(-1)}>Anterior</button>
        <span>Página {vm.params.get('page') || 1}</span><button disabled={!vm.resource.data.next || vm.resource.loading} onClick={() => vm.go(1)}>Siguiente</button></div>
    </>}
    <ConfirmationDialog open={Boolean(vm.pending)} title="Confirmar cambio de usuario" busy={vm.busy}
      onCancel={vm.cancel} onConfirm={vm.confirm} confirmLabel="Guardar cambio">
      {vm.pending && <><p>Cuenta · {vm.pending.user.username}</p>
        {vm.pending.changes.role && <p>Nuevo rol · {vm.pending.changes.role === 'admin' ? 'Administrador' : 'Usuario'}</p>}
        {vm.pending.changes.plan && <p>Nuevo plan · {vm.pending.changes.plan === 'pro' ? 'Pro asignado por administración' : 'Free'}</p>}
        {vm.pending.changes.is_active === false && <label>Motivo del bloqueo<input required maxLength={240} value={vm.reason} onChange={event => vm.setReason(event.target.value)} /></label>}
        {vm.pending.changes.is_active === true && <p>La cuenta podrá iniciar una sesión nueva.</p>}
      </>}{vm.error && <p role="alert">{vm.error}</p>}
    </ConfirmationDialog>
  </div></WorkspaceLayout>
}
