package com.beouttv.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

private fun darkTheme(
    top: Long, bottom: Long, dense: Long, text: Long, dim: Long,
    accent: Long, accent2: Long, deep: Long, tintRaw: Long = 0xFFFFFF, glassA: Int = 0x26,
) = darkOf(top, bottom, dense, text, dim, accent, accent2, deep, tintRaw and 0xFFFFFFL, glassA)

/** tint يجب أن يكون RGB فقط (بلا ألفا)، وإلا صار الزجاج أبيض معتماً. */
private fun darkOf(
    top: Long, bottom: Long, dense: Long, text: Long, dim: Long,
    accent: Long, accent2: Long, deep: Long, tint: Long, glassA: Int,
) = BeoutColors(
    bgTop = Color(top), bgBottom = Color(bottom),
    glassHi = Color((glassA.toLong() shl 24) or tint), glassLo = Color(((glassA / 3).toLong() shl 24) or tint),
    edgeHi = Color((0x40L shl 24) or tint), edgeLo = Color((0x12L shl 24) or tint),
    dense = Color(dense), text = Color(text), textDim = Color(dim),
    accent = Color(accent), accent2 = Color(accent2), accentDeep = Color(deep),
    danger = Color(0xFFE5566D), dark = true,
)

/** درجات رمادية: من الفحمي الدافئ حتى الأسود النقي. */
val GraphiteColors = darkTheme(0xFF2A2C30, 0xFF1B1D20, 0xFF33363B, 0xFFEDEEF0, 0xFF9A9FA8, 0xFF9AA3B0, 0xFFD3D8DF, 0xFF6B7482, 0xFFFFFFFF)
val CharcoalColors = darkTheme(0xFF1C1D1F, 0xFF111213, 0xFF242527, 0xFFEBEBEC, 0xFF8E9094, 0xFFA7A9AD, 0xFFDADBDD, 0xFF6D6F73, 0xFFFFFFFF)
val AmoledColors = darkTheme(0xFF050505, 0xFF000000, 0xFF111111, 0xFFF2F2F2, 0xFF85888D, 0xFF8FA6C4, 0xFFC4D3E6, 0xFF5B7494, 0xFFFFFFFF, 0x1F)
val AshColors = BeoutColors(
    bgTop = Color(0xFFECEDEF), bgBottom = Color(0xFFD9DBDF),
    glassHi = Color(0xCCFFFFFF), glassLo = Color(0x80FFFFFF),
    edgeHi = Color(0xFFFFFFFF), edgeLo = Color(0x26333A44),
    dense = Color(0xFFF7F7F8),
    text = Color(0xFF1A1C20), textDim = Color(0xFF626872),
    accent = Color(0xFF4A5565), accent2 = Color(0xFF6C7889), accentDeep = Color(0xFF343C49),
    danger = Color(0xFFC93A52), dark = false,
)
val ForestColors = darkTheme(0xFF14241E, 0xFF0B1511, 0xFF1A2F27, 0xFFECF4EF, 0xFF8FA89C, 0xFF3FA57F, 0xFF8AD6B8, 0xFF277A5C, 0xFFB8FFE0)
val WineColors = darkTheme(0xFF2A141C, 0xFF180A10, 0xFF351A24, 0xFFF6EDF0, 0xFFB398A1, 0xFFC24C69, 0xFFEE9AB0, 0xFF8F2F49, 0xFFFFC8D6)
val SandColors = BeoutColors(
    bgTop = Color(0xFFF3ECDD), bgBottom = Color(0xFFE6DAC2),
    glassHi = Color(0xCCFFFFFF), glassLo = Color(0x80FFFFFF),
    edgeHi = Color(0xFFFFFFFF), edgeLo = Color(0x2674582A),
    dense = Color(0xFFFAF5EA),
    text = Color(0xFF2B2316), textDim = Color(0xFF7A6B52),
    accent = Color(0xFFB07A2A), accent2 = Color(0xFFD29A4A), accentDeep = Color(0xFF8A5C18),
    danger = Color(0xFFC93A52), dark = false,
)

