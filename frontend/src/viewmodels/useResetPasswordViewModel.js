import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { resetPasswordSchema } from '../utils/validationSchemas'
import { authRepository } from '../models/authRepository'

export function useResetPasswordViewModel() {
  const navigate = useNavigate()
  const { uid, token } = useParams()
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: yupResolver(resetPasswordSchema),
    defaultValues: { password: '', passwordConfirm: '' },
  })

  const onSubmit = async (data) => {
    setError('')
    setSuccess(false)
    setLoading(true)
    try {
      await authRepository.resetPassword(uid, token, data.password)
      setSuccess(true)
      setTimeout(() => navigate('/login'), 3000)
    } catch (err) {
      if (err.response?.data) {
        const messages = Object.values(err.response.data).flat().join(' ')
        setError(messages)
      } else {
        setError('No se pudo cambiar la contraseña. El enlace puede haber caducado.')
      }
    } finally {
      setLoading(false)
    }
  }

  return { register, handleSubmit, errors, isSubmitting, error, success, loading, token, uid, onSubmit, navigate }
}
