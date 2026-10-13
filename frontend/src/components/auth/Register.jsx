import { useRegisterViewModel } from '../../viewmodels/useRegisterViewModel'
import { Link } from 'react-router-dom'
import AuthHeader from './AuthHeader'


export default function Register() {
  const { register, handleSubmit, errors, isSubmitting, error, loading, onSubmit } = useRegisterViewModel()
  return (
    <div className="auth-container">
      <div className="auth-card auth-card--large">
        <AuthHeader mode="register" />

        {error && <div className="auth-error">{error}</div>}

        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="firstName">Nombre</label>
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
              <label htmlFor="lastName">Apellido</label>
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
            <label htmlFor="username">Usuario</label>
            <input
              id="username"
              type="text"
              autoComplete="username"
              {...register('username')}
              placeholder="Elige un nombre de usuario"
            />
            {errors.username && (
              <span className="error-text">{errors.username.message}</span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="email">Correo electrónico</label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              {...register('email')}
              placeholder="tu@correo.com"
            />
            {errors.email && (
              <span className="error-text">{errors.email.message}</span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="userType">Tipo de cuenta</label>
            <select id="userType" {...register('userType')}>
              <option value="retail">Inversor individual</option>
              <option value="institutional">Inversor institucional</option>
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="password">Contraseña</label>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              {...register('password')}
              placeholder="8 caracteres: mayúscula, minúscula, número y símbolo"
            />
            {errors.password && (
              <span className="error-text">{errors.password.message}</span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="passwordConfirm">Confirmar contraseña</label>
            <input
              id="passwordConfirm"
              type="password"
              autoComplete="new-password"
              {...register('passwordConfirm')}
              placeholder="Repite tu contraseña"
            />
            {errors.passwordConfirm && (
              <span className="error-text">{errors.passwordConfirm.message}</span>
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
