import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Shield, QrCode, Smartphone, CheckCircle, AlertCircle } from 'lucide-react';
import Button from '../ui/Button';
import Input from '../ui/Input';
import { Card } from '../ui/Card';
import useAuthStore from '../../store/auth';
import QRCode from 'qrcode';

const mfaSetupSchema = z.object({
  totp_token: z.string().min(6, 'TOTP token en az 6 karakter olmalıdır'),
});

const MFASetup = ({ onSetupComplete, onCancel }) => {
  const [step, setStep] = useState('verification'); // 'verification', 'success'
  const [qrCodeData, setQrCodeData] = useState(null);
  const [qrCodeImage, setQrCodeImage] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const user = useAuthStore((s) => s.user);
  const token = useAuthStore((s) => s.token);

  const {
    register,
    handleSubmit,
    formState: { errors },
    setValue,
  } = useForm({
    resolver: zodResolver(mfaSetupSchema),
  });

  // Sayfa açılır açılmaz otomatik olarak QR/secret üretmek için
  useEffect(() => {
    initiateMFASetup();
  }, []);

  const initiateMFASetup = async () => {
    setIsLoading(true);
    setError(null);

    try {
      // Real API call to backend
      const response = await fetch('http://localhost:8080/api/auth/mfa/setup', {
        method: 'POST',
        headers: {
          ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username: user?.email }),
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || 'MFA setup başlatılamadı');
      }

      const data = await response.json();
      setQrCodeData(data);
      // Başarılı yanıtta da QR görselini üret
      if (data?.totp_uri) {
        await generateQRCode(data.totp_uri);
      }
      setStep('verification');
    } catch (error) {
      console.error('MFA setup error:', error);
      setError('MFA setup başlatılamadı. Lütfen tekrar deneyin.');
    } finally {
      setIsLoading(false);
    }
  };

  const generateQRCode = async (uri) => {
    try {
      const qrDataURL = await QRCode.toDataURL(uri, {
        width: 200,
        margin: 2,
        color: {
          dark: '#000000',
          light: '#FFFFFF'
        }
      });
      setQrCodeImage(qrDataURL);
    } catch (error) {
      console.error('QR code generation failed:', error);
    }
  };

  const verifyAndEnableMFA = async (data) => {
    setIsLoading(true);
    setError(null);

    try {
      // Real API call to backend
      const response = await fetch('http://localhost:8080/api/auth/mfa/verify', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: user?.email,
          totp_token: data.totp_token,
        }),
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || 'MFA verification başarısız');
      }

      setStep('success');
      setTimeout(() => {
        onSetupComplete();
      }, 2000);
    } catch (error) {
      console.error('MFA verification error:', error);
      setError('Geçersiz TOTP token veya secret eşleşmedi. Lütfen QR’ı yeniden tarayın ve 30 sn içinde deneyin.');
    } finally {
      setIsLoading(false);
    }
  };

  const renderSetupStep = () => (
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
          Hesabınızı daha güvenli hale getirmek için MFA'yı etkinleştirin
        </p>
      </div>

      <div className="space-y-4">
        <div className="flex items-center justify-center space-x-3 text-sm text-gray-600">
          <Smartphone className="w-4 h-4" />
          <span>Google Authenticator, Authy veya benzeri bir app kullanın</span>
        </div>

        <div className="flex items-center justify-center space-x-3 text-sm text-gray-600">
          <QrCode className="w-4 h-4" />
          <span>QR kodu tarayarak hesabınızı ekleyin</span>
        </div>
      </div>

      <Button
        onClick={initiateMFASetup}
        disabled={isLoading}
        className="w-full"
      >
        {isLoading ? 'Hazırlanıyor...' : 'MFA Kurulumunu Başlat'}
      </Button>

      <Button
        variant="outline"
        onClick={onCancel}
        className="w-full"
      >
        İptal
      </Button>
    </div>
  );

  const renderVerificationStep = () => (
    <div className="space-y-6">
      <div className="text-center">
        <h3 className="text-xl font-semibold text-gray-900 mb-2">
          QR Kodu Tarayın
        </h3>
        <p className="text-gray-600 mb-4">
          Authenticator app'inizde QR kodu tarayın veya kodu manuel olarak girin
        </p>
      </div>

      {/* QR Code Display */}
      <div className="flex justify-center">
        <div className="bg-white p-4 rounded-lg border">
          {qrCodeImage ? (
            <img
              src={qrCodeImage}
              alt="MFA QR Code"
              className="w-48 h-48 rounded"
            />
          ) : (
            <div className="w-48 h-48 bg-gray-100 rounded flex items-center justify-center">
              <QrCode className="w-24 h-24 text-gray-400" />
              <div className="text-xs text-gray-500 text-center">
                QR Kod<br />
                Yükleniyor...
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Manual Secret Entry */}
      <div className="text-center">
        <p className="text-sm text-gray-600 mb-2">Veya manuel olarak girin:</p>
        <div className="bg-gray-50 p-3 rounded font-mono text-sm">
          {qrCodeData?.totp_secret}
        </div>
      </div>

      {/* TOTP Verification Form */}
      <form onSubmit={handleSubmit(verifyAndEnableMFA)} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            TOTP Token
          </label>
          <Input
            {...register('totp_token')}
            placeholder="6 haneli kodu girin"
            maxLength={6}
            className="text-center text-lg tracking-widest"
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
          {isLoading ? 'Doğrulanıyor...' : 'MFA\'yı Etkinleştir'}
        </Button>
      </form>

      <Button
        variant="outline"
        onClick={() => setStep('setup')}
        className="w-full"
      >
        Geri
      </Button>
    </div>
  );

  const renderSuccessStep = () => (
    <div className="text-center space-y-6">
      <div className="flex justify-center">
        <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center">
          <CheckCircle className="w-8 h-8 text-green-600" />
        </div>
      </div>

      <div>
        <h3 className="text-xl font-semibold text-gray-900 mb-2">
          MFA Başarıyla Etkinleştirildi!
        </h3>
        <p className="text-gray-600">
          Artık hesabınız iki faktörlü kimlik doğrulama ile korunuyor
        </p>
      </div>

      <div className="bg-green-50 p-4 rounded-lg">
        <p className="text-sm text-green-800">
          <strong>Önemli:</strong> Backup kodlarınızı güvenli bir yerde saklayın.
          Telefonunuzu kaybederseniz, bu kodlar olmadan hesabınıza erişemeyebilirsiniz.
        </p>
      </div>
    </div>
  );

  return (
    <Card className="max-w-md mx-auto">
      <div className="p-6">
        {step === 'setup' && renderSetupStep()}
        {step === 'verification' && renderVerificationStep()}
        {step === 'success' && renderSuccessStep()}
      </div>
    </Card>
  );
};

export default MFASetup;
