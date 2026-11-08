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

/** رمادي سينمائي (المرجع الأول): سلات رمادي مزرق وزجاج رمادي. */
val SlateColors = BeoutColors(
    bgTop = Color(0xFF182026), bgBottom = Color(0xFF0E1317),
    glassHi = Color(0x2E8FA3AE), glassLo = Color(0x0F8FA3AE),
    edgeHi = Color(0x40B4C3CC), edgeLo = Color(0x12B4C3CC),
    dense = Color(0xFF1F292F),
    text = Color(0xFFE9EFF2), textDim = Color(0xFF8C9BA5),
    accent = Color(0xFF7E97A4), accent2 = Color(0xFFC9D8DF), accentDeep = Color(0xFF546A76),
    danger = Color(0xFFE5636F), dark = true,
)

/** بنفسجي داكن (المرجع الثاني). */
val VioletColors = BeoutColors(
    bgTop = Color(0xFF1D1830), bgBottom = Color(0xFF120F1D),
    glassHi = Color(0x2EB9A8FF), glassLo = Color(0x0CB9A8FF),
    edgeHi = Color(0x40CFC4FF), edgeLo = Color(0x14CFC4FF),
    dense = Color(0xFF231D38),
    text = Color(0xFFF3F0FB), textDim = Color(0xFF9E97B8),
    accent = Color(0xFF6C5CE0), accent2 = Color(0xFFB0A5FF), accentDeep = Color(0xFF4636A8),
    danger = Color(0xFFF0677E), dark = true,
)

fun colorsFor(theme: String) = when (theme) {
    "slate" -> SlateColors
    "violet" -> VioletColors
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
    val look: Look @Composable get() = LocalLook.current
    val card: Shape @Composable get() = RoundedCornerShape((16 * look.radius).dp)
    val row: Shape @Composable get() = RoundedCornerShape((12 * look.radius).dp)
    val hero: Shape @Composable get() = RoundedCornerShape((28 * look.radius).dp)
}

val GlassShape: Shape = RoundedCornerShape(20)
val CardShape: Shape = RoundedCornerShape(16.dp)

/** سطح زجاجي خفيف: تدرج شبه شفاف + حافة مضيئة رفيعة. بدون Blur فهو رخيص على الأجهزة الضعيفة. */
@Composable
fun Modifier.glass(shape: Shape = GlassShape, strong: Boolean = false): Modifier {
    val c = T.c
    val k = T.look.glass
    val base = c.glassHi.alpha * (if (strong) 1f else 0.8f) * k * 1.15f
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(c.glassHi.copy(alpha = base.coerceAtMost(0.9f)), c.glassLo.copy(alpha = (c.glassLo.alpha * k).coerceAtMost(0.9f)))))
}

/** زجاج كثيف: قاعدة شبه معتمة + لمعة زجاجية. للشريط السفلي واللوحات فوق الفيديو. */
@Composable
fun Modifier.glassDense(shape: Shape = GlassShape): Modifier {
    val c = T.c
    return this
        .clip(shape)
        .background(c.dense.copy(alpha = (0.94f * (0.6f + 0.4f * T.look.glass)).coerceIn(0.55f, 0.98f)))
        .background(Brush.verticalGradient(listOf(c.glassHi, c.glassLo)))
}

/** سطح مسطح للمحتوى العادي (بطاقات، صفوف، تصنيفات): لون واحد بلا حافة ولا زجاج. */
@Composable
fun Modifier.flat(shape: Shape = T.card): Modifier =
    clip(shape).background(T.c.dense.copy(alpha = 0.72f))

/** حالة التحديد: صبغة زرقاء شفافة وحافة رفيعة (بدل التعبئة الصلبة والتوهج). */
@Composable
fun Modifier.tinted(shape: Shape): Modifier {
    val c = T.c
    return clip(shape).background(c.accent.copy(alpha = 0.32f))
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
