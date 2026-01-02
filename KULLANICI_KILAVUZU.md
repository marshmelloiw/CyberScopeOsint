# CyberScope OSINT Platform - Kullanıcı Kılavuzu

## 📋 İçindekiler
1. [Sistem Gereksinimleri](#sistem-gereksinimleri)
2. [Kurulum Adımları](#kurulum-adımları)
3. [Veritabanı Kurulumu](#veritabanı-kurulumu)
4. [Backend Yapılandırması](#backend-yapılandırması)
5. [Frontend Yapılandırması](#frontend-yapılandırması)
6. [Uygulamayı Çalıştırma](#uygulamayı-çalıştırma)
7. [İlk Giriş](#ilk-giriş)
8. [API Anahtarları](#api-anahtarları)
9. [Sorun Giderme](#sorun-giderme)
10. [Kullanım Kılavuzu](#kullanım-kılavuzu)

---

## 🖥️ Sistem Gereksinimleri

### Zorunlu Yazılımlar
- **Java JDK 17** veya üzeri
- **Node.js 18** veya üzeri
- **PostgreSQL 15** veya üzeri
- **Maven 3.8** veya üzeri
- **Git** (proje indirmek için)

### Önerilen Sistem
- **İşletim Sistemi**: Windows 10/11, macOS, Linux
- **RAM**: Minimum 8GB (16GB önerilir)
- **Disk Alanı**: Minimum 5GB boş alan

---

## 📥 Kurulum Adımları

### 1. Projeyi İndirme

```bash
git clone https://github.com/marshmelloiw/CyberScopeOsint.git
cd CyberScopeOsint
```

---

## 🗄️ Veritabanı Kurulumu

### Adım 1: PostgreSQL Kurulumu

#### Windows
1. [PostgreSQL İndir](https://www.postgresql.org/download/windows/)
2. Kurulum sırasında şifre belirleyin (örn: `postgres`)
3. Port: `5432` (varsayılan)

#### macOS
```bash
brew install postgresql@15
brew services start postgresql@15
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

### Adım 2: Veritabanı Oluşturma

**pgAdmin** veya **psql** ile bağlanın:

```bash
psql -U postgres
```

Veritabanını oluşturun:

```sql
CREATE DATABASE cyberscope_local;
CREATE USER cyber WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE cyberscope_local TO cyber;
\q
```

### Adım 3: Veritabanı Şemasını Yükleme

#### Yöntem 1: pgAdmin ile (Önerilen)

1. **pgAdmin**'i açın
2. **Servers** → **PostgreSQL** → **Databases** → **cyberscope_local** → Sağ tık → **Query Tool**
3. `init-db` klasöründeki SQL dosyalarını **sırayla** çalıştırın:
   - `00_sequences.sql` → Sequence'ları oluşturur
   - `01_users.sql` → Kullanıcılar tablosu
   - `02_roles.sql` → Roller tablosu
   - `03_user_roles.sql` → Kullanıcı-Rol ilişkisi
   - `04_scans.sql` → Taramalar tablosu
   - `05_scan_targets.sql` → Tarama hedefleri
   - `06_scan_providers.sql` → Tarama sağlayıcıları
   - `07_scan_results.sql` → Tarama sonuçları
   - `08_scan_logs.sql` → Tarama logları
   - `09_api_keys.sql` → API anahtarları
   - `10_notifications.sql` → Bildirimler
   - `11_notification_preferences.sql` → Bildirim tercihleri
   - `12_password_reset_tokens.sql` → Şifre sıfırlama
   - `13_refresh_tokens.sql` → Yenileme token'ları

**ÖNEMLİ**: Dosyaları **mutlaka sırayla** çalıştırın!

#### Yöntem 2: Komut Satırı ile

**Windows PowerShell:**
```powershell
cd init-db
Get-ChildItem -Filter *.sql | Sort-Object Name | ForEach-Object {
    Write-Host "Çalıştırılıyor: $($_.Name)"
    psql -U cyber -d cyberscope_local -f $_.FullName
}
```

**Linux/macOS:**
```bash
cd init-db
for file in *.sql; do
    echo "Çalıştırılıyor: $file"
    psql -U cyber -d cyberscope_local -f "$file"
done
```

### Adım 4: Veritabanını Doğrulama

```sql
psql -U cyber -d cyberscope_local
\dt  -- Tabloları listele
```

Beklenen çıktı: 13 tablo (users, roles, scans, vb.)

---

## ⚙️ Backend Yapılandırması

### Adım 1: Bağımlılıkları İndirme

```bash
cd backend
mvn clean install
```

**Beklenen Çıktı:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXX s
```

### Adım 2: Yapılandırma Dosyası

`backend/src/main/resources/application.yml` dosyasını düzenleyin:

```yaml
spring:
  application:
    name: cyberscope-osint
  
  datasource:
    url: jdbc:postgresql://localhost:5432/cyberscope_local
    username: cyber
    password: your_database_password
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: none  # Veritabanı şeması manuel oluşturuldu
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  flyway:
    enabled: false  # Manuel migration kullanıyoruz

server:
  port: 8080

security:
  jwt:
    secret: BURAYA_GUCLU_BIR_SIFRE_GIRIN_MIN_256_BIT
    expiration: 3600        # 1 saat
    refresh-expiration: 604800  # 7 gün

# OSINT API Anahtarları (Opsiyonel)
osint:
  shodan:
    api-key: SHODAN_API_ANAHTARINIZ
  virustotal:
    api-key: VIRUSTOTAL_API_ANAHTARINIZ
  haveibeenpwned:
    api-key: HIBP_API_ANAHTARINIZ
  gemini:
    api-key: GEMINI_API_ANAHTARINIZ
  zap:
    url: http://localhost:8090
    api-key: ZAP_API_ANAHTARINIZ
```

**ÖNEMLİ**: Production için `jwt.secret` değerini **mutlaka** değiştirin!

---

## 🎨 Frontend Yapılandırması

### Adım 1: Bağımlılıkları İndirme

```bash
cd frontend
npm install
```

**Beklenen Çıktı:**
```
added XXX packages in XXs
```

### Adım 2: API URL Kontrolü

`frontend/src/config/api.js` dosyasını kontrol edin:

```javascript
const API_BASE_URL = 'http://localhost:8080/api';
export default API_BASE_URL;
```

---

## 🚀 Uygulamayı Çalıştırma

### Backend'i Başlatma

```bash
cd backend
mvn spring-boot:run
```

**Başarılı başlatma çıktısı:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

Started OsintBackendApplication in 3.XXX seconds
Tomcat started on port 8080 (http)
```

### Frontend'i Başlatma

**Yeni bir terminal** açın:

```bash
cd frontend
npm run dev
```

**Başarılı başlatma çıktısı:**
```
VITE v5.x.x  ready in XXX ms

➜  Local:   http://localhost:5173/
➜  Network: use --host to expose
```

### Uygulamaya Erişim

Tarayıcınızda açın:
- **Frontend (Ana Uygulama)**: http://localhost:5173
- **Backend API**: http://localhost:8080/api
- **Sağlık Kontrolü**: http://localhost:8080/actuator/health

---

## 🔐 İlk Giriş

### Mevcut Kullanıcıları Kontrol Etme

```sql
psql -U cyber -d cyberscope_local
SELECT email, role FROM users;
```

### Admin Kullanıcısı Oluşturma (gerekirse)

```sql
-- Admin rolünü oluştur
INSERT INTO roles (id, name) 
VALUES (nextval('roles_id_seq'), 'ROLE_ADMIN') 
ON CONFLICT DO NOTHING;

-- Admin kullanıcısı oluştur (kendi güvenli şifrenizi belirleyin)
INSERT INTO users (
    user_id, email, password_hash, full_name, 
    role, is_verified, mfa_enabled, created_at
)
VALUES (
    nextval('users_user_id_seq'),
    'admin@cyberscope.com',
    '[Kendi şifre hash\'inizi BCrypt ile oluşturun]',
    'Sistem Yöneticisi',
    'admin',
    'true',
    'false',
    CURRENT_TIMESTAMP::text
);
```

**Giriş Bilgileri:**
- **Email**: `admin@example.com`
- **Şifre**: `[güvenli şifre]`

**⚠️ ÇOK ÖNEMLİ**: İlk girişten sonra **mutlaka** şifreyi değiştirin!

---

## 🔑 API Anahtarları

### 1. Shodan API

**Amaç**: IP adresleri ve domainler için açık port taraması

1. [Shodan.io](https://account.shodan.io/) hesabı oluşturun
2. **My Account** → **API Key** bölümünden anahtarınızı alın
3. `application.yml` dosyasına ekleyin

**Fiyatlandırma**: Ücretsiz plan (100 sorgu/ay)

### 2. VirusTotal API

**Amaç**: Domain, IP ve dosya güvenlik analizi

1. [VirusTotal](https://www.virustotal.com/) hesabı oluşturun
2. **Profile** → **API Key** bölümünden anahtarınızı alın
3. `application.yml` dosyasına ekleyin

**Fiyatlandırma**: Ücretsiz plan (500 sorgu/gün)

### 3. HaveIBeenPwned API

**Amaç**: Email adresi veri ihlali kontrolü

1. [HIBP API](https://haveibeenpwned.com/API/Key) anahtarı satın alın ($3.50/ay)
2. `application.yml` dosyasına ekleyin

### 4. Google Gemini API

**Amaç**: AI destekli tehdit analizi ve raporlama

1. [Google AI Studio](https://makersuite.google.com/app/apikey) hesabı oluşturun
2. **Get API Key** → **Create API Key** tıklayın
3. `application.yml` dosyasına ekleyin

**Fiyatlandırma**: Ücretsiz plan (60 sorgu/dakika)

### 5. OWASP ZAP

**Amaç**: Web uygulaması güvenlik taraması

1. [ZAP](https://www.zaproxy.org/download/) indirin ve kurun
2. ZAP'ı başlatın (varsayılan port: 8090)
3. **Tools** → **Options** → **API** → API Key'i kopyalayın
4. `application.yml` dosyasına ekleyin

**Not**: ZAP'ın sürekli çalışması gerekir

---

## 🛠️ Sorun Giderme

### 1. Port Zaten Kullanımda

**Hata**: `Port 8080 was already in use`

**Çözüm:**

**Windows:**
```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Linux/macOS:**
```bash
lsof -ti:8080 | xargs kill -9
```

### 2. Veritabanı Bağlantı Hatası

**Hata**: `Connection refused`

**Çözüm:**

1. PostgreSQL çalışıyor mu?
   ```bash
   # Windows: services.msc → PostgreSQL
   # Linux: sudo systemctl status postgresql
   # macOS: brew services list | grep postgresql
   ```

2. Kullanıcı adı ve şifre doğru mu?
3. Veritabanı var mı?
   ```sql
   psql -U postgres
   \l
   ```

### 3. Maven Build Hatası

**Hata**: `BUILD FAILURE`

**Çözüm:**
```bash
mvn clean install -U
# Testleri atla (gerekirse):
mvn clean install -DskipTests
```

### 4. NPM Install Hatası

**Hata**: `npm ERR!`

**Çözüm:**
```bash
rm -rf node_modules package-lock.json
npm cache clean --force
npm install
```

---

## 📊 Kullanım Kılavuzu

### 1. Dashboard

**Erişim**: Ana sayfa (http://localhost:5173)

**Özellikler**:
- Toplam tarama sayısı
- Aktif kullanıcı sayısı
- Risk dağılımı grafikleri
- Son taramalar listesi
- Bildirimler

### 2. Yeni Tarama Oluşturma

**Adımlar**:

1. **Scans** menüsüne gidin
2. **New Scan** butonuna tıklayın
3. Tarama bilgilerini girin:
   - **Scan Name**: Tarama adı (örn: "Google Güvenlik Taraması")
   - **Target Type**: Hedef tipi seçin
     - Domain (örn: google.com)
     - IP Address (örn: 8.8.8.8)
     - Email (örn: test@example.com)
     - URL (örn: http://example.com)
     - Social (örn: @username)
   - **Target Value**: Hedef değeri girin
   - **Priority**: Öncelik seçin (Low, Medium, High, Critical)
4. **Scan Tools**: Kullanılacak araçları seçin
   - ☑️ Shodan
   - ☑️ VirusTotal
   - ☑️ HaveIBeenPwned
   - ☑️ ZAP
5. **Start Scan** butonuna tıklayın

**Not**: API anahtarları olmayan araçlar devre dışı görünür.

### 3. Tarama Sonuçlarını İnceleme

**Adımlar**:

1. **Scans** menüsünde taramayı bulun
2. Taramaya tıklayın
3. **Results** sekmesini görüntüleyin
4. Sonuçları inceleyin:
   - **Summary**: Genel özet
   - **Findings**: Bulgular listesi
   - **Risk Score**: Risk skoru (0-10)
   - **AI Analysis**: Yapay zeka analizi
5. **Export** butonu ile rapor oluşturun:
   - PDF formatında
   - Excel formatında
   - JSON formatında

### 4. Kullanıcı Yönetimi

**Erişim**: **Users** menüsü (Sadece Admin)

**Roller**:
- **Admin**: Tam yetki (kullanıcı yönetimi, sistem ayarları)
- **Analyst**: Tarama oluşturma ve analiz
- **Viewer**: Sadece görüntüleme

**Yeni Kullanıcı Ekleme**:

1. **Add User** butonuna tıklayın
2. Bilgileri girin:
   - Email
   - Full Name
   - Role
   - Password
3. **Create** butonuna tıklayın

### 5. Bildirim Ayarları

**Erişim**: **Settings** → **Notifications**

**Ayarlar**:
- ☑️ Email bildirimleri
- ☑️ SMS bildirimleri (opsiyonel)
- ☑️ Tarama tamamlandığında bildir
- ☑️ Yüksek risk bulunduğunda bildir
- ☑️ Haftalık özet raporu

---

## 🔒 Güvenlik Önerileri

### Production Ortamı İçin

1. **JWT Secret**: Güçlü, rastgele bir secret kullanın (min 256 bit)
2. **Şifreler**: Varsayılan şifreleri değiştirin
3. **HTTPS**: Production'da SSL/TLS kullanın
4. **API Anahtarları**: Ortam değişkenlerinde saklayın
5. **Firewall**: Gereksiz portları kapatın
6. **Güncellemeler**: Düzenli olarak güncelleyin

### Şifre Politikası

- Minimum 8 karakter
- En az 1 büyük harf
- En az 1 küçük harf
- En az 1 rakam
- En az 1 özel karakter

---

## 📞 Destek

- **GitHub Issues**: https://github.com/marshmelloiw/CyberScopeOsint/issues
- **Email**: support@cyberscope.com
- **Dokümantasyon**: https://docs.cyberscope.com

---

## 📝 Lisans

Bu proje [MIT Lisansı](LICENSE) altında lisanslanmıştır.

---

## 🙏 Teşekkürler

CyberScope OSINT Platform'u kullandığınız için teşekkür ederiz!

**Güvenli taramalar! 🔍🛡️**

---

**Son Güncelleme**: 2 Ocak 2026  
**Versiyon**: 1.0.0
