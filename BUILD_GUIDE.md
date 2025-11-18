# Build ve Deploy Rehberi

## 🛠️ DEVELOPMENT BUILD

### 1. Proje Hazırlığı
```bash
# Repository'yi klonlayın
git clone https://github.com/Azerii76/APK_PROJE.git
cd APK_PROJE

# Güncellenmiş dosyaları kopyalayın
# fixed_files/ klasöründeki dosyaları ilgili konumlara kopyalayın
```

### 2. Dosya Değişiklikleri

Aşağıdaki dosyaları değiştirin:
```
✅ gradle.properties                    → Kök dizin
✅ app/build.gradle.kts                 → app/
✅ app/proguard-rules.pro              → app/
✅ app/src/main/res/layout/activity_main.xml
✅ app/src/main/java/com/example/otomasyon/MainActivity.kt
✅ app/src/main/java/com/example/otomasyon/LicenseManager.kt
✅ app/src/main/java/com/example/otomasyon/AutomationService.kt
✅ app/src/main/java/com/example/otomasyon/LocationChanger.kt
```

### 3. Android Studio'da Sync
```
1. Android Studio'yu açın
2. File → Open → APK_PROJE klasörünü seçin
3. Gradle sync bekleyin
4. Build → Clean Project
5. Build → Rebuild Project
```

### 4. Debug Build Oluşturma
```bash
# Terminal'den
./gradlew clean
./gradlew assembleDebug

# Çıktı
# app/build/outputs/apk/debug/app-debug.apk
```

### 5. Cihaza Yükleme
```bash
# USB ile bağlı cihaza
adb install app/build/outputs/apk/debug/app-debug.apk

# Veya Android Studio'dan
# Run → Run 'app'
```

---

## 🚀 PRODUCTION BUILD

### 1. Keystore Oluşturma
```bash
keytool -genkey -v \
  -keystore otoservice-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias otoservice

# Şifreyi güvenli saklayın!
```

### 2. Signing Config Ekleme

`app/build.gradle.kts` dosyasına ekleyin:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../otoservice-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "your-password"
            keyAlias = "otoservice"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "your-password"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### 3. Release Build
```bash
# Environment variables (güvenli)
export KEYSTORE_PASSWORD="your-keystore-password"
export KEY_PASSWORD="your-key-password"

# Build
./gradlew clean
./gradlew assembleRelease

# Çıktı
# app/build/outputs/apk/release/app-release.apk
```

### 4. APK Doğrulama
```bash
# İmza kontrolü
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# APK içeriği
aapt dump badging app/build/outputs/apk/release/app-release.apk
```

---

## 🔐 LİSANS ANAHTARI ÜRETME

### 1. Python Kurulumu
```bash
# Python 3.8+ gerekli
python3 --version

# Virtual environment (önerilen)
python3 -m venv venv
source venv/bin/activate  # Linux/Mac
# venv\Scripts\activate   # Windows

# Dependencies
pip install -r requirements.txt
```

### 2. Cihaz ID Alma
```
1. Uygulamayı cihaza yükleyin
2. Uygulamayı açın
3. Lisans ekranında "Cihaz ID" gösterilir
4. ID'yi kopyalayın (örn: abc123def456...)
```

### 3. Lisans Üretme
```bash
python3 generate_license.py <DEVICE_ID> <EXPIRE_DATE>

# Örnek
python3 generate_license.py abc123def456789abcdef12345678 2026-12-31

# Çıktı
============================================================
LİSANS ANAHTARI OLUŞTURULDU
============================================================

SGVsbG9Xb3JsZFRoaXNJc0FuRXhhbXBsZUtleQ==...

============================================================
```

### 4. Lisans Aktivasyonu
```
1. Uygulamada lisans ekranına gidin
2. Üretilen anahtarı yapıştırın
3. "Aktif Et" butonuna tıklayın
4. Başarılı mesajı görünce ana sayfaya geçilir
```

---

## 📦 APK DAĞITIMI

### Yöntem 1: Direct APK
```bash
# APK'yı güvenli kanaldan paylaşın
# - USB transfer
# - Şifreli email
# - Secure file sharing service
```

### Yöntem 2: Internal App Distribution
```bash
# Firebase App Distribution (önerilen)
# 1. Firebase Console → App Distribution
# 2. APK'yı yükleyin
# 3. Test kullanıcılarını ekleyin
# 4. Release notes ekleyin
```

### Yöntem 3: Private APK Hosting
```bash
# Kendi sunucunuzda host edin
# - HTTPS zorunlu
# - Basic auth ile koruyun
# - Version tracking yapın
```

