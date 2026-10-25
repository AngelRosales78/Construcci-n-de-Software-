import { Navigate, Outlet } from 'react-router-dom'
import { useRouteSessionViewModel } from '../../viewmodels/useRouteSessionViewModel'

export default function ProtectedRoute() {
  const sessionIdentity = useRouteSessionViewModel()
  return sessionIdentity ? <Outlet key={sessionIdentity} /> : <Navigate to="/login" replace />
}
