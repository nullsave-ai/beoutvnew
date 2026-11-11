package com.beouttv.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beouttv.app.BuildConfig
import com.beouttv.app.data.AP
import com.beouttv.app.data.Img
import com.beouttv.app.notify.Reminders
import kotlinx.coroutines.launch

// ---------- لبنات الإعدادات بأسلوب iOS: عنوان كبير، مجموعات مدورة، صفوف بأيقونات ملونة، فواصل رفيعة ----------

@Composable
private fun Sec(title: String? = null, footer: String? = null, rows: List<@Composable () -> Unit>) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        title?.let { Txt(it, Modifier.padding(horizontal = 14.dp), T.small, T.c.textDim) }
        Column(Modifier.fillMaxWidth().flat(RoundedCornerShape(16.dp))) {
            rows.forEachIndexed { i, r ->
                r()
                if (i < rows.lastIndex) Box(Modifier.padding(start = 14.dp).fillMaxWidth().height(0.5.dp).background(T.c.edgeLo))
            }
        }
        footer?.let { Txt(it, Modifier.padding(horizontal = 14.dp), T.small, T.c.textDim) }
    }
}

@Composable
private fun IconSquare(ic: Ic, color: Color) {
    Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(color), contentAlignment = Alignment.Center) {
        BIcon(ic, size = 18.dp, tint = Color.White, active = true)
    }
}

@Composable
private fun NavRow(ic: Ic, color: Color, title: String, value: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().tvFocus().tap(onClick).padding(horizontal = 14.dp, vertical = (11 * T.look.space).dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconSquare(ic, color)
        Txt(title, Modifier.weight(1f), T.body)
        value?.let { Txt(it, style = T.small, color = T.c.textDim, maxLines = 1) }
        BIcon(Ic.Next, size = 16.dp, tint = T.c.textDim)
    }
}

@Composable
private fun ToggleRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val x by animateDpAsState(if (on) 20.dp else 0.dp, spring(dampingRatio = 0.85f, stiffness = 600f), label = "knob")
    Row(Modifier.fillMaxWidth().tvFocus().tap { onChange(!on) }.padding(horizontal = 14.dp, vertical = (11 * T.look.space).dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(label, Modifier.weight(1f), T.body)
        val shape = RoundedCornerShape(50)
        Box(Modifier.size(46.dp, 26.dp).then(if (on) Modifier.accentFill(shape) else Modifier.background(T.c.edgeLo, shape)).padding(3.dp)) {
            Box(Modifier.align(Alignment.CenterStart).offset(x = x).size(20.dp).clip(shape).background(Color.White))
        }
    }
}

@Composable
private fun CheckRow(label: String, sub: String? = null, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().tvFocus().tap(onClick).padding(horizontal = 14.dp, vertical = (11 * T.look.space).dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Txt(label, style = T.body, color = if (selected) T.c.accent2 else T.c.text)
            sub?.let { Txt(it, style = T.small, color = T.c.textDim) }
        }
        if (selected) BIcon(Ic.Check, size = 20.dp, tint = T.c.accent2, active = true)
    }
}

@Composable
private fun Choice(vm: AppVM, title: String?, key: String, cur: String, list: List<Pair<String, String>>, footer: String? = null) =
    Sec(title, footer, list.map { (k, l) -> { CheckRow(l, null, cur == k) { vm.setStr(key, k) } } })

@Composable
private fun ResetSec(vm: AppVM, group: String, label: String = "إعادة الافتراضي لهذه المجموعة") =
    Sec(rows = listOf({ Row(Modifier.fillMaxWidth().tvFocus().tap { vm.resetGroup(group) }.padding(14.dp)) { Txt(label, style = T.body, color = T.c.danger) } }))

