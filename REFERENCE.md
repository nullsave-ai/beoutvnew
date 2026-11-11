# BEOUT TV — المرجع الكامل

منصة بث: تطبيق Android + خادم Rust/Axum/PostgreSQL/Redis + لوحة تحكم React/TypeScript. الإصدار 1.2.0.

## 1. الشجرة

```
BEOUT TV
├── android/
│   ├── app/
│   │   ├── src/
│   │   │   └── main/
│   │   │       ├── java/
│   │   │       │   └── com/
│   │   │       │       └── beouttv/
│   │   │       │           └── app/
│   │   │       │               ├── core/
│   │   │       │               │   ├── Startup.kt
│   │   │       │               │   └── UiState.kt
│   │   │       │               ├── data/
│   │   │       │               │   ├── Api.kt
│   │   │       │               │   ├── ImageLoader.kt
│   │   │       │               │   ├── Look.kt
│   │   │       │               │   ├── Models.kt
│   │   │       │               │   ├── Prefs.kt
│   │   │       │               │   └── Repository.kt
│   │   │       │               ├── notify/
│   │   │       │               │   └── Reminders.kt
│   │   │       │               ├── ui/
│   │   │       │               │   ├── Adaptive.kt
│   │   │       │               │   ├── Bars.kt
│   │   │       │               │   ├── Chrome.kt
│   │   │       │               │   ├── Components.kt
│   │   │       │               │   ├── DetailScreen.kt
│   │   │       │               │   ├── Engine.kt
│   │   │       │               │   ├── HomeScreen.kt
│   │   │       │               │   ├── Icons.kt
│   │   │       │               │   ├── ListScreens.kt
│   │   │       │               │   ├── Live.kt
│   │   │       │               │   ├── LivePanes.kt
│   │   │       │               │   ├── LiveScreen.kt
│   │   │       │               │   ├── Motion.kt
│   │   │       │               │   ├── Nav.kt
│   │   │       │               │   ├── PlayerScreen.kt
│   │   │       │               │   ├── PlayerUi.kt
│   │   │       │               │   ├── Root.kt
│   │   │       │               │   ├── SearchScreen.kt
│   │   │       │               │   ├── SettingsScreen.kt
│   │   │       │               │   ├── Strings.kt
│   │   │       │               │   ├── SystemBars.kt
│   │   │       │               │   ├── Theme.kt
│   │   │       │               │   ├── UiMode.kt
│   │   │       │               │   └── ViewModels.kt
│   │   │       │               ├── App.kt
│   │   │       │               └── MainActivity.kt
│   │   │       ├── res/
│   │   │       │   ├── mipmap-hdpi/
│   │   │       │   │   └── ic_launcher.png
│   │   │       │   ├── mipmap-mdpi/
│   │   │       │   │   └── ic_launcher.png
│   │   │       │   ├── mipmap-xhdpi/
│   │   │       │   │   └── ic_launcher.png
│   │   │       │   ├── mipmap-xxhdpi/
│   │   │       │   │   └── ic_launcher.png
│   │   │       │   └── mipmap-xxxhdpi/
│   │   │       │       └── ic_launcher.png
│   │   │       └── AndroidManifest.xml
│   │   ├── build.gradle.kts
│   │   └── proguard-rules.pro
│   ├── gradle/
│   │   └── wrapper/
│   │       ├── gradle-wrapper.jar
│   │       └── gradle-wrapper.properties
│   ├── build.gradle.kts
│   ├── gradle.properties
│   ├── gradlew
│   ├── gradlew.bat
│   └── settings.gradle.kts
├── backend/
│   ├── migrations/
│   │   ├── 0001_init.sql
│   │   ├── 0002_home_cleanup.sql
│   │   └── 0003_home_live.sql
│   ├── seed/
│   │   └── demo.sql
│   ├── src/
│   │   ├── admin.rs
│   │   ├── error.rs
│   │   ├── main.rs
│   │   ├── mw.rs
│   │   └── public.rs
│   ├── .env.example
│   ├── Cargo.lock
│   └── Cargo.toml
├── dashboard/
│   ├── src/
│   │   ├── pages/
│   │   │   ├── Ads.tsx
│   │   │   ├── Categories.tsx
│   │   │   ├── Config.tsx
│   │   │   ├── HomeLayout.tsx
│   │   │   ├── Items.tsx
│   │   │   ├── Notifications.tsx
│   │   │   ├── Stats.tsx
│   │   │   └── Users.tsx
│   │   ├── App.tsx
│   │   ├── api.ts
│   │   ├── main.tsx
│   │   ├── styles.css
│   │   └── vite-env.d.ts
│   ├── .env.example
│   ├── index.html
│   ├── package-lock.json
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts
├── .gitignore
├── CHANGES.md
├── README.md
├── REFERENCE.md
├── TREE.txt
└── docker-compose.yml
```

