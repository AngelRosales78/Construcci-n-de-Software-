import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { resetPasswordSchema } from '../utils/validationSchemas'
import { authRepository } from '../models/authRepository'

export function useResetPasswordViewModel() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  const token = searchParams.get('token')
  const uid = searchParams.get('uid')

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
      await authRepository.resetPassword({
        uid,
        token,
        password: data.password,
        password_confirm: data.passwordConfirm,
      })
      setSuccess(true)
      setTimeout(() => navigate('/login'), 3000)
    } catch (err) {
      if (err.response?.data) {
        const messages = Object.values(err.response.data).flat().join(' ')
        setError(messages)
      } else {
        setError('Failed to reset password. The link may be expired.')
      }
    } finally {
      setLoading(false)
    }
  }

  return { register, handleSubmit, errors, isSubmitting, error, success, loading, token, uid, onSubmit, navigate }
}
