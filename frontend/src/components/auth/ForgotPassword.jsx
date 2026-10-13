import { useForgotPasswordViewModel } from '../../viewmodels/useForgotPasswordViewModel'


export default function ForgotPassword() {
  const { register, handleSubmit, errors, isSubmitting, error, success, loading, onSubmit, navigate } = useForgotPasswordViewModel()
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
