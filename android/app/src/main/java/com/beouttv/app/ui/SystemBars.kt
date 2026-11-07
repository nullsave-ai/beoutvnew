package com.beouttv.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/**
 * يطابق شريطي النظام وخلفية النافذة مع السمة الحالية.
 * لا يغيّر وضع التنقل (إيماءات أو ثلاثة أزرار): الشريطان شفافان والمحتوى يرسم خلفهما،
 * والإزاحات تُطبَّق في الواجهة عبر WindowInsets فيعمل الوضعان بلا تعديل.
 * playerOn: المشغل خلفيته سوداء دائماً فتبقى أيقونات الشريط فاتحة مهما كانت السمة.
 */
@Composable
fun SystemBarsEffect(c: BeoutColors, playerOn: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val ctl = WindowCompat.getInsetsController(window, view)
        val lightIcons = c.dark || playerOn
        ctl.isAppearanceLightStatusBars = !lightIcons
        ctl.isAppearanceLightNavigationBars = !lightIcons
        // خلفية النافذة تظهر خلف الواجهة أثناء تغيير الاتجاه أو الإقلاع: نفس لون الخلفية فلا وميض
        window.setBackgroundDrawable(ColorDrawable(if (playerOn) 0xFF000000.toInt() else c.bgBottom.toArgb()))
    }
}
