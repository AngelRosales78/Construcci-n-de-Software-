import api from '../services/api'

const data = promise => promise.then(response => response.data)
export const companyRepository = {
  search: parameters => data(api.get(`/companies/search/?${parameters}`)),
  autocomplete: query => data(api.get(`/companies/autocomplete/?q=${encodeURIComponent(query)}`)),
  sectors: () => data(api.get('/companies/sectors/')),
  summary: ticker => data(api.get(`/companies/${encodeURIComponent(ticker)}/summary/`)),
  prices: (ticker, period) => data(api.get(`/companies/${encodeURIComponent(ticker)}/history/?range=${period}`)),
}
