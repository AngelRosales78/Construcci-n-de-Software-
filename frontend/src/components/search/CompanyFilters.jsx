import { useCompanyFiltersViewModel } from '../../viewmodels/useCompanyFiltersViewModel'


export default function CompanyFilters({ onFilterChange, initialSector = '', initialOrdering = '-market_cap' }) {
  const { sectors, selectedSector, ordering, selectSector, selectOrdering, clearFilters } = useCompanyFiltersViewModel({ onFilterChange, initialSector, initialOrdering })
  return (
    <div className="company-filters">
      <p aria-live="polite">Orden: {ordering.startsWith('-') ? '↓ Mayor a menor' : '↑ Menor a mayor'}</p>
      {selectedSector && <div className="active-filter">
        <span>{sectors.find(s => s.value === selectedSector)?.label || selectedSector}</span>
        <button type="button" onClick={clearFilters}>Limpiar filtros</button>
      </div>}
      <div className="filter-group">
        <label htmlFor="sector-filter">Sector:</label>
        <select
          id="sector-filter"
          value={selectedSector}
          onChange={e => selectSector(e.target.value)}
        >
          <option value="">Todos los sectores</option>
          {sectors.map((s) => (
            <option key={s.value} value={s.value}>{s.label}</option>
          ))}
        </select>
      </div>

      <div className="filter-group">
        <label htmlFor="ordering-filter">Ordenar por:</label>
        <select
          id="ordering-filter"
          value={ordering}
          onChange={e => selectOrdering(e.target.value)}
        >
          <option value="-market_cap">Market Cap (mayor a menor)</option>
          <option value="market_cap">Market Cap (menor a mayor)</option>
        </select>
      </div>
    </div>
  )
}
