import FavoriteRemovalDialog from './FavoriteRemovalDialog'
import { useEffect, useRef, useState } from 'react'

export default function FavoriteButton({ ticker, favorites }) {
  const saved = favorites.isSaved(ticker)
  const saving = favorites.pending.includes(ticker)
  const requested = useRef(false)
  const [confirmed, setConfirmed] = useState(false)
  useEffect(() => {
    if (!requested.current || saving) return
    requested.current = false
    if (saved) setConfirmed(true)
  }, [saved, saving])
  return <><button type="button" className={`favorite-button${confirmed ? ' favorite-confirmed' : ''}`} aria-pressed={saved} aria-busy={saving}
    aria-label={saved ? `${ticker} guardado en favoritos` : `Guardar ${ticker} en favoritos`}
    disabled={saving || !favorites.ready || favorites.loading}
    onClick={() => {
      setConfirmed(false)
      if (saved) favorites.requestRemove(ticker)
      else { requested.current = true; favorites.add(ticker) }
    }}>
    <span className="favorite-glyph" aria-hidden="true" onAnimationEnd={() => setConfirmed(false)}>{saved ? '★' : '☆'}</span> {saving ? 'Guardando…' : saved ? 'Guardado' : 'Guardar favorito'}
  </button>{favorites.removing?.ticker === ticker && <FavoriteRemovalDialog favorites={favorites} />}</>
}
