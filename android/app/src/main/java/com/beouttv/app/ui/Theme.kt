package com.beouttv.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
class BeoutColors(
    val bgTop: Color,
    val bgBottom: Color,
    val glassHi: Color,
    val glassLo: Color,
    val edgeHi: Color,
    val edgeLo: Color,
    /** قاعدة معتمة نسبياً تحت الزجاج الكثيف (الشريط السفلي واللوحات فوق الفيديو). */
    val dense: Color,
    val text: Color,
    val textDim: Color,
    val accent: Color,
    val accent2: Color,
    val accentDeep: Color,
    val danger: Color,
    val dark: Boolean,
)

/*
 * أزرق مصقول: تشبّع متوسط مع ميل طفيف نحو السماوي الرمادي (steel / cobalt) بدل الأزرق الساطع.
 * الخلفيات سلات-نيفي عميق بدل الأسود النقي، فتبقى الحواف الزجاجية مقروءة بلا توهج.
 */
val DarkColors = BeoutColors(
    bgTop = Color(0xFF0D1626), bgBottom = Color(0xFF080E19),
    glassHi = Color(0x24FFFFFF), glassLo = Color(0x0AFFFFFF),
    edgeHi = Color(0x40FFFFFF), edgeLo = Color(0x10FFFFFF),
    dense = Color(0xFF111B2D),
    text = Color(0xFFEEF2F8), textDim = Color(0xFF93A1B8),
    accent = Color(0xFF4C84D4), accent2 = Color(0xFF86B1E4), accentDeep = Color(0xFF35629F),
    danger = Color(0xFFE5566D), dark = true,
)

val LightColors = BeoutColors(
    bgTop = Color(0xFFEEF3FA), bgBottom = Color(0xFFDCE6F3),
    glassHi = Color(0xCCFFFFFF), glassLo = Color(0x80FFFFFF),
    edgeHi = Color(0xFFFFFFFF), edgeLo = Color(0x26294A78),
    dense = Color(0xFFF6F9FD),
    text = Color(0xFF0F1B2D), textDim = Color(0xFF5B6B83),
    accent = Color(0xFF2F68BD), accent2 = Color(0xFF4F89D9), accentDeep = Color(0xFF244F92),
    danger = Color(0xFFC93A52), dark = false,
)

val BlueColors = BeoutColors(
    bgTop = Color(0xFF0F2A4F), bgBottom = Color(0xFF091A33),
    glassHi = Color(0x2EFFFFFF), glassLo = Color(0x10FFFFFF),
    edgeHi = Color(0x4DFFFFFF), edgeLo = Color(0x14FFFFFF),
    dense = Color(0xFF10284A),
    text = Color(0xFFF3F7FD), textDim = Color(0xFFA9BDDA),
    accent = Color(0xFF6AA0E6), accent2 = Color(0xFFA2C7F0), accentDeep = Color(0xFF4A7CC4),
    danger = Color(0xFFF0677E), dark = true,
)

fun colorsFor(theme: String) = when (theme) {
    "light" -> LightColors
    "blue" -> BlueColors
    else -> DarkColors
}

val LocalColors = staticCompositionLocalOf { DarkColors }

object T {
    val c: BeoutColors @Composable get() = LocalColors.current
    // ارتفاع سطر أكبر قليلاً ليناسب الحروف العربية (التشكيل والامتدادات)
    val title = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp)
    val h2 = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 25.sp)
    val body = TextStyle(fontSize = 14.sp, lineHeight = 22.sp)
    val small = TextStyle(fontSize = 12.sp, lineHeight = 18.sp)
}

val GlassShape: Shape = RoundedCornerShape(20)
val CardShape: Shape = RoundedCornerShape(14)

/** سطح زجاجي خفيف: تدرج شبه شفاف + حافة مضيئة رفيعة. بدون Blur فهو رخيص على الأجهزة الضعيفة. */
@Composable
fun Modifier.glass(shape: Shape = GlassShape, strong: Boolean = false): Modifier {
    val c = T.c
    val hi = if (strong) c.glassHi else c.glassHi.copy(alpha = c.glassHi.alpha * 0.8f)
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(hi, c.glassLo)))
        .border(1.dp, Brush.verticalGradient(listOf(c.edgeHi, c.edgeLo)), shape)
}

/** زجاج كثيف: قاعدة شبه معتمة + لمعة زجاجية. للشريط السفلي واللوحات فوق الفيديو. */
@Composable
fun Modifier.glassDense(shape: Shape = GlassShape): Modifier {
    val c = T.c
    return this
        .clip(shape)
        .background(c.dense.copy(alpha = 0.94f))
        .background(Brush.verticalGradient(listOf(c.glassHi, c.glassLo)))
        .border(1.dp, Brush.verticalGradient(listOf(c.edgeHi, c.edgeLo)), shape)
}

@Composable
fun Modifier.screenBackground(): Modifier {
    val c = T.c
    return background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
}

/** تعبئة اللون الأساسي: تدرج رأسي هادئ بين درجتين متقاربتين (لا نيون). */
@Composable
fun Modifier.accentFill(shape: Shape): Modifier {
    val c = T.c
    return clip(shape).background(Brush.verticalGradient(listOf(c.accent, c.accentDeep)))
}
