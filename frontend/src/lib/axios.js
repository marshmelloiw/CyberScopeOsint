import axios from 'axios';

// Create axios instance
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling and token refresh
api.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // Handle 401 Unauthorized errors
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // Attempt to refresh token
        const refreshToken = localStorage.getItem('refreshToken');
        if (refreshToken) {
          const response = await axios.post('/auth/refresh', {
            refreshToken,
          });
          
          const { token } = response.data;
          localStorage.setItem('authToken', token);
          
          // Retry original request with new token
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        }
      } catch (refreshError) {
        // Refresh failed, redirect to login
        localStorage.removeItem('authToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/auth/login';
        return Promise.reject(refreshError);
      }
    }

    // Handle other errors
    if (error.response?.status === 403) {
      // Forbidden - redirect to 403 page
      window.location.href = '/403';
    }

    return Promise.reject(error);
  }
);

// API endpoints
export const endpoints = {
  // Authentication
  auth: {
    login: '/auth/login',
    register: '/auth/register',
    mfa: '/auth/mfa/verify',
    refresh: '/auth/refresh',
    forgotPassword: '/auth/forgot-password',
    resetPassword: '/auth/reset-password',
  },
  
  // User management
  user: {
    profile: '/me',
    updateProfile: '/me',
    changePassword: '/me/change-password',
  },
  
  // Scans
  scans: {
    list: '/scans',
    create: '/scans',
    get: (id) => `/scans/${id}`,
    update: (id) => `/scans/${id}`,
    delete: (id) => `/scans/${id}`,
    cancel: (id) => `/scans/${id}/cancel`,
  },
  
  // Entities
  entities: {
    emailBreach: '/entities/email/breach',
    domain: (domain) => `/entities/domain/${domain}`,
    ip: (ip) => `/entities/ip/${ip}`,
    urlScan: '/entities/url/scan',
    fileScan: '/entities/file/scan',
  },
  
  // Reports
  reports: {
    list: '/reports',
    create: '/reports',
    get: (id) => `/reports/${id}`,
    export: (id, format) => `/reports/${id}/export?format=${format}`,
  },
  
  // Notifications
  notifications: {
    list: '/notifications',
    markRead: (id) => `/notifications/${id}`,
    preferences: '/notifications/preferences',
  },
  
  // API Keys
  apiKeys: {
    list: '/apikeys',
    create: '/apikeys',
    delete: (id) => `/apikeys/${id}`,
    rotate: (id) => `/apikeys/${id}/rotate`,
  },
  
  // User management (admin)
  users: {
    list: '/users',
    create: '/users',
    get: (id) => `/users/${id}`,
    update: (id) => `/users/${id}`,
    delete: (id) => `/users/${id}`,
    audit: '/audit',
  },
  
  // Settings
  settings: {
    organization: '/settings/organization',
    integrations: '/settings/integrations',
    security: '/settings/security',
  },
};

export default api;
