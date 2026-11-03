export default function FavoriteButton({ ticker, favorites }) {
  const saved = favorites.isSaved(ticker)
  const saving = favorites.pending.includes(ticker)
  return <button type="button" className="favorite-button" aria-pressed={saved}
    aria-label={saved ? `${ticker} guardado en favoritos` : `Guardar ${ticker} en favoritos`}
    disabled={saved || saving || !favorites.ready || favorites.loading}
    onClick={() => favorites.add(ticker)}>
    <span aria-hidden="true">{saved ? '★' : '☆'}</span> {saving ? 'Guardando…' : saved ? 'Guardado' : 'Guardar favorito'}
  </button>
}
