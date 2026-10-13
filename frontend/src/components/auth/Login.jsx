import { useLoginViewModel } from '../../viewmodels/useLoginViewModel'
import { Link } from 'react-router-dom'


export default function Login() {
  const { register, handleSubmit, errors, isSubmitting, error, loading, onSubmit } = useLoginViewModel()
  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h1>S&amp;P 500 Platform</h1>
          <p>Financial Analysis &amp; AI Simulation</p>
        </div>

        {error && <div className="auth-error">{error}</div>}

        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="form-group">
            <label htmlFor="username">Username or Email</label>
            <input
              id="username"
              type="text"
              autoComplete="username"
              {...register('username')}
              placeholder="Enter username or email"
            />
            {errors.username && (
              <span className="error-text">{errors.username.message}</span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              {...register('password')}
              placeholder="Enter your password"
            />
            {errors.password && (
              <span className="error-text">{errors.password.message}</span>
            )}
          </div>

          <button type="submit" className="btn-primary" disabled={isSubmitting || loading}>
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <div className="auth-footer">
          <Link to="/forgot-password">Forgot password?</Link>
          <span> | </span>
          <Link to="/register">Create an account</Link>
        </div>
      </div>
    </div>
  )
}
