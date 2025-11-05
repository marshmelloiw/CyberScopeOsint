# 🌐 CyberScope OSINT Frontend

Modern **React tabanlı Cybersecurity OSINT platformu** frontend uygulaması.

---

## 🚀 Özellikler

- ⚛ **Modern React 18** – En son React özellikleri  
- ⚡ **Vite** – Hızlı build ve development server  
- 🎨 **Tailwind CSS** – Utility-first CSS framework  
- 🪝 **Zustand** – Hafif state management  
- 🔄 **TanStack Query** – Server state management ve caching  
- 🛣 **React Router** – Client-side routing  
- 🧪 **MSW** – API mocking (development)  
- 📱 **Responsive Design** – Mobil uyumlu tasarım  

---

## 🛠 Teknolojiler

- React 18.2.0  
- Vite 5.0.8  
- Tailwind CSS 3.4.14  
- Zustand 4.4.7  
- TanStack Query 5.17.9  
- React Router 6.21.1  
- Lucide React – Modern icon set  
- Recharts – Grafik ve chart kütüphanesi  

---

## 📦 Kurulum

### Gereksinimler
- Node.js 18+  
- npm veya yarn  

### Adımlar
```bash
# Bağımlılıkları yükle
npm install

# Development server'ı başlat
npm run dev
```

Tarayıcıda aç: [http://localhost:5173](http://localhost:5173)  

---

## 🎯 Kullanım

### Mock Giriş Bilgileri
| Kullanıcı   | Şifre       | Rol     | Açıklama              |
|-------------|------------|---------|-----------------------|
| `admin`     | `admin123` | Admin   | Tam sistem erişimi    |
| `analyst`   | `analyst123` | Analyst | Tarama ve analiz      |
| `viewer`    | `viewer123` | Viewer  | Salt okunur erişim    |

### Ana Özellikler
- 📊 **Dashboard**: Genel bakış ve istatistikler  
- 🔍 **Scans**: Tarama yönetimi ve sonuçlar  
- 🌐 **Entities**: Email, Domain, IP analizi  
- 📑 **Reports**: Rapor oluşturma ve yönetimi  
- 🔔 **Notifications**: Bildirim sistemi  
- ⚙ **Settings**: Kullanıcı ayarları  
- 🔑 **API Keys**: API anahtarı yönetimi  
- 👥 **User Management**: Kullanıcı yönetimi (Admin)  

---

## 🏗 Proje Yapısı

```
src/
├── components/          # Yeniden kullanılabilir bileşenler
│   ├── auth/           # Kimlik doğrulama bileşenleri
│   ├── common/         # Ortak bileşenler
│   ├── layout/         # Layout bileşenleri
│   └── ui/             # UI bileşenleri
├── features/           # Özellik bazlı sayfalar
│   ├── apikeys/        # API anahtarı yönetimi
│   ├── auth/           # Giriş/çıkış
│   ├── dashboard/      # Ana dashboard
│   ├── entities/       # Varlık analizi
│   ├── notifications/  # Bildirimler
│   ├── reports/        # Raporlar
│   ├── scans/          # Taramalar
│   ├── settings/       # Ayarlar
│   └── users/          # Kullanıcı yönetimi
├── hooks/              # Custom hooks
├── lib/                # Utility kütüphaneleri
├── mocks/              # Mock data & API handlers
├── store/              # Zustand store'ları
└── utils/              # Yardımcı fonksiyonlar
```

---

## 🎨 Stil Rehberi

### Renk Paleti
- **Primary**: Blue (600, 700, 800)  
- **Secondary**: Gray (100, 200, 300)  
- **Success**: Green (500, 600)  
- **Warning**: Yellow (500, 600)  
- **Error**: Red (500, 600)  
- **Info**: Blue (500, 600)  

### UI Bileşenleri
- 🔘 **Button** – Çeşitli boyut ve stiller  
- 🧩 **Card** – İçerik kartları  
- ✍ **Input** – Form input’ları  
- 🚨 **RiskBadge** – Risk seviyesi göstergeleri  
- 📈 **StatWidget** – İstatistik widget’ları  

---

## 🧪 Geliştirme

### Komutlar
```bash
# Development server
npm run dev

# Build production
npm run build

# Preview production build
npm run preview

# Linting
npm run lint

# Test
npm run test

# Test UI
npm run test:ui

# E2E Test
npm run test:e2e
```

### Mock API
Development modunda **MSW (Mock Service Worker)** kullanılarak mock API'ler sağlanır:

- **Authentication**: Login, logout, token refresh  
- **Users**: Kullanıcı yönetimi  
- **Scans**: Tarama işlemleri  
- **Entities**: Varlık analizi  
- **Reports**: Rapor yönetimi  
- **Notifications**: Bildirim sistemi  

---

## 📱 Responsive Design

- **Mobile**: 320px - 768px  
- **Tablet**: 768px - 1024px  
- **Desktop**: 1024px+  

---

## 🔧 Konfigürasyon

### Vite Config
```javascript
// vite.config.js
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true
  }
})
```

### Tailwind Config
```javascript
// tailwind.config.js
module.exports = {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#eff6ff',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#1e40af'
        }
      }
    }
  }
}
```

---

## 🚀 Production Build

```bash
# Production build oluştur
npm run build

# Build'i preview et
npm run preview

# Build dosyaları dist/ klasöründe oluşur
```

---

## 📄 Lisans

MIT License – Ayrıntılar için **LICENSE** dosyasına bakın.

---

## 🤝 Katkıda Bulunma

1. Fork yapın  
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)  
3. Commit yapın (`git commit -m 'Add amazing feature'`)  
4. Push yapın (`git push origin feature/amazing-feature`)  
5. Pull Request oluşturun  
---

## 📞 Destek

Sorularınız için:  
- Issue oluşturun  
- Dokümantasyonu inceleyin  
- Kod örneklerini kontrol edin  

