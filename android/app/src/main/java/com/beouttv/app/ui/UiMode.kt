package com.beouttv.app.ui

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

val LocalTv = staticCompositionLocalOf { false }

/**
 * كشف التلفاز بعدة إشارات لا بعرض الشاشة وحده:
 * نمط واجهة التلفاز، ميزة leanback، غياب الشاشة اللمسية، وجود D-pad، ثم عرض النافذة.
 */
fun detectTv(ctx: Context, windowWidthDp: Int): Boolean {
    val pm = ctx.packageManager
    val ui = ctx.getSystemService(UiModeManager::class.java)
    if (ui?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
    if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) return true
    val cfg = ctx.resources.configuration
    val noTouch = cfg.touchscreen == Configuration.TOUCHSCREEN_NOTOUCH || !pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    val dpad = cfg.navigation == Configuration.NAVIGATION_DPAD || cfg.navigation == Configuration.NAVIGATION_TRACKBALL
    return noTouch && (dpad || windowWidthDp >= 900)
}

/**
 * يطبّق وضع العرض على الشجرة كلها عبر CompositionLocal فقط (بدون تغيير بنية التركيب)،
 * لذلك تبديل الوضع لا يعيد إنشاء المشغل ولا يمسح أي بيانات.
 * في وضع التلفاز تُكبَّر الكثافة 1.3x لمسافة المشاهدة، فتكبر النصوص والأهداف معاً.
 */
@Composable
fun ProvideUiMode(setting: String, fontScale: Float = 1f, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val cfg = LocalConfiguration.current
    val auto = remember(cfg) { detectTv(ctx, cfg.screenWidthDp) }
    val tv = when (setting) { "tv" -> true; "phone" -> false; else -> auto }
    val base = LocalDensity.current
    val dens = remember(base, tv, fontScale) { Density(base.density * (if (tv) 1.3f else 1f), base.fontScale * fontScale) }
    CompositionLocalProvider(LocalTv provides tv, LocalDensity provides dens, content = content)
}
