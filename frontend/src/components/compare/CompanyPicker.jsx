import { useCompanyPickerViewModel } from '../../viewmodels/useCompanyPickerViewModel'

export default function CompanyPicker({ side, selected, onSelect, onRemove }) {
  const vm = useCompanyPickerViewModel({ selected, onSelect })
  return <section className="comparison-picker" aria-label={`Empresa ${side}`}>
    <h2>Empresa {side}</h2>
    {selected && <div className="comparison-selection"><strong>{selected}</strong><button type="button" onClick={onRemove} aria-label={`Quitar empresa ${side}`}>Quitar</button></div>}
    <label htmlFor={`${vm.id}-input`}>{selected ? 'Cambiar empresa' : 'Buscar por nombre o símbolo'}</label>
    <input id={`${vm.id}-input`} role="combobox" aria-label={`Buscar empresa ${side}`}
      aria-autocomplete="list" aria-expanded={vm.open && vm.rows.length > 0} aria-controls={`${vm.id}-list`}
      aria-activedescendant={vm.open && vm.active >= 0 ? `${vm.id}-option-${vm.active}` : undefined}
      value={vm.query} onChange={event => vm.change(event.target.value)} onKeyDown={vm.keyDown}
      onFocus={() => vm.setOpen(true)} onBlur={event => { if (!event.currentTarget.parentElement.contains(event.relatedTarget)) vm.setOpen(false) }}
      autoComplete="off" placeholder="Ej.: Microsoft o MSFT" />
    {vm.loading && <p role="status">Buscando…</p>}
    {vm.error && <div role="alert"><p>{vm.error}</p><button onClick={vm.retry}>Reintentar búsqueda {side}</button></div>}
    {vm.open && !vm.loading && vm.query.trim() && !vm.error && vm.rows.length === 0 && <p>Sin resultados para esta búsqueda.</p>}
    <ul id={`${vm.id}-list`} role="listbox" aria-label={`Opciones empresa ${side}`} hidden={!vm.open || !vm.rows.length}>
      {vm.rows.map((row, index) => <li key={row.ticker} role="option" id={`${vm.id}-option-${index}`} aria-selected={index === vm.active}>
        <button type="button" onClick={() => vm.choose(row)}><strong>{row.ticker}</strong> {row.name}</button>
      </li>)}
    </ul>
    <small>Usa las flechas y Enter para seleccionar.</small>
  </section>
}
