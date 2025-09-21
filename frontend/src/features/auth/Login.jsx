import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import useAuthStore from '../../store/auth';
import { Eye, EyeOff } from 'lucide-react';
import MFAVerification from '../../components/auth/MFAVerification';

const loginSchema = z.object({
  email: z.string().email('Geçerli bir email adresi giriniz'),
  password: z.string().min(6, 'Şifre en az 6 karakter olmalıdır'),
  rememberMe: z.boolean().optional(),
});

const Login = () => {
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [showMFA, setShowMFA] = useState(false);
  const [mfaUsername, setMfaUsername] = useState('');
  const navigate = useNavigate();
  const { login, error, clearError } = useAuthStore();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data) => {
    setIsLoading(true);
    clearError();
    
    try {
      console.log('Login attempt with:', data);
      const result = await login(data);
      console.log('Login result:', result);
      
      // Check if MFA is required
      if (result && result.token_type === 'mfa_required') {
        setMfaUsername(data.email);
        setShowMFA(true);
        return;
      }
      
      console.log('Navigating to /');
      navigate('/');
    } catch (err) {
      console.error('Login error:', err);
      // Error is handled by the store
    } finally {
      setIsLoading(false);
    }
  };

  const handleMFASuccess = (mfaResult) => {
    console.log('MFA verification successful:', mfaResult);
    navigate('/');
  };

  const handleMFABack = () => {
    setShowMFA(false);
    setMfaUsername('');
  };

  const handleMFACancel = () => {
    setShowMFA(false);
    setMfaUsername('');
    clearError();
  };

  // Show MFA verification if required
  if (showMFA) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-950 via-primary-900 to-primary-800 p-4">
        <div className="w-full max-w-md">
          {/* Logo and title */}
          <div className="text-center mb-8">
            <div className="flex justify-center mb-4">
              <img src="/src/assets/logo.svg" alt="CyberScope" className="h-16 w-16" />
            </div>
            <h1 className="text-3xl font-bold text-white mb-2">CyberScope OSINT</h1>
            <p className="text-primary-200">Güvenlik tehditlerini keşfedin ve analiz edin</p>
          </div>

          <MFAVerification
            username={mfaUsername}
            onVerificationSuccess={handleMFASuccess}
            onBack={handleMFABack}
            onCancel={handleMFACancel}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-950 via-primary-900 to-primary-800 p-4">
      <div className="w-full max-w-md">
        {/* Logo and title */}
        <div className="text-center mb-8">
          <div className="flex justify-center mb-4">
            <img src="/src/assets/logo.svg" alt="Card" className="h-16 w-16" />
          </div>
          <h1 className="text-3xl font-bold text-white mb-2">CyberScope OSINT</h1>
          <p className="text-primary-200">Güvenlik tehditlerini keşfedin ve analiz edin</p>
        </div>

        {/* Login form */}
        <Card className="glass border-white/20">
          <CardHeader className="text-center">
            <CardTitle className="text-2xl text-white">Giriş Yap</CardTitle>
            <p className="text-surface-muted">Hesabınıza erişmek için giriş yapın</p>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              {/* Email field */}
              <Input
                label="Email"
                type="email"
                placeholder="ornek@email.com"
                error={errors.email?.message}
                {...register('email')}
              />

              {/* Password field */}
              <div className="space-y-2">
                <label className="text-sm font-medium text-white">Şifre</label>
                <div className="relative">
                  <Input
                    type={showPassword ? 'text' : 'password'}
                    placeholder="••••••••"
                    error={errors.password?.message}
                    {...register('password')}
                    className="pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-surface-muted hover:text-white transition-colors"
                  >
                    {showPassword ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                </div>
                {errors.password && (
                  <p className="text-sm text-danger">{errors.password.message}</p>
                )}
              </div>

              {/* Remember me and forgot password */}
              <div className="flex items-center justify-between">
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    className="rounded border-surface-border bg-surface-panel text-primary-600 focus:ring-primary-500"
                    {...register('rememberMe')}
                  />
                  <span className="text-sm text-white">Beni hatırla</span>
                </label>
                <Link
                  to="/auth/forgot-password"
                  className="text-sm text-primary-400 hover:text-primary-300 transition-colors"
                >
                  Şifremi unuttum
                </Link>
              </div>

              {/* Error message */}
              {error && (
                <div className="rounded-lg bg-danger/10 border border-danger/20 p-3">
                  <p className="text-sm text-danger">{error}</p>
                </div>
              )}

              {/* Submit button */}
              <Button
                type="submit"
                className="w-full"
                loading={isLoading}
                disabled={isLoading}
              >
                {isLoading ? 'Giriş yapılıyor...' : 'Giriş Yap'}
              </Button>
            </form>

            {/* Divider */}
            <div className="relative my-6">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-surface-border" />
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="bg-surface-panel px-2 text-surface-muted">veya</span>
              </div>
            </div>

            {/* MFA button */}
            <Button
              variant="outline"
              className="w-full"
              onClick={() => navigate('/auth/mfa')}
            >
              İki Faktörlü Doğrulama
            </Button>

            {/* Register link */}
            <div className="mt-6 text-center">
              <p className="text-sm text-surface-muted">
                Hesabınız yok mu?{' '}
                <Link
                  to="/auth/register"
                  className="text-primary-400 hover:text-primary-300 transition-colors font-medium"
                >
                  Kayıt olun
                </Link>
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Footer */}
        <div className="mt-8 text-center">
          <p className="text-xs text-primary-300">
            © 2024 CyberScope OSINT. Tüm hakları saklıdır.
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;
