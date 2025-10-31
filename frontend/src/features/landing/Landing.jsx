import React from 'react';
import { Link } from 'react-router-dom';

function Landing() {
  return (
    <div
      className="min-h-screen text-white"
      style={{
        backgroundImage: 'url(/landing-bg.png)',
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat'
      }}
    >
      <header className="max-w-6xl mx-auto px-6 py-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <img
            src="/logo.png"
            alt="CyberScope OSINT"
            className="h-10 w-10 object-contain"
            loading="eager"
          />
          <span className="text-xl font-semibold tracking-wide">CyberScope OSINT</span>
        </div>
        <div className="flex items-center gap-3">
          <Link to="/auth/register" className="px-4 py-2 rounded-md bg-transparent border border-[#7b5cff] text-[#c9b9ff] hover:bg-[#1a1433]">Kayıt Ol</Link>
          <Link to="/auth/login" className="px-4 py-2 rounded-md bg-[#7b5cff] text-black font-medium hover:opacity-90">Giriş Yap</Link>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-6 pt-12 pb-24 grid grid-cols-1 md:grid-cols-2 gap-10 items-center">
        <div>
          <h1 className="text-4xl md:text-5xl font-bold leading-tight">
            Açık Kaynak İstihbarat ile<br />
            Tehditleri Erken Tespit Edin
          </h1>
          <p className="mt-6 text-base md:text-lg text-[#b5b7c0] leading-relaxed">
            CyberScope OSINT; Shodan, VirusTotal ve Have I Been Pwned gibi kaynaklardan verileri toplayıp
            birleştirir. Tehdit görünürlüğü, uyarılar ve raporlama ile güvenlik ekiplerinin karar alma
            hızını artırır. Gerçek zamanlı analiz ve gelişmiş risk skoru ile olaylara daha hızlı müdahale edin.
          </p>

          <div className="mt-8 flex items-center gap-4">
            <Link to="/auth/register" className="px-5 py-3 rounded-md bg-[#7b5cff] text-black font-medium hover:opacity-90">Hemen Başla</Link>
            <Link to="/auth/login" className="px-5 py-3 rounded-md border border-[#2b2d3a] text-[#e2e4ea] hover:bg-[#141428]">Demo Girişi</Link>
          </div>
        </div>

        <div className="relative">
          <div className="absolute -inset-10 blur-3xl opacity-30" style={{background: 'radial-gradient(60% 60% at 50% 40%, #7b5cff 0%, rgba(123,92,255,0) 60%)'}} />
          <div className="relative bg-[#121225]/70 rounded-2xl border border-[#2b2d3a] p-8">
            <div className="flex items-center justify-center">
              <img
                src="/logo.png"
                alt="Logo"
                className="h-40 w-40 object-contain"
                loading="eager"
              />
            </div>
            <div className="mt-6 grid grid-cols-2 gap-4 text-sm">
              <div className="p-4 rounded-lg bg-[#0f0f21] border border-[#2b2d3a]">
                <div className="text-[#c9b9ff] font-medium">Shodan Entegrasyonu</div>
                <div className="text-[#9aa0ae] mt-1">IP ve hizmet keşfi</div>
              </div>
              <div className="p-4 rounded-lg bg-[#0f0f21] border border-[#2b2d3a]">
                <div className="text-[#c9b9ff] font-medium">VirusTotal</div>
                <div className="text-[#9aa0ae] mt-1">Alan/IP analizleri</div>
              </div>
              <div className="p-4 rounded-lg bg-[#0f0f21] border border-[#2b2d3a]">
                <div className="text-[#c9b9ff] font-medium">HIBP</div>
                <div className="text-[#9aa0ae] mt-1">E-posta ihlalleri</div>
              </div>
              <div className="p-4 rounded-lg bg-[#0f0f21] border border-[#2b2d3a]">
                <div className="text-[#c9b9ff] font-medium">Gerçek Zamanlı Uyarı</div>
                <div className="text-[#9aa0ae] mt-1">Bildirim ve raporlar</div>
              </div>
            </div>
          </div>
        </div>
      </main>

      <footer className="max-w-6xl mx-auto px-6 pb-10 text-[#6d7280] text-sm">
        © {new Date().getFullYear()} CyberScope OSINT
      </footer>
    </div>
  );
}

export default Landing;
