package com.beouttv.app.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import com.beouttv.app.data.CARD_TYPES

/** النصوص: العائلة والوزن والأحجام والتباعد وارتفاع السطر والشفافية. تُطبَّق مركزياً في Txt. */
@Immutable
class TypoCfg(
    val family: FontFamily?, val weightShift: Int, val title: Float, val body: Float, val small: Float,
    val letter: Float, val lineH: Float, val alpha: Float,
) {
    companion object { val Default = TypoCfg(null, 0, 1f, 1f, 1f, 0f, 1f, 1f) }
}

fun buildTypo(ap: Map<String, String>): TypoCfg {
    fun p(k: String, d: Float) = ap[k]?.toFloatOrNull() ?: d
    val fam = when (ap["fontFam"]) { "sans" -> FontFamily.SansSerif; "serif" -> FontFamily.Serif; "mono" -> FontFamily.Monospace; "cursive" -> FontFamily.Cursive; else -> null }
    return TypoCfg(
        fam, p("fontW", 0f).toInt(), p("fontTitle", 100f) / 100f, p("fontBody", 100f) / 100f, p("fontSmall", 100f) / 100f,
        p("letterSp", 0f) / 10f, p("lineH", 100f) / 100f, p("textA", 100f) / 100f,
    )
}

/** نظام الحركة: نمط + سرعة + شدة + مرونة؛ ويحترم "تقليل الحركة" وإعداد النظام. */
@Immutable
class MotionCfg(val mode: String, val speed: Float, val intensity: Float, val damp: Float, val reduce: Boolean) {
    val off get() = reduce || mode == "disabled"

    fun ms(base: Int): Int = if (off) 0 else (base / speed * (if (mode == "minimal") 0.6f else 1f)).toInt().coerceAtLeast(1)

    fun <V> spec(base: Int = 300): FiniteAnimationSpec<V> = when {
        off -> snap()
        mode == "spring" || mode == "depth" -> spring(dampingRatio = (0.45f + 0.5f * damp).coerceIn(0.3f, 1.2f), stiffness = 420f * speed)
        else -> tween(ms(base), easing = FastOutSlowInEasing)
    }

    /** انتقال تبديل الأقسام الرئيسية. */
    fun page(): ContentTransform {
        if (off) return EnterTransition.None togetherWith ExitTransition.None
        val k = intensity
        return when (mode) {
            "slide" -> (fadeIn(tween(ms(220))) + slideInHorizontally(tween(ms(260), easing = FastOutSlowInEasing)) { (it * 0.12f * k).toInt() }) togetherWith
                (fadeOut(tween(ms(140))) + slideOutHorizontally(tween(ms(200))) { -(it * 0.08f * k).toInt() })
            "scale" -> (fadeIn(tween(ms(220))) + scaleIn(tween(ms(240)), initialScale = 1f - 0.08f * k)) togetherWith fadeOut(tween(ms(140)))
            "depth" -> (fadeIn(spec(260)) + scaleIn(spec(260), initialScale = 1f - 0.06f * k)) togetherWith (fadeOut(tween(ms(160))) + scaleOut(tween(ms(160)), targetScale = 1f + 0.04f * k))
            "spring" -> fadeIn(spec(240)) togetherWith fadeOut(tween(ms(140)))
            "minimal" -> fadeIn(tween(ms(120))) togetherWith fadeOut(tween(ms(80)))
            else -> fadeIn(tween(ms(220))) togetherWith fadeOut(tween(ms(140)))
        }
    }
}

fun buildMotion(ap: Map<String, String>, systemOff: Boolean): MotionCfg = MotionCfg(
    ap["motion"] ?: "smooth",
    ((ap["mSpeed"]?.toFloatOrNull() ?: 100f) / 100f).coerceIn(0.4f, 2.5f),
    ((ap["mIntensity"]?.toFloatOrNull() ?: 100f) / 100f).coerceIn(0f, 2f),
    ((ap["mDamp"]?.toFloatOrNull() ?: 100f) / 100f).coerceIn(0.2f, 1.5f),
    ap["reduceMotion"] == "1" || systemOff,
)

/** التخطيط: هوامش الشاشة، مسافات الأقسام والبطاقات، أعمدة الشبكة، العرض الأقصى. */
@Immutable
class LayoutCfg(val hPad: Float, val secGap: Float, val cardGap: Float, val cols: Int, val maxW: Float)

fun buildLayout(ap: Map<String, String>): LayoutCfg {
    fun p(k: String, d: Float) = ap[k]?.toFloatOrNull() ?: d
    return LayoutCfg(p("hPad", 16f), p("secGap", 14f), p("cardGap", 10f), p("gridCols", 0f).toInt(), p("contentW", 0f))
}

/** تخصيص بطاقة لنوع واحد؛ القيم الفارغة تعني "اتبع الإعداد العام". */
@Immutable
class CardCfg(
    val r: Float?, val sh: Int?, val bd: Float?, val op: Float, val tp: Int?, val ts: Float, val pd: Float?, val gr: Float?, val rf: Float, val sz: Float,
) {
    companion object { val Default = CardCfg(null, null, null, 1f, null, 1f, null, null, 0f, 1f) }
}

fun buildCards(ap: Map<String, String>): Map<String, CardCfg> = CARD_TYPES.associateWith { t ->
    fun p(k: String) = ap["card_${t}_$k"]?.toFloatOrNull()
    CardCfg(p("r"), p("sh")?.toInt(), p("bd"), (p("op") ?: 100f) / 100f, p("tp")?.toInt(), (p("ts") ?: 100f) / 100f, p("pd"), p("gr"), (p("rf") ?: 0f) / 100f, (p("sz") ?: 100f) / 100f)
}