private val sml = listOf("s" to S.sSmall, "m" to S.sMed, "l" to S.sLarge)
private val styleNames = listOf(
    Triple("cinema", "سينمائي بسيط", "صور كبيرة، بطاقات بعنوان فوق الصورة، عناصر تحكم قليلة"),
    Triple("glass", "زجاج ناعم", "شفافية خفيفة وعمق بصري ناعم وبطاقات غير مبالغ فيها"),
    Triple("ios", "مستوحى من iOS", "قوائم مجمّعة، مساحات مريحة، شريط تبويب كلاسيكي"),
    Triple("flat", "مسطح حديث", "كثافة محتوى أعلى، زوايا صغيرة، واجهة عملية"),
    Triple("ambient", "عمق محيطي", "طبقات وظلال ناعمة وعناصر عائمة محدودة"),
)
private val navList = listOf(
    "labels" to "أسماء دائمة (سائل)", "compact" to "كبسولة نشطة", "icons" to "أيقونات", "dock" to "لوح سفلي",
    "ios" to "شريط تبويب iOS", "round" to "دوائر منفصلة", "text" to "نصوص فقط", "bubble" to "فقاعة نشطة",
)
private val tbList = listOf(
    "pill" to "كبسولة", "circles" to "دوائر", "square" to "مربعات", "bar" to "شريط كامل",
    "ghost" to "أيقونات شفافة", "island" to "جزيرة بحث", "stack" to "عمود جانبي", "chips" to "أزرار بأسماء",
)
private val navTitles = mapOf(
    "main" to "الإعدادات", "styles" to "أنماط الواجهة", "theme" to "الثيم والألوان", "nav" to "التنقل والشريط العلوي",
    "depth" to "العمق والبطاقات", "layout" to "التخطيط والكثافة", "presets" to "ملفات التصميم",
    "player" to "المشغل", "storage" to "التخزين", "about" to "حول",
)

