package com.beouttv.app.ui

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.beouttv.app.data.AP
import com.beouttv.app.data.BUILTIN_PRESETS
import com.beouttv.app.data.CAROUSEL_PRESETS
import com.beouttv.app.data.CAROUSEL_SPECS
import com.beouttv.app.data.CARD_SPECS
import com.beouttv.app.data.CARD_TYPES
import com.beouttv.app.data.CARD_TYPE_NAMES
import com.beouttv.app.data.GLASS_PRESETS
import com.beouttv.app.data.GLASS_SPECS
import com.beouttv.app.data.Item
import com.beouttv.app.data.Settings
import com.beouttv.app.data.carPreset
import com.beouttv.app.data.glassPreset
import com.beouttv.app.optical.NativeEngine

/** صفحات BEOUT Design Studio: كل قسم يكتب مفاتيح المظهر نفسها التي يقرؤها Root فتتحدث الواجهة فوراً. */
internal val studioList = listOf(
    Triple("ds_mode", "وضع التصميم · Design Mode", Ic.Aspect),
    Triple("ds_glass", "استوديو الزجاج · Glass", Ic.Quality),
    Triple("ds_car", "استوديو الكاروسيل · Carousel", Ic.Movies),
    Triple("ds_cards", "استوديو البطاقات · Cards", Ic.Heart),
    Triple("ds_nav", "شريط التنقل · Navigation", Ic.Home),
    Triple("ds_tb", "الشريط العلوي · Toolbar", Ic.Search),
    Triple("ds_btn", "الأزرار والحوارات والبحث · Buttons", Ic.Play),
    Triple("ds_typo", "الخطوط والأيقونات · Typography", Ic.Subtitle),
    Triple("ds_layout", "التخطيط والكثافة · Layout", Ic.Channels),
    Triple("ds_anim", "الحركة · Animation", Ic.Forward10),
    Triple("ds_bg", "الخلفية · Background", Ic.Fullscreen),
    Triple("ds_theme", "الثيم والألوان · Theme", Ic.Audio),
    Triple("ds_perf", "الأداء · Performance", Ic.Settings),
    Triple("ds_presets", "مكتبة الأنماط · Presets", Ic.Download),
)

internal val studioTitles: Map<String, String> = mapOf("studio" to "BEOUT Design Studio") + studioList.associate { it.first to it.second }

private val studioColors = listOf(
    0xFFD4AF37, 0xFF3F8EE8, 0xFF8E6CF0, 0xFFE25B7B, 0xFF2FB58C, 0xFFE2873A, 0xFF4C84D4,
    0xFF7E97A4, 0xFFB04A8F, 0xFF3FB5C9, 0xFF6C5CE0, 0xFF9AB93A, 0xFF8C8C95, 0xFFD4AF37,
)

internal fun LazyListScope.studioPage(page: String, setPage: (String) -> Unit, vm: AppVM, s: Settings, presets: Map<String, Map<String, String>>) {
    when (page) {
        "studio" -> item {
            Column {
                Sec(
                    "كل معامل هنا يغيّر الواجهة فعلياً. التصميم الأصلي يبقى متاحاً دائماً، وOptical اختياري.", null,
                    studioList.mapIndexed { i, (id, title, ic) ->
                        @Composable { NavRow(ic, Color(studioColors[i % studioColors.size]), title, studioValue(id, s)) { setPage(id) } }
                    },
                )
            }
        }
        "ds_mode" -> modePage(vm, s)
        "ds_glass" -> glassPage(vm, s)
        "ds_car" -> carPage(vm, s)
        "ds_cards" -> cardsPage(vm, s)
        "ds_nav" -> navPage(vm, s)
        "ds_tb" -> tbPage(vm, s)
        "ds_btn" -> btnPage(vm, s)
        "ds_typo" -> typoPage(vm, s)
        "ds_layout" -> layoutPage(vm, s)
        "ds_anim" -> animPage(vm, s)
        "ds_bg" -> bgPage(vm, s)
        "ds_theme" -> themePage(vm, s)
        "ds_perf" -> perfPage(vm, s)
        "ds_presets" -> presetsPage(vm, presets)
    }
}

