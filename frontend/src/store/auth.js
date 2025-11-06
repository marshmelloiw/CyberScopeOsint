import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import api, { endpoints } from '../lib/axios';

const buildUserFromPayload = (payload = {}) => {
  const email = payload.email || '';
  const fullName = payload.fullName || '';
  return {
    id: payload.userId ?? null,
    name: fullName || email.split('@')[0] || email,
    email,
    role: payload.role || 'viewer',
    status: payload.verified ? 'active' : 'inactive',
    verified: !!payload.verified,
    mfaEnabled: !!payload.mfaEnabled,
    totp_enabled: !!payload.mfaEnabled,
    lastActive: new Date().toISOString(),
    avatar: email
      ? `https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(email)}`
      : undefined,
  };
};

const useAuthStore = create(
  persist(
    (set, get) => ({
      // State
      user: null,
      token: null,
      refreshToken: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,

      // Actions
      setUser: (user) => set({ user, isAuthenticated: !!user }),

      setToken: (token) => set({ token }),

      setRefreshToken: (refreshToken) => set({ refreshToken }),

      setLoading: (isLoading) => set({ isLoading }),

      setError: (error) => set({ error }),

      completeLogin: (payload) => {
        if (!payload || !payload.token) {
          throw new Error('Geçersiz oturum yanıtı');
        }

        const user = buildUserFromPayload(payload);
        const refreshToken = payload.refreshToken || null;

        set({
          user,
          token: payload.token,
          refreshToken,
          isAuthenticated: true,
          isLoading: false,
          error: null,
        });

        localStorage.setItem('authToken', payload.token);
        if (refreshToken) {
          localStorage.setItem('refreshToken', refreshToken);
        } else {
          localStorage.removeItem('refreshToken');
        }
      },

      login: async (credentials) => {
        console.log('Auth store login called with:', credentials);
        set({ isLoading: true, error: null });
        try {
          // Mock login - accept any email/password for demo
          // In production, this would validate against the backend
          if (!credentials.email || !credentials.password) {
            throw new Error('Email ve şifre gerekli');
          }

          // Real API call to backend
          const base = import.meta?.env?.VITE_API_BASE_URL || 'http://localhost:8080';
          const normalizedEmail = credentials.email?.trim().toLowerCase();
          const normalizedPassword = credentials.password?.trim();

          if (!normalizedEmail || !normalizedPassword) {
            throw new Error('Email ve şifre gerekli');
          }

          const response = await fetch(`${base}/api/auth/login`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              email: normalizedEmail,
              password: normalizedPassword,
            }),
          });

          let rawText = '';
          let data = null;

          try {
            rawText = await response.text();
          } catch (e) {
            console.warn('Yanıt metni okunamadı:', e);
          }

          if (rawText) {
            try {
              data = JSON.parse(rawText);
            } catch (e) {
              console.warn('Boş veya geçersiz JSON döndü:', e);
            }
          }

          if (!response.ok) {
            let message = `Giriş başarısız (HTTP ${response.status})`;

            if (data && typeof data === 'object') {
              message = data.error || data.message || data.detail || message;
            } else if (rawText) {
              message = rawText;
            }

            if (message.toLowerCase().includes('aktif değil')) {
              throw new Error('Hesabınız henüz aktif değil. Lütfen yöneticinizin onayını bekleyin.');
            }

            throw new Error(message);
          }

          if (!data || typeof data !== 'object') {
            throw new Error('Sunucudan geçerli bir yanıt alınamadı.');
          }


          if (data?.mfaRequired || data?.tokenType === 'mfa_required') {
            set({ isLoading: false });
            return {
              mfaRequired: true,
              email: data?.email || credentials.email,
            };
          }

          get().completeLogin(data);
          return data;
        } catch (error) {
          console.error('Login error in store:', error);
          set({
            isLoading: false,
            error: error.message,
          });
          throw error;
        }
      },

      logout: () => {
        set({
          user: null,
          token: null,
          refreshToken: null,
          isAuthenticated: false,
          isLoading: false,
          error: null,
        });

        // Clear localStorage
        localStorage.removeItem('authToken');
        localStorage.removeItem('refreshToken');
      },

      refreshAuth: async () => {
        const { refreshToken } = get();
        if (!refreshToken) return false;

        try {
          const { data } = await api.post(endpoints.auth.refresh, { refreshToken });
          if (!data || !data.token) {
            throw new Error('Token refresh failed');
          }

          set({
            token: data.token,
            refreshToken: data.refreshToken || refreshToken,
          });

          localStorage.setItem('authToken', data.token);
          if (data.refreshToken) {
            localStorage.setItem('refreshToken', data.refreshToken);
          }

          return true;
        } catch (error) {
          get().logout();
          return false;
        }
      },

      updateProfile: (updates) => {
        const { user } = get();
        if (user) {
          set({ user: { ...user, ...updates } });
        }
      },

      clearError: () => set({ error: null }),

      register: async (userData) => {
        console.log('Auth store register called with:', userData);
        set({ isLoading: true, error: null });
        try {
          if (!userData.name || !userData.email || !userData.password || !userData.file) {
            throw new Error('Tüm alanlar gerekli (Ad, Email, Şifre, Dosya)');
          }

          if (userData.password.trim().length < 8) {
            throw new Error('Şifre en az 8 karakter olmalıdır');
          }

          // Create FormData for file upload
          const formData = new FormData();
          formData.append('name', userData.name);
          formData.append('email', userData.email);
          formData.append('password', userData.password.trim());
          formData.append('file', userData.file);

          // Real API call to backend with multipart/form-data
          const base = import.meta?.env?.VITE_API_BASE_URL || 'http://localhost:8080';
          const response = await fetch(`${base}/api/auth/register`, {
            method: 'POST',
            // Don't set Content-Type header, browser will set it with boundary for FormData
            body: formData,
          });

          if (!response.ok) {
            // Boş hata response'una karşı koruma
            let message = 'Kayıt başarısız';
            try {
              const text = await response.text();
              if (text) {
                const errorData = JSON.parse(text);
                message = errorData.detail || errorData.message || errorData.error || message;
              }
            } catch (_) {}
            throw new Error(message);
          }
          
          // ✅ Backend 201 Created döndürüyor, body'de token olabilir
          let data = null;
          try {
            const text = await response.text();
            if (text) {
              data = JSON.parse(text);
            }
          } catch (_) {
            // boş body, sorun değil
          }
          
          console.log('Register response:', data);
          
          set({
            isLoading: false,
            error: null,
          });
          
          return { message: data?.message || 'Kayıt başarılı. Hesabınızın aktifleştirilmesi için yönetici onayı gerekmektedir.' };
        } catch (error) {
          console.error('Register error in store:', error);
          set({
            isLoading: false,
            error: error.message,
          });
          throw error;
        }
      },

      forgotPassword: async (email) => {
        console.log('Auth store forgotPassword called with:', email);
        set({ isLoading: true, error: null });
        try {
          if (!email) {
            throw new Error('Email adresi gerekli');
          }

          // Real API call to backend
          const base = import.meta?.env?.VITE_API_BASE_URL || 'http://localhost:8080';
          const response = await fetch(`${base}/api/auth/forgot-password`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              email: email,
            }),
          });

          if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.detail || 'Şifre sıfırlama isteği başarısız');
          }

          const data = await response.json();
          console.log('Forgot password response:', data);

          set({
            isLoading: false,
            error: null,
          });

          return { message: 'Şifre sıfırlama linki gönderildi' };
        } catch (error) {
          console.error('Forgot password error in store:', error);
          set({
            isLoading: false,
            error: error.message,
          });
          throw error;
        }
      },

      resetPassword: async (token, email, newPassword) => {
        console.log('Auth store resetPassword called');
        set({ isLoading: true, error: null });
        try {
          if (!token || !email || !newPassword) {
            throw new Error('Tüm alanlar gerekli');
          }

          // Real API call to backend
          const base = import.meta?.env?.VITE_API_BASE_URL || 'http://localhost:8080';
          const response = await fetch(`${base}/api/auth/reset-password`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              token: token,
              email: email,
              new_password: newPassword,
            }),
          });

          if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.detail || 'Şifre sıfırlama başarısız');
          }

          const data = await response.json();
          console.log('Reset password response:', data);

          set({
            isLoading: false,
            error: null,
          });

          return { message: 'Şifre başarıyla sıfırlandı' };
        } catch (error) {
          console.error('Reset password error in store:', error);
          set({
            isLoading: false,
            error: error.message,
          });
          throw error;
        }
      },
    }),
    {
      name: 'cyberscope-auth',
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);

export default useAuthStore;
