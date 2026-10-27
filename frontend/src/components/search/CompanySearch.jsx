import { useCompanySearchViewModel } from '../../viewmodels/useCompanySearchViewModel'
import SearchBar from './SearchBar'
import CompanyFilters from './CompanyFilters'
import SearchHistory from './SearchHistory'
import CompanyList from './CompanyList'
import WorkspaceLayout from '../layout/WorkspaceLayout'
import { Link } from 'react-router-dom'


export default function CompanySearch() {
  const { companies, loading, error, count, hasNext, historyVersion, params, handleSearch, saveHistory, handleFilterChange, previousPage, nextPage, retry, setWithData, searchPath } = useCompanySearchViewModel()
  return (
    <WorkspaceLayout><div className="company-search">
      <div className="search-header">
        <p className="section-eyebrow">EXPLORACIÓN DE MERCADOS</p>
        <h1>Explorar empresas</h1>
        <p className="section-description">Busca por nombre o símbolo. Filtra las fichas con indicadores cargados.</p>
        <SearchBar onSearch={handleSearch} onSubmitSearch={saveHistory} initialValue={params.q} />
      </div>
      <div className="catalog-notice"><p><strong>Catálogo S&amp;P 500.</strong> Microsoft es el caso completo de esta demostración.</p><Link to="/company/MSFT" state={{ searchReturn: searchPath }}>Abrir Microsoft →</Link></div>
      <div className="search-content">
        <aside className="search-sidebar">
          <SearchHistory onSelectHistory={saveHistory} refreshKey={historyVersion} />
        </aside>
        <section className="search-main" aria-label="Resultados de búsqueda">
          <div className="results-heading"><h2>{params.q ? `Resultados para “${params.q}”` : 'Empresas del índice'}</h2><span>{loading ? 'Consultando…' : `${count} resultados`}</span></div>
          <CompanyFilters onFilterChange={handleFilterChange}
            initialSector={params.sector} initialOrdering={params.ordering} />
          <label className="coverage-filter"><input type="checkbox" checked={params.withData} onChange={event => setWithData(event.target.checked)} /> Solo empresas con indicadores cargados</label>
          {error ? <div className="search-error" role="alert"><p>{error}</p><button type="button" onClick={retry}>Reintentar</button></div> : <CompanyList companies={companies} loading={loading} />}
          {!error && <nav aria-label="Páginas de empresas">
            <button disabled={loading || params.page === 1}
              onClick={previousPage}>Anterior</button>
            <span>Página {params.page} · {count} empresas</span>
            <button disabled={loading || !hasNext}
              onClick={nextPage}>Siguiente</button>
          </nav>}
        </section>
      </div>
    </div></WorkspaceLayout>
  )
}
