package com.beouttv.app.ui

import android.app.Application
import android.content.pm.ActivityInfo
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.layout.absoluteOffset
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.beouttv.app.data.Item
import com.beouttv.app.data.Prefs
import com.beouttv.app.data.Repo
import com.beouttv.app.data.Settings
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * مالك البث المباشر: المحرك والقناة الحالية وقائمة التنقل (الأخ التالي/السابق) وحالة ملء الشاشة.
 * يعيش في ViewModel فلا ينقطع البث بتغيير الاتجاه أو التنقل بين الصفحتين.
 */
class LiveVM(app: Application) : AndroidViewModel(app) {
    val engine = PlaybackEngine(app, viewModelScope)

    val current = MutableStateFlow<Item?>(null)
    private val queue = MutableStateFlow<List<Item>>(emptyList())
    private val _canStep = MutableStateFlow(false)
    val canStep: StateFlow<Boolean> = _canStep

    val fullscreen = MutableStateFlow(false)
    /** دخل ملء الشاشة تلقائياً (دوران الهاتف) فيخرج تلقائياً عند العودة للوضع العمودي. */
    val autoEntered = MutableStateFlow(false)
    val moreOpen = MutableStateFlow(false)

    val favorites = Prefs.favorites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val recents = Prefs.recentChannels.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** بعد خروج يدوي من ملء الشاشة لا يعود تلقائياً حتى يعود الهاتف للوضع العمودي. */
    var autoSuppressed = false

    private var visible = true
    private var resumeOnShow = false
    private var autoJob: Job? = null

    fun play(item: Item, list: List<Item> = emptyList()) {
        val q = list.filter { it.isChannel }.ifEmpty { listOf(item) }
        queue.value = q
        _canStep.value = q.size > 1
        if (current.value?.id == item.id && engine.hasMedia && !engine.fatal) {
            resumeOnShow = true
            if (visible && !engine.player.playWhenReady) engine.play()
            return
        }
        current.value = item
        resumeOnShow = true
        engine.holdStart = !visible
        engine.start(listOf(PlayEntry(item)))
        viewModelScope.launch { Prefs.pushRecentChannel(item) }
    }

    /** التنقل الدائري بين القنوات (أزرار القناة التالية/السابقة وأزرار الريموت). */
    fun step(delta: Int) {
        val q = queue.value
        if (q.size < 2) return
        val i = q.indexOfFirst { it.id == current.value?.id }
        val n = if (i < 0) 0 else ((i + delta) % q.size + q.size) % q.size
        play(q[n], q)
    }

    /** يُستدعى حين يظهر المشغل أو يختفي (تبويب الأفلام، صفحة مفتوحة فوقه). */
    fun onVisible(v: Boolean) {
        if (v == visible) return
        visible = v
        if (!v) {
            resumeOnShow = engine.player.playWhenReady
            engine.holdStart = true
            engine.pause()
        } else {
            engine.holdStart = false
            if (resumeOnShow && current.value != null) engine.play()
        }
    }

    fun onStop() {
        resumeOnShow = false
        engine.pause()
    }

    fun enterFullscreen(auto: Boolean) {
        if (current.value == null) return
        autoEntered.value = auto
        fullscreen.value = true
    }

    fun exitFullscreen(manual: Boolean) {
        if (manual) autoSuppressed = true
        autoEntered.value = false
        fullscreen.value = false
    }

    /** بدء تلقائي: آخر قناة شوهدت، وإن لم توجد فأول قناة في القائمة. يعمل مرة واحدة ما لم تُختر قناة. */
    fun autoStart(fallback: List<Item>) {
        if (current.value != null || fallback.isEmpty()) return
        autoJob?.cancel()
        autoJob = viewModelScope.launch {
            if (!Prefs.settingsNow().autoPlay) return@launch
            val last = Prefs.recentChannels.first().firstOrNull()
            val pick = fallback.firstOrNull { it.id == last?.id } ?: last ?: fallback.first()
            if (current.value == null) play(pick, fallback)
        }
    }

    fun toggleFav(item: Item) { viewModelScope.launch { Repo.toggleFavorite(item) } }

    override fun onCleared() {
        engine.release()
    }
}

// ===================== النافذة =====================