private fun studioValue(id: String, s: Settings): String? {
    val ap = s.ap
    return when (id) {
        "ds_mode" -> if (ap["mode"] == "optical") "Optical" else "Original"
        "ds_glass" -> GLASS_PRESETS.firstOrNull { it.first == (ap["glassPreset"] ?: "pure") }?.second
        "ds_car" -> CAROUSEL_PRESETS.firstOrNull { it.first == (ap["carPreset"] ?: "classic3d") }?.second
        "ds_nav" -> navList.firstOrNull { it.first == s.navStyle }?.second
        "ds_tb" -> tbList.firstOrNull { it.first == s.toolbarStyle }?.second
        "ds_theme" -> ThemeList.firstOrNull { it.first == s.theme }?.second
        "ds_perf" -> Perf.names.firstOrNull { it.first == ap["perf"] }?.second ?: "تلقائي"
        else -> null
    }
}

// ---------------- 1. وضع التصميم ----------------
private fun LazyListScope.modePage(vm: AppVM, s: Settings) {
    val ap = s.ap
    val optical = ap["mode"] == "optical"
    item(key = "m1") {
        Column {
            Sec(
                "وضع التصميم", "Original هو التصميم الحالي ويبقى متاحاً دائماً. Optical تجربة زجاجية بصرية اختيارية.",
                listOf(
                    @Composable { CheckRow("Original — التصميم الحالي", "زجاج خفيف بلا طبقات بصرية إضافية", !optical) { vm.setStr("mode", "original") } },
                    @Composable { CheckRow("Optical — التجربة البصرية", "انكسار وانعكاس وفرينل وطبقات حسب مستوى الأداء", optical) { vm.setStr("mode", "optical") } },
                ),
            )
            Sec(
                null, "يطبّق الزجاج البصري على البطاقات وصفوف القوائم أيضاً. أثقل على الأجهزة الضعيفة، ويُخفَّض تلقائياً حسب مستوى الأداء.",
                listOf(@Composable { ToggleRow("Full Optical", ap["fullOptical"] == "1") { vm.setStr("fullOptical", if (it) "1" else "0") } }),
            )
            Sec(
                null, null,
                listOf(
                    @Composable { ActionRow("تطبيق Default Optical (يعيد بقية الخيارات للافتراضي)", accent = true) { vm.applyAp(AP.def + BUILTIN_PRESETS.getValue("Default Optical")) } },
                    @Composable { ActionRow("استعادة التصميم الأصلي (الافتراضيات)") { vm.applyAp(AP.def + AP.styleBundle("glass")) } },
                    @Composable { ActionRow("العودة إلى التصميم السابق") { vm.undoAp() } },
                ),
            )
        }
    }
}

// ---------------- 2. الزجاج ----------------
private fun LazyListScope.glassPage(vm: AppVM, s: Settings) {
    val ap = s.ap
    val optical = ap["mode"] == "optical"
    val preset = ap["glassPreset"] ?: "pure"
    item(key = "gp") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassPreview(optical)
            if (!optical) Row { Chip("تفعيل وضع Optical", false, { vm.setStr("mode", "optical") }) }
        }
    }
    item(key = "gpresets") {
        ChipsRow("نمط الزجاج", GLASS_PRESETS.map { it.second }, GLASS_PRESETS.indexOfFirst { it.first == preset }.coerceAtLeast(0)) { i ->
            vm.applyAp(mapOf("glassPreset" to GLASS_PRESETS[i].first) + GLASS_SPECS.associate { it.key to "" })
        }
    }
    specItems(vm, ap, GLASS_SPECS, glassPreset(preset), "g")
    item(key = "gcolors") {
        Sec(
            "ألوان الزجاج (فارغ = حسب الثيم)", "صيغة #RRGGBB",
            listOf(
                @Composable { HexField(vm, "لون التوهج", "ct_glow", ap["ct_glow"] ?: "") },
                @Composable { HexField(vm, "لون الانعكاس", "ct_refl", ap["ct_refl"] ?: "") },
                @Composable { HexField(vm, "لون الحد", "ct_border", ap["ct_border"] ?: "") },
            ),
        )
    }
    item(key = "greset") { ResetSec(vm, "glass") }
}

