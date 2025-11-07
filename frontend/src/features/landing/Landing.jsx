import React from 'react';
import { useNavigate } from 'react-router-dom';

function Landing() {
  const navigate = useNavigate();

  const handleLoginClick = (e) => {
    e.preventDefault();
    navigate('/auth/login');
  };

  const handleRegisterClick = (e) => {
    e.preventDefault();
    navigate('/auth/register');
  };

  return (
    <div
      className="min-h-screen text-white landing-page"
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
          <span className="text-xl font-semibold tracking-wide text-white">CyberScope OSINT</span>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={handleRegisterClick}
            className="px-4 py-2 rounded-md bg-transparent border border-[#7b5cff] text-white hover:bg-[#1a1433] transition-colors"
          >
            Sign Up
          </button>
          <button
            onClick={handleLoginClick}
            className="px-4 py-2 rounded-md bg-[#7b5cff] text-white font-medium hover:opacity-90 transition-opacity"
          >
            Sign In
          </button>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-6 pt-12 pb-24 grid grid-cols-1 md:grid-cols-2 gap-10 items-center">
        <div>
          <h1 className="text-4xl md:text-5xl font-bold leading-tight text-white">
            Detect Threats Early<br />
          </h1>
          <p className="mt-6 text-base md:text-lg text-white leading-relaxed">
            CyberScope OSINT consolidates data from Shodan, VirusTotal and Have I Been Pwned to deliver
            unified visibility, alerts and reporting. Real-time analysis and advanced risk scoring help
            security teams make faster decisions and respond to incidents earlier.
          </p>
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
                <div className="text-[#7b5cff] font-medium">Shodan Integration</div>
                <div className="text-white mt-1">IP and service discovery</div>
              </div>
              <div className="p-4 rounded-lg bg-[#0f0f21] border border-[#2b2d3a]">
                <div className="text-[#7b5cff] font-medium">VirusTotal</div>
                <div className="text-white mt-1">Domain/IP analysis</div>
              </div>
              <div className="p-4 rounded-lg bg-[#0f0f21] border border-[#2b2d3a]">
                <div className="text-[#7b5cff] font-medium">HIBP</div>
                <div className="text-white mt-1">Email breach checks</div>
              </div>
              <div className="p-4 rounded-lg bg-[#0f0f21] border border-[#2b2d3a]">
                <div className="text-[#7b5cff] font-medium">Real-time Alerts</div>
                <div className="text-white mt-1">Notifications and reports</div>
              </div>
            </div>
          </div>
        </div>
      </main>

      <footer className="max-w-6xl mx-auto px-6 pb-10 text-white text-sm">
        © {new Date().getFullYear()} CyberScope OSINT
      </footer>
    </div>
  );
}

export default Landing;

