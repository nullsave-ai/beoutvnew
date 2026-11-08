package com.beouttv.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** نابض الانتقالات: شبه حرج الإخماد (بلا ارتداد ظاهر) وسريع الاستجابة. */
val RouteSpring = spring<Float>(dampingRatio = 0.92f, stiffness = 420f)

/**
 * يستضيف شاشة مكدس ويدير تقدم انتقالها p في [0,1] بنابض واحد للدخول والخروج.
 * يُمرَّر p كدالة لتُقرأ داخل graphicsLayer فقط (طور الرسم) فلا تحدث إعادة تركيب أثناء الحركة.
 */
@Composable
fun RouteHost(entry: NavEntry, nav: NavVM, content: @Composable (progress: () -> Float) -> Unit) {
    val p = remember(entry.id) { Animatable(0f) }
    LaunchedEffect(entry.id, entry.closing) {
        p.animateTo(if (entry.closing) 0f else 1f, RouteSpring)
        if (entry.closing) nav.remove(entry.id)
    }
    content { p.value }
}

/** انزلاق جانبي خفيف مع ظهور تدريجي (البحث والإعدادات). يتبع اتجاه الواجهة. */
@Composable
fun Modifier.slideRoute(p: () -> Float): Modifier {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return graphicsLayer {
        val v = p().coerceIn(0f, 1f)
        alpha = v
        translationX = (if (rtl) -1f else 1f) * (1f - v) * size.width * 0.12f
    }
}
