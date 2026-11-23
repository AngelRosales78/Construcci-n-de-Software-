export const hasIndicatorValue = value =>
  (typeof value === 'number' || (typeof value === 'string' && value.trim() !== '')) && Number.isFinite(Number(value))

export const countAvailableIndicators = company => ['market_cap', 'per', 'roe', 'beta', 'dividend_yield']
  .filter(key => hasIndicatorValue(company[key])).length

export const hasIndicators = company => countAvailableIndicators(company) > 0

const sectorNames = {
  technology: 'Tecnología', healthcare: 'Salud', financials: 'Finanzas', energy: 'Energía',
  industrials: 'Industria', materials: 'Materiales', utilities: 'Servicios públicos',
  real_estate: 'Bienes raíces', communication_services: 'Comunicaciones',
  consumer_discretionary: 'Consumo discrecional', consumer_staples: 'Consumo básico',
}
export const sectorLabel = (value, fallback) => sectorNames[value] || fallback || value