/** ثيم مخصص: ألوان المستخدم (HEX) فوق قاعدة داكنة؛ القيم الناقصة تُشتق من الموجود. */
fun customColors(ap: Map<String, String>): BeoutColors {
    val bg = parseHex(ap["ct_bg"]) ?: Color(0xFF0D1626)
    val bg2 = parseHex(ap["ct_bg2"]) ?: lerpColor(bg, Color.Black, 0.4f)
    val surface = parseHex(ap["ct_surface"]) ?: lerpColor(bg, Color.White, 0.08f)
    val text = parseHex(ap["ct_text"]) ?: Color(0xFFEEF2F8)
    val accent = parseHex(ap["ct_accent"]) ?: Color(0xFF4C84D4)
    val dark = (bg.red * 0.299f + bg.green * 0.587f + bg.blue * 0.114f) < 0.5f
    val tint = if (dark) Color.White else Color.White
    return BeoutColors(
        bgTop = bg, bgBottom = bg2,
        glassHi = tint.copy(alpha = if (dark) 0.14f else 0.8f), glassLo = tint.copy(alpha = if (dark) 0.05f else 0.5f),
        edgeHi = tint.copy(alpha = 0.25f), edgeLo = (if (dark) Color.White else Color(0xFF223044)).copy(alpha = 0.1f),
        dense = surface, text = text, textDim = text.copy(alpha = 0.6f),
        accent = accent, accent2 = lerpColor(accent, Color.White, 0.35f), accentDeep = lerpColor(accent, Color.Black, 0.35f),
        danger = Color(0xFFE5566D), dark = dark,
    )
}

fun colorsFor(theme: String, ap: Map<String, String> = emptyMap()) = when (theme) {
    "custom" -> customColors(ap)
    "slate" -> SlateColors
    "violet" -> VioletColors
    "light" -> LightColors
    "blue" -> BlueColors
    "graphite" -> GraphiteColors
    "charcoal" -> CharcoalColors
    "amoled" -> AmoledColors
    "ash" -> AshColors
    "forest" -> ForestColors
    "wine" -> WineColors
    "sand" -> SandColors
    else -> DarkColors
}

/** كل الثيمات بترتيب العرض (المفتاح، الاسم). */
val ThemeList = listOf(
    "dark" to S.dark, "light" to S.light, "blue" to S.blue, "slate" to S.slate, "violet" to S.violet,
    "graphite" to "جرافيت", "charcoal" to "فحمي", "amoled" to "أسود نقي", "ash" to "رمادي فاتح",
    "forest" to "غابة", "wine" to "نبيذ", "sand" to "رمل", "custom" to "مخصص",
)

// ---------- اللون المميز ----------
private class AccentSet(val a: Color, val a2: Color, val deep: Color)
private val AccentSets = mapOf(
    "teal" to AccentSet(Color(0xFF2FB5A6), Color(0xFF7FDCCF), Color(0xFF1C8478)),
    "rose" to AccentSet(Color(0xFFE0587A), Color(0xFFF2A0B6), Color(0xFFA83856)),
    "amber" to AccentSet(Color(0xFFE59A2E), Color(0xFFF5C77A), Color(0xFFA9701A)),
    "green" to AccentSet(Color(0xFF45B36B), Color(0xFF93DDAB), Color(0xFF2E8049)),
    "gold" to AccentSet(Color(0xFFD4AF37), Color(0xFFEBD27A), Color(0xFF9C7E1B)),
    "yellow" to AccentSet(Color(0xFFF2D024), Color(0xFFF8E57E), Color(0xFFB59A0F)),
    "orange" to AccentSet(Color(0xFFF07A2A), Color(0xFFF7AE78), Color(0xFFB4551A)),
    "red" to AccentSet(Color(0xFFE04B4B), Color(0xFFF09A9A), Color(0xFFA53030)),
    "pink" to AccentSet(Color(0xFFEC5FA8), Color(0xFFF7A8D0), Color(0xFFB03A7C)),
    "cyan" to AccentSet(Color(0xFF29B6E8), Color(0xFF86D7F3), Color(0xFF1A82A8)),
    "lime" to AccentSet(Color(0xFF9BD42B), Color(0xFFC6EC7A), Color(0xFF6C9A16)),
    "purple" to AccentSet(Color(0xFF9B5CE0), Color(0xFFC7A2F2), Color(0xFF6F38A8)),
    "mono" to AccentSet(Color(0xFFB9BEC6), Color(0xFFE6E9ED), Color(0xFF7D838D)),
)

