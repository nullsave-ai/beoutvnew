package com.beouttv.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.beouttv.app.data.Item
import com.beouttv.app.data.Spec
import java.util.Locale
import kotlin.math.roundToInt

/** تنسيق رقم للتخزين: دائماً بنقطة إنجليزية كي يُقرأ لاحقاً بـ toFloatOrNull. */
internal fun fmtNum(v: Float, step: Float): String =
    if (step >= 1f) v.roundToInt().toString() else String.format(Locale.US, "%.1f", v)

/**
 * منزلق الاستوديو: القيمة الحية محلية أثناء السحب (معاينة فورية)، والتخزين مؤجل بدفعة واحدة عبر onValue.
 * المسار دائماً من اليسار لليمين (الأصغر يساراً) بغض النظر عن اتجاه الواجهة.
 */
@Composable
internal fun StudioSlider(label: String, value: Float, min: Float, max: Float, step: Float, onValue: (Float) -> Unit) {
    var live by remember { mutableStateOf<Float?>(null) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (!dragging) live = null }
    val v = live ?: value
    val frac = if (max > min) ((v - min) / (max - min)).coerceIn(0f, 1f) else 0f
    var wPx by remember { mutableIntStateOf(1) }
    val thumbPx = with(LocalDensity.current) { 22.dp.toPx() }
    val cb by rememberUpdatedState(onValue)
    val usable = (wPx - thumbPx).coerceAtLeast(1f)
    fun fromX(x: Float): Float {
        val f = ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f)
        val raw = min + f * (max - min)
        val sn = if (step > 0f) kotlin.math.round((raw - min) / step) * step + min else raw
        return sn.coerceIn(min, max)
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Txt(label, Modifier.weight(1f), T.small, T.c.textDim, maxLines = 1)
            Txt(fmtNum(v, step), style = T.small, color = T.c.accent2)
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                Modifier.fillMaxWidth().height(34.dp).onSizeChanged { wPx = it.width.coerceAtLeast(1) }
                    .pointerInput(min, max, step) {
                        detectTapGestures { o -> val nv = fromX(o.x); live = nv; cb(nv) }
                    }
                    .pointerInput(min, max, step) {
                        detectHorizontalDragGestures(
                            onDragStart = { o -> dragging = true; val nv = fromX(o.x); live = nv; cb(nv) },
                            onDragEnd = { dragging = false },
                            onDragCancel = { dragging = false },
                            onHorizontalDrag = { ch, _ ->
                                ch.consume()
                                val nv = fromX(ch.position.x)
                                if (nv != live) { live = nv; cb(nv) }
                            },
                        )
                    },
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(T.c.edgeLo))
                val fillDp = with(LocalDensity.current) { (thumbPx / 2f + frac * usable).toDp() }
                Box(Modifier.width(fillDp).height(4.dp).clip(RoundedCornerShape(50)).background(T.c.accent))
                Box(
                    Modifier.offset { IntOffset((frac * usable).roundToInt(), 0) }.size(22.dp)
                        .shadow(3.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Color.White)
                        .border(2.dp, T.c.accent, RoundedCornerShape(50)),
                )
            }
        }
    }
}

/** صف خيارات أفقي قابل للتمرير؛ الخيار النشط بنمط التمييز. */
@Composable
internal fun ChipsRow(label: String?, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        label?.let { Txt(it, Modifier.padding(horizontal = 14.dp, vertical = 2.dp), T.small, T.c.textDim) }
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(options) { i, o -> Chip(o, i == selected, { onSelect(i) }) }
        }
    }
}

/** حقل لون سداسي (#RRGGBB أو #AARRGGBB)؛ يُحفظ فقط إذا كان صالحاً أو فارغاً (= اتباع الثيم). */
@Composable
internal fun HexField(vm: AppVM, label: String, key: String, value: String) {
    var text by remember(value) { mutableStateOf(value) }
    val col = parseHex(text)
    val sh = RoundedCornerShape(8.dp)
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(26.dp).clip(sh).background(col ?: Color.Transparent).border(1.dp, T.c.edgeLo, sh))
        Txt(label, Modifier.weight(1f), T.body, maxLines = 1)
        BasicTextField(
            text,
            { t ->
                text = t.take(9)
                val clean = t.trim()
                if (clean.isEmpty() || parseHex(clean) != null) vm.setStr(key, clean)
            },
            Modifier.width(104.dp).glass(RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
            singleLine = true,
            textStyle = T.small.copy(color = T.c.text, textDirection = TextDirection.Ltr),
            cursorBrush = SolidColor(T.c.accent2),
        )
    }
}

/** صف مواصفة واحدة: تبديل (0..1) أو خيارات (opts) أو منزلق. */
@Composable
internal fun SpecRow(vm: AppVM, ap: Map<String, String>, sp: Spec, base: Map<String, Float>, keys: List<String> = listOf(sp.key)) {
    val cur = keys.firstNotNullOfOrNull { ap[it]?.toFloatOrNull() } ?: base[sp.key] ?: sp.min
    val opts = sp.opts
    when {
        sp.toggle -> ToggleRow(sp.label, cur >= 0.5f) { on -> keys.forEach { vm.setStr(it, if (on) "1" else "0") } }
        opts != null -> ChipsRow(sp.label, opts, cur.roundToInt().coerceIn(0, opts.lastIndex)) { i -> keys.forEach { vm.setStr(it, i.toString()) } }
        else -> StudioSlider(sp.label, cur, sp.min, sp.max, sp.step) { v -> keys.forEach { vm.setNum(it, fmtNum(v, sp.step)) } }
    }
}

