import { useResetPasswordViewModel } from '../../viewmodels/useResetPasswordViewModel'


export default function ResetPassword() {
  const { register, handleSubmit, errors, isSubmitting, error, success, loading, token, uid, onSubmit, navigate } = useResetPasswordViewModel()
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
