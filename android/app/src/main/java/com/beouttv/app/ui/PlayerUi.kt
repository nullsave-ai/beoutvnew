package com.beouttv.app.ui

import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.beouttv.app.data.Settings
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Inline: مضمّن داخل الصفحة. Fullscreen: ملء الشاشة للبث المباشر. Route: صفحة تشغيل VOD كاملة. */
enum class PlayerMode { Inline, Fullscreen, Route }

private enum class Menu { Volume, Subs, Audio, Quality, Options }

private val Speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
private val FitSpring = spring<Float>(dampingRatio = 0.92f, stiffness = 400f)

/**
 * واجهة المشغل الواحدة لكل الأوضاع: سطح الفيديو + أدوات التحكم + الإيماءات + مفاتيح الريموت.
 * هي نفسها تبقى في الشجرة عند الانتقال بين المضمّن وملء الشاشة (يتغير الحجم فقط)،
 * فلا يُعاد إنشاء سطح الفيديو ولا ينقطع التشغيل.
 *
 * onBack/onFullscreen/onPrev/onNext/onMore: إن كانت null فالزر المقابل غير معروض.
 * settling: أثناء حركة التوسّع/الانكماش لا تظهر الأدوات (تظهر بعد الاستقرار).
 */
@Composable
fun PlayerUi(
    engine: PlaybackEngine,
    mode: PlayerMode,
    settings: Settings,
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)?,
    onFullscreen: (() -> Unit)?,
    onPrev: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onMore: (() -> Unit)?,
    onFit: (String) -> Unit,
    modifier: Modifier = Modifier,
    orientation: String? = null,
    onOrientation: ((String) -> Unit)? = null,
    closing: Boolean = false,
    settling: Boolean = false,
    idleLabel: String? = null,
) {
    val tv = LocalTv.current
    val player = engine.player
    val isInline = mode == PlayerMode.Inline
    val live = engine.live
    val idle = engine.entry == null

    var controls by remember { mutableStateOf(true) }
    var menu by remember { mutableStateOf<Menu?>(null) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var buf by remember { mutableLongStateOf(0L) }
    var interaction by remember { mutableIntStateOf(0) }
    var boosting by remember { mutableStateOf(false) }
    var flashSide by remember { mutableIntStateOf(-1) }
    var flashOn by remember { mutableStateOf(false) }
    var flashKey by remember { mutableIntStateOf(0) }
    val rootFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }

    fun poke() { controls = true; interaction++ }

    // ---- وضع عرض الفيديو: ينتقل بنابض بين الوضع السابق والجديد (يتبع حجم الحاوية لحظياً) ----
    val fitMode = settings.videoFit
    var fromFit by remember { mutableStateOf(fitMode) }
    var toFit by remember { mutableStateOf(fitMode) }
    val mix = remember { Animatable(1f) }
    LaunchedEffect(fitMode) {
        if (fitMode != toFit) {
            fromFit = toFit
            toFit = fitMode
            mix.snapTo(0f)
            mix.animateTo(1f, FitSpring)
        }
    }
    val ratio by animateFloatAsState(engine.videoRatio, FitSpring, label = "ratio")

    // ---- التركيز: في ملء الشاشة/المسار يأخذ الجذر التركيز ليعمل الريموت ----
    LaunchedEffect(mode) {
        if (!isInline) runCatching { rootFocus.requestFocus() }
        poke()
    }

    // ---- إظهار/إخفاء الأدوات ----
    LaunchedEffect(controls, engine.isPlaying, interaction, menu) {
        if (controls && engine.isPlaying && menu == null) { delay(3_500); controls = false }
    }
    LaunchedEffect(controls, live) {
        while (controls && !live) {
            pos = player.currentPosition; dur = player.duration.coerceAtLeast(0); buf = player.bufferedPosition
            delay(500)
        }
    }
    LaunchedEffect(controls) { if (controls && tv && !isInline) runCatching { playFocus.requestFocus() } }
    LaunchedEffect(flashKey) { if (flashKey > 0) { delay(550); flashOn = false } }

    BackHandler(menu != null) { menu = null }

    // واجهة المشغل فيزيائية (يسار/يمين) بغض النظر عن اتجاه التطبيق
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier.fillMaxSize()
                .background(Color.Black)
                .onPreviewKeyEvent { ev ->
                    if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (isInline) {
                        return@onPreviewKeyEvent when (ev.key) {
                            Key.DirectionCenter, Key.Enter -> if (onFullscreen != null) { onFullscreen(); true } else false
                            Key.MediaPlayPause, Key.Spacebar -> { engine.toggle(); true }
                            Key.MediaNext, Key.ChannelUp -> if (onNext != null) { onNext(); true } else false
                            Key.MediaPrevious, Key.ChannelDown -> if (onPrev != null) { onPrev(); true } else false
                            else -> false
                        }
                    }
                    if (!controls) when (ev.key) {
                        Key.DirectionLeft -> { if (!live) engine.seekBy(-10_000); poke(); return@onPreviewKeyEvent true }
                        Key.DirectionRight -> { if (!live) engine.seekBy(10_000); poke(); return@onPreviewKeyEvent true }
                        Key.DirectionCenter, Key.Enter, Key.DirectionUp, Key.DirectionDown -> { poke(); return@onPreviewKeyEvent true }
                        else -> {}
                    }
                    when (ev.key) {
                        Key.MediaPlayPause, Key.Spacebar -> { engine.toggle(); poke(); true }
                        Key.MediaFastForward -> { if (!live) engine.seekBy(10_000); poke(); true }
                        Key.MediaRewind -> { if (!live) engine.seekBy(-10_000); poke(); true }
                        Key.MediaNext, Key.ChannelUp -> if (onNext != null) { onNext(); true } else false
                        Key.MediaPrevious, Key.ChannelDown -> if (onPrev != null) { onPrev(); true } else false
                        else -> false
                    }
                }
                .focusRequester(rootFocus)
                .focusable()
                .pointerInput(live, settings.longPressBoost, isInline) {
                    detectTapGestures(
                        onTap = { controls = !controls; if (controls) interaction++ },
                        onDoubleTap = if (live || isInline) null else fun(o: Offset) {
                            val left = o.x < size.width / 2
                            engine.seekBy(if (left) -10_000 else 10_000)
                            flashSide = if (left) -1 else 1; flashOn = true; flashKey++
                        },
                        onLongPress = {
                            if (settings.longPressBoost && !live && !isInline) { boosting = true; engine.boost(true) }
                        },
                        onPress = {
                            tryAwaitRelease()
                            if (boosting) { boosting = false; engine.boost(false) }
                        },
                    )
                },
        ) {
            // الفيديو: يُزال أثناء إغلاق المسار قبل حركة التلاشي (SurfaceView لا يتلاشى مع الحاوية)
            if (!closing) Box(Modifier.fillMaxSize().clipToBounds()) {
                AndroidView(
                    factory = { c -> SurfaceView(c).also { player.setVideoSurfaceView(it) } },
                    modifier = Modifier.layout { m, c ->
                        val cw = c.maxWidth.toFloat(); val ch = c.maxHeight.toFloat()
                        val (fw, fh) = videoBox(fromFit, cw, ch, ratio)
                        val (tw, th) = videoBox(toFit, cw, ch, ratio)
                        val t = mix.value
                        val w = (fw + (tw - fw) * t).roundToInt().coerceAtLeast(1)
                        val h = (fh + (th - fh) * t).roundToInt().coerceAtLeast(1)
                        val pl = m.measure(Constraints.fixed(w, h))
                        layout(c.maxWidth, c.maxHeight) { pl.place((c.maxWidth - w) / 2, (c.maxHeight - h) / 2) }
                    },
                )
            }

            if (idle) {
                Column(Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BIcon(Ic.Play, size = if (isInline) 34.dp else 48.dp, tint = Color(0x99FFFFFF))
                    if (idleLabel != null) Txt(idleLabel, style = T.body, color = Color(0xCCFFFFFF), maxLines = 2)
                }
            }

            val cue = engine.cueText
            if (cue.isNotEmpty())
                Txt(
                    cue,
                    Modifier.align(Alignment.BottomCenter).padding(bottom = if (controls && !isInline) 120.dp else if (isInline) 36.dp else 32.dp, start = 24.dp, end = 24.dp)
                        .background(Color(0x99000000), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                    if (isInline) T.small else T.body, Color.White,
                )

            if (!idle && engine.buffering && !engine.fatal) Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Spinner(if (isInline) 28.dp else 36.dp, Color.White)
                if (engine.reconnecting) Txt(S.reconnecting, style = T.small, color = Color.White)
            }

            if (engine.fatal) Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Txt(S.playbackErr, style = if (isInline) T.body else T.h2, color = Color.White)
                PrimaryButton(S.retry, { engine.retry() })
            }

            // ردّ فعل النقر المزدوج
            AnimatedVisibility(
                flashOn, enter = fadeIn(tween(100)), exit = fadeOut(tween(250)),
                modifier = Modifier.align(if (flashSide < 0) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 48.dp),
            ) {
                Box(Modifier.size(64.dp).glassDense(RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                    BIcon(if (flashSide < 0) Ic.Back10 else Ic.Forward10, size = 34.dp, tint = Color.White)
                }
            }

            // مؤشر التسريع بالضغط المطول
            AnimatedVisibility(
                boosting, enter = fadeIn(tween(120)), exit = fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp),
            ) {
                Txt("2x", Modifier.glassDense(RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 4.dp), T.h2, Color.White)
            }

            AnimatedVisibility(
                controls && !closing && !settling && !idle,
                enter = fadeIn(tween(160)), exit = fadeOut(tween(220)),
            ) {
                if (isInline) InlineControls(engine, title, onFullscreen, onMore, onPrev, onNext, ::poke, playFocus)
                else FullControls(
                    engine, tv, title, subtitle, mode, onBack, onFullscreen, onPrev, onNext,
                    settings.videoFit, onFit, orientation, onOrientation,
                    menu, { menu = it }, pos, dur, buf, { pos = it }, ::poke, playFocus,
                )
            }
        }
    }
}

