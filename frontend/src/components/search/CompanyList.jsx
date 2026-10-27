import { useNavigate, useLocation } from 'react-router-dom'
import { hasIndicators, sectorLabel } from '../../utils/financialPresentation'
import { useState } from 'react'
import verifiedLogos from '../../data/verifiedLogos.json'
import lightLogos from '../../data/lightLogos.json'

function CompanyLogo({ company }) {
  const [failed, setFailed] = useState(false)
  const local = verifiedLogos.includes(company.ticker)
    ? `/company-logos/${company.ticker.replaceAll('.', '-')}.png` : ''
  const source = local || company.logo_url
  return source && !failed
    ? <img src={source} className={lightLogos.includes(company.ticker) ? 'company-logo-on-dark' : undefined} alt={`Logo de ${company.name}`} width="40" height="40" loading="lazy" onError={() => setFailed(true)} />
    : <span className="company-logo-fallback" aria-label={`Logo no disponible para ${company.name}`}>{company.ticker}</span>
}

export default function CompanyList({ companies, loading }) {
  const navigate = useNavigate()
  const location = useLocation()

  const handleSelectCompany = (ticker) => {
    navigate(`/company/${ticker}`, { state: { searchReturn: '/search' + location.search } })
  }

  if (loading) {
    return <div className="company-list-loading">Cargando empresas...</div>
  }

  if (companies.length === 0) {
    return <div className="company-list-empty">No se encontraron empresas</div>
  }

  return (
    <div className="company-list">
      {companies.map((company) => (
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
          <h3 className="company-name">{company.name}</h3>
          <span className={`company-sector sector-${company.sector}`}>{sectorLabel(company.sector, company.sector_display)}</span>
          <span className={`coverage-label ${hasIndicators(company) ? 'coverage-available' : ''}`}>{hasIndicators(company) ? 'Indicadores disponibles' : 'Información financiera pendiente'}</span>
          {hasIndicators(company) ? <div className="company-metrics">
            <div className="metric">
              <span className="metric-label">Capitalización</span>
              <span className="metric-value">{company.market_cap == null ? 'Sin datos' : Number(company.market_cap) >= 1e12 ? `$${(company.market_cap / 1e12).toFixed(2)}T` : `$${(company.market_cap / 1e9).toFixed(2)}B`}</span>
            </div>
            <div className="metric">
              <span className="metric-label">PER</span>
              <span className="metric-value">{company.per ?? 'Sin datos'}</span>
            </div>
            <div className="metric">
              <span className="metric-label">ROE</span>
              <span className="metric-value">{company.roe != null ? `${company.roe}%` : 'Sin datos'}</span>
            </div>
          </div> : null}
          <span className="company-card-action">Consultar ficha <span aria-hidden="true">↗</span></span>
        </button>
      ))}
    </div>
  )
}
