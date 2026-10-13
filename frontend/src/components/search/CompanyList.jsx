import { useNavigate } from 'react-router-dom'
import { useState } from 'react'
import verifiedLogos from '../../data/verifiedLogos.json'

function CompanyLogo({ company }) {
  const [failed, setFailed] = useState(false)
  const local = verifiedLogos.includes(company.ticker)
    ? `/company-logos/${company.ticker.replaceAll('.', '-')}.png` : ''
  const source = local || company.logo_url
  return source && !failed
    ? <img src={source} alt={`Logo de ${company.name}`} width="40" height="40" loading="lazy" onError={() => setFailed(true)} />
    : <span className="company-logo-fallback" aria-label={`Logo no disponible para ${company.name}`}>{company.ticker}</span>
}

export default function CompanyList({ companies, loading }) {
  const navigate = useNavigate()

  const handleSelectCompany = (ticker) => {
    navigate(`/company/${ticker}`)
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
        <div
          key={company.ticker}
          className="company-card"
          onClick={() => handleSelectCompany(company.ticker)}
        >
          <div className="company-card-header">
            <CompanyLogo company={company} />
            <strong className="company-ticker">{company.ticker}</strong>
            <span className={`company-sector sector-${company.sector}`}>{company.sector_display}</span>
          </div>
          <h3 className="company-name">{company.name}</h3>
          <div className="company-metrics">
            <div className="metric">
              <span className="metric-label">Market Cap</span>
              <span className="metric-value">{company.market_cap == null ? 'Sin datos' : `$${(company.market_cap / 1e9).toFixed(2)}B`}</span>
            </div>
            <div className="metric">
              <span className="metric-label">PER</span>
              <span className="metric-value">{company.per ?? 'N/A'}</span>
            </div>
            <div className="metric">
              <span className="metric-label">ROE</span>
              <span className="metric-value">{company.roe != null ? `${company.roe}%` : 'N/A'}</span>
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}