// ---------------- 3. الكاروسيل ----------------
private fun LazyListScope.carPage(vm: AppVM, s: Settings) {
    val ap = s.ap
    val preset = ap["carPreset"] ?: "classic3d"
    item(key = "cp") { Column(Modifier.padding(16.dp)) { CarouselPreview(ap) } }
    item(key = "chome") {
        Sec(
            null, "يظهر الكاروسيل في الرئيسية عند تفعيل هذا الخيار، والغلاف المركزي يغذي الخلفية المموهة إن اخترتها.",
            listOf(@Composable { ToggleRow("عرض الكاروسيل في الرئيسية", s.homeLayout == "carousel") { vm.setStr("homeLayout", if (it) "carousel" else "hero") } }),
        )
    }
    item(key = "cpresets") {
        ChipsRow("النمط", CAROUSEL_PRESETS.map { it.second }, CAROUSEL_PRESETS.indexOfFirst { it.first == preset }.coerceAtLeast(0)) { i ->
            vm.applyAp(mapOf("carPreset" to CAROUSEL_PRESETS[i].first) + CAROUSEL_SPECS.associate { it.key to "" })
        }
    }
    specItems(vm, ap, CAROUSEL_SPECS, carPreset(preset), "c")
    item(key = "creset") { ResetSec(vm, "carousel") }
}

// ---------------- 4. البطاقات ----------------
private val cardDef = mapOf("r" to 16f, "sh" to 1f, "bd" to 0f, "op" to 100f, "tp" to 0f, "ts" to 100f, "pd" to 8f, "gr" to 55f, "rf" to 0f, "sz" to 100f)
private val sampleChannel = Item(id = "c1", kind = "channel", title = "BEOUT Sports", poster = "gradient:c1", live = true)

private fun LazyListScope.cardsPage(vm: AppVM, s: Settings) {
    val ap = s.ap
    val target = ap["cardTarget"] ?: "all"
    val targets = listOf("all") + CARD_TYPES
    val types = if (target == "all") CARD_TYPES else listOf(target)
    val base = cardDef + ("tp" to if (s.cardStyle == "overlay") 1f else 0f)
    item(key = "cprev") {
        Column(Modifier.padding(16.dp)) {
            BokehBackdrop(Modifier.fillMaxWidth()) {
                LazyRow(contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { PosterCard(SampleItems[0], {}) }
                    item { PosterCard(SampleItems[1], {}, progress = 0.4f) }
                    item { ChannelCard(sampleChannel, false, {}, {}, Modifier.width(posterWidth() * 1.2f)) }
                }
            }
        }
    }
    item(key = "ctarget") {
        ChipsRow("النوع المستهدف", targets.map { CARD_TYPE_NAMES.getValue(it) }, targets.indexOf(target).coerceAtLeast(0)) { vm.setStr("cardTarget", targets[it]) }
    }
    item(key = "cspecs") {
        Sec(
            CARD_TYPE_NAMES[target], "عند اختيار «كل الأنواع» تُكتب القيمة على كل الأنواع معاً.",
            CARD_SPECS.map { sp -> @Composable { SpecRow(vm, ap, sp, base, types.map { "card_${it}_${sp.key}" }) } },
        )
    }
    item(key = "creset") {
        Sec(
            null, null,
            listOf(@Composable {
                ActionRow("إعادة هذا النوع للافتراضي", danger = true) {
                    vm.applyAp(types.flatMap { t -> CARD_SPECS.map { "card_${t}_${it.key}" } }.associateWith { "" })
                }
            }),
        )
    }
}

