import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { authRepository } from '../models/authRepository'
import { profileRepository } from '../models/profileRepository'

function readUser() {
  try { return authRepository.getCurrentUser() || {} } catch { return {} }
}

export function useSessionViewModel() {
  const navigate = useNavigate()
  const [leaving, setLeaving] = useState(false)
  const [user, setUser] = useState(readUser)
  const [avatarUrl, setAvatarUrl] = useState(null)
  useEffect(() => {
    const update = () => setUser(readUser())
    window.addEventListener('sp500-profile-updated', update)
    const unsubscribe = authRepository.subscribeSession(update)
    return () => { unsubscribe(); window.removeEventListener('sp500-profile-updated', update) }
  }, [])
  useEffect(() => {
    let alive = true
    let url
    setAvatarUrl(null)
    if (user.avatar_url) profileRepository.avatar().then(blob => {
      if (alive) { url = URL.createObjectURL(blob); setAvatarUrl(url) }
    }).catch(() => { /* Se conserva la inicial cuando la imagen no se puede cargar. */ })
    return () => { alive = false; if (url) URL.revokeObjectURL(url) }
  }, [user.id, user.avatar_url, user.avatar_version])
  const name = user.first_name || user.username || 'Mi cuenta'
  const logout = async () => {
    setLeaving(true)
    try { await authRepository.logout(authRepository.getRefreshToken()) }
    catch { /* El servicio también elimina la sesión local cuando falla la red. */ }
    finally { navigate('/login') }
  }
  return { name, role: user.role, avatarUrl, leaving, logout }
}
