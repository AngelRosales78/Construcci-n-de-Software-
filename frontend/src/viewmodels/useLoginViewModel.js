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
      await authRepository.login(data)
      navigate('/dashboard')
    } catch (err) {
      setError(
        err.response?.data?.error || 'Invalid credentials. Please try again.'
      )
    } finally {
      setLoading(false)
    }
  }

  return { register, handleSubmit, errors, isSubmitting, error, loading, onSubmit }
}
