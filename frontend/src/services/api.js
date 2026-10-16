import axios from 'axios';
import { clearSession } from './sessionStore';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8000/api/v1';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });

  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (!originalRequest) return Promise.reject(error);
    if (error.response?.status === 401 && error.response?.data?.code === 'token_blacklisted') {
      const sentAuthorization = originalRequest.headers?.get?.('Authorization') || originalRequest.headers?.Authorization;
      if (sentAuthorization === `Bearer ${localStorage.getItem('access_token')}`) clearSession();
      return Promise.reject(error);
    }

    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !/\/(?:auth\/)?(?:token|register|refresh|password\/reset)(?:\/|$)/.test(originalRequest.url) &&
      error.response?.data?.code !== 'token_blacklisted' &&
      localStorage.getItem('refresh_token')
    ) {
      originalRequest._retry = true;
      if (isRefreshing) {
        try {
          const token = await new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject });
          });
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        } catch (err) {
          return Promise.reject(err);
        }
      }

      isRefreshing = true;

      const refreshToken = localStorage.getItem('refresh_token');

      try {
        const response = await axios.post(`${API_URL}/auth/refresh/`, {
          refresh: refreshToken,
        });

        // A late refresh must not resurrect a session closed in another tab.
        if (localStorage.getItem('refresh_token') !== refreshToken) {
          throw new Error('La sesión cambió durante la renovación.');
        }
        const { access } = response.data;
        localStorage.setItem('access_token', access);
        if (response.data.refresh) localStorage.setItem('refresh_token', response.data.refresh);
        processQueue(null, access);
        originalRequest.headers.Authorization = `Bearer ${access}`;
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        if (localStorage.getItem('refresh_token') === refreshToken) clearSession();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default api;
