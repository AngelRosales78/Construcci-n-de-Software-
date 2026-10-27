import { useForgotPasswordViewModel } from '../../viewmodels/useForgotPasswordViewModel'


export default function ForgotPassword() {
  const { register, handleSubmit, errors, isSubmitting, error, success, loading, onSubmit, navigate } = useForgotPasswordViewModel()
  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h1>Restablecer contraseña</h1>
          <p>Ingresa el correo asociado a tu cuenta para solicitar un enlace</p>
        </div>

        {error && <div className="auth-error" role="alert">{error}</div>}
        {success && <div className="auth-success" role="status">Solicitud recibida. Si la cuenta existe, se generará un enlace de recuperación.</div>}

        {!success && (
          <form onSubmit={handleSubmit(onSubmit)}>
            <div className="form-group">
              <label htmlFor="email">Correo electrónico</label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                {...register('email')}
                aria-invalid={Boolean(errors.email)}
                aria-describedby={errors.email ? 'email-error' : undefined}
                placeholder="tu@correo.com"
              />
              {errors.email && (
                <span id="email-error" className="error-text" role="alert">{errors.email.message}</span>
              )}
            </div>

            <button type="submit" className="btn-primary" disabled={isSubmitting || loading}>
              {loading ? 'Enviando...' : 'Solicitar enlace'}
            </button>
          </form>
        )}

        <div className="auth-footer">
          <span>¿Recuerdas tu contraseña? </span>
          <a href="/login">Iniciar sesión</a>
        </div>
      </div>
    </div>
  )
}
