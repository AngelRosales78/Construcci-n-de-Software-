import api from '../services/api'

function validFavorite(row) {
  return row && Number.isSafeInteger(row.id) && row.id > 0 && typeof row.ticker === 'string' && typeof row.name === 'string'
}

export const favoritesRepository = {
  async remove(id) {
    const response = await api.delete(`/favorites/${id}/`)
    if (response.status !== 204) throw new Error('La baja no fue confirmada.')
  },
  async list() {
    const { data } = await api.get('/favorites/')
    if (data.schema_version !== 'favorites-v1' || !Array.isArray(data.results) || !data.results.every(validFavorite)) {
      throw new Error('Respuesta de favoritos no válida.')
    }
    return data.results
  },
  async add(ticker) {
    const { data } = await api.post('/favorites/', { ticker })
    if (data.schema_version !== 'favorites-v1' || !validFavorite(data.favorite) || data.favorite.ticker !== ticker) {
      throw new Error('El servidor no confirmó el favorito solicitado.')
    }
    return data.favorite
  },
}
