import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { authRepository } from '../models/authRepository'

export function useSessionViewModel() {
  const navigate = useNavigate()
  const [leaving, setLeaving] = useState(false)
  let user = {}
  try { user = authRepository.getCurrentUser() || {} } catch { /* Sesión local inválida. */ }
  const name = user.first_name || user.username || 'Mi cuenta'
  const logout = async () => {
    setLeaving(true)
    try { await authRepository.logout(authRepository.getRefreshToken()) }
    catch { /* El servicio también elimina la sesión local cuando falla la red. */ }
    finally { navigate('/login') }
  }
  return { name, leaving, logout }
}
