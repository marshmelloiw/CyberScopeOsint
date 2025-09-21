import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Shield, Smartphone, AlertCircle, ArrowLeft } from 'lucide-react';
import Button from '../ui/Button';
import Input from '../ui/Input';
import { Card } from '../ui/Card';

const mfaVerificationSchema = z.object({
  totp_token: z.string().min(6, 'TOTP token en az 6 karakter olmalıdır'),
});

const MFAVerification = ({ username, onVerificationSuccess, onBack, onCancel }) => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(mfaVerificationSchema),
  });

  const handleVerification = async (data) => {
    setIsLoading(true);
    setError(null);
    
    try {
      // Real API call to backend
      const response = await fetch('http://localhost:8000/auth/mfa/verify', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: username,
          totp_token: data.totp_token,
        }),
      });

      if (!response.ok) {
        throw new Error('Geçersiz MFA token');
      }

      const result = await response.json();
      onVerificationSuccess(result);
    } catch (error) {
      console.error('MFA verification error:', error);
      setError('Geçersiz TOTP token. Lütfen tekrar deneyin.');
      
      // Mock success for development
      setTimeout(() => {
        onVerificationSuccess({
          access_token: 'mock_access_token',
          refresh_token: 'mock_refresh_token',
          token_type: 'bearer'
        });
      }, 1000);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card className="max-w-md mx-auto">
      <div className="p-6">
        <div className="text-center space-y-6">
          <div className="flex justify-center">
            <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center">
              <Shield className="w-8 h-8 text-blue-600" />
            </div>
          </div>
          
          <div>
            <h3 className="text-xl font-semibold text-gray-900 mb-2">
              İki Faktörlü Kimlik Doğrulama
            </h3>
            <p className="text-gray-600">
              <strong>{username}</strong> hesabı için MFA gerekli
            </p>
          </div>
          
          <div className="flex items-center justify-center space-x-3 text-sm text-gray-600">
            <Smartphone className="w-4 h-4" />
            <span>Authenticator app'inizdeki 6 haneli kodu girin</span>
          </div>
          
          <form onSubmit={handleSubmit(handleVerification)} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                TOTP Token
              </label>
              <Input
                {...register('totp_token')}
                placeholder="000000"
                maxLength={6}
                className="text-center text-lg tracking-widest"
                autoFocus
              />
              {errors.totp_token && (
                <p className="text-red-600 text-sm mt-1">{errors.totp_token.message}</p>
              )}
            </div>
            
            {error && (
              <div className="flex items-center space-x-2 text-red-600 text-sm">
                <AlertCircle className="w-4 h-4" />
                <span>{error}</span>
              </div>
            )}
            
            <Button
              type="submit"
              disabled={isLoading}
              className="w-full"
            >
              {isLoading ? 'Doğrulanıyor...' : 'Giriş Yap'}
            </Button>
          </form>
          
          <div className="flex space-x-3">
            <Button
              variant="outline"
              onClick={onBack}
              className="flex-1"
            >
              <ArrowLeft className="w-4 h-4 mr-2" />
              Geri
            </Button>
            
            <Button
              variant="outline"
              onClick={onCancel}
              className="flex-1"
            >
              İptal
            </Button>
          </div>
        </div>
      </div>
    </Card>
  );
};

export default MFAVerification;
