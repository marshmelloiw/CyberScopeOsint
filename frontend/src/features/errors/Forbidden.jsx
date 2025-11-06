import React from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../../components/ui/Button';

const Forbidden = () => {
  const navigate = useNavigate();

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-surface-bg px-4">
      <div className="max-w-md text-center space-y-6">
        <div>
          <p className="text-6xl font-bold text-danger">403</p>
          <h1 className="mt-4 text-3xl font-semibold text-white">Erişim Engellendi</h1>
          <p className="mt-2 text-surface-muted">
            Bu sayfayı görüntülemek için gerekli yetkilere sahip değilsiniz. Eğer bir hata olduğunu
            düşünüyorsanız, lütfen sistem yöneticinizle iletişime geçin.
          </p>
        </div>

        <div className="flex flex-col gap-3 sm:flex-row sm:justify-center">
          <Button onClick={() => navigate(-1)} variant="outline">
            Geri Dön
          </Button>
          <Button onClick={() => navigate('/dashboard')}>
            Dashboard'a Git
          </Button>
        </div>
      </div>
    </div>
  );
};

export default Forbidden;

