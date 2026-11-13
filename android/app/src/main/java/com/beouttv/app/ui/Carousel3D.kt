package com.beouttv.app.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.beouttv.app.data.CAROUSEL_SPECS
import com.beouttv.app.data.Item
import com.beouttv.app.data.RemoteImage
import com.beouttv.app.data.carKind
import com.beouttv.app.data.carPreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** إعداد الكاروسيل الفعلي: قيم النمط الأساسية مع تعديلات المستخدم (المفاتيح غير الفارغة). */
@Immutable
class CarCfg(val m: Map<String, Float>, val kind: String) {
    fun f(k: String) = m["c_$k"] ?: 0f
    fun b(k: String) = f(k) >= 0.5f
}

fun carCfg(ap: Map<String, String>): CarCfg {
    val preset = ap["carPreset"] ?: "classic3d"
    val base = carPreset(preset).toMutableMap()
    CAROUSEL_SPECS.forEach { sp -> ap[sp.key]?.toFloatOrNull()?.let { base[sp.key] = it } }
    return CarCfg(base, carKind(preset))
}

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun mod(x: Float, n: Int): Float { val r = x % n; return if (r < 0f) r + n else r }

/**
 * كاروسيل ثلاثي الأبعاد مستقل: 12 نمطاً هندسياً + كل المعاملات (تخطيط، منظور، حجم، حركة، تفاعل، مظهر).
 * الموضع الكسري `pos` حالة واحدة تقرؤها طبقات الرسم فقط (graphicsLayer) فلا إعادة تركيب أثناء السحب.
 * لا حركة مستمرة: الحركة فقط أثناء اللمس أو الانتقال أو التشغيل التلقائي إن فعّله المستخدم.
 */
