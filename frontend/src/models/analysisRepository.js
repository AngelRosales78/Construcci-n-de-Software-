import api from '../services/api'

export const analysisRepository = {
  get: (ticker, year, level) => api.get(`/companies/${encodeURIComponent(ticker)}/analysis/`, {
    params: { fiscal_year: year || undefined, level }, timeout: 160000,
  }).then(response => response.data),
}