@Composable
fun SettingsScreen(vm: AppVM, nav: NavVM) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val presets by vm.presets.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var page by rememberSaveable { mutableStateOf("main") }
    var cacheMb by remember { mutableStateOf(Img.sizeBytes() / 1_048_576f) }
    var askCache by remember { mutableStateOf(false) }
    var askHistory by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    BackHandler(page != "main") { page = "main" }

    Box(Modifier.fillMaxSize().screenBackground()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassIconButton(Ic.Back, { if (page == "main") nav.pop() else page = "main" })
                if (page != "main") Txt(navTitles[page] ?: "", style = T.h2)
            }
            if (page == "main") Txt(S.settings, Modifier.padding(horizontal = 20.dp, vertical = 2.dp), androidx.compose.ui.text.TextStyle(fontSize = 32.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, lineHeight = 42.sp))
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                when (page) {
                    "main" -> {
                        item {
                          Column {
                            Sec(null, null, listOf(
                                { NavRow(Ic.Aspect, Color(0xFF8E6CF0), "أنماط الواجهة", styleNames.firstOrNull { it.first == s.ui }?.second) { page = "styles" } },
                                { NavRow(Ic.Quality, Color(0xFFE2873A), "الثيم والألوان", ThemeList.firstOrNull { it.first == s.theme }?.second) { page = "theme" } },
                                { NavRow(Ic.Home, Color(0xFF3F8EE8), "التنقل والشريط العلوي") { page = "nav" } },
                                { NavRow(Ic.Movies, Color(0xFF2FB58C), "العمق والبطاقات") { page = "depth" } },
                                { NavRow(Ic.Channels, Color(0xFFE25B7B), "التخطيط والكثافة") { page = "layout" } },
                                { NavRow(Ic.Heart, Color(0xFFD4AF37), "ملفات التصميم", if (presets.isEmpty()) null else presets.size.toString()) { page = "presets" } },
                            ))
                            Sec(null, null, listOf(
                                { NavRow(Ic.Play, Color(0xFF4C84D4), "المشغل") { page = "player" } },
                                { ToggleRow(S.notifications, s.notifications) { on -> vm.setNotifications(on) { Reminders.apply(ctx, it) } } },
                                { NavRow(Ic.Download, Color(0xFF7E97A4), "التخزين", "%.1f MB".format(cacheMb)) { page = "storage" } },
                                { NavRow(Ic.More, Color(0xFF8C8C95), "حول", BuildConfig.VERSION_NAME) { page = "about" } },
                            ))
                            Sec(S.displayMode, S.displayModeHint, listOf({
                                val modes = listOf("auto", "phone", "tv")
                                Box(Modifier.padding(10.dp)) {
                                    SegmentedControl(listOf(S.modeAuto, S.modePhone, S.modeTv), modes.indexOf(s.uiMode).coerceAtLeast(0), { vm.setUiMode(modes[it]) })
                                }
                            }))
                          }
                        }
                    }
                    "styles" -> item { Column {
                        Sec("كل نمط يغيّر التخطيط وشكل البطاقات والقوائم والتنقل، وبعدها يمكنك تخصيص كل شيء",
                            null, styleNames.map { (k, n, d) -> { CheckRow(n, d, s.ui == k) { vm.applyStyle(k) } } })
                        Sec(rows = listOf({ Row(Modifier.fillMaxWidth().tvFocus().tap { vm.undoAp() }.padding(14.dp)) { Txt("العودة إلى التصميم السابق", style = T.body, color = T.c.accent2) } }))
                    } }
                    "theme" -> item { Column {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Txt("الثيم (اضغط للمعاينة والتطبيق فوراً)", Modifier.padding(horizontal = 14.dp), T.small, T.c.textDim)
                            ThemeList.chunked(4).forEach { rowItems ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    rowItems.forEach { (k, n) -> ThemeTile(k, n, s.theme == k, Modifier.weight(1f)) { vm.applyAp(mapOf("theme" to k)) } }
                                    repeat(4 - rowItems.size) { Box(Modifier.weight(1f)) }
                                }
                            }
                            Txt("اللون المميز", Modifier.padding(start = 14.dp, top = 8.dp), T.small, T.c.textDim)
                            AccentList.chunked(5).forEach { rowItems ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    rowItems.forEach { (k, n) -> AccentDot(k, n, s.accent == k, Modifier.weight(1f)) { vm.setStr("accent", k) } }
                                    repeat(5 - rowItems.size) { Box(Modifier.weight(1f)) }
                                }
                            }
                        }
                        ResetSec(vm, "theme")
                    } }
                    "nav" -> {
                        item {
                            var pv by remember { mutableIntStateOf(0) }
                            Column(Modifier.padding(16.dp).fillMaxWidth().flat(RoundedCornerShape(16.dp)).padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Txt("معاينة مباشرة", style = T.small, color = T.c.textDim)
                                TopBar(s.toolbarStyle, listOf(TopAction(Ic.Search, {}), TopAction(Ic.Settings, {})))
                                NavBar(s.navStyle, pv, { pv = it })
                            }
                        }
                        item { Choice(vm, "شريط التنقل", "navStyle", s.navStyle, navList) }
                        item { Choice(vm, "الشريط العلوي", "toolbarStyle", s.toolbarStyle, tbList) }
                        item { ResetSec(vm, "nav") }
                    }
                    "depth" -> {
                        item {
                            Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Txt("معاينة البطاقات", Modifier.padding(horizontal = 14.dp), T.small, T.c.textDim)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(Modifier.weight(1f).height(86.dp).flat(T.card).padding(12.dp)) { Txt("بطاقة", style = T.h2) }
                                    Box(Modifier.weight(1f).height(86.dp).glass(T.card).padding(12.dp)) { Txt("زجاج", style = T.h2) }
                                    Box(Modifier.weight(1f).height(86.dp).accentFill(T.card).padding(12.dp)) { Txt("تمييز", style = T.h2, color = Color.White) }
                                }
                            }
                        }
                        item {
                            Sec("درجة العمق", null, listOf("min" to "خفيف جداً", "bal" to "متوازن", "ref" to "عمق مصقول", "rich" to "غني").map { (k, l) -> { CheckRow(l, null, s.depth == k) { vm.applyDepth(k) } } })
                        }
                        item { Choice(vm, "قوة الزجاج", "glass", s.glass, sml) }
                        item { Choice(vm, "شدة الظلال", "shadow", s.shadow, listOf("0" to "بلا", "1" to "خفيفة", "2" to "متوسطة", "3" to "قوية")) }
                        item { Choice(vm, "سماكة الحدود", "borderW", s.borderW, listOf("0" to "بلا", "1" to "رفيعة", "2" to "سميكة")) }
                        item { Choice(vm, "عمق البطاقات", "cardDepth", s.cardDepth, listOf("0" to "مسطحة", "1" to "لمعة خفيفة", "2" to "لمعة واضحة")) }
                        item { Choice(vm, "وضوح الطبقات", "clarity", s.clarity, sml) }
                        item { ResetSec(vm, "depth") }
                    }
                    "layout" -> {
                        item { Choice(vm, "الرئيسية", "homeLayout", s.homeLayout, listOf("hero" to S.hlHero, "rows" to S.hlRows)) }
                        item { Choice(vm, "تخطيط القنوات", "chLayout", s.chLayout, listOf("auto" to S.chAuto, "stacked" to S.chStacked, "split" to S.chSplit)) }
                        item { Choice(vm, "نمط قائمة القنوات", "listStyle", s.listStyle, listOf("cards" to "بطاقات", "glass" to "زجاج", "grouped" to "مجموعة بفواصل", "plain" to "بلا حاوية", "compact" to "مضغوط")) }
                        item { Choice(vm, "بطاقات الأفلام", "cardStyle", s.cardStyle, listOf("below" to "العنوان تحت الصورة", "overlay" to "العنوان فوق الصورة")) }
                        item { Choice(vm, "المسافات الداخلية", "space", s.space, sml) }
                        item { Choice(vm, "كثافة العناصر", "density", s.density, sml) }
                        item { Choice(vm, "انحناء الزوايا", "radius", s.radius, sml) }
                        item { Choice(vm, "حجم الملصقات", "posterSize", s.posterSize, sml) }
                        item { Choice(vm, "نمط الأيقونات", "iconStyle", s.iconStyle, listOf("line" to S.iconLine, "duo" to S.iconDuo, "solid" to S.iconSolid)) }
                        item { Choice(vm, "حجم الخط", "fontScale", s.fontScale, sml) }
                        item { ResetSec(vm, "layout") }
                    }
                    "presets" -> item { Column { PresetsPage(vm, presets) } }
                    "player" -> {
                        item { Choice(vm, "أزرار المشغل", "playerLayout", s.playerLayout, listOf("classic" to "كلاسيكي (أزرار في الوسط)", "bar" to "شريط تحكم سفلي"), "كل الأزرار داخل المشغل نفسه") }
                        item {
                            val fits = listOf("fit" to S.fitFit, "fill" to S.fitFill, "zoom" to S.fitZoom, "stretch" to S.fitStretch, "auto" to S.fitAuto)
                            Sec(S.videoDisplay, null, fits.map { (k, l) -> { CheckRow(l, null, s.videoFit == k) { vm.setVideoFit(k) } } })
                        }
                        item {
                            Sec(S.videoQuality, null, listOf(0 to S.auto, 480 to "480p", 720 to "720p", 1080 to "1080p").map { (h, l) -> { CheckRow(l, null, s.maxHeight == h) { vm.setMaxHeight(h) } } })
                        }
                        item {
                            Sec(null, null, listOf(
                                { ToggleRow(S.resumePlayback, s.resume) { vm.setResume(it) } },
                                { ToggleRow(S.keepScreen, s.keepScreenOn) { vm.setKeepScreen(it) } },
                                { ToggleRow(S.longPressBoost, s.longPressBoost) { vm.setLongPressBoost(it) } },
                                { ToggleRow(S.autoPlayLive, s.autoPlay) { vm.setAutoPlay(it) } },
                                { ToggleRow(S.autoFullscreen, s.autoFullscreen) { vm.setAutoFullscreen(it) } },
                            ))
                        }
                        item { ResetSec(vm, "player", "إعادة أزرار المشغل للافتراضي") }
                    }
                    "storage" -> item {
                        Sec(S.cache, "%.1f MB".format(cacheMb), listOf(
                            { Row(Modifier.fillMaxWidth().tvFocus().tap { askCache = true }.padding(14.dp)) { Txt(S.clearCache, style = T.body, color = T.c.danger) } },
                            { Row(Modifier.fillMaxWidth().tvFocus().tap { askHistory = true }.padding(14.dp)) { Txt(S.clearHistory, style = T.body, color = T.c.danger) } },
                        ))
                    }
                    else -> item {
                        Sec(null, null, listOf(
                            { Row(Modifier.fillMaxWidth().padding(14.dp)) { Txt(S.version, Modifier.weight(1f), T.body); Txt(BuildConfig.VERSION_NAME, style = T.body, color = T.c.textDim) } },
                            { Row(Modifier.fillMaxWidth().tvFocus().tap { showPrivacy = true }.padding(14.dp)) { Txt(S.privacy, style = T.body) } },
                            { Row(Modifier.fillMaxWidth().tvFocus().tap { showTerms = true }.padding(14.dp)) { Txt(S.terms, style = T.body) } },
                        ))
                    }
                }
            }
        }

        ConfirmDialog(askCache, S.clearCache, S.clearCacheConfirm, S.delete, { vm.clearCache(); cacheMb = 0f; askCache = false }, { askCache = false })
        ConfirmDialog(askHistory, S.clearHistory, S.clearHistoryConfirm, S.delete, { vm.clearHistory(); askHistory = false }, { askHistory = false })
        GlassSheet(showPrivacy, { showPrivacy = false }) {
            Txt(S.privacy, style = T.h2)
            Txt(S.privacyText, style = T.body, color = T.c.textDim)
        }
        GlassSheet(showTerms, { showTerms = false }) {
            Txt(S.terms, style = T.h2)
            Txt(S.termsText, style = T.body, color = T.c.textDim)
        }
    }
}