## 2. البنية العامة

| الطبقة | التقنية | الدور |
|---|---|---|
| android | Kotlin 2.0.20، Compose Foundation/UI (بلا Material)، Media3 1.4.1، DataStore، WorkManager، kotlinx-serialization | تطبيق المشاهدة، minSdk 24، compileSdk 35 |
| backend | Rust، Axum، PostgreSQL، Redis | واجهة `/v1` للتطبيق و`/admin` للوحة |
| dashboard | React، TypeScript، Vite | إدارة المحتوى والأقسام والمستخدمين والإعلانات والإشعارات |

تدفق البيانات: اللوحة تكتب عبر `/admin` إلى PostgreSQL، والتطبيق يقرأ عبر `/v1` (مع كاش Redis)، ويزامن السجل والمفضلة إلى `/v1/sync/*`.

## 3. تطبيق Android

### 3.1 المعمارية
- الواجهة عربية RTL دائماً (`LocalLayoutDirection provides Rtl`)؛ طبقة المشغل فقط فيزيائية LTR.
- التنقل مخصص (`NavVM`): ثلاثة أقسام (الرئيسية، القنوات، الأفلام) + مكدس صفحات بحركة دخول/خروج بنابض واحد.
- كل شاشة تقرأ حالتها من `UiState` عبر `Loader`/`PagedLoader` و`collectAsStateWithLifecycle`.
- وضع التلفاز: `LocalDensity` ×1.3 و`LocalTv`، فكل الأبعاد بوحدات dp المكبّرة.

### 3.2 الصفحتان المباشرتان (الرئيسية والقنوات) — الإصدار 1.2.0
- المشغل في الأعلى مثل يوتيوب، تحته شريط "يُعرض الآن" (شعار، حالة، السابق/التالي، مفضلة، ملء الشاشة).
- تحته لوحتان: **القنوات على اليمين** و**الأقسام على اليسار**.
- الرئيسية: أقسام القنوات من الخادم + "المفضلة" + "آخر ما شاهدت". لا أفلام ولا مسلسلات داخل الأقسام؛ تظهر فقط في شريط **اقتراح** منفصل (قسم `suggest` من الخادم، أو بديلاً عناصر `row`).
- القنوات: الأقسام = الكل / المفضلة / تصنيفات الخادم، مع بحث داخل لوحة القنوات.
- **طبقة المشغل الدائمة** (`PlayerLayer`): مشغل واحد فوق المحتوى يتبع مستطيل الفتحة ويتمدد بنابض لملء الشاشة، فلا يُعاد إنشاء سطح الفيديو ولا ينقطع البث.
- ملء الشاشة: زر، أو Enter على التلفاز، أو تدوير الهاتف للأفقي (قابل للإيقاف من الإعدادات). الرجوع يخرج منه.
- التخطيط المتجاوب (`Adaptive.kt`): `isSplitForm = العرض ≥ 480dp وأكبر من 1.1× الارتفاع`.
  - عمودي (هاتف/لوحي): مشغل بعرض كامل ثم الشريط ثم اللوحتان، تنقل سفلي عائم.
  - مقسوم (تلفاز/أفقي): عمود المشغل بجانب اللوحتين، شريط تنقل جانبي (`NavRail`).
  - إزاحات الجانبين تراعي النوتش وأشرطة النظام.
- قواعد الإيقاف: يتوقف عند فتح تبويب الأفلام أو أي صفحة فوقه أو عند `ON_STOP`، ويُستأنف عند الظهور.
- الريموت: Enter في المشغل المضمّن = ملء الشاشة، `ChannelUp/Down` و`MediaNext/Prev` = قناة تالية/سابقة (دائرياً).