// ===================== أدوات المشغل المضمّن =====================

@Composable
private fun InlineControls(
    engine: PlaybackEngine,
    title: String,
    onFullscreen: (() -> Unit)?,
    onMore: (() -> Unit)?,
    onPrev: (() -> Unit)?,
    onNext: (() -> Unit)?,
    poke: () -> Unit,
    playFocus: FocusRequester,
) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(56.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(Color(0xAA000000), Color.Transparent))))
        Box(Modifier.fillMaxWidth().height(64.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))

        // أعلى: شارة مباشر + اسم القناة + المزيد
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (engine.live) Txt(S.live, Modifier.background(T.c.danger, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 1.dp), T.small, Color.White)
            Txt(title, Modifier.weight(1f), T.body, Color.White, maxLines = 1)
            if (onMore != null) IconAction(Ic.More, { onMore(); poke() }, size = 36.dp, tint = Color.White)
        }

        // الوسط
        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onPrev != null) GlassIconButton(Ic.Prev, { onPrev(); poke() }, size = 40.dp)
            GlassIconButton(
                if (engine.isPlaying) Ic.Pause else Ic.Play,
                { engine.toggle(); poke() },
                Modifier.focusRequester(playFocus), size = 52.dp,
            )
            if (onNext != null) GlassIconButton(Ic.Next, { onNext(); poke() }, size = 40.dp)
        }

        // أسفل: كتم + ملء الشاشة
        Row(
            Modifier.align(Alignment.BottomEnd).padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(
                if (engine.volume == 0f) Ic.Mute else Ic.Volume,
                { engine.setVolume(if (engine.volume == 0f) 1f else 0f); poke() },
                size = 36.dp, tint = Color.White,
            )
            if (onFullscreen != null) IconAction(Ic.Fullscreen, { onFullscreen() }, size = 36.dp, tint = Color.White)
        }
    }
}

