import api from '../services/api'

export const adminRepository = {
  async summary() { return (await api.get('/auth/admin/summary/')).data },
}