### 3.3 المشغل
- `PlaybackEngine` يملك ExoPlayer ويكشف حالات Compose (تشغيل، تخزين، مسارات، ترجمة، نسبة العرض). إعادة اتصال بتراجع متدرج، حفظ تقدم VOD، قفز للحافة الحية عند الاستئناف.
- `PlayerUi` واجهة واحدة لثلاثة أوضاع: `Inline` و`Fullscreen` (بث) و`Route` (أفلام/حلقات).
- البث المباشر محرك واحد في `LiveVM`؛ صفحة VOD تملك محركها وتحرره عند الإغلاق.

### 3.4 الإعدادات (DataStore)
الثيم، وضع العرض، وضع الفيديو (Fit/Fill/Zoom/Stretch/Auto)، تسريع الضغط المطول، **تشغيل آخر قناة تلقائياً**، **ملء الشاشة التلقائي في الأفقي**، أقصى جودة، الاستئناف، إبقاء الشاشة، الإشعارات. كما تُحفظ المفضلة، السجل، آخر القنوات (20)، سجل البحث.

### 3.5 مرجع الملفات (تلقائي من الكود)


#### `App.kt`

- `class App`

#### `MainActivity.kt`

- `class MainActivity`

#### `core/Startup.kt`

- `sealed interface Startup` — مراحل بدء التشغيل. تُشتق من حالات التحميل الفعلية (لا مؤقتات ولا تأخير اصطناعي):  Initializing  -> تهيئة الخدمات المحلية (الإعدادات)  LoadingConfig -> الإعدادات الأساسية (محفوظة ثم بعيدة)  LoadingCache  -> قراءة المحتوى 
- `fun deriveStartup`

#### `core/UiState.kt`

- `sealed interface UiState`
- `fun <T> UiState`
- `object Net`
- `class Loader` — محمّل بيانات أحادي: يلغي الطلب السابق، يعرض النسخة المحفوظة فوراً إن وُجدت ثم يحدّث من الخادم، ويحتفظ بآخر بيانات ناجحة عند الفشل، ويمنع الكتابة ببيانات قديمة. /
- `data class PagedState`
- `class PagedLoader` — تحميل تدريجي بمؤشر cursor. إعادة الضبط تلغي أي طلب قديم، وkeepOld تُبقي المحتوى ظاهراً أثناء تغيير الفلتر.

#### `data/Api.kt`

- `class HttpException`
- `object Api` — عميل شبكة خفيف: HttpURLConnection + ETag + دمج الطلبات المتطابقة + إلغاء فعلي للاتصال + كاش قرص لكل استجابة GET ناجحة يُستخدم عند انقطاع الشبكة أو أخطاء الخادم (5xx). /

#### `data/ImageLoader.kt`

- `object Img` — محمّل صور صغير: ذاكرة LRU + قرص + تصغير أثناء فك الترميز. - peek(): قراءة متزامنة من الذاكرة (تمنع إطاراً فارغاً عند عودة البطاقة للشاشة). - قفل لكل رابط: تنزيل واحد فقط لنفس الصورة حتى لو طلبتها بطاقتان معاً. - سقف 6 مه
- `fun RemoteImage`

#### `data/Look.kt`

- `object AP` — نظام المظهر: كل خيار مفتاح نصي واحد له قيمة افتراضية. الأنماط (ui) حزم قيم جاهزة تُكتب على المفاتيح ثم يعدّلها المستخدم بحرية. الملفات الشخصية (Presets) لقطات كاملة من هذه المفاتيح. /

#### `data/Models.kt`

- `data class Item` — عنصر محتوى موحد: movie | series | channel | episode
- `data class Section`
- `data class HomeResponse`
- `data class Category`
- `data class Page`
- `data class Episode`
- `data class Season`
- `data class Detail`
- `data class PlayInfo`
- `data class AppConfig`
- `data class HistoryEntry`
- `data class Settings`

#### `data/Prefs.kt`

- `object Prefs` — التخزين المحلي (DataStore). اخترنا DataStore بدل Room لأن البيانات المحلية صغيرة (إعدادات، مفضلة، سجل، مواضع) وهذا يوفر مئات الكيلوبايت من الحجم. /

#### `data/Repository.kt`

- `object Repo` — كل بيانات المحتوى تأتي من الـ Backend. المحلي فقط: مفضلة، سجل، مواضع، إعدادات.

#### `notify/Reminders.kt`

- `object Reminders` — إشعارات تذكير محلية بالكامل: رسائل مخزنة داخل التطبيق، جدولة WorkManager، بدون شبكة.
- `class ReminderWorker`

