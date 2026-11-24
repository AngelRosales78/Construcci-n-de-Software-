export const hasIndicatorValue = value =>
  (typeof value === 'number' || (typeof value === 'string' && value.trim() !== '')) && Number.isFinite(Number(value))

export const countAvailableIndicators = company => ['market_cap', 'per', 'roe', 'beta', 'dividend_yield']
  .filter(key => hasIndicatorValue(company[key])).length

export const hasIndicators = company => countAvailableIndicators(company) > 0

// Presentation only: keep API values and financial calculations unchanged.
export const formatFinancialNumber = (value, decimals = 2) => {
  if (!hasIndicatorValue(value)) return 'Sin datos'
  const amount = Number(value)
  // A small available value must not be presented as an exact zero.
  const options = amount !== 0 && Math.abs(amount) < 10 ** -decimals
    ? { maximumSignificantDigits: 2 }
    : { minimumFractionDigits: decimals, maximumFractionDigits: decimals }
  return amount.toLocaleString('es-PE', options)
}

export const formatMarketValue = value => {
  if (!hasIndicatorValue(value)) return 'Sin datos'
  const amount = Number(value)
  const divisor = Math.abs(amount) >= 1e12 ? 1e12 : Math.abs(amount) >= 1e9 ? 1e9 : 1
  const unit = divisor === 1e12 ? ' billones' : divisor === 1e9 ? ' mil millones' : ''
  return `USD ${formatFinancialNumber(amount / divisor)}${unit}`
}

const sectorNames = {
  technology: 'Tecnología', healthcare: 'Salud', financials: 'Finanzas', energy: 'Energía',
  industrials: 'Industria', materials: 'Materiales', utilities: 'Servicios públicos',
  real_estate: 'Bienes raíces', communication_services: 'Comunicaciones',
  consumer_discretionary: 'Consumo discrecional', consumer_staples: 'Consumo básico',
}
export const sectorLabel = (value, fallback) => sectorNames[value] || fallback || value
