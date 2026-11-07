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
 * أيقونات مرسومة بالكامل بـ Canvas على شبكة 24x24.
 * سماكة خط موحدة (1.8 وحدة). الحالة النشطة تملأ الجزء المميز بلون ثانوي خافت.
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
    Canvas(modifier.size(size)) {
        if (flip) withTransform({ scale(-1f, 1f) }) { drawIc(ic, color, active) }
        else drawIc(ic, color, active)
    }
}

private fun DrawScope.drawIc(ic: Ic, c: Color, active: Boolean) {
    val u = size.minDimension / 24f
    val sw = 1.8f * u
    val st = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val fillA = c.copy(alpha = 0.28f)
    fun o(x: Float, y: Float) = Offset(x * u, y * u)
    fun path(block: Path.() -> Unit) = Path().apply(block)
    fun Path.m(x: Float, y: Float) = moveTo(x * u, y * u)
    fun Path.l(x: Float, y: Float) = lineTo(x * u, y * u)
    fun Path.q(x1: Float, y1: Float, x2: Float, y2: Float) = quadraticBezierTo(x1 * u, y1 * u, x2 * u, y2 * u)
    fun shape(p: Path, fillIt: Boolean = false) {
        if (fillIt) drawPath(p, fillA, style = Fill)
        drawPath(p, c, style = st)
    }

    when (ic) {
        Ic.Home -> shape(path {
            m(3.5f, 11f); l(12f, 3.8f); l(20.5f, 11f); l(20.5f, 19.5f); q(20.5f, 20.5f, 19.5f, 20.5f)
            l(14.5f, 20.5f); l(14.5f, 14.5f); l(9.5f, 14.5f); l(9.5f, 20.5f); l(4.5f, 20.5f); q(3.5f, 20.5f, 3.5f, 19.5f); close()
        }, active)
        Ic.Channels -> {
            val r = androidx.compose.ui.geometry.Rect(o(3f, 7f), Size(18f * u, 12.5f * u))
            val p = path { addRoundRect(androidx.compose.ui.geometry.RoundRect(r, androidx.compose.ui.geometry.CornerRadius(3f * u))) }
            shape(p, active)
            drawLine(c, o(8.5f, 3.5f), o(12f, 7f), sw, StrokeCap.Round)
            drawLine(c, o(15.5f, 3.5f), o(12f, 7f), sw, StrokeCap.Round)
            drawCircle(c, 1.3f * u, o(12f, 13.2f), style = if (active) Fill else st)
        }
        Ic.Movies -> {
            val r = androidx.compose.ui.geometry.Rect(o(3.5f, 4f), Size(17f * u, 16f * u))
            val p = path { addRoundRect(androidx.compose.ui.geometry.RoundRect(r, androidx.compose.ui.geometry.CornerRadius(3f * u))) }
            shape(p, active)
            drawLine(c, o(3.5f, 9f), o(20.5f, 9f), sw)
            drawLine(c, o(8f, 4f), o(6.5f, 9f), sw, StrokeCap.Round)
            drawLine(c, o(13f, 4f), o(11.5f, 9f), sw, StrokeCap.Round)
            drawLine(c, o(18f, 4f), o(16.5f, 9f), sw, StrokeCap.Round)
        }
        Ic.Search -> {
            drawCircle(c, 6.3f * u, o(10.5f, 10.5f), style = st)
            if (active) drawCircle(fillA, 6.3f * u, o(10.5f, 10.5f))
            drawLine(c, o(15.3f, 15.3f), o(20.5f, 20.5f), sw, StrokeCap.Round)
        }
        Ic.Settings -> {
            for (i in 0..2) {
                val y = 6f + i * 6f
                drawLine(c, o(3.5f, y), o(20.5f, y), sw, StrokeCap.Round)
            }
            drawCircle(if (active) c else Color.Transparent, 2.3f * u, o(8f, 6f))
            drawCircle(c, 2.3f * u, o(8f, 6f), style = st)
            drawCircle(if (active) c else Color.Transparent, 2.3f * u, o(16f, 12f))
            drawCircle(c, 2.3f * u, o(16f, 12f), style = st)
            drawCircle(if (active) c else Color.Transparent, 2.3f * u, o(10f, 18f))
            drawCircle(c, 2.3f * u, o(10f, 18f), style = st)
        }
        Ic.Play -> shape(path { m(7f, 4.5f); l(19f, 12f); l(7f, 19.5f); close() }, true)
        Ic.Pause -> {
            drawRoundRect(c, o(6f, 4.5f), Size(4f * u, 15f * u), androidx.compose.ui.geometry.CornerRadius(1.5f * u))
            drawRoundRect(c, o(14f, 4.5f), Size(4f * u, 15f * u), androidx.compose.ui.geometry.CornerRadius(1.5f * u))
        }
        Ic.Volume -> {
            shape(path { m(4f, 9.5f); l(8f, 9.5f); l(12.5f, 5.5f); l(12.5f, 18.5f); l(8f, 14.5f); l(4f, 14.5f); close() }, active)
            drawArc(c, -45f, 90f, false, o(10f, 8f), Size(8f * u, 8f * u), style = st)
            drawArc(c, -55f, 110f, false, o(7f, 4.5f), Size(15f * u, 15f * u), style = st)
        }
        Ic.Mute -> {
            shape(path { m(4f, 9.5f); l(8f, 9.5f); l(12.5f, 5.5f); l(12.5f, 18.5f); l(8f, 14.5f); l(4f, 14.5f); close() })
            drawLine(c, o(16f, 9.5f), o(21f, 14.5f), sw, StrokeCap.Round)
            drawLine(c, o(21f, 9.5f), o(16f, 14.5f), sw, StrokeCap.Round)
        }
        Ic.Subtitle -> {
            val r = androidx.compose.ui.geometry.Rect(o(3f, 5f), Size(18f * u, 14f * u))
            shape(path { addRoundRect(androidx.compose.ui.geometry.RoundRect(r, androidx.compose.ui.geometry.CornerRadius(3.5f * u))) }, active)
            drawLine(c, o(7f, 11f), o(11f, 11f), sw, StrokeCap.Round)
            drawLine(c, o(13.5f, 11f), o(17f, 11f), sw, StrokeCap.Round)
            drawLine(c, o(7f, 15f), o(9.5f, 15f), sw, StrokeCap.Round)
            drawLine(c, o(12f, 15f), o(17f, 15f), sw, StrokeCap.Round)
        }
        Ic.Quality -> {
            val r = androidx.compose.ui.geometry.Rect(o(3f, 6f), Size(18f * u, 12f * u))
            shape(path { addRoundRect(androidx.compose.ui.geometry.RoundRect(r, androidx.compose.ui.geometry.CornerRadius(3f * u))) }, active)
            drawLine(c, o(7f, 10f), o(7f, 14f), sw, StrokeCap.Round)
            drawLine(c, o(7f, 12f), o(10f, 12f), sw, StrokeCap.Round)
            drawLine(c, o(10f, 10f), o(10f, 14f), sw, StrokeCap.Round)
            drawLine(c, o(13.5f, 14f), o(13.5f, 10f), sw, StrokeCap.Round)
            drawLine(c, o(13.5f, 10f), o(17f, 10f), sw, StrokeCap.Round)
            drawLine(c, o(13.5f, 14f), o(17f, 14f), sw, StrokeCap.Round)
        }
        Ic.Fullscreen -> {
            shape(path { m(4f, 9f); l(4f, 4f); l(9f, 4f) })
            shape(path { m(15f, 4f); l(20f, 4f); l(20f, 9f) })
            shape(path { m(20f, 15f); l(20f, 20f); l(15f, 20f) })
            shape(path { m(9f, 20f); l(4f, 20f); l(4f, 15f) })
        }
        Ic.ExitFullscreen -> {
            shape(path { m(9f, 4f); l(9f, 9f); l(4f, 9f) })
            shape(path { m(20f, 9f); l(15f, 9f); l(15f, 4f) })
            shape(path { m(15f, 20f); l(15f, 15f); l(20f, 15f) })
            shape(path { m(4f, 15f); l(9f, 15f); l(9f, 20f) })
        }
        Ic.Heart -> shape(path {
            m(12f, 20f)
            cubicTo(4f * u, 14.5f * u, 3f * u, 10f * u, 5.2f * u, 7f * u)
            cubicTo(7f * u, 4.8f * u, 10.5f * u, 5.2f * u, 12f * u, 8f * u)
            cubicTo(13.5f * u, 5.2f * u, 17f * u, 4.8f * u, 18.8f * u, 7f * u)
            cubicTo(21f * u, 10f * u, 20f * u, 14.5f * u, 12f * u, 20f * u)
            close()
        }, active)
        Ic.Download -> {
            drawLine(c, o(12f, 4f), o(12f, 15f), sw, StrokeCap.Round)
            shape(path { m(7.5f, 10.8f); l(12f, 15.3f); l(16.5f, 10.8f) })
            shape(path { m(4.5f, 16.5f); l(4.5f, 19f); q(4.5f, 20f, 5.5f, 20f); l(18.5f, 20f); q(19.5f, 20f, 19.5f, 19f); l(19.5f, 16.5f) })
        }
        Ic.Back -> {
            drawLine(c, o(20f, 12f), o(5f, 12f), sw, StrokeCap.Round)
            shape(path { m(11f, 5.5f); l(4.5f, 12f); l(11f, 18.5f) })
        }
        Ic.Next -> {
            shape(path { m(6f, 5f); l(15f, 12f); l(6f, 19f); close() }, true)
            drawLine(c, o(18.5f, 5f), o(18.5f, 19f), sw, StrokeCap.Round)
        }
        Ic.Prev -> {
            shape(path { m(18f, 5f); l(9f, 12f); l(18f, 19f); close() }, true)
            drawLine(c, o(5.5f, 5f), o(5.5f, 19f), sw, StrokeCap.Round)
        }
        Ic.More -> {
            drawCircle(c, 1.7f * u, o(12f, 5.5f)); drawCircle(c, 1.7f * u, o(12f, 12f)); drawCircle(c, 1.7f * u, o(12f, 18.5f))
        }
        Ic.Forward10 -> {
            drawArc(c, -60f, 280f, false, o(4.5f, 5f), Size(15f * u, 15f * u), style = st)
            shape(path { m(15f, 3.2f); l(18.6f, 5.6f); l(15.2f, 8.2f) })
            drawLine(c, o(10.2f, 10.8f), o(10.2f, 15f), sw * 0.9f, StrokeCap.Round)
            drawCircle(c, 1.5f * u, o(14f, 12.9f), style = Stroke(sw * 0.9f))
        }
        Ic.Back10 -> {
            drawArc(c, -220f, 280f, false, o(4.5f, 5f), Size(15f * u, 15f * u), style = st)
            shape(path { m(9f, 3.2f); l(5.4f, 5.6f); l(8.8f, 8.2f) })
            drawLine(c, o(10.2f, 10.8f), o(10.2f, 15f), sw * 0.9f, StrokeCap.Round)
            drawCircle(c, 1.5f * u, o(14f, 12.9f), style = Stroke(sw * 0.9f))
        }
        Ic.Check -> shape(path { m(5f, 12.5f); l(10f, 17.5f); l(19f, 7f) })
        Ic.Close -> {
            drawLine(c, o(6f, 6f), o(18f, 18f), sw, StrokeCap.Round)
            drawLine(c, o(18f, 6f), o(6f, 18f), sw, StrokeCap.Round)
        }
        Ic.Aspect -> {
            val r = androidx.compose.ui.geometry.Rect(o(3f, 6f), Size(18f * u, 12f * u))
            shape(path { addRoundRect(androidx.compose.ui.geometry.RoundRect(r, androidx.compose.ui.geometry.CornerRadius(3f * u))) }, active)
            shape(path { m(7.5f, 11.5f); l(7.5f, 9.5f); l(9.5f, 9.5f) })
            shape(path { m(16.5f, 12.5f); l(16.5f, 14.5f); l(14.5f, 14.5f) })
        }
        Ic.Audio -> {
            drawLine(c, o(5f, 9f), o(5f, 15f), sw, StrokeCap.Round)
            drawLine(c, o(9f, 5f), o(9f, 19f), sw, StrokeCap.Round)
            drawLine(c, o(13f, 8f), o(13f, 16f), sw, StrokeCap.Round)
            drawLine(c, o(17f, 4f), o(17f, 20f), sw, StrokeCap.Round)
            drawLine(c, o(21f, 10f), o(21f, 14f), sw, StrokeCap.Round)
        }
    }
}
