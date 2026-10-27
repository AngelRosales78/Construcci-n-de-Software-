import PasswordInput from './PasswordInput'
import { Link } from 'react-router-dom'
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

        {error && <div className="auth-error" role="alert">{error}</div>}
        {success && <div className="auth-success" role="status">Contraseña restablecida exitosamente. Redirigiendo...</div>}

        {!success && (
          <form onSubmit={handleSubmit(onSubmit)}>
            <div className="form-group">
              <label htmlFor="password">Nueva Contraseña</label>
              <PasswordInput
                id="password"
                type="password"
                autoComplete="new-password"
                {...register('password')}
                aria-invalid={Boolean(errors.password)}
                aria-describedby={errors.password ? 'password-error' : undefined}
                placeholder="Min. 8 caracteres"
              />
              {errors.password && (
                <span id="password-error" className="error-text" role="alert">{errors.password.message}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="passwordConfirm">Confirmar Nueva Contraseña</label>
              <PasswordInput
                visibilityLabel="confirmación de contraseña"
                id="passwordConfirm"
                type="password"
                autoComplete="new-password"
                {...register('passwordConfirm')}
                aria-invalid={Boolean(errors.passwordConfirm)}
                aria-describedby={errors.passwordConfirm ? 'passwordConfirm-error' : undefined}
                placeholder="Re-ingresa tu contraseña"
              />
              {errors.passwordConfirm && (
                <span id="passwordConfirm-error" className="error-text" role="alert">{errors.passwordConfirm.message}</span>
              )}
            </div>

            <button type="submit" className="btn-primary" disabled={isSubmitting || loading}>
              {loading ? 'Restableciendo...' : 'Restablecer Contraseña'}
            </button>
          </form>
        )}
        {!success && <div className="auth-footer"><Link to="/forgot-password">Solicitar otro enlace</Link><span> · </span><Link to="/login">Volver al acceso</Link></div>}
      </div>
    </div>
  )
}
