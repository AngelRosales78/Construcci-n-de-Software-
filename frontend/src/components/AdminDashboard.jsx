import { Link } from 'react-router-dom'
import WorkspaceLayout from './layout/WorkspaceLayout'
import { useAdminSummaryViewModel } from '../viewmodels/useAdminSummaryViewModel'

export default function AdminDashboard() {
  const { summary, error } = useAdminSummaryViewModel()
  return <WorkspaceLayout>
    <section className="welcome-dashboard admin-cover">
      <p className="section-eyebrow">ADMINISTRACIÓN</p>
      <h1>Estado de la plataforma</h1>
      {error ? <p role="alert">{error}</p> : summary
        ? <p>{summary.users} usuarios registrados · {summary.companies} empresas en el catálogo.</p>
        : <p role="status">Consultando el resumen…</p>}
      <div className="admin-destinations">
        <Link to="/search"><span>01</span>Explorar empresas <b aria-hidden="true">↗</b></Link>
        <Link to="/admin-panel/users"><span>02</span>Administrar usuarios <b aria-hidden="true">↗</b></Link>
        <Link to="/admin-panel/companies"><span>03</span>Administrar empresas y datos <b aria-hidden="true">↗</b></Link>
        <Link to="/admin-panel/monitoring"><span>04</span>Consultar uso y operación <b aria-hidden="true">↗</b></Link>
      </div>
    </section>
  </WorkspaceLayout>
}
