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
  email: z.string().email('Enter a valid email address'),
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
              <img src="/logo.png" alt="CyberScope" className="h-16 w-16" />
            </div>
            <h1 className="text-3xl font-bold text-white mb-2">CyberScope OSINT</h1>
            <p className="text-primary-200">Discover and analyze security threats</p>
          </div>

          {/* Success message */}
          <Card className="glass border-white/20">
            <CardHeader className="text-center">
              <div className="flex justify-center mb-4">
                <div className="rounded-full bg-success/20 p-3">
                  <Mail className="h-8 w-8 text-success" />
                </div>
              </div>
              <CardTitle className="text-2xl text-white">Email Sent!</CardTitle>
              <p className="text-surface-muted">
                Password reset link has been sent to <strong>{getValues('email')}</strong> address.
              </p>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="rounded-lg bg-info/10 border border-info/20 p-4">
                <p className="text-sm text-info">
                  <strong>Important:</strong> Check your email and don't forget to check your spam folder.
                  The link is valid for 24 hours.
                </p>
              </div>

              <div className="space-y-3">
                <Button
                  onClick={handleResendEmail}
                  variant="outline"
                  className="w-full"
                  disabled={isLoading}
                >
                  {isLoading ? 'Sending...' : 'Resend Email'}
                </Button>

                <Link to="/auth/login">
                  <Button variant="ghost" className="w-full">
                    <ArrowLeft className="h-4 w-4 mr-2" />
                    Back to Login Page
                  </Button>
                </Link>
              </div>
            </CardContent>
          </Card>

          {/* Footer */}
          <div className="mt-8 text-center">
            <p className="text-xs text-primary-300">
              © 2025 CyberScope OSINT. All rights reserved.
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
            <img src="/logo.png" alt="CyberScope" className="h-16 w-16" />
          </div>
          <h1 className="text-3xl font-bold text-white mb-2">CyberScope OSINT</h1>
          <p className="text-primary-200">Discover and analyze security threats</p>
        </div>

        {/* Forgot password form */}
        <Card className="glass border-white/20">
          <CardHeader className="text-center">
            <CardTitle className="text-2xl text-white">Forgot Password</CardTitle>
            <p className="text-surface-muted">
              Enter your email address and we'll send you a password reset link
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
                {isLoading ? 'Sending...' : 'Send Password Reset Link'}
              </Button>
            </form>

            {/* Back to login */}
            <div className="mt-6 text-center">
              <Link
                to="/auth/login"
                className="inline-flex items-center text-sm text-primary-400 hover:text-primary-300 transition-colors"
              >
                <ArrowLeft className="h-4 w-4 mr-1" />
                Back to login page
              </Link>
            </div>
          </CardContent>
        </Card>

        {/* Footer */}
        <div className="mt-8 text-center">
          <p className="text-xs text-primary-300">
            © 2025 CyberScope OSINT. All rights reserved.
          </p>
        </div>
      </div>
    </div>
  );
};

export default ForgotPassword;
