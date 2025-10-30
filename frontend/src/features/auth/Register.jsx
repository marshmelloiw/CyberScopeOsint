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

const registerSchema = z.object({
  name: z.string().min(2, 'Ad en az 2 karakter olmalıdır'),
  email: z.string().email('Geçerli bir email adresi giriniz'),
  password: z.string().min(6, 'Şifre en az 6 karakter olmalıdır'),
  confirmPassword: z.string(),
  agreeToTerms: z.boolean().refine(val => val === true, 'Kullanım şartlarını kabul etmelisiniz'),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Şifreler eşleşmiyor",
  path: ["confirmPassword"],
});

const Register = () => {
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const { register: registerUser, error, clearError } = useAuthStore();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (data) => {
    setIsLoading(true);
    clearError();
    
    try {
      console.log('Register attempt with:', data);
      const result = await registerUser({
        name: data.name,
        email: data.email,
        password: data.password,
      });
      console.log('Register result:', result);
      
      // Show success message and redirect to login
      alert('Kayıt başarılı! Giriş yapabilirsiniz.');
      navigate('/auth/login');
      
    } catch (err) {
      console.error('Register error:', err);
      // Error is handled by the store
    } finally {
      setIsLoading(false);
    }
  };

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

        {/* Register form */}
        <Card className="glass border-white/20">
          <CardHeader className="text-center">
            <CardTitle className="text-2xl text-white">Kayıt Ol</CardTitle>
            <p className="text-surface-muted">Hesabınızı oluşturun ve güvenlik analizlerine başlayın</p>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              {/* Name field */}
              <Input
                label="Ad Soyad"
                type="text"
                placeholder="Adınız ve soyadınız"
                error={errors.name?.message}
                {...register('name')}
              />

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

              {/* Confirm Password field */}
              <div className="space-y-2">
                <label className="text-sm font-medium text-white">Şifre Tekrar</label>
                <div className="relative">
                  <Input
                    type={showConfirmPassword ? 'text' : 'password'}
                    placeholder="••••••••"
                    error={errors.confirmPassword?.message}
                    {...register('confirmPassword')}
                    className="pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-surface-muted hover:text-white transition-colors"
                  >
                    {showConfirmPassword ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                </div>
                {errors.confirmPassword && (
                  <p className="text-sm text-danger">{errors.confirmPassword.message}</p>
                )}
              </div>

              {/* Terms agreement */}
              <div className="space-y-2">
                <label className="flex items-start space-x-2">
                  <input
                    type="checkbox"
                    className="mt-1 rounded border-surface-border bg-surface-panel text-primary-600 focus:ring-primary-500"
                    {...register('agreeToTerms')}
                  />
                  <span className="text-sm text-white">
                    <Link
                      to="/terms"
                      className="text-primary-400 hover:text-primary-300 transition-colors"
                    >
                      Kullanım şartlarını
                    </Link>
                    {' '}ve{' '}
                    <Link
                      to="/privacy"
                      className="text-primary-400 hover:text-primary-300 transition-colors"
                    >
                      gizlilik politikasını
                    </Link>
                    {' '}kabul ediyorum
                  </span>
                </label>
                {errors.agreeToTerms && (
                  <p className="text-sm text-danger">{errors.agreeToTerms.message}</p>
                )}
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
                {isLoading ? 'Kayıt oluşturuluyor...' : 'Kayıt Ol'}
              </Button>
            </form>

            {/* Login link */}
            <div className="mt-6 text-center">
              <p className="text-sm text-surface-muted">
                Zaten hesabınız var mı?{' '}
                <Link
                  to="/auth/login"
                  className="text-primary-400 hover:text-primary-300 transition-colors font-medium"
                >
                  Giriş yapın
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

export default Register;
