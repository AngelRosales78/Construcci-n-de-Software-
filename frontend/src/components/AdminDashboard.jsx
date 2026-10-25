import { Link } from 'react-router-dom'
import WorkspaceLayout from './layout/WorkspaceLayout'
import { useAdminSummaryViewModel } from '../viewmodels/useAdminSummaryViewModel'

export default function AdminDashboard() {
  const { summary, error } = useAdminSummaryViewModel()
  return <WorkspaceLayout>
    <section className="welcome-dashboard">
      <p className="section-eyebrow">ADMINISTRACIÓN</p>
      <h1>Estado de la plataforma</h1>
      {error ? <p role="alert">{error}</p> : summary
        ? <p>{summary.users} usuarios registrados · {summary.companies} empresas en el catálogo.</p>
        : <p role="status">Consultando el resumen…</p>}
      <Link to="/search">Explorar empresas</Link>
    </section>
  </WorkspaceLayout>
}