// ---------------- 5/6. التنقل والشريط العلوي ----------------
private fun LazyListScope.navPage(vm: AppVM, s: Settings) {
    val ap = s.ap
    item(key = "np") {
        var pv by remember { mutableIntStateOf(0) }
        Column(Modifier.padding(16.dp)) {
            BokehBackdrop(Modifier.fillMaxWidth().height(110.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { NavBar(s.navStyle, pv, { pv = it }) }
            }
        }
    }
    item(key = "nstyle") { Choice(vm, "نمط شريط التنقل", "navStyle", s.navStyle, navList) }
    item(key = "nsl") {
        Sec(
            "الحجم والشفافية", "الحجم لا يؤثر على اللوح السفلي وشريط التبويب بعرض كامل.",
            listOf(
                @Composable { ApSlider(vm, ap, "navSize", "حجم الشريط %", 100f, 70f, 130f) },
                @Composable { ApSlider(vm, ap, "navAlpha", "شفافية الشريط %", 100f, 30f, 100f) },
            ),
        )
    }
    item(key = "nreset") {
        Sec(null, null, listOf(@Composable { ActionRow("إعادة شريط التنقل للافتراضي", danger = true) { vm.applyAp(mapOf("navStyle" to "labels", "navSize" to "100", "navAlpha" to "100")) } }))
    }
}

private fun LazyListScope.tbPage(vm: AppVM, s: Settings) {
    val ap = s.ap
    item(key = "tp") {
        Column(Modifier.padding(16.dp)) {
            BokehBackdrop(Modifier.fillMaxWidth().height(100.dp)) {
                Box(Modifier.fillMaxSize().padding(top = 14.dp), contentAlignment = Alignment.TopCenter) {
                    TopBar(s.toolbarStyle, listOf(TopAction(Ic.Search, {}), TopAction(Ic.Settings, {})))
                }
            }
        }
    }
    item(key = "tstyle") { Choice(vm, "نمط الشريط العلوي", "toolbarStyle", s.toolbarStyle, tbList) }
    item(key = "tsl") {
        Sec(
            "الحجم والشفافية", "الأنماط بعرض كامل تدعم التصغير فقط.",
            listOf(
                @Composable { ApSlider(vm, ap, "tbSize", "حجم الشريط %", 100f, 70f, 130f) },
                @Composable { ApSlider(vm, ap, "tbAlpha", "شفافية الشريط %", 100f, 30f, 100f) },
            ),
        )
    }
    item(key = "treset") {
        Sec(null, null, listOf(@Composable { ActionRow("إعادة الشريط العلوي للافتراضي", danger = true) { vm.applyAp(mapOf("toolbarStyle" to "pill", "tbSize" to "100", "tbAlpha" to "100")) } }))
    }
}

// ---------------- 7. الأزرار والحوارات والبحث ----------------
private fun LazyListScope.btnPage(vm: AppVM, s: Settings) {
    val ap = s.ap
    item(key = "bp") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BokehBackdrop(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        PrimaryButton("زر رئيسي", {})
                        Chip("ثانوي", false, {})
                        Chip("نشط", true, {})
                    }
                    SearchField("", {}, "بحث...")
                    Column(Modifier.fillMaxWidth().dialogSurface(GlassShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Txt("عنوان الحوار", style = T.h2)
                        Txt("نص توضيحي قصير داخل الحوار.", style = T.body, color = T.c.textDim)
                    }
                }
            }
        }
    }
    item(key = "bs") {
        Choice(
            vm, "نمط الأزرار", "btnStyle", ap["btnStyle"] ?: "default",
            listOf("default" to "افتراضي (كبسولة)", "rounded" to "مدور", "squared" to "مربع الزوايا", "glass" to "زجاجي", "floating" to "عائم بظل", "minimal" to "شفاف", "gradient" to "تدرج"),
        )
    }
    item(key = "ds") {
        Choice(vm, "نمط الحوارات واللوحات", "dlgStyle", ap["dlgStyle"] ?: "sheet", listOf("sheet" to "زجاجي (افتراضي)", "solid" to "معتم", "flat" to "مسطح", "outlined" to "زجاجي بحد"))
    }
    item(key = "ss") {
        Choice(vm, "نمط حقل البحث", "srchStyle", ap["srchStyle"] ?: "glass", listOf("glass" to "زجاجي (افتراضي)", "flat" to "مسطح", "solid" to "معتم", "underline" to "خط سفلي"))
    }
    item(key = "br") { ResetSec(vm, "buttons") }
}