/** يولّد مجموعة لكل Spec.group كعناصر قائمة مستقلة (تركيب كسول). */
internal fun LazyListScope.specItems(vm: AppVM, ap: Map<String, String>, specs: List<Spec>, base: Map<String, Float>, keyPrefix: String) {
    specs.groupBy { it.group }.forEach { (g, list) ->
        item(key = "$keyPrefix:$g") {
            Sec(g, null, list.map { sp -> @Composable { SpecRow(vm, ap, sp, base) } })
        }
    }
}

// ---------------- معاينات حية ----------------

internal val SampleItems = listOf(
    Item(id = "s1", kind = "movie", title = "Midnight Run", poster = "gradient:s1", year = 2024, quality = "4K", category = "Action"),
    Item(id = "s2", kind = "series", title = "Golden Hour", poster = "gradient:s2", year = 2023, quality = "HD", category = "Drama"),
    Item(id = "s3", kind = "movie", title = "Neon City", poster = "gradient:s3", year = 2025, quality = "4K", category = "Sci-Fi"),
    Item(id = "s4", kind = "movie", title = "Blue Lagoon", poster = "gradient:s4", year = 2022, quality = "HD", category = "Adventure"),
    Item(id = "s5", kind = "series", title = "Silent Echo", poster = "gradient:s5", year = 2024, quality = "HD", category = "Mystery"),
    Item(id = "s6", kind = "movie", title = "Last Horizon", poster = "gradient:s6", year = 2021, quality = "FHD", category = "Drama"),
    Item(id = "s7", kind = "movie", title = "Crimson Tide", poster = "gradient:s7", year = 2020, quality = "HD", category = "Thriller"),
)

/** خلفية ملونة ببقع ضوء (تُرسم مرة واحدة) تُظهر الزجاج والانكسار بوضوح في المعاينة. */
@Composable
internal fun BokehBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val c = T.c
    Box(
        modifier.clip(RoundedCornerShape(20.dp)).clipToBounds().drawBehind {
            val w = size.width; val h = size.height
            drawRect(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
            drawRect(Brush.radialGradient(listOf(Color(0xFFE5A93A).copy(alpha = 0.55f), Color.Transparent), Offset(w * 0.12f, h * 0.2f), w * 0.55f))
            drawRect(Brush.radialGradient(listOf(Color(0xFFC2467F).copy(alpha = 0.5f), Color.Transparent), Offset(w * 0.9f, h * 0.35f), w * 0.5f))
            drawRect(Brush.radialGradient(listOf(Color(0xFF3F7FE0).copy(alpha = 0.5f), Color.Transparent), Offset(w * 0.45f, h * 1.0f), w * 0.6f))
        },
        content = content,
    )
}

@Composable
internal fun GlassPreview(optical: Boolean) {
    BokehBackdrop(Modifier.fillMaxWidth().height(200.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).glass(RoundedCornerShape(50)), contentAlignment = Alignment.Center) { BIcon(Ic.Search, size = 22.dp, tint = T.c.text) }
                Row(Modifier.height(48.dp).glassDense(RoundedCornerShape(50)).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BIcon(Ic.Home, size = 20.dp, tint = T.c.accent2, active = true)
                    Txt("Glass Bar", style = T.small, maxLines = 1)
                }
                Box(Modifier.size(48.dp).glass(RoundedCornerShape(50)), contentAlignment = Alignment.Center) { BIcon(Ic.Heart, size = 22.dp, tint = T.c.text) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f).height(86.dp).glass(T.card).padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Txt("بطاقة زجاجية", style = T.h2, maxLines = 1)
                    Txt("Glass card", style = T.small, color = T.c.textDim, maxLines = 1)
                }
                Column(Modifier.weight(1f).height(86.dp).glassDense(T.card).padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Txt("سطح كثيف", style = T.h2, maxLines = 1)
                    Txt("Dense surface", style = T.small, color = T.c.textDim, maxLines = 1)
                }
            }
            if (!optical) Txt("فعّل وضع Optical ليظهر أثر معاملات الزجاج هنا", style = T.small, color = T.c.textDim)
        }
    }
}

@Composable
internal fun CarouselPreview(ap: Map<String, String>) {
    val cfg = remember(ap) { carCfg(ap) }
    BokehBackdrop(Modifier.fillMaxWidth()) {
        Carousel3D(SampleItems, cfg, Modifier.padding(vertical = 10.dp))
    }
}

/** خيارات بمفاتيح نصية (قيمة، تسمية) تُكتب مباشرة في مفتاح واحد. */
@Composable
internal fun KeyChips(vm: AppVM, label: String?, key: String, cur: String, list: List<Pair<String, String>>) =
    ChipsRow(label, list.map { it.second }, list.indexOfFirst { it.first == cur }.coerceAtLeast(0)) { vm.setStr(key, list[it].first) }

/** منزلق مرتبط بمفتاح رقمي نصي مع قيمة افتراضية. */
@Composable
internal fun ApSlider(vm: AppVM, ap: Map<String, String>, key: String, label: String, def: Float, min: Float, max: Float, step: Float = 1f) =
    StudioSlider(label, ap[key]?.toFloatOrNull() ?: def, min, max, step) { vm.setNum(key, fmtNum(it, step)) }

/** صف إجراء نصي (يُستخدم داخل Sec). */
@Composable
internal fun ActionRow(label: String, accent: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().tvFocus().tap(onClick).padding(14.dp)) {
        Txt(label, style = T.body, color = if (danger) T.c.danger else if (accent) T.c.accent2 else T.c.text)
    }
}

/** صف معلومة: اسم وقيمة. */
@Composable
internal fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(label, Modifier.weight(1f), T.body, maxLines = 1)
        Txt(value, style = T.small, color = T.c.textDim, maxLines = 1)
    }
}
