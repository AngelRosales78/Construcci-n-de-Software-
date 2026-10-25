import { useSearchHistoryViewModel } from '../../viewmodels/useSearchHistoryViewModel'


export default function SearchHistory({ onSelectHistory, refreshKey = 0 }) {
  const { history, loading, error, busy, remove } = useSearchHistoryViewModel({ onSelectHistory, refreshKey })
  if (loading) return <p>Cargando historial...</p>
  return <section className="search-history" aria-label="Búsquedas recientes">
    <h3>Búsquedas recientes</h3>
    {error && <p role="status">{error}</p>}
    {!history.length && <p>No hay búsquedas recientes.</p>}
    <ul>{history.map(item => <li key={item.id}>
      <button type="button" onClick={() => onSelectHistory(item.query)}>{item.query}</button>
      <button type="button" disabled={busy} aria-label={`Eliminar ${item.query}`} onClick={() => remove(item.id)}>×</button>
    </li>)}</ul>
    {history.length > 0 && <button disabled={busy} onClick={() => remove(null)}>Limpiar historial</button>}
  </section>
}
