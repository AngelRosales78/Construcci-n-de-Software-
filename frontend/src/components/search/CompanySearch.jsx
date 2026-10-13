import { useCompanySearchViewModel } from '../../viewmodels/useCompanySearchViewModel'
import SearchBar from './SearchBar'
import CompanyFilters from './CompanyFilters'
import SearchHistory from './SearchHistory'
import CompanyList from './CompanyList'
import WorkspaceLayout from '../layout/WorkspaceLayout'
import { Link } from 'react-router-dom'


export default function CompanySearch() {
  const { companies, loading, error, count, hasNext, historyVersion, params, handleSearch, saveHistory, handleFilterChange, previousPage, nextPage } = useCompanySearchViewModel()
  return (
    <WorkspaceLayout><div className="company-search">
      <div className="search-header">
        <p className="section-eyebrow">EXPLORACIÓN DE MERCADOS</p>
        <h1>Buscar Empresas S&P 500</h1>
        <p className="section-description">Encuentra una empresa y entiende sus indicadores, su perfil financiero y su historia en el mercado.</p>
        <SearchBar onSearch={handleSearch} onSubmitSearch={saveHistory} initialValue={params.q} />
      </div>
      <div className="terminal-overview">
        <section><span className="section-eyebrow">CATÁLOGO DE REFERENCIA</span><h2>Del mercado a cada empresa</h2><p>Busca por nombre o símbolo y consulta su ficha financiera.</p><span className="terminal-tag">S&amp;P 500 · Sectores y empresas</span></section>
        <section><span className="section-eyebrow">FICHA DE REFERENCIA</span><h2>Microsoft · MSFT</h2><p>Explora el balance anual 2024 y sus precios históricos. La cobertura financiera de esta demostración corresponde a Microsoft.</p><Link className="reference-link" to="/company/MSFT">Ver ficha de Microsoft <span aria-hidden="true">→</span></Link></section>
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
