# Vortex TV prototype

Backdrop Launcher tabanında, gönderilen görselin yerleşimini izleyen Kotlin /
Compose Android TV arayüzü. Kaynak lisansı ve mevcut proje atıfları korunmuştur.

## Çalıştırma

`deliverables/vortex-tv-launcher.apk` bağımsız Vortex TV paketidir. Orijinal
Backdrop ile yan yana kurulabilir. Android'in varsayılan ana ekran ayarlarından
Vortex TV seçilebilir; kurulum bu ayarı otomatik değiştirmez. Prototip geliştirme
anahtarıyla imzalanır, upstream otomatik güncelleyicisi kapalıdır.

Kaynak derlemesi: Android Studio ile `native-launcher` klasörünü açın. Java 17+
ve Android SDK 36 kullanın:

```powershell
./gradlew.bat :app:assembleVortexRelease :app:lintVortexRelease
```

Windows'ta Türkçe klasör adları JVM testlerinin sınıf yüklemesini etkileyebilir.
Kaynakları taşımadan geçici bir ASCII sürücü eşlemesi kullanılabilir:

```powershell
# Depo kök klasöründen çalıştırın.
subst.exe V: $PWD.Path
Set-Location V:\native-launcher
./gradlew.bat :app:testBenchmarkDebugUnitTest
```

İş bitince bu terminalin dizininden çıkıp `subst V: /d` ile eşleme kaldırılır.

## Kumanda

| Konum | İşlem |
| --- | --- |
| Dock | Sol/Sağ: uygulamalar; OK: aç; uzun OK: uygulama eylemleri |
| Dock | Yukarı: sıra seçici; Aşağı: İzlemeye Devam Et |
| Sıra seçici | Sol/Sağ: özel sıralar; OK: sıra yönetimi; Yukarı: üst gezinme |
| Hızlı Başlat | Sol/Sağ: favoriler; OK: aç; uzun OK: düzenle; Yukarı: düzenle başlığı, sonra dock |
| Öne çıkan | İzle / Ayrıntılar yanındaki ›: sonraki program |
| Keşfet / Listem | Uzun OK: listeye ekle / çıkar |
| Menu | Launcher ayarları |
| Back | Diyaloğu / sıralamayı kapat; ana ekranda mevcut Back ayarını uygula |

`Ayarlar → Arayüz` altında **Sinematik spiral ana ekran** klasik ekranı geri
getirir. **Animasyonları azalt** geçişleri kapatır; Android'in animasyonları
kapatma ayarı da dikkate alınır. Mevcut uygulama sırası ve görsel seçimi
ayarları kullanılır; klasik ekranın satır konumu/boyutu kontrolleri sinematik
yerleşimin sabit geometrisine uygulanmaz.

## Veri ve önizleme

Sinematik ana ekranın sol altındaki **Hızlı Başlat**, en fazla altı favori
uygulamayı gösterir. **Düzenle** veya uzun OK ile uygulama seçimine girin;
seçme sırası kartların sırasını belirler. **Bitti** ya da Geri ile kaydedilir.
Bu alan için izleme takibi veya ek erişim izni gerekmez.

Paket simgeleri Android PackageManager'dan alınır; yüklenemeyen simgeye yerel
fallback sembolü çizilir. Gerçek uygulamalar ve Watch Next / kanal programları
mevcut Backdrop altyapısından gelir. İzleme listesinin kullanılabilirliği
uygulamaların o anda paylaştığı programlara bağlıdır. Geçmiş bu arayüzden
açılan uygulamaları tutar. İndirilenler için TV'nin sistem uygulaması kullanılır.
HDMI veya mirroring için desteklenmeyen cihaz yetenekleri uydurulmaz.

Öne çıkan içerik 30 saniyede bir değişir; görsel, başlık, açıklama, platform ve
oynatma bağlantısı birlikte seçilir. Kumanda hero düğmelerindeyken, bir içerik
kartı açıkça seçiliyken, ayarlar/uygulama menüsü açıkken veya launcher arka
plandayken döngü bekler. Türkçe arayüzde tanınan İngilizce açıklama için eşleşen
yerel özet kullanılır; bulunamazsa Türkçe özetin paylaşılmadığı açıkça yazılır.
Bu genel amaçlı otomatik çeviri değildir. Ayrıntılar:
[öne çıkan ve oynatma davranışı](docs/development/featured-playback.md).

Android Studio'da `ui/cinematic/CinematicHomeScreen.kt` içindeki
`CinematicHomePreview` 1080p ve 720p önizlemelerini açın. `CinematicSamples`
örnek hero, uygulama, izleme kartları ve kısayol verilerini içerir. Görsel
önizlemedeki örnek içerik gerçek ana ekrana veya kurulu uygulamalara eklenmez.

## Kod ve doğrulama

- [Dosya planı, bileşenler ve performans kararları](docs/development/vortex-implementation.md)
- [Test ve cihaz doğrulama raporu](docs/development/vortex-validation.md)
- [Öne çıkan döngüsü ve Türkçe açıklama testleri](docs/development/featured-rotation.md)
- [Hızlı Başlat kullanımı ve uygulama seçimi](docs/development/quick-launch.md)
- [Hızlı Başlat içeren sinematik örnek ekran](docs/assets/vortex/home-en.png)

Dokuz ön plan/komşu tile ve dört uzak spiral tile ile görünür uygulama sayısı
13 ile sınırlıdır. Sürekli animasyon, gerçek zamanlı blur ve parçacık efekti
yoktur. Geçişler 160 ms'dir; gerçek TCL / Google TV donanımında kare süresi ve
bellek ölçümü henüz yapılmamıştır.

Arayüz TV'nin veya uygulamanın dilini izler; sabit Türkçe kullanımı kaldırılmıştır.
Ana ekranın beş dilde başlangıç çevirisi vardır; eksik alanlar İngilizceye döner.
Tarih, saat ve sayılar geçerli yerel ayara göre biçimlenir. Seçili uygulama
yatay merkeze gelir; kart normal boyutunun %135'ine büyür. Sol/Sağ ile seçilen
her uygulama aynı merkez noktasına taşınır. Uzun uygulama adları iki satırda
görüntülenir. Paket kimlikleri ve uygulamaların marka adları korunur.

Uluslararası katkılar ve RTL testleri için [dil rehberine](docs/localization.md)
bakın. GitHub'a dahil edilmeyen `deliverables/` içindeki bağlantılar yerel test
ortamına aittir; genel tanıtım ve derleme yönergeleri [README.md](README.md)
ve [README.tr.md](README.tr.md) dosyalarındadır.
