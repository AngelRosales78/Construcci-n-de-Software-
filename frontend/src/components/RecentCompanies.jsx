import { Link } from 'react-router-dom'
import { useRemoteResource } from '../viewmodels/useRemoteResource'

export default function RecentCompanies() {
  const resource = useRemoteResource('/companies/recent/')
  const rows = resource.data?.schema_version === 'recent-companies-v1' ? resource.data.results : []
  return <section className="sprint3-panel" aria-label="Empresas recientes">
    <h2>Vistos recientemente</h2><p>Tus diez últimas empresas consultadas. Revisitar una la mueve al inicio.</p>
    {resource.loading && <p role="status">Consultando visitas…</p>}
    {resource.error && <p role="alert">{resource.error} <button onClick={resource.reload}>Reintentar recientes</button></p>}
    {!resource.loading && !resource.error && !rows.length && <p>Aún no consultaste una ficha.</p>}
    <ol>{rows.map(row => <li key={row.ticker}><Link to={`/company/${encodeURIComponent(row.ticker)}`}>{row.ticker} · {row.name}</Link> <small>{new Date(row.visited_at).toLocaleString('es-PE')}</small></li>)}</ol>
  </section>
}
