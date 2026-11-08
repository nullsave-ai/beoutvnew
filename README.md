# BEOUT TV

ثلاثة مشاريع مستقلة:

| المجلد | المحتوى |
|---|---|
| `android/` | تطبيق Android (Kotlin، Compose، Media3) |
| `backend/` | Rust + Axum + PostgreSQL + Redis |
| `dashboard/` | لوحة تحكم React + TypeScript |

المرجع الكامل: `REFERENCE.md` — الشجرة: `TREE.txt` — السجل: `CHANGES.md`

## التشغيل

### 1) Backend
```
docker compose up -d            # PostgreSQL + Redis للتطوير
cd backend
cp .env.example .env            # عدّل القيم، ثم صدّرها كمتغيرات بيئة
cargo run --release
```
عند أول تشغيل تُنفَّذ الـ migrations ويُنشأ المسؤول من `ADMIN_EMAIL` و`ADMIN_PASSWORD`.
ضع الخدمة خلف HTTPS (nginx أو caddy). التطبيق يمنع HTTP (`usesCleartextTraffic=false`).

### 2) لوحة التحكم
```
cd dashboard
npm install
npm run dev        # يمرر /v1 إلى localhost:8080
npm run build      # الناتج في dist/
```
عند النشر على نطاق مختلف عن الـ API ضع `VITE_API` وضبط `ADMIN_ORIGIN` في الـ Backend.

### 3) تطبيق Android
في `android/gradle.properties` غيّر `apiBase` إلى عنوان الـ API (ينتهي بـ `/v1/`)، أو مرّره: `-PapiBase=https://api.example.com/v1/`.
```
cd android
./gradlew assembleRelease
```
وقّع الإصدار بمفتاحك (الإعداد الحالي يستخدم debug فقط للتجربة).

## عقد الـ API (للتطبيق)
`GET /v1/home` أقسام بترتيب وإظهار يحددهما الخادم. `GET /v1/channels|catalog?category=&cursor=` تحميل تدريجي بـ cursor. `GET /v1/items/:id` تفاصيل + مواسم + مشابه. `GET /v1/search?q=&type=&cursor=`. `GET /v1/play/:kind/:id` رابط التشغيل (ترويسة `X-Device` مطلوبة). `POST /v1/sync/favorites|history`.
الجداول والحقول في `backend/migrations/`. `GET /v1/catalog?sort=recent` للأحدث أولاً.

## ما تم التحقق منه وما لم يتم
- **Backend**: يُترجم (`cargo check`) وجرى تشغيله على PostgreSQL 16 وRedis 7 واختبار: تسجيل الدخول والرفض، CRUD، الصفحة الرئيسية، البحث (أفلام + حلقات)، التفاصيل، روابط التشغيل الموقعة، بوابة الاشتراك (403 ثم 200)، ETag (304)، إبطال الكاش عند التعديل، حد المعدل (429)، حد محاولات الدخول، العدّاد الإحصائي.
- **لوحة التحكم**: `tsc` + `vite build` ناجحان. لم تُجرَّب في متصفح.
- **Android (1.1.0 إعادة التصميم)**: تحقق نحوي فقط بمحلل Kotlin، ولا بناء. التصميم من النص لأن صور المواصفة لم تصل. **لم يُبنَ ولم يُشغَّل** لأن بيئة العمل بلا Android SDK والوصول إلى Google Maven محجوب. راجعت الكود يدوياً وأصلحت ما وجدته، لكن توقع بعض أخطاء ترجمة أو واجهة عند أول بناء.
- **حجم APK**: **غير مقاس**. هدف 1MB غير واقعي مع Compose + Media3 (غالباً عدة MB بعد R8). لقياس حقيقي: `./gradlew assembleRelease` ثم حجم `app/build/outputs/apk/release/*.apk`، وللتفصيل Android Studio > Build > Analyze APK.

## غير منفّذ / للتنبيه
- الإشعارات الفورية من المسؤول: تُسجَّل في جدول `notifications` فقط. الإرسال (FCM) خدمة مستقلة لم تُبنَ.
- الإعلانات: تُدار من اللوحة وتُخزَّن، ولا يوجد بعد endpoint عام ولا عرض لها في التطبيق.
- تسجيل دخول المستخدم النهائي: التطبيق يستخدم معرّف جهاز عشوائي. حسابات البريد للمسؤولين فقط.
- أيقونة "التنزيل" مرسومة لكن لا توجد ميزة تنزيل (طُلبت "عند دعمه").
- الترجمة الخارجية (ملفات SRT منفصلة) غير مدعومة، فقط المسارات داخل البث.
- لا توجد بيانات تجريبية: أضف المحتوى من اللوحة.
