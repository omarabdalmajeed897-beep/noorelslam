# تطبيق نور الإسلام — نسخة أندرويد الأصلية (Kotlin + Jetpack Compose)

هذا المجلد هو مشروع أندرويد أصلي متكامل مبني بلغة **Kotlin** وواجهة **Jetpack Compose (Material 3)** — وليس تطبيق ويب.

## محتويات المشروع:
- المصحف الشريف كاملاً (١١٤ سورة / ٦٢٣٦ آية) مع التفسير الميسر مدمجاً للعمل بدون إنترنت (`app/src/main/assets/quran-offline.json`).
- ٥٤ حديثاً نبوياً صحيحاً (`hadiths.json`) + الأذكار (`adhkar.json`) + ٩٩ اسماً من أسماء الله الحسنى (`names.json`) + ٣٦ قارئاً (`reciters.json`).
- مدير تحميل تلاوات القراء أوفلاين (`OfflineReciterManager.kt`) + التسميع الصوتي بالمايك (`TasmeeNormalizer.kt`) + حساب المواقيت والقبلة (`PrayerQiblaMath.kt`).
- أيقونة التطبيق الرسمية بجميع المقاسات (`mipmap-mdpi` إلى `mipmap-xxxhdpi`).

## خطوات استخراج ملف التثبيت (APK) خطوة بخطوة:
1. فك الضغط عن الملف `noor-islam-android-kotlin.zip` لتحصل على مجلد `NoorAlIslamAndroid`.
2. افتح برنامج **Android Studio** على جهازك (النسخة الحديثة Koala / Ladybug أو أحدث مع JDK 17).
3. اختر **Open** ثم حدد مجلد `NoorAlIslamAndroid`.
4. انتظر حتى ينتهي Android Studio من تحميل Gradle ومزامنة المشروع (Gradle Sync).
5. من القائمة العلوية اختر:
   **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. بعد انتهاء البناء اضغط **locate** للحصول على ملف `app-debug.apk` وانقله لهاتفك للتثبيت مباشرة.
