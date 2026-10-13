import { useState, useEffect, useRef, useCallback } from 'react'
import api from '../../services/api'
import SearchBar from './SearchBar'
import CompanyFilters from './CompanyFilters'
import SearchHistory from './SearchHistory'
import CompanyList from './CompanyList'
import WorkspaceLayout from '../layout/WorkspaceLayout'

export default function CompanySearch() {
  const [companies, setCompanies] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [count, setCount] = useState(0)
  const [hasNext, setHasNext] = useState(false)
  const [historyVersion, setHistoryVersion] = useState(0)
  const [params, setParams] = useState({ q: '', sector: '', ordering: '-market_cap', page: 1 })
  const requestId = useRef(0)

  useEffect(() => {
    const id = ++requestId.current
    setLoading(true)
    setError('')
    const query = new URLSearchParams({ ordering: params.ordering, page: String(params.page) })
    if (params.q) query.set('q', params.q)
    if (params.sector) query.set('sector', params.sector)
    api.get(`/companies/search/?${query}`).then(({ data }) => {
      if (id !== requestId.current) return
      setCompanies(data.results || [])
      setCount(data.count || 0)
      setHasNext(Boolean(data.next))
    }).catch(() => {
      if (id !== requestId.current) return
      setCompanies([])
      setCount(0)
      setHasNext(false)
      setError('No se pudieron cargar las empresas. Intenta nuevamente.')
    }).finally(() => {
      if (id === requestId.current) setLoading(false)
    })
    return () => { requestId.current++ }
  }, [params])

  const handleSearch = useCallback((query) => {
    const q = query.trim()
    setParams(previous => previous.q === q && previous.page === 1
      ? previous : { ...previous, q, page: 1 })
  }, [])

  const saveHistory = useCallback(async (query) => {
    handleSearch(query)
    if (!query.trim()) return
    try {
      await api.post('/companies/history/', { query: query.trim() })
      setHistoryVersion(version => version + 1)
    } catch {
      // Una falla del historial no impide consultar empresas.
    }
  }, [handleSearch])

  const handleFilterChange = useCallback(({ sector, ordering }) => {
    setParams(previous => ({ ...previous, sector, ordering, page: 1 }))
  }, [])

  return (
    <WorkspaceLayout><div className="company-search">
      <div className="search-header">
        <p className="section-eyebrow">EXPLORADOR DE EMPRESAS</p>
        <h1>Buscar Empresas S&P 500</h1>
        <p className="section-description">Encuentra una empresa y entiende sus indicadores, su perfil financiero y su historia en el mercado.</p>
        <SearchBar onSearch={handleSearch} onSubmitSearch={saveHistory} initialValue={params.q} />
      </div>
      <div className="search-content">
        <aside className="search-sidebar">
          <SearchHistory onSelectHistory={saveHistory} refreshKey={historyVersion} />
          <section className="search-guide"><span className="guide-label">TU PRÓXIMO PASO</span><h3>Del símbolo a los datos</h3><p>Busca por nombre o ticker. Abre una ficha para ver las métricas, la fecha del balance y los precios históricos.</p><p className="guide-note">Los indicadores conservan su fuente y fecha. Si falta información, verás «Sin datos».</p></section>
        </aside>
        <section className="search-main" aria-label="Resultados de búsqueda">
          <div className="results-heading"><h2>{params.q ? `Resultados para “${params.q}”` : 'Empresas del índice'}</h2><span>{loading ? 'Consultando…' : `${count} resultados`}</span></div>
          <CompanyFilters onFilterChange={handleFilterChange}
            initialSector={params.sector} initialOrdering={params.ordering} />
          {error && <p role="alert">{error}</p>}
          <CompanyList companies={companies} loading={loading} />
          {!error && <nav aria-label="Páginas de empresas">
            <button disabled={loading || params.page === 1}
              onClick={() => setParams(previous => ({ ...previous, page: previous.page - 1 }))}>Anterior</button>
            <span>Página {params.page} · {count} empresas</span>
            <button disabled={loading || !hasNext}
              onClick={() => setParams(previous => ({ ...previous, page: previous.page + 1 }))}>Siguiente</button>
          </nav>}
        </section>
      </div>
    </div></WorkspaceLayout>
  )
}