// ===================== أدوات ملء الشاشة / المسار =====================

@Composable
private fun FullControls(
    engine: PlaybackEngine,
    tv: Boolean,
    title: String,
    subtitle: String?,
    mode: PlayerMode,
    onBack: (() -> Unit)?,
    onFullscreen: (() -> Unit)?,
    onPrev: (() -> Unit)?,
    onNext: (() -> Unit)?,
    fit: String,
    onFit: (String) -> Unit,
    orientation: String?,
    onOrientation: ((String) -> Unit)?,
    menu: Menu?,
    setMenu: (Menu?) -> Unit,
    pos: Long,
    dur: Long,
    buf: Long,
    setPos: (Long) -> Unit,
    poke: () -> Unit,
    playFocus: FocusRequester,
) {
    val live = engine.live
    val player = engine.player
    val onOri: ((String) -> Unit)? = onOrientation?.let { f -> { o: String -> f(o); poke() } }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(110.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(Color(0xAA000000), Color.Transparent))))
        Box(Modifier.fillMaxWidth().height(160.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))

        // أعلى
        Row(
            Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onBack != null) GlassIconButton(Ic.Back, { onBack() })
            Column(Modifier.weight(1f)) {
                Txt(title, style = T.h2, color = Color.White, maxLines = 1)
                subtitle?.let { Txt(it, style = T.small, color = Color(0xFFCBD7EC), maxLines = 1) }
            }
            if (live) Txt(S.live, Modifier.background(T.c.danger, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp), T.small, Color.White)
        }

        // الوسط: البث المباشر بلا تقديم/تأخير ولا شريط تقدم
        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onPrev != null) GlassIconButton(Ic.Prev, { onPrev(); poke() }, size = 48.dp)
            if (!live) GlassIconButton(Ic.Back10, { engine.seekBy(-10_000); poke() }, size = 48.dp)
            GlassIconButton(
                if (engine.isPlaying) Ic.Pause else Ic.Play,
                { engine.toggle(); poke() },
                Modifier.focusRequester(playFocus), size = 64.dp,
            )
            if (!live) GlassIconButton(Ic.Forward10, { engine.seekBy(10_000); poke() }, size = 48.dp)
            if (onNext != null) GlassIconButton(Ic.Next, { onNext(); poke() }, size = 48.dp)
        }

        // أسفل
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            if (!live) {
                SeekBar(pos, dur, buf) { player.seekTo(it); setPos(it); poke() }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Txt(fmt(pos), style = T.small, color = Color.White)
                    Txt(fmt(dur), style = T.small, color = Color.White)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(if (engine.volume == 0f) Ic.Mute else Ic.Volume, { setMenu(if (menu == Menu.Volume) null else Menu.Volume) }, size = 40.dp)
                Box(Modifier.width(10.dp))
                GlassIconButton(Ic.Subtitle, { setMenu(if (menu == Menu.Subs) null else Menu.Subs) }, size = 40.dp)
                Box(Modifier.width(10.dp))
                GlassIconButton(Ic.Audio, { setMenu(if (menu == Menu.Audio) null else Menu.Audio) }, size = 40.dp)
                Box(Modifier.width(10.dp))
                GlassIconButton(Ic.Quality, { setMenu(if (menu == Menu.Quality) null else Menu.Quality) }, size = 40.dp)
                Box(Modifier.width(10.dp))
                GlassIconButton(Ic.Aspect, { setMenu(if (menu == Menu.Options) null else Menu.Options) }, size = 40.dp, active = menu == Menu.Options)
                if (onFullscreen != null) {
                    Box(Modifier.width(10.dp))
                    GlassIconButton(if (mode == PlayerMode.Fullscreen) Ic.ExitFullscreen else Ic.Fullscreen, { onFullscreen() }, size = 40.dp)
                }
            }
        }

        // القوائم: آخر قائمة تبقى معروضة أثناء حركة الإخفاء بدل أن تفرغ فجأة
        val lastMenu = remember { arrayOfNulls<Menu>(1) }
        menu?.let { lastMenu[0] = it }
        val m = menu ?: lastMenu[0]
        AnimatedVisibility(
            menu != null,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = fadeIn(tween(140)) + scaleIn(spring(dampingRatio = 0.9f, stiffness = 500f), 0.9f, TransformOrigin(1f, 1f)),
            exit = fadeOut(tween(110)) + scaleOut(tween(110), 0.95f, TransformOrigin(1f, 1f)),
        ) {
            Column(
                Modifier.navigationBarsPadding().padding(end = 16.dp, bottom = 104.dp)
                    .width(if (m == Menu.Options) 300.dp else 240.dp).glassDense(GlassShape).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                when (m) {
                    Menu.Volume -> {
                        Txt(S.volume, style = T.small, color = T.c.textDim)
                        SeekBar((engine.volume * 1000).toLong(), 1000, 0) { engine.setVolume(it / 1000f); poke() }
                    }
                    Menu.Quality -> OptList(qualityOptions(player, engine.tracks)) { setMenu(null) }
                    Menu.Audio -> OptList(audioOptions(player, engine.tracks)) { setMenu(null) }
                    Menu.Subs -> OptList(subtitleOptions(player, engine.tracks)) { setMenu(null) }
                    Menu.Options -> OptionsPanel(
                        live = live, tv = tv, fit = fit, speed = engine.speed, orientation = orientation,
                        onFit = { onFit(it); poke() },
                        onSpeed = { engine.setSpeed(it); poke() },
                        onOrientation = onOri,
                    )
                    null -> {}
                }
            }
        }
    }
}

