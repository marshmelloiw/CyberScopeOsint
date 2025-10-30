import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import useAuthStore from '../../store/auth';
import { ArrowLeft, Mail } from 'lucide-react';

const forgotPasswordSchema = z.object({
  email: z.string().email('Geçerli bir email adresi giriniz'),
});

const ForgotPassword = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [isEmailSent, setIsEmailSent] = useState(false);
  const { forgotPassword, error, clearError } = useAuthStore();

  const {
    register,
    handleSubmit,
    formState: { errors },
    getValues,
  } = useForm({
    resolver: zodResolver(forgotPasswordSchema),
  });

  const onSubmit = async (data) => {
    setIsLoading(true);
    clearError();
    
    try {
      console.log('Forgot password attempt with:', data);
      const result = await forgotPassword(data.email);
      console.log('Forgot password result:', result);
      
      // Show success message
      setIsEmailSent(true);
      
    } catch (err) {
      console.error('Forgot password error:', err);
      // Error is handled by the store
    } finally {
      setIsLoading(false);
    }
  };

  const handleResendEmail = async () => {
    const email = getValues('email');
    if (email) {
      await onSubmit({ email });
    }
  };

  if (isEmailSent) {
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
                  <Mail className="h-8 w-8 text-success" />
                </div>
              </div>
              <CardTitle className="text-2xl text-white">Email Gönderildi!</CardTitle>
              <p className="text-surface-muted">
                Şifre sıfırlama linki <strong>{getValues('email')}</strong> adresine gönderildi.
              </p>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="rounded-lg bg-info/10 border border-info/20 p-4">
                <p className="text-sm text-info">
                  <strong>Önemli:</strong> Email'inizi kontrol edin ve spam klasörünü de kontrol etmeyi unutmayın. 
                  Link 24 saat geçerlidir.
                </p>
              </div>

              <div className="space-y-3">
                <Button
                  onClick={handleResendEmail}
                  variant="outline"
                  className="w-full"
                  disabled={isLoading}
                >
                  {isLoading ? 'Gönderiliyor...' : 'Emaili Tekrar Gönder'}
                </Button>

                <Link to="/auth/login">
                  <Button variant="ghost" className="w-full">
                    <ArrowLeft className="h-4 w-4 mr-2" />
                    Giriş Sayfasına Dön
                  </Button>
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

        {/* Forgot password form */}
        <Card className="glass border-white/20">
          <CardHeader className="text-center">
            <CardTitle className="text-2xl text-white">Şifremi Unuttum</CardTitle>
            <p className="text-surface-muted">
              Email adresinizi girin, size şifre sıfırlama linki gönderelim
            </p>
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
                {isLoading ? 'Gönderiliyor...' : 'Şifre Sıfırlama Linki Gönder'}
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

export default ForgotPassword;
