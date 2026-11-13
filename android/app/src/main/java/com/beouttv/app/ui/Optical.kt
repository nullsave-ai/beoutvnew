package com.beouttv.app.ui

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.beouttv.app.data.AP
import com.beouttv.app.data.Img
import com.beouttv.app.data.Settings
import com.beouttv.app.data.glassPreset
import com.beouttv.app.optical.NativeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** مستويات الأداء: 0 خفيف جداً، 1 متوازن، 2 متقدم، 3 بصري كامل. */
object Perf {
    val names = listOf("ultra" to "خفيف جداً", "balanced" to "متوازن", "advanced" to "متقدم", "full" to "بصري كامل")
    private var autoTier = -1

    /** اختيار تلقائي من القدرات الفعلية (ذاكرة، أنوية، إصدار النظام، معيار المحرك) لا من اسم الجهاز. */
    fun auto(ctx: Context): Int {
        if (autoTier >= 0) return autoTier
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val mem = am?.memoryClass ?: 128
        val low = am?.isLowRamDevice == true
        val cores = Runtime.getRuntime().availableProcessors()
        var score = 0
        score += when { mem >= 384 -> 3; mem >= 256 -> 2; mem >= 192 -> 1; else -> 0 }
        score += when { cores >= 8 -> 2; cores >= 6 -> 1; else -> 0 }
        if (Build.VERSION.SDK_INT >= 31) score += 1
        val b = NativeEngine.bench
        if (b in 1..2500) score += 1 else if (b > 9000) score -= 1
        if (low) score -= 3
        autoTier = when { score >= 6 -> 3; score >= 4 -> 2; score >= 2 -> 1; else -> 0 }
        return autoTier
    }

