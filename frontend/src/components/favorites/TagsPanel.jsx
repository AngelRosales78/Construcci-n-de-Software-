import { useTagsViewModel } from '../../viewmodels/useTagsViewModel'

export default function TagsPanel({ items, onFilter }) {
  const { open, setOpen, tags, form, setForm, pending, error, deleting, setDeleting, filter, data, save, chooseFilter } = useTagsViewModel(onFilter)
  return <details className="sprint3-panel" onToggle={e => { setOpen(e.currentTarget.open); if (!e.currentTarget.open) { setFilter(''); onFilter(null) } }}>
    <summary>Organizar favoritos con etiquetas</summary>
    {open && <>
      <p>Borrar una etiqueta conserva tus empresas favoritas.</p>
      {(tags.error || error) && <p role="alert">{error || tags.error}</p>}
      {tags.loading && !data && <p role="status">Consultando etiquetas…</p>}
      {data && <>
        <label>Filtrar por etiqueta<select value={filter} onChange={e => chooseFilter(e.target.value)}>
          <option value="">Todas</option>{data.tags.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
        </select></label>
        <form onSubmit={e => { e.preventDefault(); save(form.id ? 'patch' : 'post', form.id ? `/tags/${form.id}/` : '/tags/', { name: form.name, color: form.color, version: data.version }) }}>
          <label>Nombre de etiqueta<input required maxLength={40} value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></label>
          <label>Color de etiqueta<input type="color" value={form.color} onChange={e => setForm({ ...form, color: e.target.value })} /></label>
          <button disabled={pending}>{form.id ? 'Guardar etiqueta' : 'Crear etiqueta'}</button>
          {form.id && <button type="button" onClick={() => setForm({ name: '', color: '#158569', id: null })}>Cancelar edición</button>}
        </form>
        <ul>{data.tags.map(t => <li key={t.id}>
          <span className="tag-chip"><span aria-hidden="true" style={{ background: t.color }} />{t.name}</span>
          <button disabled={pending} onClick={() => setForm(t)}>Editar {t.name}</button>
          <button disabled={pending} onClick={() => setDeleting(t)}>Borrar {t.name}</button>
        </li>)}</ul>
        {deleting && <div role="alertdialog" aria-label="Borrar etiqueta"><p>¿Borrar {deleting.name}? Las empresas se conservan.</p>
          <button disabled={pending} onClick={() => setDeleting(null)}>Cancelar</button>
          <button disabled={pending} onClick={() => { chooseFilter(''); save('delete', `/tags/${deleting.id}/?version=${data.version}`) }}>Confirmar borrado</button>
        </div>}
        {items.map(item => <fieldset key={item.id} disabled={pending}><legend>Etiquetas de {item.ticker}</legend>
          {data.tags.map(t => {
            const ids = data.assignments.find(a => a.favorite_id === item.id)?.tag_ids || []
            return <label key={t.id}><input type="checkbox" checked={ids.includes(t.id)} onChange={e => save('put', `/favorites/${item.id}/tags/`, {
              tag_ids: e.target.checked ? [...ids, t.id] : ids.filter(id => id !== t.id), version: data.version,
            })} />{t.name}</label>
          })}
        </fieldset>)}
      </>}
    </>}
  </details>
}
