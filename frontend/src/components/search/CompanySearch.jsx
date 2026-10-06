import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../../services/api'
import SearchBar from './SearchBar'
import CompanyFilters from './CompanyFilters'
import SearchHistory from './SearchHistory'
import CompanyList from './CompanyList'

export default function CompanySearch() {
  const [companies, setCompanies] = useState([])
  const [loading, setLoading] = useState(false)
  const [searchParams, setSearchParams] = useState({
    q: '',
    sector: '',
    ordering: '-market_cap'
  })
  const navigate = useNavigate()

  const performSearch = useCallback(async (params) => {
    setLoading(true)
    try {
      const queryParams = new URLSearchParams()
      if (params.q) queryParams.append('q', params.q)
      if (params.sector) queryParams.append('sector', params.sector)
      if (params.ordering) queryParams.append('ordering', params.ordering)

      const response = await api.get(`/companies/search/?${queryParams.toString()}`)
      setCompanies(response.data || [])

      if (params.q) {
        try {
          await api.post('/companies/history/', { query: params.q })
        } catch (err) {
          console.error('Error saving search history:', err)
        }
      }
    } catch (err) {
      console.error('Error searching companies:', err)
      setCompanies([])
    } finally {
      setLoading(false)
    }
  }, [])

  const handleSearch = (query) => {
    const newParams = { ...searchParams, q: query }
    setSearchParams(newParams)
    performSearch(newParams)
  }

  const handleFilterChange = ({ sector, ordering }) => {
    const newParams = { ...searchParams, sector, ordering }
    setSearchParams(newParams)
    if (searchParams.q) {
      performSearch(newParams)
    }
  }

  const handleSelectHistory = (query) => {
    handleSearch(query)
  }

  return (
    <div className="company-search">
      <div className="search-header">
        <h1>Buscar Empresas S&P 500</h1>
        <SearchBar onSearch={handleSearch} initialValue={searchParams.q} />
      </div>

      <div className="search-content">
        <aside className="search-sidebar">
          <SearchHistory onSelectHistory={handleSelectHistory} />
        </aside>

        <main className="search-main">
          <CompanyFilters
            onFilterChange={handleFilterChange}
            initialSector={searchParams.sector}
            initialOrdering={searchParams.ordering}
          />
          <CompanyList companies={companies} loading={loading} />
        </main>
      </div>
    </div>
  )
}
