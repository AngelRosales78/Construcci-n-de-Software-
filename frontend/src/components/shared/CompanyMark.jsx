import { useState } from 'react'
import verifiedLogos from '../../data/verifiedLogos.json'
import lightLogos from '../../data/lightLogos.json'

export default function CompanyMark({ company, large = false }) {
  const [failedSource, setFailedSource] = useState(null)
  const ticker = company.ticker || ''
  const name = company.name || ticker
  const source = verifiedLogos.includes(ticker) ? `/company-logos/${ticker.replaceAll('.', '-')}.png` : company.logo_url
  return <span className={`company-mark${large ? ' company-mark-large' : ''}`}>
    {source && source !== failedSource
      ? <img src={source} className={lightLogos.includes(ticker) ? 'company-logo-on-dark' : undefined} alt={`Logo de ${name}`} width={large ? 64 : 40} height={large ? 64 : 40} loading={large ? 'eager' : 'lazy'} onError={() => setFailedSource(source)} />
      : <span className="company-logo-fallback" aria-label={`Logo no disponible para ${name}`}>{ticker}</span>}
  </span>
}
