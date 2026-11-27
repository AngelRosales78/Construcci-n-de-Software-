import { useNavigate, useLocation } from 'react-router-dom'
import { hasIndicatorValue, sectorLabel, formatMarketValue, formatFinancialNumber } from '../../utils/financialPresentation'
import FavoriteButton from '../favorites/FavoriteButton'
import CompanyMark from '../shared/CompanyMark'
import LoadingState from '../shared/LoadingState'

export default function CompanyList({ companies, loading, favorites }) {
  const navigate = useNavigate()
  const location = useLocation()

  const handleSelectCompany = (ticker) => {
    navigate(`/company/${ticker}`, { state: { searchReturn: '/search' + location.search } })
  }

  if (loading) {
    return <LoadingState variant="catalog" />
  }

  if (companies.length === 0) {
    return <div className="company-list-empty"><h3>No se encontraron empresas</h3><p>Prueba otro nombre o revisa los filtros.</p><button type="button" onClick={() => navigate('/search')}>Ver todo el catálogo</button></div>
  }

  return (
    <div className="company-list">
      {companies.map((company, index) => {
        const visibleCount = ['market_cap', 'per', 'roe'].filter(key => hasIndicatorValue(company[key])).length
        return (
        <div className="company-card-wrapper" key={company.ticker} style={{'--item-delay':`${Math.min(index,4) * 25}ms`}}>
        <button
          type="button"
          key={company.ticker}
          className="company-card"
          aria-label={`Ver ficha de ${company.name}`}
          onClick={() => handleSelectCompany(company.ticker)}
        >
          <div className="company-card-header">
            <CompanyMark company={company} />
            <strong className="company-ticker">{company.ticker}</strong>
          </div>
          <h3 className="company-name">{company.name}{company.is_index_member === false && <small className="company-membership">Alta manual · índice sin verificar</small>}</h3>
          <span className={`company-sector sector-${company.sector}`} title={sectorLabel(company.sector, company.sector_display)}>{company.industry || sectorLabel(company.sector, company.sector_display)}</span>
          <span className={`coverage-label ${visibleCount || company.has_annual_data ? 'coverage-available' : ''}`}>{visibleCount ? `${visibleCount} de 3 cifras de esta lista` : company.has_annual_data ? 'Balances anuales disponibles' : company.has_price_data ? 'Solo precios históricos' : 'Información financiera pendiente'}</span>
          {visibleCount > 0 ? <div className="company-metrics">
            <div className="metric">
              <span className="metric-label">Valor en bolsa</span>
              <span className="metric-value">{formatMarketValue(company.market_cap)}</span>
            </div>
            <div className="metric">
              <span className="metric-label">Precio / beneficio · PER</span>
              <span className="metric-value">{formatFinancialNumber(company.per)}</span>
            </div>
            <div className="metric">
              <span className="metric-label">Rentabilidad del patrimonio · ROE</span>
              <span className="metric-value">{hasIndicatorValue(company.roe) ? `${formatFinancialNumber(company.roe)}%` : 'Sin datos'}</span>
            </div>
          </div> : null}
          <span className="company-card-action">Consultar ficha <span aria-hidden="true">↗</span></span>
        </button>
        {favorites && <FavoriteButton ticker={company.ticker} favorites={favorites} />}
        </div>
        )
      })}
    </div>
  )
}
