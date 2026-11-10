import { useEffect, useRef } from 'react'

export default function FavoriteRemovalDialog({ favorites }) {
  const cancel = useRef(null)
  const selected = favorites.removing
  useEffect(() => { if (selected) cancel.current?.focus() }, [selected])
  if (!selected) return null
  const busy = favorites.pending.includes(selected.ticker)
  return <div className="favorite-confirmation" role="alertdialog" aria-modal="false" aria-label={`Quitar ${selected.ticker} de favoritos`}>
    <p>¿Quieres quitar {selected.name} ({selected.ticker}) de tus favoritos?</p>
    <button ref={cancel} type="button" disabled={busy} onClick={favorites.cancelRemove}>Cancelar</button>
    <button type="button" disabled={busy} onClick={favorites.confirmRemove}>{busy ? 'Quitando…' : 'Confirmar eliminación'}</button>
  </div>
}
