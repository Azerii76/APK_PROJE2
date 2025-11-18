# OtoService Android Uygulaması

## 📱 Proje Hakkında

OtoService, mesajlaşma uygulamalarında ilk gelen mesajlara otomatik yanıt veren, güvenli lisans sistemi ile korunan bir Android otomasyon uygulamasıdır.

### Temel Özellikler

- ✅ **Lisans Sistemi**: AES şifreleme ve çok katmanlı imza doğrulama
- 📱 **Otomatik Mesaj Yanıtlama**: AccessibilityService ile mesaj otomasyonu
- 🔔 **Bildirim Dinleme**: NotificationListenerService entegrasyonu
- 📍 **Konum Değiştirme**: Mock location desteği
- 🔒 **Güvenlik**: ProGuard/R8 ile kod obfuscation
- 🚀 **Ön Plan Servisi**: Sürekli çalışan background service

---

## 🔧 Kurulum ve Derleme

### Gereksinimler

- Android Studio Hedgehog | 2023.1.1 veya üstü
- JDK 17
- Android SDK 34
- Gradle 8.13

### Debug Build
```bash
# Gradle sync
./gradlew clean

# Debug APK oluştur
./gradlew assembleDebug

# APK konumu
# app/build/outputs/apk/debug/app-debug.apk
```

### Release Build
```bash
# Önce proguard-rules.pro dosyasını kontrol edin
# Sonra release build yapın

./gradlew assembleRelease

# Obfuscated ve optimize edilmiş APK
# app/build/outputs/apk/release/app-release.apk
```

### Signing Config (İsteğe Bağlı)

Release build için imzalama yapılandırması:
```kotlin
// build.gradle.kts içinde
android {
    signingConfigs {
        create("release") {
            storeFile = file("your-keystore.jks")
            storePassword = "your-store-password"
            keyAlias = "your-key-alias"
            keyPassword = "your-key-password"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ...
        }
    }
}
```

---

## 🔐 Lisans Sistemi

### Lisans Anahtarı Üretme

1. **Python Script ile:**
```bash
# Gereksinimleri yükle
pip install -r requirements.txt

# Lisans üret
python3 generate_license.py <cihaz_id> <bitiş_tarihi>

# Örnek
python3 generate_license.py abc123def456 2026-12-31
```

2. **Cihaz ID Alma:**

Uygulamayı çalıştırın → Lisans ekranında cihaz ID'si gösterilir.

### Lisans Güvenlik Özellikleri

- ✅ AES/ECB/PKCS5Padding şifreleme
- ✅ Çok katmanlı SHA-256 imza doğrulama
- ✅ Tek cihaz sınırlaması (device fingerprinting)
- ✅ Tarih manipülasyonu koruması
- ✅ Timing attack koruması
- ✅ Checksum doğrulama
- ✅ Periyodik lisans kontrolü

### Lisans Format
```
Şifrelenmemiş format:
DEVICE_ID|EXPIRE_DATE|SIGNATURE

Şifrelenmiş format (kullanıcıya verilen):
Base64(AES_Encrypt(DEVICE_ID|EXPIRE_DATE|SIGNATURE))
```

---

## 🏗️ Mimari

### Bileşenler
```
MainActivity
├── LicenseManager (Lisans kontrolü)
├── SettingsStore (Veri saklama)
└── AutomationService
    ├── NotificationReceiver (Bildirim dinleme)
    ├── MessageAutomationHelper (Mesaj gönderme)
    └── LocationChanger (Konum değiştirme)
```

### Servisler

1. **AutomationService** (Foreground Service)
    - Sürekli çalışır
    - Konum değiştirme döngüsü
    - Periyodik lisans kontrolü

2. **NotificationReceiver** (NotificationListenerService)
    - Hedef uygulamadan gelen bildirimleri dinler
    - İlk mesajı tespit eder
    - AutomationService'e mesaj gönderir

3. **MessageAutomationHelper** (AccessibilityService)
    - Mesajlaşma uygulamasını açar
    - Mesaj kutusuna text yazar
    - Gönder butonuna tıklar

### Veri Akışı
```
Bildirim Geldi
    ↓
NotificationReceiver
    ↓
Intent → AutomationService
    ↓
Broadcast → MessageAutomationHelper
    ↓
Accessibility API → Mesaj Gönder
```

---

