import PasswordInput from './PasswordInput'
import { useRegisterViewModel } from '../../viewmodels/useRegisterViewModel'
import { Link } from 'react-router-dom'
import AuthHeader from './AuthHeader'


export default function Register() {
  const { register, handleSubmit, errors, isSubmitting, error, loading, onSubmit } = useRegisterViewModel()
  return (
    <div className="auth-container">
      <div className="auth-card auth-card--large">
        <AuthHeader mode="register" />

        {error && <div className="auth-error" role="alert">{error}</div>}

        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="firstName">Nombre (opcional)</label>
              <input
                id="firstName"
                type="text"
                {...register('firstName')}
                aria-invalid={Boolean(errors.firstName)}
                aria-describedby={errors.firstName ? 'firstName-error' : undefined}
                placeholder="Tu nombre"
              />
              {errors.firstName && (
                <span id="firstName-error" className="error-text" role="alert">{errors.firstName.message}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="lastName">Apellido (opcional)</label>
              <input
                id="lastName"
                type="text"
                {...register('lastName')}
                aria-invalid={Boolean(errors.lastName)}
                aria-describedby={errors.lastName ? 'lastName-error' : undefined}
                placeholder="Tu apellido"
              />
              {errors.lastName && (
                <span id="lastName-error" className="error-text" role="alert">{errors.lastName.message}</span>
              )}
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="username">Usuario</label>
            <input
              id="username"
              type="text"
              autoComplete="username"
              {...register('username')}
                aria-invalid={Boolean(errors.username)}
                aria-describedby={errors.username ? 'username-error' : undefined}
              placeholder="Elige un nombre de usuario"
            />
            {errors.username && (
              <span id="username-error" className="error-text" role="alert">{errors.username.message}</span>
            )}
          </div>

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

          <div className="form-group">
            <label htmlFor="userType">Tipo de cuenta</label>
            <select id="userType" {...register('userType')}>
              <option value="retail">Inversor individual</option>
              <option value="institutional">Inversor institucional</option>
            </select>
            <small className="auth-hint">Describe tu perfil; no otorga permisos de administrador.</small>
          </div>

          <div className="form-group">
            <label htmlFor="password">Contraseña</label>
            <PasswordInput
              id="password"
              type="password"
              autoComplete="new-password"
              {...register('password')}
                aria-invalid={Boolean(errors.password)}
                aria-describedby={errors.password ? 'password-error' : undefined}
              placeholder="Crea una contraseña segura"
            />
            {errors.password && (
              <span id="password-error" className="error-text" role="alert">{errors.password.message}</span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="passwordConfirm">Confirmar contraseña</label>
            <PasswordInput
              visibilityLabel="confirmación de contraseña"
              id="passwordConfirm"
              type="password"
              autoComplete="new-password"
              {...register('passwordConfirm')}
                aria-invalid={Boolean(errors.passwordConfirm)}
                aria-describedby={errors.passwordConfirm ? 'passwordConfirm-error' : undefined}
              placeholder="Repite tu contraseña"
            />
            {errors.passwordConfirm && (
              <span id="passwordConfirm-error" className="error-text" role="alert">{errors.passwordConfirm.message}</span>
            )}
          </div>

          <button type="submit" className="btn-primary" disabled={isSubmitting || loading}>
            {loading ? 'Creando cuenta...' : 'Crear cuenta'}
          </button>
        </form>

        <div className="auth-footer">
          <span>¿Ya tienes una cuenta? </span>
          <Link to="/login">Iniciar sesión</Link>
        </div>
      </div>
    </div>
  )
}
