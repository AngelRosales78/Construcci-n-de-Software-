import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Login from './components/auth/Login'
import Register from './components/auth/Register'
import ForgotPassword from './components/auth/ForgotPassword'
import ResetPassword from './components/auth/ResetPassword'
import Dashboard from './components/Dashboard'
import CompanySearch from './components/search/CompanySearch'
import CompanyDashboard from './components/company/CompanyDashboard'
import AdminDashboard from './components/AdminDashboard'
import ProtectedRoute from './components/auth/ProtectedRoute'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password/:uid/:token" element={<ResetPassword />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/admin-panel" element={<AdminDashboard />} />
          <Route path="/search" element={<CompanySearch />} />
          <Route path="/company/:ticker" element={<CompanyDashboard />} />
        </Route>
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
