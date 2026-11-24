import { Link } from 'react-router-dom'

export default function AuthHeader({ mode }) {
  return <>
    <div className="auth-header">
      <p className="editorial-kicker">{mode === 'login' ? 'CONTINÚA TU INVESTIGACIÓN' : 'UN ESPACIO PARA TU CRITERIO'}</p>
      <h1>{mode === 'login' ? 'Bienvenido a tu cuaderno.' : 'Comienza tu lectura.'}</h1>
      <p>{mode === 'login' ? 'Ingresa a tu cuenta para explorar empresas y retomar tus análisis.' : 'Crea tu cuenta y organiza las empresas que quieres conocer mejor.'}</p>
    </div>
    <nav className="auth-tabs" aria-label="Acceso a la plataforma">
      <Link to="/login" aria-current={mode === 'login' ? 'page' : undefined}>Iniciar sesión</Link>
      <Link to="/register" aria-current={mode === 'register' ? 'page' : undefined}>Crear cuenta</Link>
    </nav>
  </>
}
