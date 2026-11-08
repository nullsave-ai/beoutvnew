package com.beouttv.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class Ic {
    Home, Channels, Movies, Search, Settings, Play, Pause, Volume, Mute, Subtitle,
    Quality, Fullscreen, ExitFullscreen, Heart, Download, Back, Next, Prev, More,
    Forward10, Back10, Check, Close, Audio, Aspect,
}

/** أيقونات اتجاهية تنعكس تلقائياً في واجهة RTL (الشعارات والصور لا تنعكس أبداً). */
private val Directional = setOf(Ic.Back, Ic.Next, Ic.Prev)

/**
 * مجموعة أيقونات مرسومة بالكامل بـ Canvas على شبكة 24x24، بلغة بصرية واحدة:
 * أجسام مستديرة الزوايا، خط 1.7 بنهايات دائرية، ولمسة تمييز صغيرة (نقطة/شرارة) في كل أيقونة.
 * النمط (من الإعدادات): line = خطوط فقط، duo = جسم شفاف + خط (الافتراضي)، solid = مصمتة.
 * الحالة النشطة تُغمّق تعبئة الجسم.
 */
@Composable
fun BIcon(
    ic: Ic,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = T.c.text,
    active: Boolean = false,
) {
    val color by animateColorAsState(tint, tween(160), label = "ic")
    val flip = ic in Directional && LocalLayoutDirection.current == LayoutDirection.Rtl
    val style = T.look.icon
    val spark = T.c.accent2
    Canvas(modifier.size(size)) {
        if (flip) withTransform({ scale(-1f, 1f) }) { drawIc(ic, color, spark, style, active) }
        else drawIc(ic, color, spark, style, active)
    }
}