---

## 🔍 BUILD SORUN GİDERME

### Gradle Sync Hatası
```bash
# Cache temizle
./gradlew clean
./gradlew --stop

# Android Studio
File → Invalidate Caches → Invalidate and Restart
```

### Dependency Resolution Hatası
```bash
# Dependencies güncelle
./gradlew --refresh-dependencies

# Gradle wrapper güncelle
./gradlew wrapper --gradle-version 8.13 --distribution-type all
```

### ProGuard/R8 Hatası
```bash
# Mapping dosyasını kontrol edin
cat app/build/outputs/mapping/release/mapping.txt

# ProGuard kurallarını debug edin
# build.gradle.kts içinde
android {
    buildTypes {
        release {
            // Geçici olarak kapatın
            isMinifyEnabled = false
        }
    }
}
```

### Signing Hatası
```bash
# Keystore bilgilerini doğrulayın
keytool -list -v -keystore otoservice-release.jks

# Şifreleri kontrol edin
# Environment variables doğru set edilmiş mi?
echo $KEYSTORE_PASSWORD
echo $KEY_PASSWORD
```

---

## 📊 BUILD METRIKLERI

### Debug Build
```
APK Boyutu: ~10-15 MB
Build Süresi: ~30-60 saniye
Obfuscation: Yok
Logging: Aktif
```

### Release Build
```
APK Boyutu: ~5-8 MB (R8 ile %40-50 küçülme)
Build Süresi: ~60-120 saniye
Obfuscation: Aktif
Logging: Kaldırılmış
```

---

## 🔄 VERSİYON YÜKSELTME

### Version Code/Name Güncelleme

`app/build.gradle.kts`:
```kotlin
defaultConfig {
    versionCode = 2  // Her release'de +1
    versionName = "1.1"  // Semantic versioning
}
```

### Changelog
```markdown
## v1.1 (2024-11-17)

### Yenilikler
- ✅ UI/UX iyileştirmeleri
- ✅ Lisans güvenliği artırıldı

### Düzeltmeler
- 🐛 Switch senkronizasyon hatası
- 🐛 Mock location setup

### İyileştirmeler
- ⚡ ProGuard optimizasyonu
- ⚡ Build performance
```

---

## 🎯 ÜRETİM KONTROL LİSTESİ

### Build Öncesi

- [ ] Version code/name güncellendi
- [ ] ProGuard kuralları test edildi
- [ ] Keystore hazır ve güvenli
- [ ] Signing config doğru
- [ ] Log statements kaldırıldı (release)

### Build Sonrası

- [ ] APK imzası doğrulandı
- [ ] APK boyutu makul (<10 MB)
- [ ] Test cihazda çalıştı
- [ ] Lisans aktivasyonu test edildi
- [ ] Tüm özellikler çalışıyor

### Dağıtım Öncesi

- [ ] Release notes hazırlandı
- [ ] Lisans anahtarları üretildi
- [ ] Kullanıcı dokümantasyonu hazır
- [ ] Backup alındı

---

## 📝 NOTLAR

### Önemli Dosyalar
```
⚠️ ASLA COMMIT ETMEYİN:
- otoservice-release.jks
- keystore passwords
- license keys
- user data

✅ GIT'E EKLEYİN:
- Kaynak kodlar
- Build scripts
- Documentation
- ProGuard rules
```

### .gitignore Kontrolü
```gitignore
# Keystore
*.jks
*.keystore

# Passwords
keystore.properties
local.properties

# Build outputs
/build
/app/build

# License files
licenses/
*.license
```

---

## 🚨 ACİL DURUM

### Build Pipeline Hatası
```bash
# 1. Temiz başlangıç
rm -rf .gradle build app/build
./gradlew clean

# 2. Gradle daemon'u yeniden başlat
./gradlew --stop
./gradlew --status

# 3. Dependencies yeniden indir
./gradlew --refresh-dependencies
./gradlew assembleDebug
```

### Corrupted Keystore
```bash
# Yedek keystore kullanın
# VEYA
# Yeni keystore oluşturun (farklı package name gerekir)
keytool -genkey -v -keystore new-keystore.jks ...
```

### License System Down
```bash
# Geliştirme bypass'ı (sadece debug)
# LicenseManager.kt içinde

fun checkLicenseStatus(): String {
    if (BuildConfig.DEBUG) {
        return LICENSE_ACTIVE  // Sadece geliştirme için!
    }
    // Normal kontrol devam eder...
}
```

---

**Son Güncelleme**: 2024-11-17  
**Yazar**: DevOps Team  
**Versiyon**: 1.1