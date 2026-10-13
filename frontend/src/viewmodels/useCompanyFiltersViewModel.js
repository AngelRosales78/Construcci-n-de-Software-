import { useState, useEffect } from 'react'
import { companyRepository } from '../models/companyRepository'

export function useCompanyFiltersViewModel({ onFilterChange, initialSector = '', initialOrdering = '-market_cap' }) {
  const [sectors, setSectors] = useState([])
  const [selectedSector, setSelectedSector] = useState(initialSector)
  const [ordering, setOrdering] = useState(initialOrdering)

  useEffect(() => {
    const fetchSectors = async () => {
      try {
        const response = await companyRepository.sectors()
        setSectors(response.sectors || [])
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
