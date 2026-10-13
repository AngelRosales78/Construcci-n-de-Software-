export const hasIndicators = company => ['market_cap', 'per', 'roe', 'beta', 'dividend_yield']
  .some(key => company[key] != null && Number.isFinite(Number(company[key])))

const sectorNames = {
  technology: 'Tecnología', healthcare: 'Salud', financials: 'Finanzas', energy: 'Energía',
  industrials: 'Industria', materials: 'Materiales', utilities: 'Servicios públicos',
  real_estate: 'Bienes raíces', communication_services: 'Comunicaciones',
  consumer_discretionary: 'Consumo discrecional', consumer_staples: 'Consumo básico',
}
export const sectorLabel = (value, fallback) => sectorNames[value] || fallback || value