/** مفاتيح اللون المميز مع الاسم؛ "theme" يعني لون الثيم نفسه. */
val AccentList = listOf(
    "theme" to S.accTheme, "gold" to "ذهبي", "yellow" to "أصفر", "amber" to S.accAmber, "orange" to "برتقالي",
    "red" to "أحمر", "rose" to S.accRose, "pink" to "وردي", "purple" to "بنفسجي", "cyan" to "سماوي",
    "teal" to S.accTeal, "green" to S.accGreen, "lime" to "ليموني", "mono" to "رمادي",
)

fun accentColor(key: String): Color? = AccentSets[key]?.a

fun withAccent(base: BeoutColors, key: String): BeoutColors {
    val a = AccentSets[key] ?: return base
    return BeoutColors(
        base.bgTop, base.bgBottom, base.glassHi, base.glassLo, base.edgeHi, base.edgeLo, base.dense,
        base.text, base.textDim, a.a, a.a2, a.deep, base.danger, base.dark,
    )
}

// ---------- نظام المظهر ----------
@Immutable
class Look(
    val radius: Float,
    val glass: Float,
    val icon: String,
    val poster: Float,
    val ui: String,
    /** ارتفاع الظل بالـ dp (0 = بلا ظل). */
    val shadow: Float,
    val borderW: Float,
    val cardDepth: Int,
    val clarity: Float,
    val space: Float,
    val density: Float,
    val listStyle: String,
    val cardStyle: String,
    val ap: Map<String, String> = emptyMap(),
    val opt: OpticalCfg = OpticalCfg.Off,
    val typo: TypoCfg = TypoCfg.Default,
    val mot: MotionCfg = MotionCfg("smooth", 1f, 1f, 1f, false),
    val lay: LayoutCfg = LayoutCfg(16f, 14f, 10f, 0, 0f),
    val cards: Map<String, CardCfg> = emptyMap(),
    val iconScale: Float = 1f,
    val iconStroke: Float = 1f,
    val iconActive: String = "accent",
    val btnStyle: String = "default",
) {
    fun card(type: String) = cards[type] ?: CardCfg.Default
}

val LocalLook = staticCompositionLocalOf { lookOf(com.beouttv.app.data.Settings()) }

fun lookOf(s: com.beouttv.app.data.Settings, tier: Int = 2, systemMotionOff: Boolean = false): Look {
    val ap = s.ap
    val opt = buildOptical(s, tier)
    // انحناء الزوايا في الوضع البصري يتبع "انحناء الزوايا" من استوديو الزجاج
    val cornerK = if (opt.on) (0.5f + opt.f("g_corner") / 100f) else 1f
    return Look(
        radius = when (s.radius) { "s" -> 0.6f; "l" -> 1.4f; else -> 1f } * cornerK,
        glass = when (s.glass) { "s" -> 0.6f; "l" -> 1.5f; else -> 1f },
        icon = s.iconStyle,
        poster = when (s.posterSize) { "s" -> 0.85f; "l" -> 1.2f; else -> 1f },
        ui = s.ui,
        shadow = when (s.shadow) { "0" -> 0f; "2" -> 8f; "3" -> 16f; else -> 3f },
        borderW = when (s.borderW) { "1" -> 0.6f; "2" -> 1.2f; else -> 0f },
        cardDepth = s.cardDepth.toIntOrNull()?.coerceIn(0, 2) ?: 1,
        clarity = when (s.clarity) { "s" -> 0.85f; "l" -> 1.2f; else -> 1f },
        space = when (s.space) { "s" -> 0.75f; "l" -> 1.3f; else -> 1f },
        density = when (s.density) { "s" -> 0.85f; "l" -> 1.12f; else -> 1f },
        listStyle = s.listStyle,
        cardStyle = s.cardStyle,
        ap = ap, opt = opt, typo = buildTypo(ap), mot = buildMotion(ap, systemMotionOff), lay = buildLayout(ap), cards = buildCards(ap),
        iconScale = when (ap["iconSize"]) { "s" -> 0.88f; "l" -> 1.14f; else -> 1f },
        iconStroke = when (ap["iconW"]) { "s" -> 0.72f; "l" -> 1.45f; else -> 1f },
        iconActive = ap["iconActive"] ?: "accent",
        btnStyle = ap["btnStyle"] ?: "default",
    )
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

/** ظل ناعم مرتبط بإعداد "شدة الظلال"؛ لا يرسم شيئاً إذا كان صفراً. */
@Composable
private fun Modifier.soft(shape: Shape, boost: Float = 1f): Modifier {
    val e = T.look.shadow * boost
    return if (e <= 0f) this else shadow(e.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.45f))
}

