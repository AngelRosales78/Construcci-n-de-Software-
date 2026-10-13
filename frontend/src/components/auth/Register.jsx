import { useRegisterViewModel } from '../../viewmodels/useRegisterViewModel'
import { Link } from 'react-router-dom'


export default function Register() {
  const { register, handleSubmit, errors, isSubmitting, error, loading, onSubmit } = useRegisterViewModel()
  return (
    <div className="auth-container">
      <div className="auth-card auth-card--large">
        <div className="auth-header">
          <h1>Create Account</h1>
          <p>Join the S&amp;P 500 Financial Analysis Platform</p>
        </div>

        {error && <div className="auth-error">{error}</div>}

        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="firstName">First Name</label>
              <input
                id="firstName"
                type="text"
                {...register('firstName')}
                placeholder="John"
              />
              {errors.firstName && (
                <span className="error-text">{errors.firstName.message}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="lastName">Last Name</label>
              <input
                id="lastName"
                type="text"
                {...register('lastName')}
                placeholder="Doe"
              />
              {errors.lastName && (
                <span className="error-text">{errors.lastName.message}</span>
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
            />
            {errors.username && (
              <span className="error-text">{errors.username.message}</span>
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
            />
            {errors.email && (
              <span className="error-text">{errors.email.message}</span>
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
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              {...register('password')}
              placeholder="Min. 8 chars with uppercase, lowercase, number &amp; symbol"
            />
            {errors.password && (
              <span className="error-text">{errors.password.message}</span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="passwordConfirm">Confirm Password</label>
            <input
              id="passwordConfirm"
              type="password"
              autoComplete="new-password"
              {...register('passwordConfirm')}
              placeholder="Re-enter your password"
            />
            {errors.passwordConfirm && (
              <span className="error-text">{errors.passwordConfirm.message}</span>
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
