import api from '../services/api'

export const adminRepository = {
  async summary() { return (await api.get('/auth/admin/summary/')).data },
  async users(parameters) { return (await api.get(`/admin/users/?${parameters}`)).data },
  async updateUser(id, changes) { return (await api.patch(`/admin/users/${id}/`, changes)).data },
}