private fun DrawScope.drawIc(ic: Ic, c: Color, spark: Color, style: String, active: Boolean) {
    val u = size.minDimension / 24f
    val sw = 1.7f * u
    val st = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val bodyA = when (style) { "line" -> if (active) 0.2f else 0f; "solid" -> 1f; else -> if (active) 0.5f else 0.2f }
    val accentC = if (style == "line") c else spark
    fun o(x: Float, y: Float) = Offset(x * u, y * u)
    fun path(block: Path.() -> Unit) = Path().apply(block)
    fun Path.m(x: Float, y: Float) = moveTo(x * u, y * u)
    fun Path.l(x: Float, y: Float) = lineTo(x * u, y * u)
    fun Path.q(x1: Float, y1: Float, x2: Float, y2: Float) = quadraticBezierTo(x1 * u, y1 * u, x2 * u, y2 * u)
    fun Path.cb(a: Float, b: Float, c2: Float, d: Float, e: Float, f: Float) = cubicTo(a * u, b * u, c2 * u, d * u, e * u, f * u)
    /** جسم: تعبئة شفافة ثم الخط (ولا خط في النمط المصمت). */
    fun body(p: Path) {
        if (bodyA > 0f) drawPath(p, c.copy(alpha = bodyA), style = Fill)
        if (style != "solid") drawPath(p, c, style = st)
    }
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, w: Float = sw, col: Color = c) = drawLine(col, o(x1, y1), o(x2, y2), w, StrokeCap.Round)
    fun dot(x: Float, y: Float, r: Float, col: Color = accentC) = drawCircle(col, r * u, o(x, y))
    /** مضلع بزوايا مستديرة نصف قطرها r. */
    fun rpoly(pts: List<Pair<Float, Float>>, r: Float): Path = Path().apply {
        val n = pts.size
        for (i in 0 until n) {
            val p = pts[i]; val a = pts[(i + n - 1) % n]; val b = pts[(i + 1) % n]
            fun unit(from: Pair<Float, Float>, to: Pair<Float, Float>): Pair<Float, Float> {
                val dx = to.first - from.first; val dy = to.second - from.second
                val len = kotlin.math.sqrt(dx * dx + dy * dy)
                return dx / len to dy / len
            }
            val ua = unit(p, a); val ub = unit(p, b)
            val sx = (p.first + ua.first * r) * u; val sy = (p.second + ua.second * r) * u
            val ex = (p.first + ub.first * r) * u; val ey = (p.second + ub.second * r) * u
            if (i == 0) moveTo(sx, sy) else lineTo(sx, sy)
            quadraticBezierTo(p.first * u, p.second * u, ex, ey)
        }
        close()
    }
    fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float) = path {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(androidx.compose.ui.geometry.Rect(o(x, y), Size(w * u, h * u)), androidx.compose.ui.geometry.CornerRadius(r * u)))
    }

    when (ic) {
        // منزل: سقف منحنٍ، باب قوسي، وشرارة المدخنة
        Ic.Home -> {
            body(path {
                m(4f, 11.4f); q(4f, 10.4f, 4.8f, 9.8f); l(10.8f, 5f); q(12f, 4.1f, 13.2f, 5f); l(19.2f, 9.8f)
                q(20f, 10.4f, 20f, 11.4f); l(20f, 17f); q(20f, 20f, 17f, 20f); l(7f, 20f); q(4f, 20f, 4f, 17f); close()
            })
            drawPath(path { m(10f, 20f); l(10f, 15.5f); q(10f, 14.3f, 11.2f, 14.3f); l(12.8f, 14.3f); q(14f, 14.3f, 14f, 15.5f); l(14f, 20f) }, if (style == "solid") Color.Black.copy(alpha = 0.35f) else c, style = st)
            dot(18f, 5.2f, 1.2f)
        }
        // قنوات: شاشة بث بأمواج إشارة حول نقطة
        Ic.Channels -> {
            body(rrect(3f, 4.5f, 18f, 15f, 5f))
            dot(12f, 12f, 1.9f, if (style == "solid") Color.Black.copy(alpha = 0.4f) else accentC)
            drawArc(c, -42f, 84f, false, o(8.2f, 8.2f), Size(7.6f * u, 7.6f * u), style = st)
            drawArc(c, 138f, 84f, false, o(8.2f, 8.2f), Size(7.6f * u, 7.6f * u), style = st)
        }
        // أفلام: بكرة فيلم بأربع فتحات
        Ic.Movies -> {
            body(path { addOval(androidx.compose.ui.geometry.Rect(o(3.5f, 3.5f), Size(17f * u, 17f * u))) })
            val hole = if (style == "solid") Color.Black.copy(alpha = 0.4f) else accentC
            dot(12f, 12f, 1.5f, hole)
            dot(12f, 7.4f, 1.4f, hole); dot(12f, 16.6f, 1.4f, hole); dot(7.4f, 12f, 1.4f, hole); dot(16.6f, 12f, 1.4f, hole)
        }
        Ic.Search -> {
            body(path { addOval(androidx.compose.ui.geometry.Rect(o(3.5f, 3.5f), Size(12.5f * u, 12.5f * u))) })
            line(14.6f, 14.6f, 20f, 20f, sw * 1.25f)
            dot(19f, 5.2f, 1.1f)
        }
        // إعدادات: مفاتيح انزلاق رأسية
        Ic.Settings -> {
            for ((x, y) in listOf(6f to 14f, 12f to 8f, 18f to 15.5f)) {
                line(x, 4f, x, y - 2.9f); line(x, y + 2.9f, x, 20f)
                body(path { addOval(androidx.compose.ui.geometry.Rect(o(x - 2.5f, y - 2.5f), Size(5f * u, 5f * u))) })
            }
        }
        Ic.Play -> body(rpoly(listOf(7f to 4.5f, 19.5f to 12f, 7f to 19.5f), 2.2f))
        Ic.Pause -> {
            body(rrect(6f, 4.5f, 4f, 15f, 1.8f)); body(rrect(14f, 4.5f, 4f, 15f, 1.8f))
        }
        Ic.Volume -> {
            body(rpoly(listOf(4f to 9.5f, 8f to 9.5f, 12.5f to 5.5f, 12.5f to 18.5f, 8f to 14.5f, 4f to 14.5f), 1.2f))
            drawArc(accentC, -45f, 90f, false, o(10f, 8f), Size(8f * u, 8f * u), style = st)
            drawArc(c, -55f, 110f, false, o(7f, 4.5f), Size(15f * u, 15f * u), style = st)
        }
        Ic.Mute -> {
            body(rpoly(listOf(4f to 9.5f, 8f to 9.5f, 12.5f to 5.5f, 12.5f to 18.5f, 8f to 14.5f, 4f to 14.5f), 1.2f))
            line(16f, 9.5f, 21f, 14.5f, col = accentC); line(21f, 9.5f, 16f, 14.5f, col = accentC)
        }
        // ترجمة: فقاعة حوار بذيل وسطرين
        Ic.Subtitle -> {
            body(path {
                m(6.5f, 4.5f); l(17.5f, 4.5f); q(21f, 4.5f, 21f, 8f); l(21f, 12.5f); q(21f, 16f, 17.5f, 16f)
                l(12f, 16f); l(7.5f, 19.8f); l(7.5f, 16f); l(6.5f, 16f); q(3f, 16f, 3f, 12.5f); l(3f, 8f); q(3f, 4.5f, 6.5f, 4.5f); close()
            })
            line(7f, 9f, 12f, 9f); line(14.5f, 9f, 17f, 9f, col = accentC); line(7f, 12f, 9.5f, 12f, col = accentC); line(12f, 12f, 17f, 12f)
        }
        // جودة: أعمدة إشارة متصاعدة
        Ic.Quality -> {
            body(rrect(4f, 14f, 3.4f, 6f, 1.4f)); body(rrect(9f, 10f, 3.4f, 10f, 1.4f))
            body(rrect(14f, 6f, 3.4f, 14f, 1.4f)); dot(19.7f, 4.6f, 1.2f)
        }
        Ic.Fullscreen -> {
            drawPath(path { m(4f, 9.5f); l(4f, 6.5f); q(4f, 4f, 6.5f, 4f); l(9.5f, 4f) }, c, style = st)
            drawPath(path { m(14.5f, 4f); l(17.5f, 4f); q(20f, 4f, 20f, 6.5f); l(20f, 9.5f) }, c, style = st)
            drawPath(path { m(20f, 14.5f); l(20f, 17.5f); q(20f, 20f, 17.5f, 20f); l(14.5f, 20f) }, c, style = st)
            drawPath(path { m(9.5f, 20f); l(6.5f, 20f); q(4f, 20f, 4f, 17.5f); l(4f, 14.5f) }, c, style = st)
            dot(12f, 12f, 1.3f)
        }
        Ic.ExitFullscreen -> {
            drawPath(path { m(9.5f, 4f); l(9.5f, 7f); q(9.5f, 9.5f, 7f, 9.5f); l(4f, 9.5f) }, c, style = st)
            drawPath(path { m(20f, 9.5f); l(17f, 9.5f); q(14.5f, 9.5f, 14.5f, 7f); l(14.5f, 4f) }, c, style = st)
            drawPath(path { m(14.5f, 20f); l(14.5f, 17f); q(14.5f, 14.5f, 17f, 14.5f); l(20f, 14.5f) }, c, style = st)
            drawPath(path { m(4f, 14.5f); l(7f, 14.5f); q(9.5f, 14.5f, 9.5f, 17f); l(9.5f, 20f) }, c, style = st)
        }
        Ic.Heart -> {
            body(path {
                m(12f, 20.2f)
                cb(5f, 15.2f, 2.6f, 11.4f, 4.4f, 8f)
                cb(6f, 5f, 10.2f, 5.2f, 12f, 8.4f)
                cb(13.8f, 5.2f, 18f, 5f, 19.6f, 8f)
                cb(21.4f, 11.4f, 19f, 15.2f, 12f, 20.2f)
                close()
            })
            dot(8f, 8.6f, 0.9f, if (style == "solid") Color.White.copy(alpha = 0.6f) else accentC)
        }
        Ic.Download -> {
            body(rrect(4f, 15f, 16f, 5f, 2.4f))
            line(12f, 3.5f, 12f, 13f)
            drawPath(path { m(8f, 9.2f); l(12f, 13.2f); l(16f, 9.2f) }, accentC, style = st)
        }
        Ic.Back -> {
            drawPath(path { m(14.5f, 5.5f); l(8f, 12f); l(14.5f, 18.5f) }, c, style = Stroke(sw * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Ic.Next -> {
            body(rpoly(listOf(5.5f to 5f, 15.5f to 12f, 5.5f to 19f), 1.6f)); body(rrect(17.5f, 5f, 2.8f, 14f, 1.2f))
        }
        Ic.Prev -> {
            body(rpoly(listOf(18.5f to 5f, 8.5f to 12f, 18.5f to 19f), 1.6f)); body(rrect(3.7f, 5f, 2.8f, 14f, 1.2f))
        }
        Ic.More -> {
            dot(5.5f, 12f, 1.8f, c); dot(12f, 12f, 1.8f, accentC); dot(18.5f, 12f, 1.8f, c)
        }
        Ic.Forward10 -> {
            drawArc(c, -60f, 280f, false, o(4.5f, 5f), Size(15f * u, 15f * u), style = st)
            drawPath(path { m(15f, 3.2f); l(18.6f, 5.6f); l(15.2f, 8.2f) }, accentC, style = st)
            line(10.2f, 10.8f, 10.2f, 15f, sw * 0.9f)
            drawCircle(c, 1.5f * u, o(14f, 12.9f), style = Stroke(sw * 0.9f))
        }
        Ic.Back10 -> {
            drawArc(c, -220f, 280f, false, o(4.5f, 5f), Size(15f * u, 15f * u), style = st)
            drawPath(path { m(9f, 3.2f); l(5.4f, 5.6f); l(8.8f, 8.2f) }, accentC, style = st)
            line(10.2f, 10.8f, 10.2f, 15f, sw * 0.9f)
            drawCircle(c, 1.5f * u, o(14f, 12.9f), style = Stroke(sw * 0.9f))
        }
        Ic.Check -> drawPath(path { m(5f, 12.5f); l(10f, 17.5f); l(19f, 7f) }, c, style = Stroke(sw * 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        Ic.Close -> {
            line(6.5f, 6.5f, 17.5f, 17.5f, sw * 1.15f); line(17.5f, 6.5f, 6.5f, 17.5f, sw * 1.15f)
        }
        // نسبة العرض: إطار بزوايا داخلية
        Ic.Aspect -> {
            body(rrect(3f, 6f, 18f, 12f, 4f))
            drawPath(path { m(7.5f, 11f); l(7.5f, 9.5f); l(9f, 9.5f) }, accentC, style = st)
            drawPath(path { m(16.5f, 13f); l(16.5f, 14.5f); l(15f, 14.5f) }, accentC, style = st)
        }
        // صوت: موجات متساوية بارتفاعات مختلفة
        Ic.Audio -> {
            line(5f, 10f, 5f, 14f); line(9f, 6f, 9f, 18f, col = accentC); line(13f, 9f, 13f, 15f)
            line(17f, 4.5f, 17f, 19.5f, col = accentC); line(21f, 10.5f, 21f, 13.5f)
        }
    }
}
