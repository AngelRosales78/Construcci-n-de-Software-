import { useState, useEffect } from 'react'
import { companyRepository } from '../models/companyRepository'
import { sectorLabel } from '../utils/financialPresentation'

export function useCompanyFiltersViewModel({ onFilterChange, initialSector = '', initialOrdering = 'name' }) {
  const [sectors, setSectors] = useState([])
  const [selectedSector, setSelectedSector] = useState(initialSector)
  const [ordering, setOrdering] = useState(initialOrdering)

  useEffect(() => {
    const fetchSectors = async () => {
      try {
        const response = await companyRepository.sectors()
        setSectors([...(response.sectors || [])].sort((a, b) =>
          sectorLabel(a.value, a.label).localeCompare(sectorLabel(b.value, b.label), 'es', { sensitivity: 'base' })))
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

  const selectSector = sector => { setSelectedSector(sector); onFilterChange({ sector, ordering }) }
  const selectOrdering = value => { setOrdering(value); onFilterChange({ sector: selectedSector, ordering: value }) }
  const clearFilters = () => selectSector('')
  return { sectors, selectedSector, ordering, selectSector, selectOrdering, clearFilters }
}