/** بلاطة ثيم: معاينة مصغرة بألوان الثيم الفعلية (خلفية، شريط، لون مميز). */
@Composable
private fun ThemeTile(key: String, name: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = colorsFor(key)
    val shape = RoundedCornerShape(14.dp)
    Column(modifier.tvFocus(shape).tap(onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.fillMaxWidth().height(64.dp).clip(shape).background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
                .then(if (selected) Modifier.border(2.dp, T.c.accent2, shape) else Modifier),
        ) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).size(30.dp, 8.dp).background(c.dense, RoundedCornerShape(50)))
            Box(Modifier.align(Alignment.TopStart).padding(8.dp).size(16.dp, 6.dp).background(c.accent, RoundedCornerShape(50)))
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(10.dp, 6.dp).background(c.text.copy(alpha = 0.6f), RoundedCornerShape(50)))
        }
        Txt(name, style = T.small, color = if (selected) T.c.accent2 else T.c.textDim, maxLines = 1)
    }
}

@Composable
private fun AccentDot(key: String, name: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val col = accentColor(key) ?: T.c.accent
    val shape = RoundedCornerShape(50)
    Column(modifier.tvFocus(shape).tap(onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(36.dp).clip(shape).background(col).then(if (selected) Modifier.border(2.5.dp, T.c.text, shape) else Modifier), contentAlignment = Alignment.Center) {
            if (selected) BIcon(Ic.Check, size = 18.dp, tint = Color.White, active = true)
        }
        Txt(name, style = T.small, color = if (selected) T.c.text else T.c.textDim, maxLines = 1)
    }
}

