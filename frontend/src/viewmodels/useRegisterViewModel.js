import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { registerSchema } from '../utils/validationSchemas'
import { authRepository } from '../models/authRepository'

export function useRegisterViewModel() {
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: yupResolver(registerSchema),
    defaultValues: {
      username: '',
      email: '',
      password: '',
      passwordConfirm: '',
      firstName: '',
      lastName: '',
      userType: 'retail',
    },
  })

  const onSubmit = async (data) => {
    setError('')
    setLoading(true)
    try {
      const payload = {
        username: data.username,
        email: data.email,
        password: data.password,
        password_confirm: data.passwordConfirm,
        first_name: data.firstName || '',
        last_name: data.lastName || '',
        user_type: data.userType || 'retail',
      }
      await authRepository.register(payload)
      navigate('/dashboard')
    } catch (err) {
      if (err.response?.data) {
        const serverErrors = err.response.data
        if (typeof serverErrors === 'object') {
          const messages = Object.values(serverErrors)
          setError(messages.flat().join(' '))
        } else {
          setError(typeof serverErrors === 'string' ? serverErrors : 'Registration failed')
        }
      } else {
        setError('Registration failed. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  return { register, handleSubmit, errors, isSubmitting, error, loading, onSubmit }
}