// ---------------- 8. الخطوط والأيقونات ----------------
private fun LazyListScope.typoPage(vm: AppVM, s: Settings) {
    val ap = s.ap
    item(key = "tyP") {
        Column(Modifier.padding(16.dp)) {
            Column(Modifier.fillMaxWidth().flat(RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt("عنوان رئيسي · Title", style = T.title)
                Txt("نص أساسي يوضح شكل القراءة في القوائم والتفاصيل.", style = T.body)
                Txt("نص صغير للبيانات الثانوية · Small", style = T.small, color = T.c.textDim)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf(Ic.Home, Ic.Channels, Ic.Movies, Ic.Search, Ic.Heart, Ic.Play).forEachIndexed { i, ic ->
                        BIcon(ic, size = 26.dp, tint = if (i == 0) T.c.accent2 else T.c.text, active = i == 0)
                    }
                }
            }
        }
    }
    item(key = "tyF") {
        KeyChips(vm, "العائلة", "fontFam", ap["fontFam"] ?: "default", listOf("default" to "افتراضي", "sans" to "Sans", "serif" to "Serif", "mono" to "Mono", "cursive" to "Cursive"))
    }
    item(key = "tyS") {
        Sec(
            "الأحجام والتباعد", null,
            listOf(
                @Composable { ApSlider(vm, ap, "fontW", "وزن الخط (إزاحة)", 0f, -3f, 3f) },
                @Composable { ApSlider(vm, ap, "fontTitle", "حجم العناوين %", 100f, 70f, 150f) },
                @Composable { ApSlider(vm, ap, "fontBody", "حجم النص %", 100f, 70f, 150f) },
                @Composable { ApSlider(vm, ap, "fontSmall", "حجم النص الصغير %", 100f, 70f, 150f) },
                @Composable { ApSlider(vm, ap, "letterSp", "تباعد الأحرف", 0f, -10f, 30f) },
                @Composable { ApSlider(vm, ap, "lineH", "ارتفاع السطر %", 100f, 80f, 160f) },
                @Composable { ApSlider(vm, ap, "textA", "شفافية النص %", 100f, 40f, 100f) },
            ),
        )
    }
    item(key = "tyI") {
        Column {
            KeyChips(
                vm, "نمط الأيقونات", "iconStyle", s.iconStyle,
                listOf("line" to "خطوط", "duo" to "ثنائي", "solid" to "مصمت", "soft" to "ناعم", "sharp" to "حاد", "glass" to "زجاجي", "ghost" to "شفاف"),
            )
            KeyChips(vm, "حجم الأيقونات", "iconSize", ap["iconSize"] ?: "m", listOf("s" to "صغير", "m" to "متوسط", "l" to "كبير"))
            KeyChips(vm, "سماكة الخط", "iconW", ap["iconW"] ?: "m", listOf("s" to "رفيع", "m" to "متوسط", "l" to "سميك"))
            KeyChips(vm, "إبراز الأيقونة النشطة", "iconActive", ap["iconActive"] ?: "accent", listOf("accent" to "بلون التمييز", "text" to "بلون النص", "filled" to "تعبئة"))
        }
    }
    item(key = "tyR") { ResetSec(vm, "text") }
}

