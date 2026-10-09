import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { forgotPasswordSchema } from '../utils/validationSchemas'
import { authService } from '../services/authService'

export default function ForgotPassword() {
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: yupResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  })

  const onSubmit = async (data) => {
    setError('')
    setSuccess(false)
    setLoading(true)
    try {
      await authService.forgotPassword(data.email)
      setSuccess(true)
    } catch (err) {
      if (err.response?.data?.detail) {
        setError(err.response.data.detail)
      } else {
        setError('Failed to send reset email. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h1>Reset Password</h1>
          <p>Enter your email and we'll send you a link to reset your password</p>
        </div>

        {error && <div className="auth-error">{error}</div>}
        {success && <div className="auth-success">Check your email for the reset link</div>}

        {!success && (
          <form onSubmit={handleSubmit(onSubmit)}>
            <div className="form-group">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                {...register('email')}
                placeholder="your@email.com"
              />
              {errors.email && (
                <span className="error-text">{errors.email.message}</span>
              )}
            </div>

            <button type="submit" className="btn-primary" disabled={isSubmitting || loading}>
              {loading ? 'Sending...' : 'Send Reset Link'}
            </button>
          </form>
        )}

        <div className="auth-footer">
          <span>Remember your password? </span>
          <a href="/login">Sign in</a>
        </div>
      </div>
    </div>
  )
}
