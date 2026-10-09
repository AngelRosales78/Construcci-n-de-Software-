import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { registerSchema } from '../../utils/validationSchemas'
import { authService } from '../../services/authService'

function getPasswordStrength(password) {
  let score = 0
  if (!password) return { score: 0, label: 'Ninguna', color: 'gray', text: '' }

  if (password.length >= 8) score++
  if (/[A-Z]/.test(password)) score++
  if (/[a-z]/.test(password)) score++
  if (/[0-9]/.test(password)) score++
  if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) score++

  if (score <= 2) return { score: 1, label: 'Débil', color: '#ef4444', text: 'Contraseña débil' }
  if (score <= 3) return { score: 2, label: 'Media', color: '#f59e0b', text: 'Contraseña media' }
  return { score: 3, label: 'Fuerte', color: '#22c55e', text: 'Contraseña fuerte' }
}

export default function Register() {
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [passwordStrength, setPasswordStrength] = useState({ score: 0, label: 'Ninguna', color: 'gray', text: '' })
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)

  const {
    register,
    handleSubmit,
    watch,
    setValue,
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

  const password = watch('password')

  useEffect(() => {
    setPasswordStrength(getPasswordStrength(password))
  }, [password])

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
      await authService.register(payload)
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

  const strength = passwordStrength

  return (
    <div className="auth-container">
      <div className="auth-card auth-card--large">
        <div className="auth-header">
          <h1>Create Account</h1>
          <p>Join the S&amp;P 500 Financial Analysis Platform</p>
        </div>

        {error && (
          <div className="auth-error" role="alert" aria-live="assertive">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="firstName">First Name</label>
              <input
                id="firstName"
                type="text"
                {...register('firstName')}
                placeholder="John"
                aria-invalid={!!errors.firstName}
                aria-describedby={errors.firstName ? 'firstName-error' : undefined}
              />
              {errors.firstName && (
                <span className="error-text" id="firstName-error" role="alert">
                  {errors.firstName.message}
                </span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="lastName">Last Name</label>
              <input
                id="lastName"
                type="text"
                {...register('lastName')}
                placeholder="Doe"
                aria-invalid={!!errors.lastName}
                aria-describedby={errors.lastName ? 'lastName-error' : undefined}
              />
              {errors.lastName && (
                <span className="error-text" id="lastName-error" role="alert">
                  {errors.lastName.message}
                </span>
              )}
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              autoComplete="username"
              {...register('username')}
              placeholder="Choose a username"
              aria-invalid={!!errors.username}
              aria-describedby={errors.username ? 'username-error' : undefined}
            />
            {errors.username && (
              <span className="error-text" id="username-error" role="alert">
                {errors.username.message}
              </span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              {...register('email')}
              placeholder="your@email.com"
              aria-invalid={!!errors.email}
              aria-describedby={errors.email ? 'email-error' : undefined}
            />
            {errors.email && (
              <span className="error-text" id="email-error" role="alert">
                {errors.email.message}
              </span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="userType">Account Type</label>
            <select id="userType" {...register('userType')}>
              <option value="retail">Retail Investor</option>
              <option value="institutional">Institutional Investor</option>
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <div style={{ position: 'relative' }}>
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="new-password"
                {...register('password')}
                placeholder="Min. 8 chars with uppercase, lowercase, number &amp; symbol"
                aria-invalid={!!errors.password}
                aria-describedby={errors.password ? 'password-error' : 'password-strength'}
                onChange={(e) => {
                  register('password').onChange(e)
                  setValue('password', e.target.value, { shouldValidate: true })
                }}
              />
              <button
                type="button"
                className="password-toggle-btn"
                onClick={() => setShowPassword(!showPassword)}
                style={{
                  position: 'absolute',
                  right: 10,
                  top: '50%',
                  transform: 'translateY(-50%)',
                  background: 'none',
                  border: 'none',
                  cursor: 'pointer',
                  fontSize: 16,
                  color: '#6b7280',
                }}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                tabIndex={-1}
              >
                {showPassword ? '🙈' : '👁️'}
              </button>
            </div>

            {password && (
              <div id="password-strength" style={{ marginTop: 6 }}>
                <div
                  style={{
                    display: 'flex',
                    gap: 4,
                    marginBottom: 4,
                  }}
                >
                  <div
                    style={{
                      flex: 1,
                      height: 4,
                      borderRadius: 2,
                      backgroundColor: strength.score >= 1 ? strength.color : '#e5e7eb',
                      transition: 'background-color 0.3s',
                    }}
                  />
                  <div
                    style={{
                      flex: 1,
                      height: 4,
                      borderRadius: 2,
                      backgroundColor: strength.score >= 2 ? strength.color : '#e5e7eb',
                      transition: 'background-color 0.3s',
                    }}
                  />
                  <div
                    style={{
                      flex: 1,
                      height: 4,
                      borderRadius: 2,
                      backgroundColor: strength.score >= 3 ? strength.color : '#e5e7eb',
                      transition: 'background-color 0.3s',
                    }}
                  />
                </div>
                <small
                  style={{
                    color: strength.color,
                    fontSize: 12,
                    display: 'block',
                  }}
                >
                  {strength.text}
                </small>
              </div>
            )}

            {errors.password && (
              <span className="error-text" id="password-error" role="alert">
                {errors.password.message}
              </span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="passwordConfirm">Confirm Password</label>
            <div style={{ position: 'relative' }}>
              <input
                id="passwordConfirm"
                type={showConfirmPassword ? 'text' : 'password'}
                autoComplete="new-password"
                {...register('passwordConfirm')}
                placeholder="Re-enter your password"
                aria-invalid={!!errors.passwordConfirm}
                aria-describedby={errors.passwordConfirm ? 'passwordConfirm-error' : undefined}
              />
              <button
                type="button"
                className="password-toggle-btn"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                style={{
                  position: 'absolute',
                  right: 10,
                  top: '50%',
                  transform: 'translateY(-50%)',
                  background: 'none',
                  border: 'none',
                  cursor: 'pointer',
                  fontSize: 16,
                  color: '#6b7280',
                }}
                aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                tabIndex={-1}
              >
                {showConfirmPassword ? '🙈' : '👁️'}
              </button>
            </div>
            {errors.passwordConfirm && (
              <span className="error-text" id="passwordConfirm-error" role="alert">
                {errors.passwordConfirm.message}
              </span>
            )}
          </div>

          <button type="submit" className="btn-primary" disabled={isSubmitting || loading}>
            {loading ? 'Creating account...' : 'Create Account'}
          </button>
        </form>

        <div className="auth-footer">
          <span>Already have an account? </span>
          <Link to="/login">Sign in</Link>
        </div>
      </div>
    </div>
  )
}