@Composable
private fun PresetsPage(vm: AppVM, presets: Map<String, Map<String, String>>) {
    var name by remember { mutableStateOf("") }
    var imp by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    val clip = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    Sec("حفظ التصميم الحالي كاملاً (الثيم، النمط، التنقل، الشريط العلوي، البطاقات، الكثافة، الزجاج)", null, listOf(
        { Box(Modifier.padding(8.dp)) { SearchField(name, { name = it }, "اسم الملف، مثل: My Cinematic") } },
        { Row(Modifier.fillMaxWidth().tvFocus().tap { if (name.isNotBlank()) { vm.savePreset(name.trim()); name = "" } }.padding(14.dp)) { Txt("حفظ", style = T.body, color = T.c.accent2) } },
    ))
    if (presets.isNotEmpty()) Sec("ملفاتك", "الضغط يطبّق الملف كاملاً دون ضبط كل خيار يدوياً", presets.keys.sorted().map { n ->
        {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt(n, Modifier.weight(1f).tvFocus().tap { vm.applyAp(presets.getValue(n)) }.padding(vertical = 8.dp), T.body, T.c.text, maxLines = 1)
                Chip(S.delete, false, { vm.deletePreset(n) }, compact = true)
            }
        }
    })
    Sec(null, null, listOf { Row(Modifier.fillMaxWidth().tvFocus().tap { vm.undoAp() }.padding(14.dp)) { Txt("العودة إلى التصميم السابق", style = T.body, color = T.c.accent2) } })
    Sec("تصدير واستيراد", msg, listOf(
        { Row(Modifier.fillMaxWidth().tvFocus().tap { scope.launch { clip.setText(AnnotatedString(vm.exportAp())); msg = "نُسخ إلى الحافظة" } }.padding(14.dp)) { Txt("نسخ إعدادات المظهر", style = T.body) } },
        { Box(Modifier.padding(8.dp)) { SearchField(imp, { imp = it }, "الصق الإعدادات هنا") } },
        { Row(Modifier.fillMaxWidth().tvFocus().tap { msg = if (vm.importAp(imp)) "تم التطبيق" else "نص غير صالح"; imp = "" }.padding(14.dp)) { Txt("استيراد وتطبيق", style = T.body, color = T.c.accent2) } },
    ))
}
