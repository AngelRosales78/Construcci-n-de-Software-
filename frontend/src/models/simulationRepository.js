import api from '../services/api'

export const simulationRepository = {
  create: (ticker, parameters) => api.post(`/companies/${encodeURIComponent(ticker)}/simulations/`, parameters).then(response => response.data),
  get: id => api.get(`/simulations/${encodeURIComponent(id)}/`).then(response => response.data),
}
