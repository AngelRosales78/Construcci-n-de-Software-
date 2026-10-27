import PasswordInput from './PasswordInput'
import { useLoginViewModel } from '../../viewmodels/useLoginViewModel'
import { Link } from 'react-router-dom'
import AuthHeader from './AuthHeader'


export default function Login() {
  const { register, handleSubmit, errors, isSubmitting, error, loading, onSubmit } = useLoginViewModel()
  return (
    <div className="auth-container">
      <div className="auth-card">
        <AuthHeader mode="login" />

        {error && <div className="auth-error" role="alert">{error}</div>}

        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="form-group">
            <label htmlFor="username">Usuario</label>
            <input
              id="username"
              type="text"
              autoComplete="username"
              {...register('username')}
                aria-invalid={Boolean(errors.username)}
                aria-describedby={errors.username ? 'username-error' : undefined}
              placeholder="Tu nombre de usuario"
            />
            {errors.username && (
              <span id="username-error" className="error-text" role="alert">{errors.username.message}</span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="password">Contraseña</label>
            <PasswordInput
              id="password"
              type="password"
              autoComplete="current-password"
              {...register('password')}
                aria-invalid={Boolean(errors.password)}
                aria-describedby={errors.password ? 'password-error' : undefined}
              placeholder="Tu contraseña"
            />
            {errors.password && (
              <span id="password-error" className="error-text" role="alert">{errors.password.message}</span>
            )}
          </div>

          <button type="submit" className="btn-primary" disabled={isSubmitting || loading}>
            {loading ? 'Iniciando sesión...' : 'Iniciar sesión'}
          </button>
        </form>

        <div className="auth-footer">
          <Link to="/forgot-password">¿Olvidaste tu contraseña?</Link>
          <span> | </span>
          <Link to="/register">Crear cuenta</Link>
        </div>
      </div>
    </div>
  )
}
