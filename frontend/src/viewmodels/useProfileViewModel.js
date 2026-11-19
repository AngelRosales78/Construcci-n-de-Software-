import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useRemoteResource } from './useRemoteResource'
import { profileRepository } from '../models/profileRepository'
import { currentOwner, errorMessage } from '../models/sprint3Repository'
import { clearSession } from '../services/sessionStore'

export function useProfileViewModel() {
  const profile = useRemoteResource('/auth/profile/')
  const navigate = useNavigate()
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [avatar, setAvatar] = useState(null)
  const [preview, setPreview] = useState(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [deleting, setDeleting] = useState(false)
  const [password, setPassword] = useState('')
  const working = useRef(false)
  useEffect(() => {
    if (profile.data) { setFirstName(profile.data.first_name || ''); setLastName(profile.data.last_name || '') }
  }, [profile.data])
  useEffect(() => {
    setAvatar(null); setPassword(''); setDeleting(false); setMessage(''); setError('')
  }, [profile.owner])
  useEffect(() => {
    if (!avatar) { setPreview(null); return }
    const url = URL.createObjectURL(avatar)
    setPreview(url)
    return () => URL.revokeObjectURL(url)
  }, [avatar])
  const selectAvatar = file => {
    setError('')
    if (file && file.size >= 2 * 1024 * 1024) { setAvatar(null); setError('La imagen debe ocupar menos de 2 MB.'); return }
    setAvatar(file || null)
  }
  const perform = async remove => {
    if (working.current) return
    const owner = currentOwner()
    const currentPassword = password
    if (remove && !currentPassword) { setError('Introduce tu contraseña actual.'); return }
    working.current = true; setBusy(true); setError(''); setMessage(''); setPassword('')
    try {
      if (remove) {
        await profileRepository.removeAccount(currentPassword)
        if (owner === currentOwner()) { clearSession(); navigate('/login', { replace: true }) }
      } else {
        const data = new FormData()
        data.append('first_name', firstName); data.append('last_name', lastName)
        if (avatar) data.append('avatar', avatar)
        await profileRepository.save(data)
        if (owner === currentOwner()) { profile.reload(); setMessage('Perfil guardado.'); setAvatar(null) }
      }
    } catch (failure) { if (owner === currentOwner()) setError(errorMessage(failure)) }
    finally { working.current = false; setBusy(false) }
  }
  return { profile, firstName, setFirstName, lastName, setLastName, preview, selectAvatar, busy,
    error, message, deleting, setDeleting, password, setPassword,
    save: () => perform(false), remove: () => perform(true) }
}