/** حافة اختيارية بسماكة المستخدم (الافتراضي بلا حافة). */
@Composable
private fun Modifier.edge(shape: Shape): Modifier {
    val w = T.look.borderW
    return if (w <= 0f) this else border(w.dp, T.c.edgeHi.copy(alpha = if (T.c.dark) 0.28f else 0.5f), shape)
}

/** لمعة علوية خفيفة تعطي عمق البطاقة (cardDepth 0..2). */
@Composable
private fun Modifier.topLight(): Modifier {
    val d = T.look.cardDepth
    if (d <= 0) return this
    val a = 0.035f * d * (if (T.c.dark) 1f else 0.6f)
    return background(Brush.verticalGradient(listOf(Color.White.copy(alpha = a * 2f), Color.Transparent)))
}

/** سطح زجاجي خفيف: تدرج شبه شفاف بلا Blur (رخيص). يتأثر بقوة الزجاج والظل والحافة والعمق. */
@Composable
fun Modifier.glass(shape: Shape = GlassShape, strong: Boolean = false): Modifier {
    if (T.look.opt.on) return opticalSurface(shape, ROLE_BUTTON)
    val c = T.c
    val k = T.look.glass
    val base = c.glassHi.alpha * (if (strong) 1f else 0.8f) * k * 1.15f
    return this
        .soft(shape, 0.8f)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(c.glassHi.copy(alpha = base.coerceAtMost(0.9f)), c.glassLo.copy(alpha = (c.glassLo.alpha * k).coerceAtMost(0.9f)))))
        .edge(shape)
}

/** زجاج كثيف: قاعدة شبه معتمة. للشرائط العائمة واللوحات فوق الفيديو. */
@Composable
fun Modifier.glassDense(shape: Shape = GlassShape): Modifier {
    if (T.look.opt.on) return opticalSurface(shape, ROLE_BAR)
    val c = T.c
    return this
        .soft(shape)
        .clip(shape)
        .background(c.dense.copy(alpha = ((0.94f * (0.6f + 0.4f * T.look.glass)) * T.look.clarity).coerceIn(0.55f, 0.99f)))
        .background(Brush.verticalGradient(listOf(c.glassHi, c.glassLo)))
        .edge(shape)
}

/** سطح المحتوى العادي (بطاقات، صفوف). يتبع العمق: ظل، لمعة علوية، حافة، ووضوح الطبقة. */
@Composable
fun Modifier.flat(shape: Shape = T.card): Modifier =
    if (T.look.opt.on && T.look.opt.full) opticalSurface(shape, ROLE_CARD, tierCap = 1) else soft(shape, 0.6f)
        .clip(shape)
        .background(T.c.dense.copy(alpha = (0.72f * T.look.clarity).coerceAtMost(0.98f)))
        .topLight()
        .edge(shape)

/** حالة التحديد: صبغة زرقاء شفافة وحافة رفيعة (بدل التعبئة الصلبة والتوهج). */
@Composable
fun Modifier.tinted(shape: Shape): Modifier {
    val c = T.c
    return clip(shape).background(c.accent.copy(alpha = 0.32f))
}

/** تعبئة اللون الأساسي: تدرج رأسي هادئ بين درجتين متقاربتين (لا نيون). */
@Composable
fun Modifier.accentFill(shape: Shape): Modifier {
    val c = T.c
    return clip(shape).background(Brush.verticalGradient(listOf(c.accent, c.accentDeep)))
}