    /** الإعداد اليدوي يتقدّم على التلقائي. وضع توفير الطاقة يخفض المستوى التلقائي فقط. */
    fun resolve(setting: String, ctx: Context): Int = when (setting) {
        "ultra" -> 0; "balanced" -> 1; "advanced" -> 2; "full" -> 3
        else -> {
            val t = auto(ctx)
            val saver = (ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
            if (saver) min(t, 1) else min(t, 2)
        }
    }
}

@Immutable
class OpticalCfg(
    val on: Boolean,
    val full: Boolean,
    val tier: Int,
    val g: Map<String, Float>,
    val glow: Color?,
    val refl: Color?,
    val border: Color?,
) {
    fun f(k: String) = g[k] ?: 0f
    companion object { val Off = OpticalCfg(false, false, 1, emptyMap(), null, null, null) }
}

fun parseHex(s: String?): Color? {
    val t = s?.trim()?.removePrefix("#") ?: return null
    if (t.length != 6 && t.length != 8) return null
    val v = t.toLongOrNull(16) ?: return null
    return Color(if (t.length == 6) (0xFF000000L or v) else v)
}

fun buildOptical(s: Settings, tier: Int): OpticalCfg {
    val ap = s.ap
    val base = glassPreset(ap["glassPreset"] ?: "pure").toMutableMap()
    base.keys.toList().forEach { k -> ap[k]?.toFloatOrNull()?.let { base[k] = it } }
    return OpticalCfg(
        on = ap["mode"] == "optical", full = ap["fullOptical"] == "1", tier = tier, g = base,
        glow = parseHex(ap["ct_glow"]), refl = parseHex(ap["ct_refl"]), border = parseHex(ap["ct_border"]),
    )
}

const val ROLE_BAR = 0
const val ROLE_BUTTON = 1
const val ROLE_CARD = 2

/**
 * سطح زجاجي بصري: طبقات مرسومة بحسب مستوى الأداء. كل معامل من لوحة الزجاج يغيّر الرسم فعلياً.
 * ultra: تعبئة + حد | balanced: + عمق ولمعة داخلية | advanced: + انعكاس وانكسار وفرينل وتوهج | full: + زيغ لوني وطبقات.
 */
@Composable
fun Modifier.opticalSurface(shape: Shape, role: Int, tierCap: Int = 3): Modifier {
    val o = T.look.opt
    val c = T.c
    val tier = min(o.tier, tierCap)
    val dark = c.dark
    val accent = c.accent2
    val glowC = o.glow ?: accent
    val reflC = o.refl ?: Color.White
    val borderC = o.border ?: (if (dark) Color.White else Color(0xFF223044))
    val outer = o.f("g_outer"); val shd = o.f("g_shadow")
    val elev = if (tier >= 1 && outer > 0f) (outer / 100f * 14f * (0.4f + shd / 100f)).dp else 0.dp
    val m = if (elev > 0.dp) this.shadow(elev, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.3f * shd / 50f), spotColor = Color.Black.copy(alpha = 0.4f * shd / 50f)) else this
    val denseC = c.dense
    return m.clip(shape).drawWithCache {
        val w = size.width; val h = size.height
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = Path().apply {
            when (outline) {
                is Outline.Rounded -> addRoundRect(outline.roundRect)
                is Outline.Generic -> addPath(outline.path)
                is Outline.Rectangle -> addRect(outline.rect)
            }
        }
        val opacity = o.f("g_opacity") / 100f
        val surfaceT = o.f("g_surface") / 100f
        val frost = o.f("g_frost") / 100f
        val bgT = o.f("g_bgT") / 100f
        val diff = o.f("g_diff") / 100f
        val depth = o.f("g_depth") / 100f
        val hiPos = o.f("g_hiPos") / 100f
        val tint = if (dark) Color.White else Color(0xFFFFFFFF)
        // تعبئة: دعامة كثيفة (تصنيف) + تدرج لون الزجاج
        val backA = ((frost * 0.65f + (1f - bgT) * 0.25f) * (1f - surfaceT * 0.45f)).coerceIn(0f, 0.95f)
        val tintTop = tint.copy(alpha = (opacity * 0.34f * (if (dark) 1f else 1.6f)).coerceIn(0f, 0.9f))
        val tintBot = tint.copy(alpha = (opacity * 0.12f * (if (dark) 1f else 1.8f)).coerceIn(0f, 0.8f))
        val fillBrush = Brush.verticalGradient(listOf(tintTop, tintBot))
        val topLight = Brush.verticalGradient(0f to Color.White.copy(alpha = depth * (if (dark) 0.14f else 0.5f)), 0.45f to Color.Transparent)
        val bottomDark = Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = o.f("g_inner") / 100f * (if (dark) 0.35f else 0.12f)))
        val spec = o.f("g_spec") / 100f
        val specBrush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = spec * 0.5f), Color.Transparent),
            center = Offset(w * (0.12f + 0.76f * hiPos), h * 0.08f), radius = min(w, h) * (0.35f + 0.5f * o.f("g_sharp").let { 1f - it / 100f }),
        )
        val diffBrush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = diff * 0.10f), Color.Transparent),
            center = Offset(w * (0.2f + 0.6f * hiPos), h * 0.3f), radius = max(w, h) * 0.8f,
        )
        val ambBrush = Brush.radialGradient(
            listOf(glowC.copy(alpha = o.f("g_amb") / 100f * 0.22f), Color.Transparent),
            center = Offset(w * 0.85f, h * 1.0f), radius = max(w, h) * 0.9f,
        )
        val refr = o.f("g_refr") / 100f
        val rimW = (o.f("g_refrR") / 100f * 7f + 1.5f).dp.toPx()
        val ang = Math.toRadians(o.f("g_refrDir").toDouble())
        val dx = cos(ang).toFloat(); val dy = sin(ang).toFloat()
        val len = abs(w * dx) + abs(h * dy)
        val cx = w / 2f; val cy = h / 2f
        val rimBrush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = refr * 0.55f), Color.Transparent, Color.White.copy(alpha = refr * 0.22f)),
            start = Offset(cx - dx * len / 2f, cy - dy * len / 2f), end = Offset(cx + dx * len / 2f, cy + dy * len / 2f),
        )
        val edgeRefr = o.f("g_edgeRefr") / 100f
        val fres = o.f("g_fres") / 100f
        val fresBrush = Brush.radialGradient(
            0.5f to Color.Transparent, 1f to tint.copy(alpha = fres * (if (dark) 0.2f else 0.3f)),
            center = Offset(cx, cy), radius = sqrt(cx * cx + cy * cy),
        )
        val reflA = o.f("g_refl") / 100f
        val sharp = o.f("g_sharp") / 100f
        val reflAngle = o.f("g_reflDir") + o.f("g_distort") * 0.25f
        val reflBrush = Brush.horizontalGradient(
            0f to Color.Transparent, (0.5f - 0.35f * sharp).coerceAtLeast(0.05f) to Color.Transparent,
            0.5f to reflC.copy(alpha = reflA * 0.28f), (0.5f + 0.35f * sharp).coerceAtMost(0.95f) to Color.Transparent, 1f to Color.Transparent,
        )
        val glowA = o.f("g_glow") / 100f
        val bw = (o.f("g_bw") / 10f).coerceAtLeast(0f).dp.toPx()
        val ba = o.f("g_ba") / 100f
        val chroma = o.f("g_chroma") / 100f
        val disp = (o.f("g_disp") / 100f * 2.2f + 0.4f).dp.toPx()
        val layers = o.f("g_layer").toInt().coerceIn(1, 4)
        onDrawBehind {
            if (backA > 0f) drawRect(denseC.copy(alpha = backA))
            drawRect(fillBrush)
            if (tier >= 1) {
                if (depth > 0f) drawRect(topLight)
                if (o.f("g_inner") > 0f) drawRect(bottomDark)
                if (spec > 0f) drawRect(specBrush)
                if (diff > 0f) drawRect(diffBrush)
            }
            if (tier >= 2) {
                if (o.f("g_amb") > 0f) drawRect(ambBrush)
                if (reflA > 0f && role != ROLE_BUTTON) rotate(reflAngle - 90f, Offset(cx, cy)) {
                    drawRect(reflBrush, topLeft = Offset(-w * 0.5f, -h * 0.9f), size = Size(w * 2f, h * 2.8f))
                }
                if (refr > 0f) drawPath(path, rimBrush, style = Stroke(rimW * 2f))
                if (fres > 0f) drawRect(fresBrush)
                if (glowA > 0f) drawPath(path, glowC.copy(alpha = glowA * 0.5f), style = Stroke(3.dp.toPx()))
                if (edgeRefr > 0f) drawPath(path, Color.White.copy(alpha = edgeRefr * 0.35f), style = Stroke(1.2.dp.toPx()))
            }
            if (tier >= 3) {
                if (chroma > 0f) {
                    translateStroke(path, Color(0xFFFF4D6D).copy(alpha = chroma * 0.5f), -disp)
                    translateStroke(path, Color(0xFF4DA3FF).copy(alpha = chroma * 0.5f), disp)
                }
                if (layers > 1) {
                    val inset = 2.5.dp.toPx()
                    for (i in 1 until layers) {
                        val k = i * inset
                        drawRect(
                            Color.White.copy(alpha = 0.025f * (layers - i)), topLeft = Offset(k, k), size = Size((w - 2 * k).coerceAtLeast(1f), (h - 2 * k).coerceAtLeast(1f)),
                        )
                    }
                }
            }
            if (bw > 0f && ba > 0f) drawPath(path, borderC.copy(alpha = ba * (if (dark) 0.5f else 0.35f)), style = Stroke(bw * 2f))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.translateStroke(path: Path, color: Color, dx: Float) {
    androidx.compose.ui.graphics.drawscope.translate(dx, 0f) { drawPath(path, color, style = Stroke(1.2.dp.toPx())) }
}

// ===================== الخلفيات =====================

/** صورة الخلفية الجاهزة (مموهة) تُحسب مرة في Root وتُستهلك عبر screenBackground. */
val LocalBackdrop = staticCompositionLocalOf<ImageBitmap?> { null }

/** رابط الخلفية الديناميكي (غلاف العمل المركزي في الكاروسيل) لنمط "خلفية مموهة". */
object BackdropHub { val url = mutableStateOf<String?>(null) }

/** يحمّل صورة مصغرة (≤96px) ويمزجها بالمحرك (Native أو Kotlin). التكبير الثنائي الخطي يكمل النعومة. */
suspend fun blurredBitmap(url: String, radius: Int): ImageBitmap? = withContext(Dispatchers.Default) {
    val src: Bitmap = Img.load(url, 96) ?: return@withContext null
    val copy = runCatching { src.copy(Bitmap.Config.ARGB_8888, true) }.getOrNull() ?: return@withContext null
    NativeEngine.blur(copy, radius.coerceIn(1, 24), 2)
    copy.asImageBitmap()
}

@Composable
fun rememberBackdrop(s: Settings): ImageBitmap? {
    val mode = s.ap["bgMode"]
    val url = when (mode) { "image" -> s.ap["bgImg"]; "blurred" -> BackdropHub.url.value; else -> null }
    val radius = ((s.ap["bgBlur"]?.toFloatOrNull() ?: 40f) / 100f * 14f + 1f).toInt()
    val bmp by produceState<ImageBitmap?>(null, url, radius) {
        value = if (url.isNullOrBlank()) null else blurredBitmap(url, radius)
    }
    return bmp
}

@Composable
fun Modifier.screenBackground(): Modifier {
    val c = T.c
    val ap = T.look.ap
    val mode = ap["bgMode"] ?: "gradient"
    val c1 = parseHex(ap["bgC1"]) ?: c.bgTop
    val c2 = parseHex(ap["bgC2"]) ?: c.bgBottom
    val dim = (ap["bgDim"]?.toFloatOrNull() ?: 0f) / 100f
    val img = LocalBackdrop.current
    val accent = c.accent; val accent2 = c.accent2
    val dark = c.dark
    return drawBehind {
        val w = size.width; val h = size.height
        when {
            (mode == "image" || mode == "blurred") && img != null -> {
                // قص مركزي بنسبة الشاشة
                val ir = img.width.toFloat() / img.height
                val sr = w / h
                val srcW: Int; val srcH: Int
                if (ir > sr) { srcH = img.height; srcW = (img.height * sr).toInt().coerceAtLeast(1) } else { srcW = img.width; srcH = (img.width / sr).toInt().coerceAtLeast(1) }
                drawRect(c1)
                drawImage(
                    img, srcOffset = IntOffset((img.width - srcW) / 2, (img.height - srcH) / 2), srcSize = IntSize(srcW, srcH),
                    dstSize = IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1)),
                )
                drawRect(Color.Black.copy(alpha = if (dark) 0.45f else 0.15f))
            }
            mode == "solid" -> drawRect(c1)
            mode == "dynamic" -> drawRect(
                Brush.linearGradient(
                    listOf(lerpColor(c1, accent, if (dark) 0.28f else 0.18f), c2, lerpColor(c2, accent2, if (dark) 0.22f else 0.14f)),
                    start = Offset(0f, 0f), end = Offset(w, h),
                ),
            )
            mode == "optical" || mode == "blurred" || mode == "image" -> {
                drawRect(Brush.verticalGradient(listOf(c1, c2)))
                // بقع ضوء ملونة ناعمة (تُرسم مرة واحدة، بلا حركة)
                val a1 = if (dark) 0.34f else 0.22f
                drawRect(Brush.radialGradient(listOf(accent.copy(alpha = a1), Color.Transparent), Offset(w * 0.15f, h * 0.18f), w * 0.9f))
                drawRect(Brush.radialGradient(listOf(Color(0xFFB04A8F).copy(alpha = a1 * 0.9f), Color.Transparent), Offset(w * 0.95f, h * 0.55f), w * 0.8f))
                drawRect(Brush.radialGradient(listOf(Color(0xFF3F6FD8).copy(alpha = a1 * 0.8f), Color.Transparent), Offset(w * 0.3f, h * 1.0f), w * 0.9f))
            }
            else -> drawRect(Brush.verticalGradient(listOf(c1, c2)))
        }
        if (dim > 0f) drawRect(Color.Black.copy(alpha = dim * 0.7f))
    }
}

fun lerpColor(a: Color, b: Color, t: Float) = Color(
    a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, a.alpha + (b.alpha - a.alpha) * t,
)
