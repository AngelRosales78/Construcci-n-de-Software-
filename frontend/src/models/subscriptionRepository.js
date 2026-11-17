import api from '../services/api'

export const subscriptionRepository = {
  async load() {
    const [plans, history] = await Promise.all([api.get('/plans/'), api.get('/user/subscription/history/')])
    return { plans: plans.data, ...history.data }
  },
  async checkout(operation) {
    const { data } = await api.post('/user/subscription/checkout/', operation)
    return data
  },
  async cancel() {
    const { data } = await api.post('/user/subscription/cancel/', {})
    return data
  },
}
