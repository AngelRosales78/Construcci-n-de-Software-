import { useNavigate } from 'react-router-dom'
import { authService } from '../services/authService'

export default function Dashboard() {
  const navigate = useNavigate()

  const handleLogout = async () => {
    try {
      const refreshToken = localStorage.getItem('refresh_token')
      await authService.logout(refreshToken)
    } catch (error) {
      console.error('Error during logout:', error)
    } finally {
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      navigate('/login')
    }
  }

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <h1>Panel S&amp;P 500</h1>
        <button onClick={handleLogout} className="btn-secondary">
          Cerrar Sesión
        </button>
      </header>

      <main className="dashboard-content">
        <section className="welcome-section">
          <h2>Bienvenido a la Plataforma de Análisis Financiero</h2>
          <p>Próximamente: Análisis de datos del S&amp;P 500 con Inteligencia Artificial</p>
        </section>

        <section className="features-grid">
          <div className="feature-card">
            <h3>📊 Análisis de Acciones</h3>
            <p>Visualización interactiva de datos históricos del S&amp;P 500</p>
          </div>
          <div className="feature-card">
            <h3>🤖 Predicciones IA</h3>
            <p>Modelos de machine learning para predicción de tendencias</p>
          </div>
          <div className="feature-card">
            <h3>📈 Portafolios</h3>
            <p>Gestión y análisis de portafolios de inversión</p>
          </div>
          <div className="feature-card">
            <h3>🔔 Alertas</h3>
            <p>Sistema de notificaciones personalizadas</p>
          </div>
        </section>
      </main>
    </div>
  )
}
