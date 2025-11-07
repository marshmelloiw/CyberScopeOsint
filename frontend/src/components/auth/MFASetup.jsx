import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Shield, QrCode, Smartphone, CheckCircle, AlertCircle } from 'lucide-react';
import Button from '../ui/Button';
import Input from '../ui/Input';
import { Card } from '../ui/Card';
import useAuthStore from '../../store/auth';
import api from '../../lib/axios';
import QRCode from 'qrcode';

const mfaSetupSchema = z.object({
  totp_token: z.string().min(6, 'TOTP token en az 6 karakter olmalıdır'),
});

const MFASetup = ({ onSetupComplete, onCancel }) => {
  const [step, setStep] = useState('setup'); // 'setup', 'verification', 'success'
  const [qrCodeData, setQrCodeData] = useState(null);
  const [qrCodeImage, setQrCodeImage] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const user = useAuthStore((s) => s.user);

  const {
    register,
    handleSubmit,
    formState: { errors },
    setValue,
  } = useForm({
    resolver: zodResolver(mfaSetupSchema),
  });

  const initiateMFASetup = async () => {
    setIsLoading(true);
    setError(null);

    try {
      const { data } = await api.post('/auth/mfa/setup', { username: user?.email });
      setQrCodeData(data);
      setError(null); // Başarılı olunca hatayı temizle
      // Başarılı yanıtta da QR görselini üret
      if (data?.totpUri) {
        await generateQRCode(data.totpUri);
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
      await api.post('/auth/mfa/verify', {
        username: user?.email,
        totpToken: data.totp_token,
      });

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
        <div className="w-16 h-16 bg-blue-100 dark:bg-primary-500/20 rounded-full flex items-center justify-center">
          <Shield className="w-8 h-8 text-blue-600 dark:text-primary-300" />
        </div>
      </div>

      <div>
        <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
          İki Faktörlü Kimlik Doğrulama
        </h3>
        <p className="text-gray-600 dark:text-surface-muted">
          Hesabınızı daha güvenli hale getirmek için MFA'yı etkinleştirin
        </p>
      </div>

      <div className="space-y-4">
        <div className="flex items-center justify-center space-x-3 text-sm text-gray-600 dark:text-surface-muted">
          <Smartphone className="w-4 h-4 text-gray-600 dark:text-primary-300" />
          <span>Google Authenticator, Authy veya benzeri bir app kullanın</span>
        </div>

        <div className="flex items-center justify-center space-x-3 text-sm text-gray-600 dark:text-surface-muted">
          <QrCode className="w-4 h-4 text-gray-600 dark:text-primary-300" />
          <span>QR kodu tarayarak hesabınızı ekleyin</span>
        </div>
      </div>

      {error && (
        <div className="flex items-center space-x-2 text-red-600 dark:text-danger text-sm bg-red-50 dark:bg-danger/20 p-3 rounded border border-red-200 dark:border-danger/30">
          <AlertCircle className="w-4 h-4" />
          <span>{error}</span>
        </div>
      )}

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
        <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
          QR Kodu Tarayın
        </h3>
        <p className="text-gray-600 dark:text-surface-muted mb-4">
          Authenticator app'inizde QR kodu tarayın veya kodu manuel olarak girin
        </p>
      </div>

      {/* QR Code Display */}
      <div className="flex justify-center">
        <div className="bg-white dark:bg-surface-panel p-4 rounded-lg border border-gray-200 dark:border-surface-border">
          {qrCodeImage ? (
            <img
              src={qrCodeImage}
              alt="MFA QR Code"
              className="w-48 h-48 rounded"
            />
          ) : (
            <div className="w-48 h-48 bg-gray-100 dark:bg-surface-panel/50 rounded flex flex-col items-center justify-center space-y-2">
              <QrCode className="w-24 h-24 text-gray-400 dark:text-primary-300" />
              <div className="text-xs text-gray-500 dark:text-surface-muted text-center">
                QR Kod<br />
                Yükleniyor...
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Manual Secret Entry */}
      <div className="text-center">
        <p className="text-sm text-gray-600 dark:text-surface-muted mb-2">Veya manuel olarak girin:</p>
        <div className="bg-gray-50 dark:bg-surface-panel/50 p-3 rounded font-mono text-sm text-gray-900 dark:text-primary-100 border border-transparent dark:border-surface-border/60">
          {qrCodeData?.totpSecret}
        </div>
      </div>

      {/* TOTP Verification Form */}
      <form onSubmit={handleSubmit(verifyAndEnableMFA)} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-surface-muted mb-2">
            TOTP Token
          </label>
          <Input
            {...register('totp_token')}
            placeholder="6 haneli kodu girin"
            maxLength={6}
            className="text-center text-lg tracking-widest bg-surface-panel dark:bg-surface-panel text-gray-900 dark:text-white"
          />
          {errors.totp_token && (
            <p className="text-red-600 dark:text-danger text-sm mt-1">{errors.totp_token.message}</p>
          )}
        </div>

        {error && (
          <div className="flex items-center space-x-2 text-red-600 dark:text-danger text-sm">
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
        <div className="w-16 h-16 bg-green-100 dark:bg-success/20 rounded-full flex items-center justify-center">
          <CheckCircle className="w-8 h-8 text-green-600 dark:text-success" />
        </div>
      </div>

      <div>
        <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
          MFA Başarıyla Etkinleştirildi!
        </h3>
        <p className="text-gray-600 dark:text-surface-muted">
          Artık hesabınız iki faktörlü kimlik doğrulama ile korunuyor
        </p>
      </div>

      <div className="bg-green-50 dark:bg-success/10 p-4 rounded-lg border border-green-200 dark:border-success/30">
        <p className="text-sm text-green-800 dark:text-success">
          <strong>Önemli:</strong> Backup kodlarınızı güvenli bir yerde saklayın.
          Telefonunuzu kaybederseniz, bu kodlar olmadan hesabınıza erişemeyebilirsiniz.
        </p>
      </div>
    </div>
  );

  return (
    <Card className="max-w-md mx-auto bg-surface-panel/80 dark:bg-surface-panel/80 border border-surface-border/60">
      <div className="p-6 space-y-4 text-gray-900 dark:text-white">
        {step === 'setup' && renderSetupStep()}
        {step === 'verification' && renderVerificationStep()}
        {step === 'success' && renderSuccessStep()}
      </div>
    </Card>
  );
};

export default MFASetup;
