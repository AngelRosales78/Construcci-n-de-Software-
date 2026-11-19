import api from './api'
import { clearSession, hasSession, saveSession, subscribeSession } from './sessionStore'

export const authService = {
  /**
   * Register a new user (HU01_01 frontend)
   */
  async register(data) {
    const response = await api.post('/auth/register/', data)
    if (response.data.access) {
      saveSession(response.data)
    }
    return response.data
  },

  /**
   * Login user and store JWT tokens (HU01_02 frontend)
   */
  async login(data) {
    const response = await api.post('/auth/token/', data)
    if (response.data.access) {
      saveSession(response.data)
    }
    return response.data
  },

  /**
   * Logout and clear tokens (HU01_04 frontend)
   */
  async logout(refreshToken) {
    try {
      await api.post('/auth/logout/', { refresh_token: refreshToken })
    } finally {
      clearSession()
    }
  },

  /**
   * Request password reset (HU01_03 frontend)
   */
  async forgotPassword(email) {
    const response = await api.post('/auth/password/reset/', { email })
    return response.data
  },

  /**
   * Confirm password reset
   */
  async resetPassword(uid, token, newPassword) {
    const response = await api.post('/auth/password/reset/confirm/', {
      token: `${uid}:${token}`,
      new_password: newPassword,
      new_password_confirm: newPassword,
    })
    return response.data
  },

  /**
   * Get current user profile
   */
  async getProfile() {
    const response = await api.get('/auth/profile/')
    return response.data
  },

  /**
   * Update user profile
   */
  async updateProfile(data) {
    const owner = String(JSON.parse(localStorage.getItem('user') || '{}').id)
    const options = data instanceof FormData ? { headers: { 'Content-Type': undefined } } : {}
    const response = await api.patch('/auth/profile/', data, options)
    const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
    if (String(currentUser.id) !== owner) throw new Error('La cuenta cambió durante la actualización.')
    localStorage.setItem('user', JSON.stringify({ ...currentUser, ...response.data }))
    window.dispatchEvent(new Event('sp500-profile-updated'))
    return response.data
  },

  /**
   * Check if user is authenticated
   */
  isAuthenticated() {
    return hasSession()
  },

  subscribeSession,

  getSessionIdentity() {
    if (!hasSession()) return ''
    try {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      return String(user.id || user.username || 'authenticated')
    }
    catch { return 'authenticated' }
  },

  /**
   * Get current user object
   */
  getCurrentUser() {
    const user = localStorage.getItem('user')
    return user ? JSON.parse(user) : null
  },

  /**
   * Get stored refresh token
   */
  getRefreshToken() {
    return localStorage.getItem('refresh_token')
  },
}
