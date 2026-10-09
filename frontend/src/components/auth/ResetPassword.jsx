import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { resetPasswordSchema } from '../utils/validationSchemas'
import { authService } from '../services/authService'

export default function ResetPassword() {
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
      await authService.resetPassword({
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

  if (!token || !uid) {
    return (
      <div className="auth-container">
        <div className="auth-card">
          <h1>Enlace Inválido</h1>
          <p>El enlace de restablecimiento no es válido o ha expirado.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h1>Nueva Contraseña</h1>
          <p>Ingresa tu nueva contraseña</p>
        </div>

        {error && <div className="auth-error">{error}</div>}
        {success && <div className="auth-success">Contraseña restablecida exitosamente. Redirigiendo...</div>}

        {!success && (
          <form onSubmit={handleSubmit(onSubmit)}>
            <div className="form-group">
              <label htmlFor="password">Nueva Contraseña</label>
              <input
                id="password"
                type="password"
                autoComplete="new-password"
                {...register('password')}
                placeholder="Min. 8 caracteres"
              />
              {errors.password && (
                <span className="error-text">{errors.password.message}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="passwordConfirm">Confirmar Nueva Contraseña</label>
              <input
                id="passwordConfirm"
                type="password"
                autoComplete="new-password"
                {...register('passwordConfirm')}
                placeholder="Re-ingresa tu contraseña"
              />
              {errors.passwordConfirm && (
                <span className="error-text">{errors.passwordConfirm.message}</span>
              )}
            </div>

            <button type="submit" className="btn-primary" disabled={isSubmitting || loading}>
              {loading ? 'Restableciendo...' : 'Restablecer Contraseña'}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}