@Composable
fun Carousel3D(
    items: List<Item>,
    cfg: CarCfg,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit = {},
    onOpen: (Item) -> Unit = {},
    onLong: (Item) -> Unit = {},
) {
    val n = items.size
    if (n == 0) return
    val look = T.look
    val mot = look.mot
    val dens = LocalDensity.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val vertical = cfg.b("orient")
    val loop = cfg.b("loop") && n > 2
    val cardW = cfg.f("w").dp
    val cardH = cfg.f("h").dp
    val sideN = min(cfg.f("side"), (cfg.f("visible") - 1f) / 2f).coerceAtLeast(0f)
    val spacing = cfg.f("spacing") / 100f
    val overlap = cfg.f("overlap") / 100f
    val textBelow = cfg.f("textPos").roundToInt() == 2
    val extra = if (textBelow) 56.dp else 28.dp
    val stepFrac = spacing * (1f - overlap)
    val totalH = if (vertical) minOf(cardH * (1f + 2f * sideN * stepFrac * 1.1f), 540.dp) + extra else cardH + extra + (cfg.f("sideDepth") / 100f * 40).dp
    val stepPx = with(dens) { (if (vertical) cardH else cardW).toPx() } * stepFrac.coerceAtLeast(0.05f)

    var pos by remember { mutableFloatStateOf(0f) }
    var touching by remember { mutableStateOf(false) }
    var anim by remember { mutableStateOf<Job?>(null) }
    val selected by remember(n) { derivedStateOf { mod(pos.roundToInt().toFloat(), n).toInt().coerceIn(0, n - 1) } }
    val onSelectNow by rememberUpdatedState(onSelect)
    val hapticOn = cfg.b("haptic")
    LaunchedEffect(selected) {
        onSelectNow(selected)
        if (hapticOn) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun animateTo(target: Float, velocity: Float = 0f, soft: Boolean = false) {
        anim?.cancel()
        val from = pos
        anim = scope.launch {
            val spec = when {
                mot.off -> tween<Float>(0)
                soft -> tween((cfg.f("dur") / mot.speed).toInt().coerceAtLeast(60), easing = FastOutSlowInEasing)
                else -> spring(dampingRatio = (cfg.f("damp") / 100f * mot.damp).coerceIn(0.3f, 1.2f), stiffness = cfg.f("stiff") * (0.5f + cfg.f("snap") / 100f) * mot.speed)
            }
            animate(from, target, velocity, spec) { v, _ -> pos = v }
            if (loop) pos = mod(pos, n)
        }
    }

    // تشغيل تلقائي اختياري
    val auto = cfg.b("auto"); val autoSpd = cfg.f("autoSpd"); val pause = cfg.b("pause")
    LaunchedEffect(auto, autoSpd, pause, n, loop) {
        if (!auto || n < 2) return@LaunchedEffect
        while (true) {
            delay((autoSpd * 1000).toLong())
            if (pause && touching) continue
            val next = if (loop) pos.roundToInt() + 1f else if (pos.roundToInt() >= n - 1) 0f else pos.roundToInt() + 1f
            animateTo(next, soft = true)
        }
    }

    val sens = cfg.f("sens").coerceAtLeast(0.2f)
    val drag = cfg.f("drag") / 100f
    val over = cfg.f("over").roundToInt()
    val dragState = rememberDraggableState { delta ->
        var d = -delta / stepPx * sens * (1f - drag * 0.7f)
        if (!loop && ((pos <= 0f && d < 0f) || (pos >= n - 1 && d > 0f))) d *= if (over == 0) 0f else 0.35f
        pos += d
    }
    val window = remember(selected, n, sideN, loop) {
        val r = ceil2(sideN) + 1
        (0 until n).filter { i ->
            val dd = abs(i - selected)
            (if (loop) min(dd, n - dd) else dd) <= r
        }.sortedByDescending { i -> val dd = abs(i - selected); if (loop) min(dd, n - dd) else dd }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(
            modifier.fillMaxWidth().height(totalH)
                .draggable(
                    dragState, if (vertical) Orientation.Vertical else Orientation.Horizontal, enabled = cfg.b("swipe"),
                    onDragStarted = { anim?.cancel(); touching = true },
                    onDragStopped = { vel ->
                        touching = false
                        val v = -vel / stepPx * sens
                        val coast = cfg.f("inertia") / 100f * (0.12f + (100f - cfg.f("decel")) / 100f * 0.3f)
                        var target = (pos + v * coast).roundToInt().toFloat()
                        if (!loop) target = target.coerceIn(0f, (n - 1).toFloat())
                        animateTo(target, v, soft = false)
                    },
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            val mainPx = with(dens) { (if (vertical) maxHeight else maxWidth).toPx() }
            val shift = (cfg.f("centerPos") - 50f) / 100f * mainPx
            val alignCross = when (cfg.f("align").roundToInt()) { 0 -> 0f; 2 -> 1f; else -> 0.5f }
            window.forEach { i ->
                key(i) {
                    CarCard(
                        item = items[i], index = i, n = n, loop = loop, cfg = cfg, vertical = vertical, stepPx = stepPx, shift = shift,
                        cardW = cardW, cardH = cardH, posProvider = { pos }, alignCross = alignCross, totalH = totalH, extra = extra,
                        onTap = {
                            if (cfg.b("tap") && i != selected) {
                                var d = i - pos
                                if (loop) { val half = n / 2f; while (d > half) d -= n; while (d < -half) d += n }
                                animateTo(pos + d, soft = true)
                            } else onOpen(items[i])
                        },
                        onLongPress = { if (cfg.b("long")) onLong(items[i]) },
                    )
                }
            }
        }
    }
}

private fun ceil2(x: Float) = kotlin.math.ceil(x).toInt()

/** يحسب تحويل البطاقة (الإزاحة، الحجم، الدوران، الشفافية...) لمسافة كسرية d عن المركز. */
private class CarT { var main = 0f; var cross = 0f; var scale = 1f; var rx = 0f; var ry = 0f; var rz = 0f; var alpha = 1f; var dim = 0f; var blur = 0f }

private fun carTransform(o: CarT, d: Float, cfg: CarCfg, step: Float, cardMain: Float, cardCross: Float, vertical: Boolean, pos: Float) {
    val a = abs(d); val s = if (d < 0f) -1f else if (d > 0f) 1f else 0f
    val sideN = min(cfg.f("side"), (cfg.f("visible") - 1f) / 2f).coerceAtLeast(0f)
    val fade = (sideN + 0.6f - a).coerceIn(0f, 1f)
    val cS = cfg.f("cScale") / 100f; val sS = cfg.f("sScale") / 100f; val fS = cfg.f("fScale") / 100f
    var scale = if (a <= 1f) lerpF(cS, sS, a) else lerpF(sS, fS, ((a - 1f) / 1.5f).coerceIn(0f, 1f))
    val cA = cfg.f("cAlpha") / 100f; val sA = cfg.f("sAlpha") / 100f
    var alpha = (if (a <= 1f) lerpF(cA, sA, a) else lerpF(sA, sA * 0.5f, ((a - 1f) / 1.5f).coerceIn(0f, 1f))) * fade
    val rotY = cfg.f("rotY"); val rotX = cfg.f("rotX"); val rotZ = cfg.f("rotZ")
    val depthK = cfg.f("sideDepth") / 100f
    val zk = cfg.f("zTrans") / 100f
    var main = 0f; var cross = 0f; var rx = 0f; var ry = 0f; var rz = 0f
    val a1 = min(a, 1f)
    when (cfg.kind) {
        "coverflow" -> { main = s * (a1 * step + max(a - 1f, 0f) * step * 0.55f); ry = -s * rotY * a1 }
        "cinematic" -> {
            main = s * (a1 * step + max(a - 1f, 0f) * step * 0.7f); ry = -s * rotY * a1
            cross = min(a, 1.6f) * cardCross * 0.07f * depthK * 2f
        }
        "floating" -> {
            main = d * step; cross = sin(d * 0.9f) * cardCross * 0.09f * (0.4f + depthK); ry = -s * rotY * a1; rz = s * rotZ * a1 + d * 1.5f
        }
        "stacked" -> {
            if (d < 0f) { main = d * cardMain * 1.1f; rz = s * (rotZ + 6f) * a1; alpha *= (1f - a1 * 0.9f) }
            else { main = d * step; cross = -d * step * 0.45f; scale *= (1f - 0.05f * a); rz = s * rotZ * a1 }
        }
        "arc" -> {
            val deg = max(abs(rotZ), 8f)
            val th = Math.toRadians((d * deg).toDouble())
            val r = step * 57.3f / deg
            main = (r * sin(th)).toFloat(); cross = (r * (1f - cos(th))).toFloat() * (0.5f + depthK); rz = d * deg
        }
        "cylinder" -> {
            val ang = d * rotY * 0.9f
            val th = Math.toRadians(ang.toDouble())
            val r = cardMain * (0.6f + spacingR(cfg))
            val c = cos(th).toFloat()
            main = (r * sin(th)).toFloat(); ry = -ang
            scale *= (0.72f + 0.28f * max(c, 0f)); alpha *= if (c <= 0f) 0f else c.toDouble().pow(0.6).toFloat()
        }
        "gallery" -> { main = s * (a1 * step + max(a - 1f, 0f) * step * 0.8f); ry = -s * rotY * min(a, 1.3f); rx = rotX * a1; cross = a1 * cardCross * 0.02f }
        "liquid" -> {
            main = s * (a1 * step + max(a - 1f, 0f) * step * 0.7f); ry = -s * rotY * min(a, 1.2f)
            val frac = pos - kotlin.math.floor(pos)
            val bump = sin(PI.toFloat() * frac)
            if (a < 1f) scale *= (1f + 0.05f * bump)
        }
        else -> { main = s * (a1 * step + max(a - 1f, 0f) * step * 0.7f); ry = -s * rotY * min(a, 1.2f) }
    }
    if (cfg.kind != "gallery") rx += rotX * a1
    scale *= (1f - zk * 0.08f * min(a, 3f)) * (1f - depthK * 0.05f * min(a, 3f) * (cfg.f("persp") / 14f))
    rz += if (cfg.kind == "arc" || cfg.kind == "floating" || cfg.kind == "stacked") 0f else s * rotZ * a1
    if (vertical) { o.main = main; o.cross = cross * 0.2f; o.rx = -s * (rotX + rotY * 0.9f) * min(a, 1.2f); o.ry = 0f; o.rz = rz * 0.3f }
    else { o.main = main; o.cross = cross; o.rx = rx; o.ry = ry; o.rz = rz }
    o.scale = scale.coerceAtLeast(0.05f); o.alpha = alpha.coerceIn(0f, 1f)
    o.dim = cfg.f("dim") / 100f * min(a, 1.5f) / 1.5f
    o.blur = cfg.f("blur") * min(a, 2f) / 2f
}

private fun spacingR(cfg: CarCfg) = cfg.f("spacing") / 100f * 0.5f

@Composable
private fun CarCard(
    item: Item, index: Int, n: Int, loop: Boolean, cfg: CarCfg, vertical: Boolean, stepPx: Float, shift: Float,
    cardW: androidx.compose.ui.unit.Dp, cardH: androidx.compose.ui.unit.Dp, posProvider: () -> Float, alignCross: Float,
    totalH: androidx.compose.ui.unit.Dp, extra: androidx.compose.ui.unit.Dp, onTap: () -> Unit, onLongPress: () -> Unit,
) {
    val dens = LocalDensity.current
    val t = remember { CarT() }
    val corner = cfg.f("corner").dp
    val cornerPx = with(dens) { corner.toPx() }
    val shape = RoundedCornerShape(corner)
    val gShape = remember(cornerPx) { RoundedCornerShape(cornerPx) }
    val free = with(dens) { (totalH - extra - cardH).toPx() }.coerceAtLeast(0f)
    val baseMain = if (vertical) with(dens) { ((totalH - extra - cardH) / 2).toPx() } else 0f
    val shadowPx = with(dens) { cfg.f("shadow").dp.toPx() }
    val elevPx = with(dens) { cfg.f("elev").dp.toPx() }
    val camPx = cfg.f("cam") * 8f * dens.density
    val cardMainPx = with(dens) { (if (vertical) cardH else cardW).toPx() }
    val cardCrossPx = with(dens) { (if (vertical) cardW else cardH).toPx() }
    val blurOk = Build.VERSION.SDK_INT >= 31 && T.look.opt.tier >= 2
    val onTapNow by rememberUpdatedState(onTap)
    val onLongNow by rememberUpdatedState(onLongPress)
    val tp = cfg.f("textPos").roundToInt()
    val textBelow = tp == 2
    val imgScale = when (cfg.f("imgMode").roundToInt()) { 1 -> ContentScale.Fit; 2 -> ContentScale.FillBounds; else -> ContentScale.Crop }
    val imgAlign = when (cfg.f("imgPos").roundToInt()) { 0 -> Alignment.TopCenter; 2 -> Alignment.BottomCenter; else -> Alignment.Center }
    val c = T.c

    fun delta(pos: Float): Float {
        var d = index - pos
        if (loop) { val half = n / 2f; while (d > half) d -= n; while (d < -half) d += n }
        return d
    }

    Column(
        Modifier
            .size(cardW, cardH + (if (textBelow) 56.dp else 0.dp))
            .graphicsLayer {
                val pos = posProvider()
                val d = delta(pos)
                carTransform(t, d, cfg, stepPx, cardMainPx, cardCrossPx, vertical, pos)
                val center = (1f - min(abs(d), 1f))
                val lift = elevPx * center
                if (vertical) { translationY = t.main + shift + baseMain; translationX = t.cross - lift * 0.3f } else { translationX = t.main + shift; translationY = t.cross - lift * 0.3f }
                // محاذاة المحور الآخر داخل الحاوية (أفقي: عمودياً بين الأعلى والأسفل)
                if (!vertical) translationY += free * alignCross
                scaleX = t.scale; scaleY = t.scale
                rotationX = t.rx; rotationY = t.ry; rotationZ = t.rz
                alpha = t.alpha
                cameraDistance = camPx
                shadowElevation = (shadowPx * (0.35f + 0.65f * center) + lift).coerceAtLeast(0f)
                this.shape = gShape
                clip = false
                if (blurOk && t.blur > 0.5f) renderEffect = BlurEffect(t.blur * density, t.blur * density, TileMode.Decal)
            }
            .pointerInput(Unit) { detectTapGestures(onTap = { onTapNow() }, onLongPress = { onLongNow() }) },
    ) {
        Box(Modifier.fillMaxWidth().height(cardH).clip(shape).background(c.dense)) {
            RemoteImage(item.poster ?: item.backdrop ?: item.logo, Modifier.fillMaxSize(), reqWidthPx = 520, scale = imgScale, alignment = imgAlign) {}
            val grad = cfg.f("grad") / 100f
            if (grad > 0f && tp != 3) {
                val top = tp == 1
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            if (top) listOf(Color.Black.copy(alpha = grad * 0.75f), Color.Transparent, Color.Transparent)
                            else listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = grad * 0.85f)),
                        ),
                    ),
                )
            }
            CardSheen(cfg.f("refl") / 100f, Modifier.fillMaxSize())
            // لمعة المركزية + تعتيم البعيدة + حد
            if (cfg.f("hi") > 0f) Box(
                Modifier.fillMaxSize().graphicsLayer { alpha = (1f - min(abs(delta(posProvider())), 1f)) * cfg.f("hi") / 100f }
                    .border(2.dp, c.accent2, shape),
            )
            if (cfg.f("border") > 0f) Box(Modifier.fillMaxSize().border((cfg.f("border") * 0.5f).dp, Color.White.copy(alpha = 0.35f), shape))
            if (cfg.f("dim") > 0f) Box(Modifier.fillMaxSize().graphicsLayer { alpha = (min(abs(delta(posProvider())), 1.5f) / 1.5f) * cfg.f("dim") / 100f * 0.8f }.background(Color.Black))
            if (tp == 0 || tp == 1) {
                val glass = cfg.f("glass") / 100f
                Box(Modifier.align(if (tp == 1) Alignment.TopStart else Alignment.BottomStart).fillMaxWidth().padding(10.dp)) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape((corner.value * 0.6f).dp))
                            .background(c.dense.copy(alpha = glass * 0.55f)).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Txt(item.title, style = T.h2, color = Color.White, maxLines = 1)
                        val meta = listOfNotNull(item.year?.toString(), item.quality, item.category).joinToString(" · ")
                        if (meta.isNotEmpty()) Txt(meta, style = T.small, color = Color.White.copy(alpha = 0.75f), maxLines = 1)
                    }
                }
            }
        }
        if (textBelow) Column(Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp, end = 4.dp)) {
            Txt(item.title, style = T.h2, maxLines = 1)
            val meta = listOfNotNull(item.year?.toString(), item.quality, item.category).joinToString(" · ")
            if (meta.isNotEmpty()) Txt(meta, style = T.small, color = T.c.textDim, maxLines = 1)
        }
    }
}
