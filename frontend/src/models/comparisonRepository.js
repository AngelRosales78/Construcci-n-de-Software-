import api from '../services/api'

export const comparisonRepository = {
  async subscription() {
    const { data } = await api.get('/user/subscription/')
    if (data.schema_version !== 'subscription-v1' || !Array.isArray(data.features)
      || !['free', 'pro'].includes(data.plan) || !Number.isFinite(Date.parse(data.valid_until))) {
      throw new Error('No se pudo verificar el acceso de esta cuenta.')
    }
    return data
  },
  async compare(a, b, year) {
    const params = new URLSearchParams({ a, b, fiscal_year: year })
    const { data } = await api.get(`/compare/?${params}`)
    if (data.schema_version !== 'comparison-v1' || data.fiscal_year !== Number(year)
      || data.a?.ticker !== a || data.b?.ticker !== b
      || data.a?.schema_version !== 'pillars-v1' || data.b?.schema_version !== 'pillars-v1'
      || data.a?.fiscal_year !== Number(year) || data.b?.fiscal_year !== Number(year)
      || !Array.isArray(data.comparisons)) {
      throw new Error('El servidor devolvió otras empresas o un período diferente.')
    }
    return data
  },
}
