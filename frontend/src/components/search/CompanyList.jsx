import { useNavigate, useLocation } from 'react-router-dom'
import { hasIndicatorValue, sectorLabel, formatMarketValue, formatFinancialNumber } from '../../utils/financialPresentation'
import { useState } from 'react'
import verifiedLogos from '../../data/verifiedLogos.json'
import lightLogos from '../../data/lightLogos.json'
import FavoriteButton from '../favorites/FavoriteButton'

function CompanyLogo({ company }) {
  const [failed, setFailed] = useState(false)
  const local = verifiedLogos.includes(company.ticker)
    ? `/company-logos/${company.ticker.replaceAll('.', '-')}.png` : ''
  const source = local || company.logo_url
  return source && !failed
    ? <img src={source} className={lightLogos.includes(company.ticker) ? 'company-logo-on-dark' : undefined} alt={`Logo de ${company.name}`} width="40" height="40" loading="lazy" onError={() => setFailed(true)} />
    : <span className="company-logo-fallback" aria-label={`Logo no disponible para ${company.name}`}>{company.ticker}</span>
}

export default function CompanyList({ companies, loading, favorites }) {
  const navigate = useNavigate()
  const location = useLocation()

  const handleSelectCompany = (ticker) => {
    navigate(`/company/${ticker}`, { state: { searchReturn: '/search' + location.search } })
  }

  if (loading) {
    return <div className="company-list-loading" role="status">Cargando empresas...</div>
  }

  if (companies.length === 0) {
    return <div className="company-list-empty"><h3>No se encontraron empresas</h3><p>Prueba otro nombre o revisa los filtros.</p><button type="button" onClick={() => navigate('/search')}>Ver todo el catálogo</button></div>
  }

  return (
    <div className="company-list">
      {companies.map((company) => {
        const visibleCount = ['market_cap', 'per', 'roe'].filter(key => hasIndicatorValue(company[key])).length
        return (
        <div className="company-card-wrapper" key={company.ticker}>
        <button
          type="button"
          key={company.ticker}
          className="company-card"
          aria-label={`Ver ficha de ${company.name}`}
          onClick={() => handleSelectCompany(company.ticker)}
        >
          <div className="company-card-header">
            <CompanyLogo company={company} />
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
