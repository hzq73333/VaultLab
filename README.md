# VaultLab

Gizlilik ve siber güvenlik İsviçre çakısı. Android (Kotlin + Jetpack Compose).

## v1 araçları
- **Geçici E-posta:** Mail.tm ile tek kullanımlık adres ve gelen kutusu. Anahtar gerekmez.
- **Virüs Tarama:** Link ve dosyaları VirusTotal ile tarar. API anahtarını kullanıcı Ayarlar'dan girer.

## v2 planı
- Şifre Üretici & 2FA Kasası
- Fotoğraf EXIF Temizleyici
- Sosyal Medya Gizlilik Rehberi
- Sızdırılmış veri kontrolü (HIBP) ve Sanal Numara (Twilio) için ayrı karar

## APK'yı telefona nasıl kurarsın (Android Studio gerekmez)

1. github.com'da ücretsiz bir hesap aç.
2. Yeni bir **public** repo oluştur (adı `VaultLab` olabilir).
3. Bu klasördeki tüm dosya ve klasörleri repo sayfasına sürükleyip yükle (`.github` klasörü dahil).
4. Repo'nun üstündeki **Actions** sekmesine gir. "Build APK" çalışmaya başlayacak. 3-5 dakika sürer.
5. Bitince çalışmaya tıkla, aşağıda **VaultLab-debug-apk** dosyasını indir. Zip gelirse içinden `app-debug.apk`yı al.
6. APK'yı telefona aktar, aç, "Bilinmeyen kaynaklardan yükleme"ye izin ver ve kur.

Build hata verirse Actions sayfasındaki kırmızı adımın logunu bana yapıştır, düzeltirim.

## Güvenlik notları
- API anahtarları kodun içinde değil. Kullanıcı kendi anahtarını girer ve Android Keystore ile şifrelenmiş olarak cihazda saklanır.
- Dosya taraması en fazla 32 MB'a kadar yapılır (VirusTotal ücretsiz limiti).
- Release build'de R8 ile kod küçültülüp karıştırılır (`isMinifyEnabled`).
