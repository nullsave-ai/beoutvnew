package com.beouttv.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import android.content.res.Configuration
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beouttv.app.BuildConfig
import com.beouttv.app.core.Net
import com.beouttv.app.core.Startup
import com.beouttv.app.core.dataOrNull
import com.beouttv.app.core.deriveStartup
import com.beouttv.app.data.Prefs

/** الإزاحة السفلية التي يجب أن تتركها الشاشات الرئيسية (شريط التنقل + شريط النظام). */
val LocalBottomPad = compositionLocalOf { 0.dp }

private val NavHeight = 72.dp
private val NavGap = 12.dp

@Composable
fun Root() {
    val app: AppVM = viewModel()
    val nav: NavVM = viewModel()
    val home: HomeVM = viewModel()
    val channels: ChannelsVM = viewModel()
    val catalog: CatalogVM = viewModel()
    val search: SearchVM = viewModel()
    val detail: DetailVM = viewModel()
    val live: LiveVM = viewModel()
    val ctx = LocalContext.current

    val settings by app.settings.collectAsStateWithLifecycle()
    val loaded by app.settingsLoaded.collectAsStateWithLifecycle()
    val stack by nav.stack.collectAsStateWithLifecycle()
    val tab by nav.tab.collectAsStateWithLifecycle()
    val cfgState by app.config.state.collectAsStateWithLifecycle()
    val homeState by home.home.state.collectAsStateWithLifecycle()
    val online by Net.state.collectAsStateWithLifecycle()
    val fullscreen by live.fullscreen.collectAsStateWithLifecycle()

    // البدء: المحتوى المحفوظ يظهر فوراً من الـ Loader ثم يُحدَّث من الخادم
    LaunchedEffect(Unit) { app.config.load(); home.home.load() }
    val lo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lo) {
        val o = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_START) { home.softRefresh(); app.softRefreshConfig() }
            if (e == Lifecycle.Event.ON_STOP) live.onStop()
        }
        lo.addObserver(o)
        onDispose { lo.removeObserver(o) }
    }

    // أولوية الرجوع (الأحدث تسجيلاً أعلى أولوية): الأقسام ثم المكدس ثم ما داخل الشاشات (حوارات/لوحات/قوائم)
    BackHandler(tab != 0 && stack.none { !it.closing }) { nav.tab(0) }
    BackHandler(stack.any { !it.closing }) { nav.pop() }

    val startup = deriveStartup(loaded, cfgState, homeState, online)
    var shown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(startup.showsApp) { if (startup.showsApp) shown = true }
    val ready = shown || startup.showsApp

    // قبل قراءة الإعدادات نستخدم السمة المنسوخة (متزامنة) فلا تتغير الألوان بعد أول إطار
    val theme = if (loaded) settings.theme else Prefs.themeMirror(ctx)
    val colors = withAccent(colorsFor(theme), settings.accent)
    val playerOn = stack.any { it.route is Route.Player }
    val cfg = cfgState.dataOrNull()

    // الواجهة عربية: نفرض RTL بغض النظر عن لغة الجهاز
    CompositionLocalProvider(
        LocalColors provides colors,
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        ProvideUiMode(settings.uiMode, when (settings.fontScale) { "s" -> 0.9f; "l" -> 1.15f; else -> 1f }) {
          CompositionLocalProvider(LocalLook provides lookOf(settings)) {
            SystemBarsEffect(colors, playerOn || fullscreen)
            Box(Modifier.fillMaxSize().screenBackground().aeroGlow()) {
                when {
                    cfg != null && cfg.minVersion > BuildConfig.VERSION_CODE -> MessageBox(S.updateRequired)
                    cfg?.maintenance != null -> MessageBox(cfg.maintenance)
                    !ready -> StartupScreen(startup) { app.config.refresh(); home.home.refresh() }
                    else -> AppShell(app, nav, home, channels, catalog, search, detail, live, tab, stack, online)
                }
            }
          }
        }
    }
}

