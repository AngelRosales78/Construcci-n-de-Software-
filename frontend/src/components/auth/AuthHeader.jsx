import { Link } from 'react-router-dom'

export default function AuthHeader({ mode }) {
  return <>
    <div className="auth-header">
      <p className="editorial-kicker">{mode === 'login' ? 'TU ESPACIO FINANCIERO' : 'EMPIEZA A ENTENDER'}</p>
      <h1>{mode === 'login' ? 'Bienvenido de nuevo.' : 'Crea tu cuenta'}</h1>
      <p>{mode === 'login' ? 'Entra para continuar con las empresas que te interesan.' : 'Guarda empresas y entiende sus cifras a tu ritmo.'}</p>
    </div>
    <nav className="auth-tabs" aria-label="Acceso a la plataforma">
      <Link to="/login" aria-current={mode === 'login' ? 'page' : undefined}>Iniciar sesión</Link>
      <Link to="/register" aria-current={mode === 'register' ? 'page' : undefined}>Crear cuenta</Link>
    </nav>
  </>
}