// ---------------- 9. التخطيط ----------------
private fun layoutBundle(mode: String, widthDp: Int): Map<String, String> = when (mode) {
    "compact" -> mapOf("space" to "s", "density" to "s", "hPad" to "12", "secGap" to "8", "cardGap" to "8", "gridCols" to "0", "contentW" to "0")
    "spacious" -> mapOf("space" to "l", "density" to "l", "hPad" to "22", "secGap" to "22", "cardGap" to "14", "gridCols" to "0", "contentW" to "0")
    "dense" -> mapOf("space" to "s", "density" to "s", "hPad" to "10", "secGap" to "6", "cardGap" to "6", "gridCols" to "0", "contentW" to "0")
    "adaptive" -> when {
        widthDp < 600 -> mapOf("space" to "m", "density" to "m", "hPad" to "16", "secGap" to "14", "cardGap" to "10", "gridCols" to "0", "contentW" to "0")
        widthDp < 840 -> mapOf("space" to "m", "density" to "m", "hPad" to "24", "secGap" to "18", "cardGap" to "12", "gridCols" to "0", "contentW" to "0")
        else -> mapOf("space" to "m", "density" to "m", "hPad" to "40", "secGap" to "22", "cardGap" to "14", "gridCols" to "0", "contentW" to "1100")
    }
    else -> mapOf("space" to "m", "density" to "m", "hPad" to "16", "secGap" to "14", "cardGap" to "10", "gridCols" to "0", "contentW" to "0")
}

private val sml3 = listOf("s" to "صغير", "m" to "متوسط", "l" to "كبير")

private fun LazyListScope.layoutPage(vm: AppVM, s: Settings) {
    val ap = s.ap
    val modes = listOf("compact" to "مضغوط", "balanced" to "متوازن", "spacious" to "واسع", "dense" to "كثيف", "adaptive" to "متكيّف مع الشاشة")
    item(key = "lm") {
        val w = LocalConfiguration.current.screenWidthDp
        ChipsRow("نمط التخطيط", modes.map { it.second }, modes.indexOfFirst { it.first == (ap["layoutMode"] ?: "balanced") }.coerceAtLeast(0)) { i ->
            vm.applyAp(layoutBundle(modes[i].first, w) + ("layoutMode" to modes[i].first))
        }
    }
    item(key = "ls") {
        Sec(
            "الأبعاد", "صفر في الأعمدة = تلقائي، وصفر في العرض الأقصى = بعرض الشاشة كاملاً.",
            listOf(
                @Composable { ApSlider(vm, ap, "hPad", "هوامش الشاشة", 16f, 0f, 40f) },
                @Composable { ApSlider(vm, ap, "secGap", "المسافة بين الأقسام", 14f, 0f, 40f) },
                @Composable { ApSlider(vm, ap, "cardGap", "المسافة بين البطاقات", 10f, 0f, 30f) },
                @Composable { ApSlider(vm, ap, "gridCols", "أعمدة الشبكة", 0f, 0f, 6f) },
                @Composable { ApSlider(vm, ap, "contentW", "العرض الأقصى للمحتوى", 0f, 0f, 1200f, 20f) },
            ),
        )
    }
    item(key = "lh") { Choice(vm, "الرئيسية", "homeLayout", s.homeLayout, listOf("hero" to S.hlHero, "rows" to S.hlRows, "carousel" to "كاروسيل 3D")) }
    item(key = "ll") { Choice(vm, "نمط قائمة القنوات", "listStyle", s.listStyle, listOf("cards" to "بطاقات", "glass" to "زجاج", "grouped" to "مجموعة بفواصل", "plain" to "بلا حاوية", "compact" to "مضغوط")) }
    item(key = "lc") { Choice(vm, "بطاقات الأفلام", "cardStyle", s.cardStyle, listOf("below" to "العنوان تحت الصورة", "overlay" to "العنوان فوق الصورة")) }
    item(key = "lr") { Choice(vm, "انحناء الزوايا", "radius", s.radius, sml3) }
    item(key = "lp") { Choice(vm, "حجم الملصقات", "posterSize", s.posterSize, sml3) }
    item(key = "lx") { ResetSec(vm, "layoutx") }
}

