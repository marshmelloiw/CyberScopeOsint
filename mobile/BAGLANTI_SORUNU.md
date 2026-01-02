# 🔌 Backend Bağlantı Sorunu - Çözüm

## ✅ Backend Çalışıyor!

Backend başarıyla çalışıyor:
- URL: `http://localhost:8080/api/scans`
- Durum: 200 OK
- Veri: Scan listesi dönüyor

## ❌ Sorun: Emülatör Bağlanamıyor

Android emülatörü `10.0.2.2:8080` adresine bağlanamıyor.

---

## 🔧 ÇÖZÜM 1: Bilgisayarınızın IP Adresini Kullanın (ÖNERİLEN)

### Adım 1: IP Adresinizi Bulun

**PowerShell'de:**
```powershell
ipconfig
```

**Çıktıda şunu arayın:**
```
Wireless LAN adapter Wi-Fi:
   IPv4 Address. . . . . . . . . . . : 192.168.1.XXX
```

VEYA

```
Ethernet adapter Ethernet:
   IPv4 Address. . . . . . . . . . . : 192.168.1.XXX
```

### Adım 2: ApiClient.kt Dosyasını Güncelleyin

`ApiClient.kt` dosyasında 12. satırı değiştirin:

**ŞU AN:**
```kotlin
private var baseUrl = "http://10.0.2.2:8080/api/"
```

**OLACAK:**
```kotlin
private var baseUrl = "http://192.168.1.XXX:8080/api/"
// XXX yerine kendi IP adresinizi yazın
```

### Adım 3: Uygulamayı Yeniden Çalıştırın

1. Android Studio'da **Stop** butonuna basın
2. **Run** butonuna basın
3. Uygulama yeniden yüklenecek ve bağlanacak

---

## 🔧 ÇÖZÜM 2: ADB Reverse (Fiziksel Cihaz İçin)

Eğer fiziksel cihaz kullanıyorsanız:

```powershell
adb reverse tcp:8080 tcp:8080
```

Sonra `ApiClient.kt`'de:
```kotlin
private var baseUrl = "http://localhost:8080/api/"
```

---

## 🔧 ÇÖZÜM 3: Backend'i Tüm Ağ Arayüzlerinde Dinletin

### application.yml'i Güncelleyin

`backend/src/main/resources/application.yml`:

```yaml
server:
  port: 8080
  address: 0.0.0.0  # Bu satırı ekleyin
```

Sonra backend'i yeniden başlatın:
```powershell
cd backend
mvn spring-boot:run
```

---

## 🎯 Hızlı Çözüm (Şimdi Deneyin)

### 1. IP Adresinizi Bulun:
```powershell
ipconfig | findstr IPv4
```

### 2. Örnek çıktı:
```
IPv4 Address. . . . . . . . . . . : 192.168.1.105
```

### 3. ApiClient.kt'yi güncelleyin:
```kotlin
private var baseUrl = "http://192.168.1.105:8080/api/"
```

### 4. Uygulamayı yeniden çalıştırın!

---

## 🔍 Bağlantıyı Test Etme

Emülatörde Chrome tarayıcısını açın ve şu adresi ziyaret edin:
```
http://192.168.1.XXX:8080/api/scans
```

Eğer JSON verisi görüyorsanız, bağlantı çalışıyor demektir!

---

## 🛡️ Güvenlik Duvarı Kontrolü

Eğer hala bağlanamıyorsanız, Windows Firewall'u kontrol edin:

1. **Windows Defender Firewall** açın
2. **Allow an app through firewall** tıklayın
3. **Java** ve **javaw** işaretli olmalı
4. Hem **Private** hem **Public** işaretli olmalı

---

## 📱 Emülatör vs Fiziksel Cihaz

| Cihaz Tipi | Backend URL |
|------------|-------------|
| Android Emulator | `http://10.0.2.2:8080/api/` VEYA `http://192.168.1.XXX:8080/api/` |
| Fiziksel Cihaz (USB) | `http://localhost:8080/api/` (adb reverse ile) |
| Fiziksel Cihaz (WiFi) | `http://192.168.1.XXX:8080/api/` |

---

## ✅ Kontrol Listesi

- [ ] Backend çalışıyor (✓ Zaten çalışıyor!)
- [ ] IP adresini buldum
- [ ] ApiClient.kt güncelledim
- [ ] Uygulamayı yeniden çalıştırdım
- [ ] Bağlantı başarılı!

---

**Hangi çözümü denemek istersiniz? IP adresinizi bulup ApiClient.kt'yi güncellememe yardım edeyim mi?** 🚀
