package com.beouttv.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp

/** أبعاد ثابتة للصفحتين المباشرتين (الرئيسية والقنوات). */
object Dm {
    val Header = 52.dp          // ترويسة الصفحة (بدون شريط الحالة)
    val NowPlaying = 60.dp      // شريط "يُعرض الآن" تحت المشغل
    val Suggest = 94.dp         // شريط الاقتراحات
    val MinPanes = 190.dp       // أقل ارتفاع مقبول للوحتين
    val RailWidth = 84.dp       // عرض شريط التنقل الجانبي
}

/** هل يُعرض التخطيط المقسوم (مشغل في جانب، لوحات في الجانب الآخر)؟ تلفاز، لوحي أفقي، هاتف أفقي. */
fun isSplitForm(w: Dp, usableH: Dp): Boolean = w >= 480.dp && w > usableH * 1.1f

/**
 * نتيجة حساب التخطيط المباشر.
 * split=false (هاتف عمودي، لوحي عمودي): المشغل بعرض كامل، تحته شريط "يُعرض الآن" ثم اللوحتان.
 * split=true (تلفاز، أفقي): عمود المشغل في جانب واللوحتان في الجانب الآخر.
 * playerW/playerH: مقاس المشغل المضمّن بنسبة 16:9 يتقلص تلقائياً إن ضاق الارتفاع.
 * colW: عرض عمود المشغل (في الوضع المقسوم). sectionsW: عرض لوحة الأقسام. suggest: هل يتسع شريط الاقتراحات.
 */
@Immutable
class LiveDims(
    val split: Boolean,
    val playerW: Dp,
    val playerH: Dp,
    val colW: Dp,
    val sectionsW: Dp,
    val suggest: Boolean,
)

/**
 * w: العرض المتاح للمحتوى، usableH: الارتفاع المتاح بعد شريط الحالة والإزاحة السفلية.
 * wantSuggest: هل توجد اقتراحات تستحق المساحة (الرئيسية فقط).
 * كل الأرقام بوحدات dp بعد تكبير التلفاز، فتعمل المعادلات نفسها على الهاتف والتلفاز.
 */
fun liveDims(w: Dp, usableH: Dp, wantSuggest: Boolean, split: Boolean = isSplitForm(w, usableH)): LiveDims {
    val fixed = Dm.Header + Dm.NowPlaying
    if (!split) {
        val full = w * 9f / 16f
        val suggest = wantSuggest && usableH - fixed - full - Dm.Suggest >= Dm.MinPanes
        var ph = full
        // إن لم يبقَ للوحتين ما يكفي (نافذة قصيرة) يتقلص المشغل بدل أن تختفي اللوحتان
        val room = usableH - fixed - (if (suggest) Dm.Suggest else 0.dp) - Dm.MinPanes
        if (room < ph) ph = room.coerceAtLeast(full * 0.5f)
        val sec = (w * 0.34f).coerceIn(112.dp, 168.dp)
        return LiveDims(false, w, ph, w, sec, suggest)
    }
    val avail = usableH - fixed
    var pw = (w * 0.56f).coerceIn(300.dp, 820.dp)
    var ph = pw * 9f / 16f
    var suggest = false
    if (wantSuggest) {
        val withSuggest = avail - Dm.Suggest
        if (withSuggest >= ph * 0.62f) {
            suggest = true
            if (withSuggest < ph) { ph = withSuggest; pw = ph * 16f / 9f }
        }
    }
    if (!suggest && ph > avail) { ph = avail.coerceAtLeast(120.dp); pw = ph * 16f / 9f }
    val rest = w - pw
    val sec = (rest * 0.36f).coerceIn(120.dp, 210.dp)
    return LiveDims(true, pw, ph, pw, sec, suggest)
}

/**
 * إزاحات الجانبين الفيزيائية (يسار/يمين) من أشرطة النظام وقصّة الشاشة (النوتش).
 * تظهر في الوضع الأفقي فقط، لذلك تُطبَّق بـ absolutePadding لا start/end.
 */
@Composable
fun physicalSideInsets(): Pair<Dp, Dp> {
    val bars: PaddingValues = WindowInsets.systemBars.asPaddingValues()
    val cut: PaddingValues = WindowInsets.displayCutout.asPaddingValues()
    val l = maxOf(bars.calculateLeftPadding(LayoutDirection.Ltr), cut.calculateLeftPadding(LayoutDirection.Ltr))
    val r = maxOf(bars.calculateRightPadding(LayoutDirection.Ltr), cut.calculateRightPadding(LayoutDirection.Ltr))
    return l to r
}