/** يخفي أشرطة النظام في ملء الشاشة، ويقفل الاتجاه الأفقي عند الدخول بالزر على الهاتف. */
@Composable
fun FullscreenWindowEffect(fullscreen: Boolean, lockLandscape: Boolean) {
    val view = LocalView.current
    DisposableEffect(fullscreen, lockLandscape) {
        val activity = view.context.findActivity()
        val window = activity?.window
        if (fullscreen && activity != null && window != null) {
            val c = WindowCompat.getInsetsController(window, view)
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
            if (lockLandscape) activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            if (fullscreen && activity != null && window != null) {
                WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.systemBars())
                if (lockLandscape) activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
}

/**
 * الهاتف في الوضع الأفقي يدخل ملء الشاشة تلقائياً، ويخرج عند العودة للعمودي إن كان الدخول تلقائياً.
 * الخروج اليدوي يمنع الدخول التلقائي مجدداً حتى يعود الهاتف للوضع العمودي.
 */
@Composable
fun AutoFullscreenEffect(live: LiveVM, enabled: Boolean, landscape: Boolean, phone: Boolean, active: Boolean) {
    LaunchedEffect(landscape, enabled, phone, active) {
        if (!phone) return@LaunchedEffect
        if (!landscape) {
            live.autoSuppressed = false
            if (live.autoEntered.value && live.fullscreen.value) live.exitFullscreen(false)
            return@LaunchedEffect
        }
        if (enabled && active && !live.fullscreen.value && !live.autoSuppressed && live.current.value != null) {
            live.enterFullscreen(true)
        }
    }
}

// ===================== طبقة المشغل الدائمة =====================

/**
 * المشغل يعيش في طبقة واحدة فوق المحتوى بدل أن يكون داخل الصفحة، فيبقى سطح الفيديو نفسه
 * عند التوسّع لملء الشاشة (لا يُعاد إنشاؤه ولا ينقطع). حجمه وموضعه يتبعان مستطيل الفتحة (slot)
 * في الوضع المضمّن، ويتمدد بنابض إلى النافذة كلها في ملء الشاشة.
 * يجب أن يكون الأب بحجم النافذة وعند أصلها.
 */
@Composable
fun PlayerLayer(
    live: LiveVM,
    settings: Settings,
    slot: () -> Rect,
    fullscreen: Boolean,
    onFit: (String) -> Unit,
) {
    val current by live.current.collectAsStateWithLifecycle()
    val canStep by live.canStep.collectAsStateWithLifecycle()
    val favs by live.favorites.collectAsStateWithLifecycle()
    val isFav = current?.let { c -> favs.any { it.id == c.id } } == true
    val cornerDp = (16 * T.look.radius).dp
    val maskColor = T.c.bgTop
    val frac = remember { Animatable(if (fullscreen) 1f else 0f) }
    var settling by remember { mutableStateOf(false) }
    LaunchedEffect(fullscreen) {
        settling = true
        frac.animateTo(if (fullscreen) 1f else 0f, spring(dampingRatio = 0.92f, stiffness = 380f))
        settling = false
    }

    // الإحداثيات فيزيائية: نثبّت LTR لهذه الطبقة فلا ينعكس الموضع في الواجهة العربية
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.fillMaxSize()) {
            PlayerUi(
                engine = live.engine,
                mode = if (fullscreen) PlayerMode.Fullscreen else PlayerMode.Inline,
                settings = settings,
                title = current?.title.orEmpty(),
                subtitle = current?.category,
                onBack = if (fullscreen) ({ live.exitFullscreen(true) }) else null,
                onFullscreen = if (fullscreen) ({ live.exitFullscreen(true) }) else ({ live.enterFullscreen(false) }),
                onPrev = if (canStep) ({ live.step(-1) }) else null,
                onNext = if (canStep) ({ live.step(1) }) else null,
                onMore = if (fullscreen) null else ({ live.moreOpen.value = true }),
                fav = isFav,
                onFav = if (current != null) ({ current?.let { live.toggleFav(it) } }) else null,
                onFit = onFit,
                settling = settling,
                idleLabel = if (current == null) S.pickChannel else null,
                modifier = Modifier
                    .absoluteOffset {
                        val r = slot()
                        IntOffset(lerp(r.left, 0f, frac.value).roundToInt(), lerp(r.top, 0f, frac.value).roundToInt())
                    }
                    .graphicsLayer {
                        val r = cornerDp.toPx() * (1f - frac.value)
                        shape = RoundedCornerShape(r)
                        clip = r > 0.5f
                    }
                    .layout { m, c ->
                        val r = slot()
                        val f = frac.value
                        val w = lerp(r.width, c.maxWidth.toFloat(), f).roundToInt().coerceAtLeast(1)
                        val h = lerp(r.height, c.maxHeight.toFloat(), f).roundToInt().coerceAtLeast(1)
                        val p = m.measure(Constraints.fixed(w, h))
                        layout(w, h) { p.place(0, 0) }
                    },
            )
            // SurfaceView قد لا يلتزم بقصّ الزوايا: قناع صغير يلوّن زوايا الفتحة بلون الخلفية
            if (!fullscreen && !settling) Canvas(Modifier.fillMaxSize()) {
                val r = slot()
                val rad = cornerDp.toPx()
                val rr = androidx.compose.ui.geometry.RoundRect(r, androidx.compose.ui.geometry.CornerRadius(rad, rad))
                val path = androidx.compose.ui.graphics.Path().apply {
                    fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                    addRect(r)
                    addRoundRect(rr)
                }
                drawPath(path, maskColor)
            }
        }
    }
}