// ---------------- 10. الحركة ----------------
private fun LazyListScope.animPage(vm: AppVM, s: Settings) {
    val ap = s.ap
    item(key = "am") {
        KeyChips(
            vm, "نمط الحركة", "motion", ap["motion"] ?: "smooth",
            listOf("smooth" to "ناعم", "slide" to "انزلاق", "scale" to "تكبير", "depth" to "عمق", "spring" to "نابض", "minimal" to "خفيف", "disabled" to "بلا حركة"),
        )
    }
    item(key = "as") {
        Sec(
            "المعاملات", "«تقليل الحركة» أو إعداد النظام يوقفان الحركة الزخرفية تماماً.",
            listOf(
                @Composable { ApSlider(vm, ap, "mSpeed", "السرعة %", 100f, 40f, 250f) },
                @Composable { ApSlider(vm, ap, "mIntensity", "الشدة %", 100f, 0f, 200f) },
                @Composable { ApSlider(vm, ap, "mDamp", "التخميد %", 100f, 20f, 150f) },
                @Composable { ToggleRow("تقليل الحركة", ap["reduceMotion"] == "1") { vm.setStr("reduceMotion", if (it) "1" else "0") } },
            ),
        )
    }
    item(key = "ar") { ResetSec(vm, "anim") }
}

// ---------------- 11. الخلفية ----------------
private fun LazyListScope.bgPage(vm: AppVM, s: Settings) {
    val ap = s.ap
    item(key = "bm") {
        KeyChips(
            vm, "نوع الخلفية", "bgMode", ap["bgMode"] ?: "gradient",
            listOf("gradient" to "تدرج", "solid" to "لون ثابت", "dynamic" to "تدرج بلون التمييز", "optical" to "بقع ضوء", "blurred" to "غلاف مموه", "image" to "صورة (رابط)"),
        )
    }
    item(key = "bc") {
        Sec(
            "الألوان (فارغ = حسب الثيم)", "صيغة #RRGGBB",
            listOf(
                @Composable { HexField(vm, "اللون الأول", "bgC1", ap["bgC1"] ?: "") },
                @Composable { HexField(vm, "اللون الثاني", "bgC2", ap["bgC2"] ?: "") },
                @Composable { ApSlider(vm, ap, "bgDim", "تعتيم الخلفية %", 0f, 0f, 80f) },
                @Composable { ApSlider(vm, ap, "bgBlur", "قوة التمويه %", 40f, 0f, 100f) },
            ),
        )
    }
    item(key = "bi") {
        var url by remember(ap["bgImg"]) { mutableStateOf(ap["bgImg"] ?: "") }
        Sec(
            "رابط الصورة", "يُحمَّل مصغراً ومموهاً مرة واحدة ويُخزَّن.",
            listOf(
                @Composable { Box(Modifier.padding(8.dp)) { SearchField(url, { url = it }, "https://...") } },
                @Composable { ActionRow("تطبيق الرابط", accent = true) { vm.setStr("bgImg", url.trim()) } },
            ),
        )
    }
    item(key = "br") { ResetSec(vm, "bg") }
}

