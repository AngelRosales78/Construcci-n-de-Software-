import { useCompanySearchViewModel } from '../../viewmodels/useCompanySearchViewModel'
import SearchBar from './SearchBar'
import CompanyFilters from './CompanyFilters'
import SearchHistory from './SearchHistory'
import CompanyList from './CompanyList'
import SectorExplorer from './SectorExplorer'
import WorkspaceLayout from '../layout/WorkspaceLayout'
import { Link } from 'react-router-dom'
import { useFavoritesViewModel } from '../../viewmodels/useFavoritesViewModel'
import ActionNotice from '../shared/ActionNotice'


export default function CompanySearch() {
  const favorites = useFavoritesViewModel()
  const { companies, loading, error, count, hasNext, historyVersion, params, handleSearch, saveHistory, handleFilterChange, previousPage, nextPage, retry, setWithData, searchPath } = useCompanySearchViewModel()
  return (
    <WorkspaceLayout><div className="company-search">
      <div className="search-header">
        <p className="section-eyebrow">EXPLORACIÓN DE MERCADOS</p>
        <h1>Explorar empresas</h1>
        <p className="section-description">Busca por nombre o símbolo y filtra por sector.</p>
        <SearchBar onSearch={handleSearch} onSubmitSearch={saveHistory} initialValue={params.q} />
      </div>
      <div className="catalog-notice"><p><strong>Catálogo S&amp;P 500 y altas manuales.</strong> Cada ficha indica sus datos disponibles y si su pertenencia al índice está verificada.</p><Link to="/company/MSFT" state={{ searchReturn: searchPath }}>Abrir Microsoft →</Link></div>
      <div className="search-content">
        <section className="search-main" aria-label="Resultados de búsqueda">
          <div className="results-heading"><h2>{params.q ? `Resultados para “${params.q}”` : 'Empresas del catálogo'}</h2><span>{loading ? 'Consultando…' : `${count} resultados`}</span></div>
          <SectorExplorer compact selected={params.sector} onSelect={sector => handleFilterChange({ sector, ordering: params.ordering })} />
          <CompanyFilters onFilterChange={handleFilterChange}
            initialSector={params.sector} initialOrdering={params.ordering} />
          <label className="coverage-filter"><input type="checkbox" checked={params.withData} onChange={event => setWithData(event.target.checked)} /> Solo con indicadores o balances cargados</label>
          <p className="catalog-reading-note">Cifras históricas. Abre una ficha para consultar qué significan, sus fechas y fuentes.</p>
          {favorites.error && <div role="alert"><p>{favorites.error}</p><button onClick={favorites.reload}>Reintentar favoritos</button></div>}
          <ActionNotice message={favorites.message}><Link to="/favorites">Ver favoritos →</Link></ActionNotice>
          <div className="search-results-stage" aria-busy={loading}>{error ? <div className="search-error" role="alert"><p>{error}</p><button type="button" onClick={retry}>Reintentar</button></div> : <CompanyList companies={companies} loading={loading} favorites={favorites} />}</div>
          {!error && <nav aria-label="Páginas de empresas">
            <button disabled={loading || params.page === 1}
              onClick={previousPage}>Anterior</button>
            <span>Página {params.page} · {count} empresas</span>
            <button disabled={loading || !hasNext}
              onClick={nextPage}>Siguiente</button>
          </nav>}
        </section>
        <aside className="search-sidebar">
          <SearchHistory onSelectHistory={saveHistory} refreshKey={historyVersion} />
        </aside>
      </div>
    </div></WorkspaceLayout>
  )
}
