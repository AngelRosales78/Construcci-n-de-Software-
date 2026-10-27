import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { loginSchema } from '../utils/validationSchemas'
import { authRepository } from '../models/authRepository'

export function useLoginViewModel() {
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: yupResolver(loginSchema),
    defaultValues: { username: '', password: '' },
  })

  const onSubmit = async (data) => {
    setError('')
    setLoading(true)
    try {
      const result = await authRepository.login(data)
      navigate(result?.user?.role === 'admin' ? '/admin-panel' : '/dashboard')
    } catch (err) {
      setError(
        err.response?.data?.error || 'No se pudo iniciar sesión. Revisa tus datos e inténtalo de nuevo.'
      )
    } finally {
      setLoading(false)
    }
  }

  return { register, handleSubmit, errors, isSubmitting, error, loading, onSubmit }
}