#### `ui/Adaptive.kt`

- `object Dm` — أبعاد ثابتة للصفحتين المباشرتين (الرئيسية والقنوات).
- `fun isSplitForm` — هل يُعرض التخطيط المقسوم (مشغل في جانب، لوحات في الجانب الآخر)؟ تلفاز، لوحي أفقي، هاتف أفقي.
- `class LiveDims` — نتيجة حساب التخطيط المباشر. split=false (هاتف عمودي، لوحي عمودي): المشغل بعرض كامل، تحته شريط "يُعرض الآن" ثم اللوحتان. split=true (تلفاز، أفقي): عمود المشغل في جانب واللوحتان في الجانب الآخر. playerW/playerH: مقاس المشغ
- `fun liveDims` — w: العرض المتاح للمحتوى، usableH: الارتفاع المتاح بعد شريط الحالة والإزاحة السفلية. wantSuggest: هل توجد اقتراحات تستحق المساحة (الرئيسية فقط). كل الأرقام بوحدات dp بعد تكبير التلفاز، فتعمل المعادلات نفسها على الهاتف وال
- `fun physicalSideInsets` — إزاحات الجانبين الفيزيائية (يسار/يمين) من أشرطة النظام وقصّة الشاشة (النوتش). تظهر في الوضع الأفقي فقط، لذلك تُطبَّق بـ absolutePadding لا start/end. /

#### `ui/Bars.kt`

- `class TopAction`
- `fun TopBar` — الشريط العلوي العائم. الأنماط: pill كبسولة | circles دوائر | square مربعات | bar شريط بعرض كامل | ghost أيقونات بلا حاوية | island جزيرة بحقل بحث | stack عمود جانبي | chips أزرار بأسماء. /
- `fun NavBar` — شريط التنقل العائم. الأنماط: labels أسماء دائمة + مؤشر سائل يتفاعل مع الضغط | compact النشط كبسولة بالاسم | icons أيقونات فقط | dock لوح بعرض كامل بخط سفلي | ios شريط تبويب كلاسيكي | round دوائر منفصلة | text أسماء فقط |

#### `ui/Chrome.kt`

- `fun ScreenHeader` — ترويسة صفحة مثبتة: العنوان في بداية السطر والإجراءات في نهايته، وتحترم شريط الحالة.
- `fun SectionHeader`
- `fun SearchField` — حقل بحث زجاجي: أيقونة، نص، وزر مسح يظهر عند وجود نص.
- `fun LoadMoreTrigger` — محفّز التحميل التدريجي عند الاقتراب من نهاية الشبكة.
- `fun CategoryChips` — صف تصنيفات: "الكل" ثم التصنيفات. extra يُعرض قبلها (مثل زر المفضلة).
- `fun SegmentedControl` — تحكم مقطعي: المؤشر ينزلق بنابض (يتبع اتجاه الواجهة).
- `fun WideCard` — بطاقة عريضة (غلاف أفقي) للكاروسيل.
- `fun ThinProgress` — شريط تقدم رفيع.

#### `ui/Components.kt`