@Composable
private fun StartupScreen(startup: Startup, onRetry: () -> Unit) {
    if (startup is Startup.Error) {
        MessageBox(
            S.startupFailed, S.retry, onRetry,
            sub = if (startup.offline) S.offlineHint else S.startupFailedSub,
        )
        return
    }
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Txt(
            S.app,
            style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp, letterSpacing = 2.sp),
            color = T.c.accent2,
        )
        Box(Modifier.padding(top = 22.dp)) { Spinner(30.dp) }
        Txt(S.loadingApp, Modifier.padding(top = 14.dp), T.small, T.c.textDim)
    }
}

@Composable
private fun AppShell(
    app: AppVM, nav: NavVM, home: HomeVM, channels: ChannelsVM, catalog: CatalogVM,
    search: SearchVM, detail: DetailVM, live: LiveVM, tab: Int, stack: List<NavEntry>, online: Boolean,
) {
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val settings by app.settings.collectAsStateWithLifecycle()
    val fullscreen by live.fullscreen.collectAsStateWithLifecycle()
    val autoEntered by live.autoEntered.collectAsStateWithLifecycle()
    val moreOpen by live.moreOpen.collectAsStateWithLifecycle()
    val topId = stack.lastOrNull { !it.closing }?.id
    val noRoutes = stack.none { !it.closing }
    val slot = remember { mutableStateOf(Rect.Zero) }

    val tv = LocalTv.current
    val view0 = LocalView.current
    val cfg = LocalConfiguration.current
    val phone = !tv && cfg.smallestScreenWidthDp < 600
    val landscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE
    // وضع التلفاز (حتى على الهاتف) يعرض أفقياً دائماً؛ وإلغاؤه يعيد الاتجاه للنظام
    val activity = remember(view0) { view0.context.findActivity() }
    LaunchedEffect(tv) {
        activity?.requestedOrientation =
            if (tv) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    // ---- المشغل المباشر: الظهور، النافذة، الشاشة ----
    val liveVisible = tab == 1 && noRoutes
    LaunchedEffect(liveVisible) { live.onVisible(liveVisible) }
    FullscreenWindowEffect(fullscreen, lockLandscape = fullscreen && !autoEntered && phone)
    AutoFullscreenEffect(live, settings.autoFullscreen, landscape, phone, active = liveVisible)
    val view = LocalView.current
    val keepOn = settings.keepScreenOn && live.engine.isPlaying && liveVisible && stack.isEmpty()
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { if (keepOn) view.keepScreenOn = false }
    }
    BackHandler(fullscreen && noRoutes) { live.exitFullscreen(true) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val (sideL, sideR) = physicalSideInsets()
        // تخطيط مقسوم (شريط جانبي + مشغل بجانب اللوحتين) للتلفاز والأفقي؛ وإلا شريط سفلي عائم
        val splitBottom = navInset + 8.dp
        val auto = isSplitForm(maxWidth - sideL - sideR - Dm.RailWidth, maxHeight - statusTop - splitBottom)
        val split = if (tv) true else when (settings.chLayout) { "stacked" -> false; "split" -> maxWidth >= 480.dp; else -> auto }
        val railW = if (split) Dm.RailWidth else 0.dp
        val bottomPad: Dp = if (split) splitBottom else navInset + NavGap + NavHeight + 8.dp

        // الشاشات الرئيسية: لا تأخذ تركيز الريموت حين تغطيها صفحة مفتوحة أو ملء الشاشة
        Box(
            Modifier.fillMaxSize()
                .absolutePadding(left = sideL, right = sideR)
                .focusProperties { canFocus = topId == null && !fullscreen },
        ) {
            CompositionLocalProvider(LocalBottomPad provides bottomPad) {
                Box(Modifier.fillMaxSize().padding(start = railW)) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            ContentTransform(
                                targetContentEnter = fadeIn(tween(220)),
                                initialContentExit = fadeOut(tween(140)),
                                targetContentZIndex = 0f,
                                sizeTransform = null,
                            )
                        },
                        label = "shell",
                    ) { t ->
                        when (t) {
                            0 -> HomeScreen(home, nav, live, settings)
                            1 -> LiveScaffold(split, nav, channels, live, slot, settings)
                            else -> CatalogScreen(catalog, nav)
                        }
                    }

                    AnimatedVisibility(
                        visible = !online && !fullscreen,
                        enter = fadeIn(tween(200)) + slideInVertically(spring(dampingRatio = 0.9f, stiffness = 420f)) { it },
                        exit = fadeOut(tween(160)) + slideOutVertically(tween(160)) { it },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomPad + 4.dp),
                    ) { OfflinePill() }
                }
            }

            AnimatedVisibility(
                visible = !fullscreen,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(120)),
                modifier = if (split) Modifier.align(Alignment.CenterStart) else Modifier.align(Alignment.BottomCenter).padding(bottom = navInset + NavGap),
            ) {
                if (split) NavRail(tab, { nav.tab(it) }, Modifier.padding(horizontal = 4.dp))
                else NavBar(settings.navStyle, tab, { nav.tab(it) })
            }
        }

        // المشغل: طبقة واحدة فوق المحتوى تتبع الفتحة وتتمدد لملء الشاشة. الصفحات المفتوحة تعلوها.
        if (tab == 1 && slot.value.width > 0f) {
            PlayerLayer(live, settings, { slot.value }, fullscreen) { app.setVideoFit(it) }
        }

        // قائمة المزيد للمشغل المضمّن (ورقة سفلية)
        GlassSheet(visible = moreOpen && !fullscreen, onDismiss = { live.moreOpen.value = false }) {
            PlayerOptionsSheet(live.engine, settings.videoFit, { app.setVideoFit(it) }, { live.moreOpen.value = false })
        }

        // صفحات المكدس فوق الأقسام. كل صفحة تدير حركتها بنابض واحد للدخول والخروج.
        stack.forEach { e ->
            key(e.id) {
                RouteHost(e, nav) { p ->
                    Box(
                        Modifier.fillMaxSize()
                            .focusProperties { canFocus = e.id == topId }
                            .pointerInput(Unit) { detectTapGestures { } },
                    ) {
                        when (val r = e.route) {
                            is Route.Detail -> DetailScreen(r.id, r.seed, e.origin, p, detail, nav)
                            is Route.Search -> Box(Modifier.fillMaxSize().slideRoute(p)) { SearchScreen(search, nav, live, r.type) }
                            Route.Settings -> Box(Modifier.fillMaxSize().slideRoute(p)) { SettingsScreen(app, nav) }
                            is Route.Player -> PlayerScreen(r.req, nav, app, p, e.closing)
                        }
                        // أثناء الإغلاق تُمتص اللمسات فلا تصل لما تحتها
                        if (e.closing) Box(Modifier.fillMaxSize().pointerInput(Unit) { })
                    }
                }
            }
        }
    }
}

/** شريط تنقل عمودي زجاجي للتلفاز والوضع الأفقي: العنصر المحدد يُملأ بلون التمييز. */
@Composable
private fun NavRail(tab: Int, onTab: (Int) -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(28.dp)
    val items = listOf(Ic.Home to S.home, Ic.Channels to S.channels, Ic.Movies to S.moviesShort)
    Column(
        modifier.glassDense(shape).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items.forEachIndexed { i, (ic, label) ->
            val sel = tab == i
            val s = RoundedCornerShape(22.dp)
            Column(
                Modifier.width(64.dp)
                    .then(if (sel) Modifier.tinted(s) else Modifier.clip(s))
                    .tvFocus(s)
                    .tap { onTab(i) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                BIcon(ic, size = 24.dp, tint = if (sel) T.c.accent2 else T.c.textDim, active = sel)
                Txt(label, style = T.small, color = if (sel) T.c.accent2 else T.c.textDim, maxLines = 1)
            }
        }
    }
}

