import { Link } from 'react-router-dom'
import Icon from '../shared/Icon'
import CompanyMark from '../shared/CompanyMark'

const sectors = [
  { id: 'technology', name: 'Tecnología', description: 'Software, dispositivos y semiconductores.', tickers: ['MSFT', 'AAPL', 'NVDA'] },
  { id: 'healthcare', name: 'Salud', description: 'Medicamentos, equipos y servicios de salud.', tickers: ['JNJ', 'ABT', 'LLY'] },
  { id: 'financials', name: 'Finanzas', description: 'Banca, seguros y servicios financieros.', tickers: ['JPM', 'V', 'MA'] },
  { id: 'industrials', name: 'Industria', description: 'Fabricación, transporte e infraestructura.', tickers: ['CAT', 'GE', 'HON'] },
  { id: 'energy', name: 'Energía', description: 'Producción, distribución y servicios energéticos.', tickers: ['XOM', 'CVX', 'COP'] },
  { id: 'consumer_discretionary', name: 'Consumo', description: 'Comercio, automóviles y experiencias.', tickers: ['AMZN', 'TSLA', 'HD'] },
]

export default function SectorExplorer({ compact = false, selected = '', onSelect }) {
  if (compact) return <nav className="sector-shortcuts" aria-label="Accesos por sector">
    <button type="button" aria-pressed={!selected} onClick={() => onSelect('')}>Todos</button>
    {sectors.map(sector => <button type="button" key={sector.id} aria-pressed={selected === sector.id} onClick={() => onSelect(sector.id)}><Icon name={sector.id} size={17} />{sector.name}</button>)}
  </nav>
  return <section className="sector-explorer" aria-labelledby="sector-explorer-title">
    <div className="sector-explorer-heading"><div><p className="section-eyebrow">ENCUENTRA TU PUNTO DE PARTIDA</p><h2 id="sector-explorer-title">Empieza por un sector</h2></div><Link to="/search">Ver todo el catálogo →</Link></div>
    <div className="sector-grid">{sectors.map(sector => <Link key={sector.id} className="sector-tile" to={`/search?sector=${sector.id}`}>
      <span className="sector-tile-icon"><Icon name={sector.id} size={25} /></span>
      <h3>{sector.name}<span aria-hidden="true">↗</span></h3><p>{sector.description}</p>
      <div className="sector-brands" aria-hidden="true">{sector.tickers.map(ticker => <CompanyMark key={ticker} company={{ ticker, name: ticker }} />)}<span>Explorar empresas</span></div>
    </Link>)}</div>
  </section>
}
