import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import useAuthStore from '../../store/auth';
import { Eye, EyeOff, CheckCircle, ArrowLeft } from 'lucide-react';

const resetPasswordSchema = z.object({
  password: z.string().min(6, 'Şifre en az 6 karakter olmalıdır'),
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Şifreler eşleşmiyor",
  path: ["confirmPassword"],
});

const ResetPassword = () => {
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { resetPassword, error, clearError } = useAuthStore();

  const token = searchParams.get('token');
  const email = searchParams.get('email');

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(resetPasswordSchema),
  });

  useEffect(() => {
    // Check if token and email are present
    if (!token || !email) {
      navigate('/auth/forgot-password');
    }
  }, [token, email, navigate]);

  const onSubmit = async (data) => {
    setIsLoading(true);
    clearError();
    
    try {
      console.log('Reset password attempt with:', data);
      const result = await resetPassword(token, email, data.password);
      console.log('Reset password result:', result);
      
      // Show success message
      setIsSuccess(true);
      
    } catch (err) {
      console.error('Reset password error:', err);
      // Error is handled by the store
    } finally {
      setIsLoading(false);
    }
  };

  if (isSuccess) {
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

          {/* Success message */}
          <Card className="glass border-white/20">
            <CardHeader className="text-center">
              <div className="flex justify-center mb-4">
                <div className="rounded-full bg-success/20 p-3">
                  <CheckCircle className="h-8 w-8 text-success" />
                </div>
              </div>
              <CardTitle className="text-2xl text-white">Şifre Başarıyla Sıfırlandı!</CardTitle>
              <p className="text-surface-muted">
                Şifreniz başarıyla güncellendi. Artık yeni şifrenizle giriş yapabilirsiniz.
              </p>
            </CardHeader>
            <CardContent>
              <Link to="/auth/login">
                <Button className="w-full">
                  Giriş Sayfasına Git
                </Button>
              </Link>
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
  }

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

        {/* Reset password form */}
        <Card className="glass border-white/20">
          <CardHeader className="text-center">
            <CardTitle className="text-2xl text-white">Yeni Şifre Belirle</CardTitle>
            <p className="text-surface-muted">
              {email} için yeni şifrenizi belirleyin
            </p>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              {/* Password field */}
              <div className="space-y-2">
                <label className="text-sm font-medium text-white">Yeni Şifre</label>
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

              {/* Password requirements */}
              <div className="rounded-lg bg-info/10 border border-info/20 p-3">
                <p className="text-sm text-info font-medium mb-2">Şifre Gereksinimleri:</p>
                <ul className="text-xs text-info space-y-1">
                  <li>• En az 6 karakter uzunluğunda olmalı</li>
                  <li>• Güçlü bir şifre seçin</li>
                  <li>• Şifrelerin eşleştiğinden emin olun</li>
                </ul>
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
                {isLoading ? 'Şifre Sıfırlanıyor...' : 'Şifreyi Sıfırla'}
              </Button>
            </form>

            {/* Back to login */}
            <div className="mt-6 text-center">
              <Link
                to="/auth/login"
                className="inline-flex items-center text-sm text-primary-400 hover:text-primary-300 transition-colors"
              >
                <ArrowLeft className="h-4 w-4 mr-1" />
                Giriş sayfasına dön
              </Link>
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

export default ResetPassword;
