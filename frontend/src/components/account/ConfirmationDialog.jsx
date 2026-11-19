import { useEffect, useRef } from 'react'

export default function ConfirmationDialog({ open, title, children, busy, onCancel, onConfirm, confirmLabel }) {
  const dialog = useRef(null)
  useEffect(() => {
    if (open && !dialog.current.open) dialog.current.showModal()
    if (!open && dialog.current.open) dialog.current.close()
  }, [open])
  return <dialog ref={dialog} className="account-dialog" aria-label={title}
    onCancel={event => { event.preventDefault(); if (!busy) onCancel() }}>
    <form onSubmit={event => { event.preventDefault(); onConfirm() }}>
      <h2>{title}</h2>{children}
      <div className="account-actions"><button type="button" autoFocus disabled={busy} onClick={onCancel}>Volver</button>
        <button type="submit" disabled={busy}>{busy ? 'Procesando…' : confirmLabel}</button></div>
    </form>
  </dialog>
}