@Composable
private fun OptionsPanel(
    live: Boolean, tv: Boolean, fit: String, speed: Float, orientation: String?,
    onFit: (String) -> Unit, onSpeed: (Float) -> Unit, onOrientation: ((String) -> Unit)?,
) {
    Txt(S.videoDisplay, style = T.small, color = T.c.textDim)
    val fits = listOf("fit" to S.fitFit, "fill" to S.fitFill, "zoom" to S.fitZoom, "stretch" to S.fitStretch, "auto" to S.fitAuto)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        fits.forEach { (k, l) -> item(key = k) { Chip(l, fit == k, { onFit(k) }, compact = true) } }
    }
    if (!live) {
        Txt(S.speed, Modifier.padding(top = 6.dp), T.small, T.c.textDim)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Speeds.forEach { s -> item(key = s) { Chip(if (s == 1f) "1x" else "${s}x", speed == s, { onSpeed(s) }, compact = true) } }
        }
    }
    if (!tv && orientation != null && onOrientation != null) {
        Txt(S.orientation, Modifier.padding(top = 6.dp), T.small, T.c.textDim)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(S.landscape, orientation == "landscape", { onOrientation("landscape") }, compact = true)
            Chip(S.portrait, orientation == "portrait", { onOrientation("portrait") }, compact = true)
            Chip(S.autoRotate, orientation == "auto", { onOrientation("auto") }, compact = true)
        }
    }
}