- `fun Txt` — نص يدعم العربية واللاتينية معاً: الاتجاه يُشتق من المحتوى نفسه لا من اتجاه الواجهة.
- `fun Modifier.tvFocus` — إطار تركيز واضح للريموت. يظهر فقط عند وجود تركيز فعلي (لا يظهر مع اللمس).
- `fun Modifier.tap`
- `fun Modifier.tapHold`
- `class CoordRef` — مرجع لإحداثيات عنصر لقراءة حدوده لحظة النقر فقط (يستخدم لانتقال البطاقة إلى التفاصيل).
- `fun Modifier.trackBounds`
- `fun IconAction` — زر أيقونة بلا حاوية: منطقة لمس 40dp والأيقونة مجردة.
- `fun GlassIconButton`
- `fun Chip`
- `fun PrimaryButton`
- `fun WatchButton` — زر المشاهدة: كبسولة بلون التمييز بارتفاع 48dp؛ أيقونة التشغيل من أيقونات التطبيق والنص متوازنان في الوسط. ليس عريضاً ولا يحوي دائرة بيضاء (كانت تغطي النص). /
- `fun TonalButton` — زر ثانوي بنفس الارتفاع (اختيار الموسم والحلقة).
- `fun posterWidth` — عرض بطاقة الملصق حسب عرض النافذة.
- `fun PosterCard`
- `fun ChannelCard` — بطاقة قناة بحجم ثابت: الشعار بنسبته الأصلية (Fit) والاسم سطران كحد أقصى.
- `fun SectionTitle`
- `fun FavoriteButton` — زر القلب: نبضة نابضة قصيرة عند التبديل (بدون ارتداد مبالغ فيه).
- `fun Spinner`
- `fun ProgressLine` — خط تقدم رفيع غير محدد: يُعرض فقط أثناء تحديث المحتوى عند تغيير الفلتر.
- `fun Skeleton` — هيكل تحميل ثابت (بدون حركة مستمرة) بنفس شبكة المحتوى.
- `fun MessageBox`
- `fun <T> StateHost` — تعرض كل حالات الواجهة الموحدة. Refreshing تُعرض كمحتوى (التحديث لا يمسح الشاشة).
- `fun PullRefresh`
- `fun OfflinePill`
- `fun ConfirmDialog` — حوار تأكيد بزجاج كثيف: يدخل بتكبير هادئ مع تعتيم الخلفية.
- `fun GlassSheet` — لوحة سفلية بزجاج كثيف: تنزلق بنابض هادئ.
- `fun Modifier.aeroGlow` — إضاءة محيطية خافتة جداً تُرسم مرة واحدة (بدون نيون ولا حركة).

#### `ui/DetailScreen.kt`

- `fun DetailScreen` — صفحة التفاصيل. p هو تقدم الانتقال [0..1]. انتقال شبيه بالعنصر المشترك: صورة البطاقة (ghost) تتمدد من حدود البطاقة المضغوطة إلى مستطيل الغلاف وتنكمش عائدة عند الإغلاق، بينما بقية المحتوى يظهر تدريجياً. بدون بذرة أو حدود ي

#### `ui/Engine.kt`

- `class PlaybackEngine` — محرك التشغيل الوحيد في التطبيق: ExoPlayer + حالة Compose + إعادة اتصال + حفظ التقدم. يعيش خارج التركيب (LiveVM للبث المباشر، و remember للمسار VOD) فلا ينقطع التشغيل بتبديل الأقسام أو الدخول والخروج من ملء الشاشة، ولا يُ
- `internal class Opt`
- `internal fun fmt`
- `internal fun videoBox` — يحسب مقاس مربع الفيديو داخل الحاوية حسب وضع العرض. ratio = عرض/ارتفاع الفيديو (من VideoSize).  fit     كامل الفيديو ظاهر بنسبته (أشرطة سوداء عند الحاجة) — الافتراضي  fill    يملأ الشاشة بنسبته (قص الأطراف الزائدة)  zoom 
- `internal fun qualityOptions`
- `internal fun audioOptions`
- `internal fun subtitleOptions`

#### `ui/HomeScreen.kt`

- `internal fun inProgress` — ما يمكن متابعته: أفلام وحلقات بدأت ولم تنتهِ (لا قنوات). تستخدمها الرئيسية وصفحة الأفلام.
- `private class HomeModel`
- `fun HomeScreen`

#### `ui/Icons.kt`

- `enum class Ic`
- `fun BIcon` — مجموعة أيقونات مرسومة بالكامل بـ Canvas على شبكة 24x24، بلغة بصرية واحدة: أجسام مستديرة الزوايا، خط 1.7 بنهايات دائرية، ولمسة تمييز صغيرة (نقطة/شرارة) في كل أيقونة. النمط (من الإعدادات): line = خطوط فقط، duo = جسم شفاف +

#### `ui/ListScreens.kt`

- `fun CatalogScreen`

#### `ui/Live.kt`

- `class LiveVM` — مالك البث المباشر: المحرك والقناة الحالية وقائمة التنقل (الأخ التالي/السابق) وحالة ملء الشاشة. يعيش في ViewModel فلا ينقطع البث بتغيير الاتجاه أو التنقل بين الصفحتين. /
- `fun FullscreenWindowEffect` — يخفي أشرطة النظام في ملء الشاشة، ويقفل الاتجاه الأفقي عند الدخول بالزر على الهاتف.
- `fun AutoFullscreenEffect` — الهاتف في الوضع الأفقي يدخل ملء الشاشة تلقائياً، ويخرج عند العودة للعمودي إن كان الدخول تلقائياً. الخروج اليدوي يمنع الدخول التلقائي مجدداً حتى يعود الهاتف للوضع العمودي. /
- `fun PlayerLayer` — المشغل يعيش في طبقة واحدة فوق المحتوى بدل أن يكون داخل الصفحة، فيبقى سطح الفيديو نفسه عند التوسّع لملء الشاشة (لا يُعاد إنشاؤه ولا ينقطع). حجمه وموضعه يتبعان مستطيل الفتحة (slot) في الوضع المضمّن، ويتمدد بنابض إلى النافذ

