package com.beouttv.app.ui

import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beouttv.app.core.dataOrNull

private fun orientationFlag(mode: String) = when (mode) {
    "portrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    "auto" -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
}

/**
 * صفحة تشغيل الأفلام والحلقات (مسار في المكدس) فوق كل شيء.
 * تملك محركها الخاص (PlaybackEngine) وتحرّره عند الإغلاق؛ البث المباشر له محرك مستقل في LiveVM.
 * الواجهة نفسها (PlayerUi) تُستخدم هنا بوضع Route.
 */
@Composable
fun PlayerScreen(req: PlayReq, nav: NavVM, app: AppVM, p: () -> Float, closing: Boolean) {
    val ctx = LocalContext.current
    val activity = remember(ctx) { ctx.findActivity() }
    val view = LocalView.current
    val tv = LocalTv.current
    val settings by app.settings.collectAsStateWithLifecycle()
    val cfgState by app.config.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // المحرك يُنشأ مرة واحدة: تغيير وضع العرض أو السمة لا يعيد إنشاءه ولا يقطع التشغيل
    val engine = remember { PlaybackEngine(ctx, scope) }
    val maxRetry = cfgState.dataOrNull()?.maxReconnect ?: 6
    SideEffect { engine.maxRetry = maxRetry }
    LaunchedEffect(Unit) { engine.start(req.entries, req.index) }

    var orientation by rememberSaveable { mutableStateOf("landscape") }
    val entry = engine.entry

    // ---- النافذة والاتجاه ----
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    val restoreWindow = remember {
        {
            activity?.window?.let { w ->
                WindowCompat.getInsetsController(w, view).show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = if (tv) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    DisposableEffect(Unit) {
        activity?.window?.let { w ->
            val c = WindowCompat.getInsetsController(w, view)
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            restoreWindow()
            engine.release()        // يحفظ الموضع ثم يحرر المشغل
        }
    }
    // الاتجاه حسب الاختيار (على التلفاز يبقى أفقياً)
    LaunchedEffect(orientation, closing) {
        if (!closing) activity?.requestedOrientation = if (tv) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else orientationFlag(orientation)
    }
    // بدء الإغلاق: يتوقف الصوت وتعود أشرطة النظام فوراً، لا بعد انتهاء حركة الخروج
    LaunchedEffect(closing) {
        if (closing) {
            engine.saveProgress()
            engine.pause()
            restoreWindow()
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) { engine.saveProgress(); engine.pause() } }
        lifecycle.addObserver(o)
        onDispose { lifecycle.removeObserver(o) }
    }

    val entering by remember { derivedStateOf { p() < 0.999f } }
    val multi = engine.entries.size > 1

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = if (closing) p().coerceIn(0f, 1f) else 1f }
            .background(Color.Black),
    ) {
        PlayerUi(
            engine = engine,
            mode = PlayerMode.Route,
            settings = settings,
            title = entry?.item?.title.orEmpty(),
            subtitle = entry?.label,
            onBack = { nav.pop() },
            onFullscreen = null,
            onPrev = if (multi) ({ engine.go(-1) }) else null,
            onNext = if (multi) ({ engine.go(1) }) else null,
            onMore = null,
            onFit = { app.setVideoFit(it) },
            orientation = orientation,
            onOrientation = { orientation = it },
            closing = closing,
        )
        // ظهور/اختفاء من الأسود عند الدخول
        if (entering) Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - p().coerceIn(0f, 1f) }.background(Color.Black))
    }
}
