import { useCompanySearchViewModel } from '../../viewmodels/useCompanySearchViewModel'
import SearchBar from './SearchBar'
import CompanyFilters from './CompanyFilters'
import SearchHistory from './SearchHistory'
import CompanyList from './CompanyList'
import WorkspaceLayout from '../layout/WorkspaceLayout'


export default function CompanySearch() {
  const { companies, loading, error, count, hasNext, historyVersion, params, handleSearch, saveHistory, handleFilterChange, previousPage, nextPage } = useCompanySearchViewModel()
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
