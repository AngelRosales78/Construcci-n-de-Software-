import { useCompanyFiltersViewModel } from '../../viewmodels/useCompanyFiltersViewModel'
import { sectorLabel } from '../../utils/financialPresentation'


export default function CompanyFilters({ onFilterChange, initialSector = '', initialOrdering = 'name' }) {
  const { sectors, selectedSector, ordering, selectSector, selectOrdering, clearFilters } = useCompanyFiltersViewModel({ onFilterChange, initialSector, initialOrdering })
  return (
    <div className="company-filters">
      <p aria-live="polite">Orden: {ordering === 'name' ? 'Nombre: A–Z' : ordering === '-name' ? 'Nombre: Z–A' : ordering.startsWith('-') ? '↓ Mayor a menor' : '↑ Menor a mayor'}</p>
      {selectedSector && <div className="active-filter">
        <span>{sectorLabel(selectedSector, sectors.find(s => s.value === selectedSector)?.label)}</span>
        <button type="button" onClick={clearFilters}>Quitar sector</button>
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
            <option key={s.value} value={s.value}>{sectorLabel(s.value, s.label)}</option>
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
          <option value="name">Nombre: A–Z</option>
          <option value="-name">Nombre: Z–A</option>
          <option value="-market_cap">Capitalización: mayor a menor</option>
          <option value="market_cap">Capitalización: menor a mayor</option>
        </select>
      </div>
    </div>
  )
}