// ---------------- 12. الثيم ----------------
private fun LazyListScope.themePage(vm: AppVM, s: Settings) {
    val ap = s.ap
    item(key = "th") {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Txt("الثيم", Modifier.padding(horizontal = 14.dp), T.small, T.c.textDim)
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
    }
    item(key = "tc") {
        Sec(
            "ثيم مخصص", "املأ الألوان ثم فعّل الثيم المخصص. الفارغ يتبع الثيم الداكن.",
            listOf(
                @Composable { HexField(vm, "الخلفية (أعلى)", "ct_bg", ap["ct_bg"] ?: "") },
                @Composable { HexField(vm, "الخلفية (أسفل)", "ct_bg2", ap["ct_bg2"] ?: "") },
                @Composable { HexField(vm, "السطح", "ct_surface", ap["ct_surface"] ?: "") },
                @Composable { HexField(vm, "النص", "ct_text", ap["ct_text"] ?: "") },
                @Composable { HexField(vm, "لون التمييز", "ct_accent", ap["ct_accent"] ?: "") },
                @Composable { ActionRow("استخدام الثيم المخصص", accent = true) { vm.applyAp(mapOf("theme" to "custom")) } },
            ),
        )
    }
    item(key = "tr") { Column { ResetSec(vm, "theme"); ResetSec(vm, "ctheme", "مسح ألوان الثيم المخصص") } }
}

// ---------------- 13. الأداء ----------------
private fun LazyListScope.perfPage(vm: AppVM, s: Settings) {
    val ap = s.ap
    item(key = "pc") {
        Choice(
            vm, "مستوى الأداء", "perf", ap["perf"] ?: "auto", listOf("auto" to "تلقائي (موصى به)") + Perf.names,
            "التلقائي يقيس ذاكرة الجهاز وأنويته وإصدار النظام وسرعة المحرك، ويتوقف عند «متقدم» ويخفض عند توفير الطاقة. «بصري كامل» يدوي فقط.",
        )
    }
    item(key = "pi") {
        val ctx = LocalContext.current
        val tier = Perf.resolve(ap["perf"] ?: "auto", ctx)
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        Sec(
            "حالة الجهاز", null,
            listOf(
                @Composable { InfoRow("المستوى الفعلي الآن", Perf.names[tier].second) },
                @Composable { InfoRow("ذاكرة التطبيق (memoryClass)", "${am?.memoryClass ?: 0} MB") },
                @Composable { InfoRow("أنوية المعالج", Runtime.getRuntime().availableProcessors().toString()) },
                @Composable { InfoRow("Android SDK", Build.VERSION.SDK_INT.toString()) },
                @Composable { InfoRow("المحرك الأصلي (C++)", if (NativeEngine.available) "مفعّل" else "غير متوفر · يعمل بديل Kotlin") },
                @Composable { InfoRow("زمن معيار المحرك", if (NativeEngine.bench > 0) "${NativeEngine.bench} µs" else "—") },
            ),
        )
    }
    item(key = "pr") { Sec(null, null, listOf(@Composable { ActionRow("إعادة الأداء للتلقائي", danger = true) { vm.applyAp(mapOf("perf" to "auto")) } })) }
}

// ---------------- 14. مكتبة الأنماط ----------------
private fun LazyListScope.presetsPage(vm: AppVM, presets: Map<String, Map<String, String>>) {
    item(key = "pb") {
        Sec(
            "أنماط جاهزة (لا تُعدَّل)", "الضغط يطبّق النمط كاملاً. للتعديل انسخه أولاً إلى ملفاتك الشخصية.",
            BUILTIN_PRESETS.map { (name, p) ->
                @Composable {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Txt(name, Modifier.weight(1f).tvFocus().tap { vm.applyAp(AP.def + p) }.padding(vertical = 8.dp), T.body, T.c.text, maxLines = 1)
                        Chip("نسخ", false, { vm.savePresetMap("$name (نسخة)", AP.def + p) }, compact = true)
                    }
                }
            },
        )
    }
    item(key = "pp") { Column { PresetsPage(vm, presets) } }
    item(key = "pa") { ResetSec(vm, "all", "إعادة ضبط كل التصميم للافتراضي") }
}
