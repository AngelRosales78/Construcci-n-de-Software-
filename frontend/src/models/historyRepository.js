import api from '../services/api'

export const historyRepository = {
  list: () => api.get('/companies/history/').then(response => response.data),
  save: query => api.post('/companies/history/', { query }),
  remove: id => api.delete(id == null ? '/companies/history/clear/' : `/companies/history/${id}/`),
  accountKey() {
    try { const user = JSON.parse(localStorage.getItem('user') || 'null'); return user?.id != null ? `sp500:history:${user.id}` : null }
    catch { return null }
  },
  read(key) {
    try { const rows = key ? JSON.parse(localStorage.getItem(key) || '[]') : []; return Array.isArray(rows) ? rows.slice(0, 10) : [] }
    catch { return [] }
  },
  persist(key, rows) {
    if (!key) return
    try { localStorage.setItem(key, JSON.stringify(rows.slice(0, 10))) } catch { /* Almacenamiento deshabilitado. */ }
  },
}
