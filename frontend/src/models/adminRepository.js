import api from '../services/api'

export const adminRepository = {
  async updateBudget(provider, values) { return (await api.patch(`/admin/monitoring/budgets/${provider}/`, values)).data },
  async summary() { return (await api.get('/auth/admin/summary/')).data },
  async users(parameters) { return (await api.get(`/admin/users/?${parameters}`)).data },
  async updateUser(id, changes) { return (await api.patch(`/admin/users/${id}/`, changes)).data },
  async updateCompany(id, changes) { return (await api.patch(`/admin/companies/${id}/`, changes)).data },
  async refreshPrices(id, request_id) { return (await api.post(`/admin/companies/${id}/refresh/`, { request_id }, { timeout: 45000 })).data },
  async verifyCompany(data) { return (await api.post('/admin/companies/verify/', data, { timeout: 45000 })).data },
  async createCompany(verification_id) { return (await api.post('/admin/companies/', { verification_id }, { timeout: 45000 })).data },
}
