import { Link } from 'react-router-dom'
import { useFavoritesViewModel } from '../../viewmodels/useFavoritesViewModel'
import WorkspaceLayout from '../layout/WorkspaceLayout'
import FavoriteRemovalDialog from './FavoriteRemovalDialog'
import TagsPanel from './TagsPanel'
import { useState } from 'react'

export default function FavoritesPage() {
  const favorites = useFavoritesViewModel()
  const [filter, setFilter] = useState(null)
  return <WorkspaceLayout><section className="favorites-page">
    <p className="section-eyebrow">TU LISTA PERSONAL</p><h1>Mis favoritos</h1>
    <p>Guarda empresas desde el explorador o su ficha. Tu lista pertenece a tu cuenta.</p>
    <p aria-live="polite">{favorites.items.length} empresas guardadas</p>
    {favorites.lastSynced && <p className="pillar-muted">{favorites.ready ? 'Sincronizado' : 'Copia anterior, pendiente de conexión'}: {new Date(favorites.lastSynced).toLocaleTimeString('es-PE')}</p>}
    <button type="button" onClick={favorites.reload} disabled={favorites.loading}>Actualizar favoritos</button>
    {favorites.message && <p role="status">{favorites.message}</p>}
    <FavoriteRemovalDialog favorites={favorites} />
    <TagsPanel items={favorites.items} onFilter={setFilter} />
    {favorites.loading && <p role="status">Consultando favoritos…</p>}
    {favorites.error && <div role="alert"><p>{favorites.error}</p><button onClick={favorites.reload}>Reintentar favoritos</button></div>}
    {!favorites.loading && favorites.ready && favorites.items.length === 0 && <p>Aún no guardaste empresas. <Link to="/search">Explorar empresas</Link></p>}
    {favorites.items.length > 0 && <ul className="favorites-list">{favorites.items.filter(item => !filter || filter.has(item.id)).map(item => <li key={item.id}>
      <div><strong>{item.ticker}</strong><span>{item.name}</span></div>
      <Link to={`/company/${encodeURIComponent(item.ticker)}`} state={{ searchReturn: '/favorites' }}>Ver ficha →</Link>
      <button type="button" disabled={!favorites.ready || favorites.pending.includes(item.ticker)} onClick={() => favorites.requestRemove(item.ticker)}>Quitar {item.ticker}</button>
    </li>)}</ul>}
  </section></WorkspaceLayout>
}