@Composable
private fun OptList(opts: List<Opt>, close: () -> Unit) {
    if (opts.size <= 1 && opts.firstOrNull()?.label != S.auto) {
        Txt(S.empty, style = T.small, color = T.c.textDim)
        return
    }
    opts.forEach { o ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).tvFocus(RoundedCornerShape(10.dp)).tap { o.pick(); close() }.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.width(20.dp)) { if (o.selected) BIcon(Ic.Check, size = 18.dp, tint = T.c.accent2) }
            Txt(o.label, style = T.body, maxLines = 1)
        }
    }
}

/** شريط التقدم: نقر وسحب باللمس، ويسار/يمين بالريموت (±10 ثوانٍ). */
@Composable
private fun SeekBar(position: Long, duration: Long, buffered: Long, onSeek: (Long) -> Unit) {
    var drag by remember { mutableFloatStateOf(-1f) }
    val frac = if (drag >= 0f) drag else if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val bufFrac = if (duration > 0) (buffered.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val c = T.c
    val shape = RoundedCornerShape(8.dp)
    Canvas(
        Modifier.fillMaxWidth().height(28.dp)
            .tvFocus(shape)
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown || duration <= 0) return@onKeyEvent false
                when (ev.key) {
                    Key.DirectionLeft -> { onSeek((position - 10_000).coerceIn(0, duration)); true }
                    Key.DirectionRight -> { onSeek((position + 10_000).coerceIn(0, duration)); true }
                    else -> false
                }
            }
            .focusable()
            .pointerInput(duration) {
                detectTapGestures { o -> onSeek((o.x / size.width * duration).toLong().coerceIn(0, duration)) }
            }
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> drag = (o.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { if (drag >= 0f) onSeek((drag * duration).toLong()); drag = -1f },
                    onDragCancel = { drag = -1f },
                    onHorizontalDrag = { ch, _ -> drag = (ch.position.x / size.width).coerceIn(0f, 1f) },
                )
            },
    ) {
        val h = 4.dp.toPx()
        val y = (size.height - h) / 2
        val r = CornerRadius(h / 2)
        drawRoundRect(Color(0x44FFFFFF), Offset(0f, y), Size(size.width, h), r)
        drawRoundRect(Color(0x55FFFFFF), Offset(0f, y), Size(size.width * bufFrac, h), r)
        drawRoundRect(
            Brush.horizontalGradient(listOf(c.accent2, c.accent), 0f, size.width.coerceAtLeast(1f)),
            Offset(0f, y), Size(size.width * frac, h), r,
        )
        drawCircle(Color.White, 6.dp.toPx(), Offset(size.width * frac, size.height / 2))
    }
}

