import { Link } from 'react-router-dom'
import { useFavoritesViewModel } from '../../viewmodels/useFavoritesViewModel'
import WorkspaceLayout from '../layout/WorkspaceLayout'
import FavoriteRemovalDialog from './FavoriteRemovalDialog'
import TagsPanel from './TagsPanel'
import { useState } from 'react'
import ActionNotice from '../shared/ActionNotice'

export default function FavoritesPage() {
  const favorites = useFavoritesViewModel()
  const [filter, setFilter] = useState(null)
  return <WorkspaceLayout><section className="favorites-page">
    <p className="section-eyebrow">TU LISTA PERSONAL</p><h1>Mis favoritos</h1>
    <p>Guarda empresas desde el explorador o su ficha. Tu lista pertenece a tu cuenta.</p>
    <p aria-live="polite">{favorites.ready || favorites.items.length > 0 ? `${favorites.items.length} ${favorites.items.length === 1 ? 'empresa guardada' : 'empresas guardadas'}` : favorites.loading ? 'Consultando tu lista…' : 'Lista pendiente de consultar'}</p>
    {favorites.lastSynced && <p className="pillar-muted">{favorites.ready ? 'Sincronizado' : 'Copia anterior, pendiente de conexión'}: {new Date(favorites.lastSynced).toLocaleTimeString('es-PE')}</p>}
    <button type="button" onClick={favorites.reload} disabled={favorites.loading}>Actualizar favoritos</button>
    <ActionNotice message={favorites.message}/>
    <FavoriteRemovalDialog favorites={favorites} />
    <TagsPanel items={favorites.items} onFilter={setFilter} />
    {favorites.loading && <p role="status">Consultando favoritos…</p>}
    {favorites.error && <div role="alert"><p>{favorites.error}</p><button onClick={favorites.reload}>Reintentar favoritos</button></div>}
    {!favorites.loading && favorites.ready && favorites.items.length === 0 && <div className="favorites-empty"><h2>Guarda tu primera empresa</h2><p>Aún no guardaste empresas. Encuentra una y usa «Guardar favorito» para volver a ella después.</p><Link to="/search">Explorar empresas →</Link></div>}
    {favorites.items.length > 0 && <ul className="favorites-list">{favorites.items.filter(item => !filter || filter.has(item.id)).map(item => <li key={item.id}>
      <div><strong>{item.ticker}</strong><span>{item.name}</span></div>
      <Link to={`/company/${encodeURIComponent(item.ticker)}`} state={{ searchReturn: '/favorites' }}>Ver ficha →</Link>
      <button type="button" disabled={!favorites.ready || favorites.pending.includes(item.ticker)} onClick={() => favorites.requestRemove(item.ticker)}>Quitar {item.ticker}</button>
    </li>)}</ul>}
  </section></WorkspaceLayout>
}
