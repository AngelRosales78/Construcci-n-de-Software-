import { useState, useEffect } from 'react'
import api from '../../services/api'

export default function CompanyFilters({ onFilterChange, initialSector = '', initialOrdering = '-market_cap' }) {
  const [sectors, setSectors] = useState([])
  const [selectedSector, setSelectedSector] = useState(initialSector)
  const [ordering, setOrdering] = useState(initialOrdering)

  useEffect(() => {
    const fetchSectors = async () => {
      try {
        const response = await api.get('/companies/sectors/')
        setSectors(response.data.sectors || [])
      } catch (err) {
        console.error('Error fetching sectors:', err)
      }
    }
    fetchSectors()
  }, [])

  useEffect(() => {
    setSelectedSector(initialSector)
    setOrdering(initialOrdering)
  }, [initialSector, initialOrdering])

  return (
    <div className="company-filters">
      <p aria-live="polite">Orden: {ordering.startsWith('-') ? '↓ Mayor a menor' : '↑ Menor a mayor'}</p>
      {selectedSector && <div className="active-filter">
        <span>{sectors.find(s => s.value === selectedSector)?.label || selectedSector}</span>
        <button type="button" onClick={() => {
          setSelectedSector('')
          onFilterChange({ sector: '', ordering })
        }}>Limpiar filtros</button>
      </div>}
      <div className="filter-group">
        <label htmlFor="sector-filter">Sector:</label>
        <select
          id="sector-filter"
          value={selectedSector}
          onChange={(e) => {
            setSelectedSector(e.target.value)
            onFilterChange({ sector: e.target.value, ordering })
          }}
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
          onChange={(e) => {
            setOrdering(e.target.value)
            onFilterChange({ sector: selectedSector, ordering: e.target.value })
          }}
        >
          <option value="-market_cap">Market Cap (mayor a menor)</option>
          <option value="market_cap">Market Cap (menor a mayor)</option>
        </select>
      </div>
    </div>
  )
}
