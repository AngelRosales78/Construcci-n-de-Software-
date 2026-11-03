import { Link } from 'react-router-dom'
import { useFavoritesViewModel } from '../../viewmodels/useFavoritesViewModel'
import WorkspaceLayout from '../layout/WorkspaceLayout'

export default function FavoritesPage() {
  const favorites = useFavoritesViewModel()
  return <WorkspaceLayout><section className="favorites-page">
    <p className="section-eyebrow">TU LISTA PERSONAL</p><h1>Mis favoritos</h1>
    <p>Guarda empresas desde el explorador o su ficha. Tu lista pertenece a tu cuenta.</p>
    {favorites.loading && <p role="status">Consultando favoritos…</p>}
    {favorites.error && <div role="alert"><p>{favorites.error}</p><button onClick={favorites.reload}>Reintentar favoritos</button></div>}
    {!favorites.loading && favorites.ready && favorites.items.length === 0 && <p>Aún no guardaste empresas. <Link to="/search">Explorar empresas</Link></p>}
    {favorites.items.length > 0 && <ul className="favorites-list">{favorites.items.map(item => <li key={item.id}>
      <div><strong>{item.ticker}</strong><span>{item.name}</span></div>
      <Link to={`/company/${encodeURIComponent(item.ticker)}`} state={{ searchReturn: '/favorites' }}>Ver ficha →</Link>
    </li>)}</ul>}
  </section></WorkspaceLayout>
}