// ===================== لوحة خيارات المشغل المضمّن (تُعرض كورقة سفلية) =====================

/** شريحة خيارات: كل خيار شريحة، والمحدد مميز. يغلق الاستدعاء بعد الاختيار. */
@Composable
private fun OptChips(opts: List<Opt>, onPick: () -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(opts) { o -> Chip(o.label, o.selected, { o.pick(); onPick() }) }
    }
}

/** محتوى ورقة "المزيد" للمشغل المضمّن: الجودة، الصوت، الترجمة، وضع العرض. */
@Composable
fun PlayerOptionsSheet(engine: PlaybackEngine, fit: String, onFit: (String) -> Unit, onClose: () -> Unit) {
    val player = engine.player
    val tracks = engine.tracks
    val quality = qualityOptions(player, tracks)
    val audio = audioOptions(player, tracks)
    val subs = subtitleOptions(player, tracks)
    Txt(S.playerSettings, style = T.h2)
    if (quality.size > 1) {
        Txt(S.quality, style = T.small, color = T.c.textDim)
        OptChips(quality, onClose)
    }
    if (audio.size > 1) {
        Txt(S.audio, style = T.small, color = T.c.textDim)
        OptChips(audio, onClose)
    }
    if (subs.isNotEmpty()) {
        Txt(S.subtitles, style = T.small, color = T.c.textDim)
        OptChips(subs, onClose)
    }
    Txt(S.videoDisplay, style = T.small, color = T.c.textDim)
    val fits = listOf("fit" to S.fitFit, "fill" to S.fitFill, "zoom" to S.fitZoom, "stretch" to S.fitStretch, "auto" to S.fitAuto)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        fits.forEach { (k, l) -> item(key = k) { Chip(l, fit == k, { onFit(k) }) } }
    }
}
