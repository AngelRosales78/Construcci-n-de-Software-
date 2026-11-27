import { Link } from 'react-router-dom'
import WorkspaceLayout from '../layout/WorkspaceLayout'
import ConfirmationDialog from './ConfirmationDialog'
import { useProfileViewModel } from '../../viewmodels/useProfileViewModel'
import './account.css'
import ActionNotice from '../shared/ActionNotice'

export default function ProfilePage() {
  const vm = useProfileViewModel()
  return <WorkspaceLayout><div className="account-page profile-page">
    <h1>Mi perfil</h1><Link to="/profile/subscription">Suscripción y comprobantes</Link>
    {vm.profile.loading && <p role="status">Consultando perfil…</p>}
    {(vm.error || vm.profile.error) && <p role="alert">{vm.error || vm.profile.error}</p>}
    <ActionNotice message={vm.message}/>
    {vm.profile.data && <>
      <form className="sprint3-panel profile-form" onSubmit={event => { event.preventDefault(); vm.save() }}>
        <h2>Información personal</h2><p>{vm.profile.data.email}</p>
        <label>Nombre<input maxLength={150} value={vm.firstName} onChange={event => vm.setFirstName(event.target.value)} /></label>
        <label>Apellido<input maxLength={150} value={vm.lastName} onChange={event => vm.setLastName(event.target.value)} /></label>
        <label className="profile-upload-field">Foto de perfil<input type="file" accept="image/jpeg,image/png,image/webp" onChange={event => vm.selectAvatar(event.target.files[0])} /></label>
        <p>JPEG, PNG o WebP, menos de 2 MB. La foto se recorta en círculo.</p>
        {vm.preview && <img className="profile-preview" src={vm.preview} alt="Vista previa de la foto de perfil" />}
        <button disabled={vm.busy || vm.profile.loading} aria-busy={vm.busy} type="submit">{vm.busy ? 'Guardando…' : 'Guardar perfil'}</button>
      </form>
      <section className="sprint3-panel account-danger"><h2>Eliminar mi cuenta</h2>
        <p>Se borrarán tu perfil, foto, sesiones, favoritos, etiquetas, historial, escenarios y reportes. Los registros administrativos y de suscripción que se conserven quedarán sin referencias a tu cuenta.</p>
        <button disabled={vm.busy} onClick={() => vm.setDeleting(true)}>Solicitar eliminación</button>
      </section>
    </>}
    <ConfirmationDialog open={vm.deleting} title="Eliminar cuenta y datos definitivamente" busy={vm.busy}
      onCancel={() => { vm.setDeleting(false); vm.setPassword('') }} onConfirm={vm.remove} confirmLabel="Eliminar mi cuenta definitivamente">
      <p>Esta acción es irreversible. Confirma tu contraseña actual para continuar.</p>
      <label>Contraseña actual<input autoComplete="current-password" type="password" value={vm.password} onChange={event => vm.setPassword(event.target.value)} /></label>
      {vm.deleting && vm.error && <p role="alert">{vm.error}</p>}
    </ConfirmationDialog>
  </div></WorkspaceLayout>
}