#### `ui/LivePanes.kt`

- `data class LiveSection` — قسم في لوحة الأقسام (يسار الصفحة). items: عناصر القسم إن كانت معروفة مسبقاً (الرئيسية)، أو null إن كانت تُحمَّل من الخادم عند اختياره (صفحة القنوات). /
- `fun PanesRow` — صف اللوحتين. الواجهة RTL، لذلك أول عنصر (القنوات) يقع في اليمين، والأقسام في اليسار. /
- `fun SectionsPane`
- `fun ChannelsPane` — قائمة القنوات. القناة الجارية مميزة وتُمرَّر إلى مجال الرؤية تلقائياً عند تغيّرها. header يُعرض فوق القائمة (عنوان القسم في الرئيسية، حقل البحث في صفحة القنوات). onEnd: يُستدعى عند الاقتراب من نهاية القائمة (التحميل التد
- `fun ChannelRow` — صف قناة. شكله يتبع نمط القائمة: cards بطاقات | glass زجاج | grouped مجموعة بفواصل | plain بلا حاوية | compact مضغوط.
- `fun EqBars` — ثلاثة أعمدة تتحرك كمؤشر موازن صوتي؛ تتجمد عند الإيقاف. الحركة تُقرأ في طور الرسم فقط.
- `fun NowPlayingBar` — تحت المشغل: معلومات فقط (شعار القناة واسمها وحالتها). كل أزرار التحكم داخل المشغل نفسه فلا تتكرر أيقونات التقديم/التأخير/التشغيل/ملء الشاشة خارجه. /
- `fun SuggestStrip` — شريط واحد منفصل عن أقسام القنوات: أفلام ومسلسلات مقترحة بعناصر مضغوطة (غلاف + عنوان). الضغط يفتح التفاصيل بانتقال الغلاف من مكان البطاقة. /
- `fun ListLoadMore`

#### `ui/LiveScreen.kt`

- `fun LiveScaffold` — الصفحة المباشرة الوحيدة (الرئيسية): شريط أدوات عائم، فتحة المشغل، شريط "يُعرض الآن"، ثم لوحتا القنوات (يمين) والأقسام (يسار). المشغل نفسه يُرسم في PlayerLayer ويتبع مستطيل الفتحة. /

#### `ui/Motion.kt`

- `val RouteSpring` — نابض الانتقالات: شبه حرج الإخماد (بلا ارتداد ظاهر) وسريع الاستجابة.
- `fun RouteHost` — يستضيف شاشة مكدس ويدير تقدم انتقالها p في [0,1] بنابض واحد للدخول والخروج. يُمرَّر p كدالة لتُقرأ داخل graphicsLayer فقط (طور الرسم) فلا تحدث إعادة تركيب أثناء الحركة. /
- `fun Modifier.slideRoute` — انزلاق جانبي خفيف مع ظهور تدريجي (البحث والإعدادات). يتبع اتجاه الواجهة.

#### `ui/Nav.kt`

- `data class PlayEntry`
- `data class PlayReq`
- `sealed interface Route`
- `data class NavEntry` — عنصر في مكدس التنقل. closing=true يعني أن شاشة الخروج تعمل حركتها العكسية، ولا يُزال العنصر فعلياً إلا بعد انتهائها (يعطي انتقالاً متصلاً بدل الاختفاء المفاجئ). origin: حدود البطاقة المضغوطة (إحداثيات النافذة) لانتقال ال
- `class NavVM` — تنقل مخصص: 3 أقسام رئيسية + مكدس صفحات. يعيش في ViewModel فيبقى عبر تغيير الاتجاه.
- `fun openItem` — يفتح عنصراً: القناة تشغَّل مباشرة، وغيرها يفتح صفحة التفاصيل (origin = حدود البطاقة للانتقال).

#### `ui/PlayerScreen.kt`

- `fun PlayerScreen` — صفحة تشغيل الأفلام والحلقات (مسار في المكدس) فوق كل شيء. تملك محركها الخاص (PlaybackEngine) وتحرّره عند الإغلاق؛ البث المباشر له محرك مستقل في LiveVM. الواجهة نفسها (PlayerUi) تُستخدم هنا بوضع Route. /

#### `ui/PlayerUi.kt`

- `enum class PlayerMode` — Inline: مضمّن داخل الصفحة. Fullscreen: ملء الشاشة للبث المباشر. Route: صفحة تشغيل VOD كاملة.
- `private enum class Menu`
- `fun PlayerUi` — واجهة المشغل الواحدة لكل الأوضاع: سطح الفيديو + أدوات التحكم + الإيماءات + مفاتيح الريموت. هي نفسها تبقى في الشجرة عند الانتقال بين المضمّن وملء الشاشة (يتغير الحجم فقط)، فلا يُعاد إنشاء سطح الفيديو ولا ينقطع التشغيل. on
- `fun PlayerOptionsSheet` — محتوى ورقة "المزيد" للمشغل المضمّن: الجودة، الصوت، الترجمة، وضع العرض.

#### `ui/Root.kt`

- `val LocalBottomPad` — الإزاحة السفلية التي يجب أن تتركها الشاشات الرئيسية (شريط التنقل + شريط النظام).
- `fun Root`

#### `ui/SearchScreen.kt`

- `fun SearchScreen` — البحث: type يحدد النوع المبدئي (أفلام/مسلسلات من صفحة الأفلام، أو الكل من الرئيسية).

#### `ui/SettingsScreen.kt`

- `fun SettingsScreen`

#### `ui/Strings.kt`

- `object S` — نصوص الواجهة (عربي). مفصولة عن الكود لتسهيل الترجمة لاحقاً دون XML.

#### `ui/SystemBars.kt`

- `fun Context.findActivity`
- `fun SystemBarsEffect` — يطابق شريطي النظام وخلفية النافذة مع السمة الحالية. لا يغيّر وضع التنقل (إيماءات أو ثلاثة أزرار): الشريطان شفافان والمحتوى يرسم خلفهما، والإزاحات تُطبَّق في الواجهة عبر WindowInsets فيعمل الوضعان بلا تعديل. playerOn: الم

#### `ui/Theme.kt`

- `class BeoutColors`
- `val DarkColors`
- `val LightColors`
- `val BlueColors`
- `val SlateColors` — رمادي سينمائي (المرجع الأول): سلات رمادي مزرق وزجاج رمادي.
- `val VioletColors` — بنفسجي داكن (المرجع الثاني).
- `val GraphiteColors` — درجات رمادية: من الفحمي الدافئ حتى الأسود النقي.
- `val CharcoalColors`
- `val AmoledColors`
- `val AshColors`
- `val ForestColors`
- `val WineColors`
- `val SandColors`
- `fun colorsFor`
- `val ThemeList` — كل الثيمات بترتيب العرض (المفتاح، الاسم).
- `private class AccentSet`
- `val AccentList` — مفاتيح اللون المميز مع الاسم؛ "theme" يعني لون الثيم نفسه.
- `fun accentColor`
- `fun withAccent`
- `class Look`
- `val LocalLook`
- `fun lookOf`
- `val LocalColors`
- `object T`
- `val GlassShape`
- `val CardShape`
- `fun Modifier.glass` — سطح زجاجي خفيف: تدرج شبه شفاف بلا Blur (رخيص). يتأثر بقوة الزجاج والظل والحافة والعمق.
- `fun Modifier.glassDense` — زجاج كثيف: قاعدة شبه معتمة. للشرائط العائمة واللوحات فوق الفيديو.
- `fun Modifier.flat` — سطح المحتوى العادي (بطاقات، صفوف). يتبع العمق: ظل، لمعة علوية، حافة، ووضوح الطبقة.
- `fun Modifier.tinted` — حالة التحديد: صبغة زرقاء شفافة وحافة رفيعة (بدل التعبئة الصلبة والتوهج).
- `fun Modifier.screenBackground`
- `fun Modifier.accentFill` — تعبئة اللون الأساسي: تدرج رأسي هادئ بين درجتين متقاربتين (لا نيون).

#### `ui/UiMode.kt`

- `val LocalTv`
- `fun detectTv` — كشف التلفاز بعدة إشارات لا بعرض الشاشة وحده: نمط واجهة التلفاز، ميزة leanback، غياب الشاشة اللمسية، وجود D-pad، ثم عرض النافذة. /
- `fun ProvideUiMode` — يطبّق وضع العرض عبر CompositionLocal فقط (لا يعيد إنشاء المشغل ولا يمسح بيانات). وضع التلفاز محاكاة: الكثافة تُحسب لتصير نافذة الجهاز بمقاس لوحة تلفاز ثابتة (960x540dp، أفقية)، فيبدو الهاتف كشاشة تلفاز بنفس النسب والحجم 

#### `ui/ViewModels.kt`

- `class AppVM`
- `class HomeVM`
- `class ChannelsVM`
- `class CatalogVM`
- `class SearchVM`
- `class DetailVM`

### 3.6 البناء
```
cd android && ./gradlew assembleDebug
```
ملاحظة: لم يُبنَ المشروع في بيئة العمل (لا Android SDK)؛ تحقق من الصياغة فقط. توقع أخطاء ترجمة عند أول بناء.


## 4. Backend

### 4.1 المسارات

- `/v1/home` — GET `home`
- `/v1/config` — GET `config`
- `/v1/categories` — GET `categories`
- `/v1/channels` — GET `channels`
- `/v1/catalog` — GET `catalog`
- `/v1/items/:id` — GET `detail`
- `/v1/search` — GET `search`
- `/v1/search/suggest` — GET `suggest`
- `/v1/play/:kind/:id` — GET `play`
- `/v1/sync/history` — POST `sync_history`
- `/v1/sync/favorites` — POST `sync_favorites`
- `/admin/logout` — POST `logout`
- `/admin/stats` — GET `stats`
- `/admin/items` — GET `items_list` ، POST `item_create`
- `/admin/items/:id` — PUT `item_update` ، DELETE `item_delete`
- `/admin/items/:id/seasons` — GET `seasons_list` ، POST `season_create`
- `/admin/seasons/:id` — PUT `season_update` ، DELETE `season_delete`
- `/admin/seasons/:id/episodes` — POST `episode_create`
- `/admin/episodes/:id` — PUT `episode_update` ، DELETE `episode_delete`
- `/admin/categories` — GET `cats_list` ، POST `cat_create`
- `/admin/categories/:id` — PUT `cat_update` ، DELETE `cat_delete`
- `/admin/home-sections` — GET `home_get` ، PUT `home_put`
- `/admin/config` — GET `config_get` ، PUT `config_put`
- `/admin/users` — GET `users_list`
- `/admin/users/:id` — PUT `user_update`
- `/admin/users/:id/subscription` — POST `sub_grant`
- `/admin/notifications` — GET `notif_list` ، POST `notif_create`
- `/admin/ads` — GET `ads_list` ، POST `ad_create`
- `/admin/ads/:id` — PUT `ad_update` ، DELETE `ad_delete`
- `/admin/login` — POST `login`
- `/health`

### 4.2 الترحيلات

- `0001_init.sql` — جداول: users, subscriptions, categories, items, seasons, episodes, home_sections, app_config, favorites, history, notifications, ads, view_stats
- `0002_home_cleanup.sql`
- `0003_home_live.sql`

### 4.3 المتغيرات البيئية

- `ADMIN_EMAIL`
- `ADMIN_ORIGIN`
- `ADMIN_PASSWORD`
- `BIND`
- `DATABASE_URL`
- `DB_POOL`
- `LOGIN_PER_MIN`
- `RATE_PER_MIN`
- `REDIS_URL`
- `STREAM_SIGN_SECRET`
- `STREAM_SIGN_TTL`

أنواع أقسام الرئيسية: `hero | row | channels | continue | categories | suggest`. مصادر الأقسام تُختار من اللوحة (منها `suggested` لاقتراح الأفلام والمسلسلات).


## 5. لوحة التحكم

- `Ads.tsx` — 
- `Categories.tsx` — 
- `Config.tsx` — 
- `HomeLayout.tsx` — 
- `Items.tsx` — 
- `Notifications.tsx` — 
- `Stats.tsx` — 
- `Users.tsx` — 

```
cd dashboard && npm i && npm run dev   # أو npm run build
```

## 6. التشغيل السريع
```
docker compose up -d        # PostgreSQL + Redis + backend
cd dashboard && npm i && npm run build
cd android && ./gradlew assembleDebug
```

