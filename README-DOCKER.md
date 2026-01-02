# Docker Kurulum Kılavuzu

Bu projeyi Docker kullanarak kolayca çalıştırabilirsiniz.

## Gereksinimler

- Docker Desktop (Windows/Mac) veya Docker Engine (Linux)
- Docker Compose

## Hızlı Başlangıç

### 1. Projeyi Docker ile Çalıştırma

Proje kök dizininde aşağıdaki komutu çalıştırın:

```bash
docker-compose up -d
```

Bu komut şunları yapacak:
- PostgreSQL veritabanını başlatacak
- Backend servisini derleyip çalıştıracak
- Frontend'i derleyip çalıştıracak

### 2. Servislere Erişim

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **PostgreSQL**: localhost:5432

### 3. Logları Görüntüleme

Tüm servislerin loglarını görmek için:

```bash
docker-compose logs -f
```

Sadece belirli bir servisin loglarını görmek için:

```bash
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

### 4. Servisleri Durdurma

```bash
docker-compose down
```

Veritabanı verilerini de silmek isterseniz:

```bash
docker-compose down -v
```

## Ortam Değişkenleri

API anahtarlarını ve diğer ayarları yapılandırmak için proje kök dizininde bir `.env` dosyası oluşturabilirsiniz:

```env
# API Keys
TWITTER_API_KEY=your_twitter_api_key
TWITTER_API_SECRET=your_twitter_api_secret
TWITTER_BEARER_TOKEN=your_twitter_bearer_token
SHODAN_API_KEY=your_shodan_api_key
VIRUSTOTAL_API_KEY=your_virustotal_api_key
HIBP_API_KEY=your_hibp_api_key
GEMINI_API_KEY=your_gemini_api_key
ZAP_API_KEY=your_zap_api_key
ZAP_BASE_URL=http://localhost:9091

# Frontend API URL
VITE_API_BASE_URL=http://localhost:8080

# JWT Secret (production için değiştirin)
SECURITY_JWT_SECRET=your_secure_jwt_secret_here
```

## Veritabanı

PostgreSQL veritabanı otomatik olarak oluşturulur ve `init-db` klasöründeki SQL dosyaları otomatik olarak çalıştırılır.

Veritabanına bağlanmak için:

```bash
docker exec -it cyberscope-postgres psql -U postgres -d cyberscope
```

## Geliştirme

### Sadece Backend'i Yeniden Derleme

```bash
docker-compose build backend
docker-compose up -d backend
```

### Sadece Frontend'i Yeniden Derleme

```bash
docker-compose build frontend
docker-compose up -d frontend
```

### Tüm Servisleri Yeniden Derleme

```bash
docker-compose build --no-cache
docker-compose up -d
```

## Sorun Giderme

### Port Zaten Kullanılıyor Hatası

Eğer 8080, 3000 veya 5432 portları zaten kullanılıyorsa, `docker-compose.yml` dosyasındaki port numaralarını değiştirebilirsiniz.

### Backend Başlamıyor

Backend'in loglarını kontrol edin:

```bash
docker-compose logs backend
```

Veritabanı bağlantısı sorunları için PostgreSQL'in hazır olup olmadığını kontrol edin:

```bash
docker-compose ps
```

### Frontend Build Hatası

Frontend build sırasında hata alırsanız, node_modules'ü temizleyip yeniden deneyin:

```bash
cd frontend
rm -rf node_modules
cd ..
docker-compose build --no-cache frontend
```

## Production Kullanımı

Production ortamı için:

1. `.env` dosyasında güvenli değerler kullanın
2. `SECURITY_JWT_SECRET` değerini güçlü bir değerle değiştirin
3. HTTPS için reverse proxy (nginx/traefik) kullanın
4. Veritabanı şifrelerini güvenli tutun
5. Volume'ları düzenli olarak yedekleyin

