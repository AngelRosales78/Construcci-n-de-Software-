import api from '../services/api'
import { authRepository } from './authRepository'

export const profileRepository = {
  load: () => authRepository.getProfile(),
  save: data => authRepository.updateProfile(data),
  async avatar() {
    return (await api.get('/auth/profile/avatar/', { responseType: 'blob' })).data
  },
  async removeAccount(password) {
    await api.delete('/auth/account/', { data: { password } })
  },
}