## ⚙️ Yapılandırma

### İzinler

Uygulamanın çalışması için gerekli izinler:

1. **Bildirim Erişimi**
    - Ayarlar → Uygulamalar → OtoService → Bildirim erişimi

2. **Erişilebilirlik Servisi**
    - Ayarlar → Erişilebilirlik → OtoService

3. **Konum İzni** (Mock Location)
    - Geliştirici seçenekleri → Mock location uygulaması → OtoService

### Ayarlar

- **Hedef Uygulama**: Hangi uygulamadan gelen mesajlara yanıt verileceği
- **Yanıt Metni**: Gönderilecek otomatik mesaj
- **Konum Listesi**: Değiştirilecek konumlar

---

## 🛡️ Güvenlik

### Kod Koruma

- **ProGuard/R8**: Kod obfuscation ve shrinking
- **String Obfuscation**: Kritik stringler Base64 ile kodlanmış
- **Function Obfuscation**: Fonksiyon isimleri karmaşıklaştırılmış
- **Logging Removal**: Release build'de log çıktıları kaldırılır

### Lisans Koruma Katmanları

1. **Şifreleme Katmanı**: AES-256
2. **İmza Katmanı**: Çok katmanlı SHA-256
3. **Device Binding**: Cihaz parmak izi
4. **Temporal Protection**: Zaman manipülasyonu kontrolü
5. **Checksum Layer**: Veri bütünlüğü kontrolü

---

## 🐛 Sorun Giderme

### Build Hataları
```bash
# Gradle cache temizle
./gradlew clean

# Gradle wrapper güncelle
./gradlew wrapper --gradle-version 8.13

# Dependencies senkronize et
./gradlew --refresh-dependencies
```

### Runtime Hataları

**1. Servis Başlamıyor**
- Lisans kontrolü yapın
- İzinlerin verildiğini kontrol edin
- Foreground service izni var mı?

**2. Mesaj Gönderilmiyor**
- Accessibility service aktif mi?
- Hedef uygulama doğru seçilmiş mi?
- Yanıt metni boş mu?

**3. Konum Değişmiyor**
- Mock location izni verilmiş mi?
- Geliştirici seçenekleri açık mı?
- Konum listesi dolu mu?

### Log İnceleme
```bash
# Logcat filtreleme
adb logcat | grep "OtoService"

# Crash logları
adb logcat | grep "AndroidRuntime"
```

---

## 📊 Test Senaryoları

### Manuel Test

1. **Lisans Aktivasyonu**
    - ✅ Geçerli lisans kabul edilmeli
    - ✅ Geçersiz lisans reddedilmeli
    - ✅ Süresi dolmuş lisans reddedilmeli
    - ✅ Farklı cihaz lisansı reddedilmeli

2. **Mesaj Otomasyonu**
    - ✅ İlk mesaj otomatik yanıtlanmalı
    - ✅ İkinci mesaja yanıt verilmemeli
    - ✅ Hedef olmayan uygulamalar göz ardı edilmeli

3. **Servis Sürekliliği**
    - ✅ Cihaz yeniden başlatılınca servis başlamalı
    - ✅ Uygulama kapatılınca servis çalışmalı
    - ✅ Lisans süresi dolunca servis durmalı

---

## 📝 Geliştirme Notları

### Kod Standartları

- Kotlin coding conventions
- Material Design 3 guidelines
- Android Architecture Components

### Commit Mesaj Formatı
```
[TİP] Kısa açıklama

Detaylı açıklama (opsiyonel)

Örnek:
[FIX] Lisans kontrolü senkronizasyon hatası düzeltildi

Switch butonu state'i doğru senkronize edilmedi.
isUpdatingSwitchProgrammatically flag'i eklendi.
```

### Versiyon Yönetimi

- Major: Büyük özellik eklemeleri
- Minor: Küçük özellikler ve iyileştirmeler
- Patch: Bug fix'ler

---

## 📄 Lisans

Bu proje özel kullanım içindir ve ticari olarak dağıtılmamaktadır.

---

## 🔗 İletişim

Sorun bildirimi için GitHub Issues kullanın.

---

## 📚 Kaynaklar

- [Android Developers](https://developer.android.com/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Material Design](https://material.io/design)
- [ProGuard Manual](https://www.guardsquare.com/manual/home)

---

**Son Güncelleme**: 2024-11-17