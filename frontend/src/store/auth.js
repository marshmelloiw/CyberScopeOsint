import { create } from 'zustand';
import { persist } from 'zustand/middleware';

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
          const response = await fetch('http://localhost:8080/api/auth/login', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              username: credentials.email,
              password: credentials.password,
            }),
          });
          
          if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.detail || 'Giriş başarısız');
          }
          
          const data = await response.json();
          console.log('Login response:', data);
          
          // Check if MFA is required
          if (data.token_type === 'mfa_required') {
            return {
              token_type: 'mfa_required',
              message: 'MFA verification required'
            };
          }
          
          // Create user object from response
          const user = {
            id: data.user_id || 1,
            name: credentials.email.split('@')[0],
            email: credentials.email,
            role: 'admin', // Backend'den alınacak
            status: 'active',
            lastActive: new Date().toISOString(),
            avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${credentials.email}`,
            totp_enabled: false,
          };
          
          console.log('Setting auth state:', { user, isAuthenticated: true });
          
          set({
            user: user,
            token: data.access_token,
            refreshToken: data.refresh_token,
            isAuthenticated: true,
            isLoading: false,
            error: null,
          });
          
          // Store tokens in localStorage
          localStorage.setItem('authToken', data.access_token);
          localStorage.setItem('refreshToken', data.refresh_token);
          
          console.log('Auth state updated, returning result');
          return { user, token: data.access_token, refreshToken: data.refresh_token };
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
          const response = await fetch('/api/auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken }),
          });
          
          if (!response.ok) {
            throw new Error('Token refresh failed');
          }
          
          const data = await response.json();
          set({
            token: data.token,
            refreshToken: data.refreshToken,
          });
          
          // Update localStorage
          localStorage.setItem('authToken', data.token);
          localStorage.setItem('refreshToken', data.refreshToken);
          
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
          if (!userData.name || !userData.email || !userData.password) {
            throw new Error('Tüm alanlar gerekli');
          }
          
          // Real API call to backend
          const response = await fetch('http://localhost:8080/api/auth/register', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              name: userData.name,
              email: userData.email,
              password: userData.password,
            }),
          });
          
          if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.detail || 'Kayıt başarısız');
          }
          
          const data = await response.json();
          console.log('Register response:', data);
          
          set({
            isLoading: false,
            error: null,
          });
          
          return { message: 'Kayıt başarılı' };
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
          const response = await fetch('http://localhost:8080/api/auth/forgot-password', {
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
          const response = await fetch('http://localhost:8080/api/auth/reset-password', {
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
