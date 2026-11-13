package com.beouttv.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beouttv.app.core.UiState
import com.beouttv.app.data.Item
import com.beouttv.app.data.RemoteImage
import kotlin.math.roundToInt

/** نص يدعم العربية واللاتينية معاً: الاتجاه يُشتق من المحتوى نفسه لا من اتجاه الواجهة. */
@Composable
fun Txt(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = T.body,
    color: Color = T.c.text,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) {
    val ty = T.look.typo
    val sz = if (style.fontSize.isSp) style.fontSize.value else 14f
    val k = when { sz >= 20f -> ty.title; sz >= 13f -> ty.body; else -> ty.small }
    var st = style
    if (k != 1f && style.fontSize.isSp) st = st.copy(fontSize = style.fontSize * k)
    if ((k != 1f || ty.lineH != 1f) && style.lineHeight.isSp) st = st.copy(lineHeight = style.lineHeight * k * ty.lineH)
    if (ty.family != null) st = st.copy(fontFamily = ty.family)
    if (ty.weightShift != 0) {
        val w = ((style.fontWeight?.weight ?: 400) + ty.weightShift * 100).coerceIn(100, 900)
        st = st.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(w))
    }
    if (ty.letter != 0f) st = st.copy(letterSpacing = ty.letter.sp)
    BasicText(
        text, modifier,
        st.copy(color = if (ty.alpha < 1f) color.copy(alpha = color.alpha * ty.alpha) else color, textDirection = TextDirection.Content),
        maxLines = maxLines, minLines = minLines, overflow = TextOverflow.Ellipsis,
    )
}

/** إطار تركيز واضح للريموت. يظهر فقط عند وجود تركيز فعلي (لا يظهر مع اللمس). */
@Composable
fun Modifier.tvFocus(shape: Shape = RoundedCornerShape(14.dp)): Modifier {
    var f by remember { mutableStateOf(false) }
    val a = T.c.accent2
    val w = if (LocalTv.current) 3.dp else 2.dp
    return this.onFocusChanged { f = it.isFocused }.then(if (f) Modifier.border(w, a, shape) else Modifier)
}

@Composable
fun Modifier.tap(onClick: () -> Unit): Modifier =
    clickable(remember { MutableInteractionSource() }, null, onClick = onClick)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.tapHold(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    combinedClickable(
        interactionSource = remember { MutableInteractionSource() }, indication = null,
        onClick = onClick, onLongClick = onLongClick,
    )

/** مرجع لإحداثيات عنصر لقراءة حدوده لحظة النقر فقط (يستخدم لانتقال البطاقة إلى التفاصيل). */
class CoordRef {
    var c: LayoutCoordinates? = null
    fun bounds(): Rect? = c?.takeIf { it.isAttached }?.boundsInWindow()
}

fun Modifier.trackBounds(ref: CoordRef): Modifier = onGloballyPositioned { ref.c = it }

/** زر أيقونة بلا حاوية: منطقة لمس 40dp والأيقونة مجردة. */
@Composable
fun IconAction(ic: Ic, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 40.dp, tint: Color = T.c.text, active: Boolean = false) {
    val shape = RoundedCornerShape(12.dp)
    Box(modifier.size(size).tvFocus(shape).tap(onClick), contentAlignment = Alignment.Center) {
        BIcon(ic, size = size * 0.55f, tint = tint, active = active)
    }
}

@Composable
fun GlassIconButton(ic: Ic, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 44.dp, active: Boolean = false, tint: Color = T.c.text) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier.size(size).glass(shape).tvFocus(shape).tap(onClick),
        contentAlignment = Alignment.Center,
    ) { BIcon(ic, size = size * 0.5f, tint = if (active) T.c.accent2 else tint, active = active) }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, compact: Boolean = false) {
    val shape = RoundedCornerShape(50)
    val m = if (selected) Modifier.tinted(shape) else Modifier.flat(shape)
    val h = if (compact) 12.dp else 14.dp
    val v = if (compact) 6.dp else 8.dp
    Box(Modifier.then(m).tvFocus(shape).tap(onClick).padding(horizontal = h, vertical = v)) {
        Txt(text, style = T.small, color = if (selected) T.c.accent2 else T.c.text, maxLines = 1)
    }
}

/** شكل الأزرار الرئيسية حسب "نمط الأزرار": rounded مدور | squared مربع | غيرهما كبسولة. */
@Composable
fun btnShape(): Shape = when (T.look.btnStyle) {
    "squared" -> RoundedCornerShape(8.dp)
    "rounded" -> RoundedCornerShape(18.dp)
    else -> RoundedCornerShape(50)
}

/** سطح الزر الرئيسي: default/rounded/squared تعبئة التمييز | glass زجاج | floating بظل | minimal شفاف | gradient تدرج أفقي. */
@Composable
fun Modifier.primarySurface(shape: Shape): Modifier = when (T.look.btnStyle) {
    "glass" -> glassDense(shape)
    "floating" -> shadow(10.dp, shape, clip = false, ambientColor = T.c.accent.copy(alpha = 0.5f), spotColor = T.c.accent.copy(alpha = 0.6f)).accentFill(shape)
    "minimal" -> clip(shape).background(T.c.accent.copy(alpha = 0.16f))
    "gradient" -> clip(shape).background(Brush.horizontalGradient(listOf(T.c.accentDeep, T.c.accent, T.c.accent2)))
    else -> accentFill(shape)
}

@Composable
fun btnTextColor(): Color = when (T.look.btnStyle) { "minimal", "glass" -> T.c.accent2; else -> Color.White }

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = btnShape()
    Box(
        modifier.primarySurface(shape).tvFocus(shape).tap(onClick).padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Txt(text, style = T.h2, color = btnTextColor(), maxLines = 1) }
}

/**
 * زر المشاهدة: كبسولة بلون التمييز بارتفاع 48dp؛ أيقونة التشغيل من أيقونات التطبيق والنص متوازنان في الوسط.
 * ليس عريضاً ولا يحوي دائرة بيضاء (كانت تغطي النص).
 */
@Composable
fun WatchButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = btnShape()
    val tc = btnTextColor()
    Row(
        modifier.height((48 * T.look.density).dp).primarySurface(shape).tvFocus(shape).tap(onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        BIcon(Ic.Play, size = 20.dp, tint = tc, active = true)
        Txt(text, style = T.h2, color = tc, maxLines = 1)
    }
}

/** زر ثانوي بنفس الارتفاع (اختيار الموسم والحلقة). */
@Composable
fun TonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = btnShape()
    Box(
        modifier.height((48 * T.look.density).dp).flat(shape).tvFocus(shape).tap(onClick).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) { Txt(text, style = T.body, color = T.c.text, maxLines = 1) }
}

/** عرض بطاقة الملصق حسب عرض النافذة. */
@Composable
fun posterWidth(): Dp {
    val w = LocalConfiguration.current.screenWidthDp
    return when {
        w < 600 -> 118.dp
        w < 840 -> 140.dp
        else -> 156.dp
    } * T.look.poster
}

/** شكل البطاقة: انحناء مخصص لنوعها أو الافتراضي. */
@Composable
fun cardShapeOf(cfg: CardCfg, def: Shape): Shape = cfg.r?.let { RoundedCornerShape(it.dp) } ?: def

/** سطح البطاقة مع تخصيص النوع: ظل إضافي، حد، شفافية. */
@Composable
fun Modifier.cardSurface(cfg: CardCfg, shape: Shape): Modifier {
    var m = this
    val sh = cfg.sh
    if (sh != null && sh > 0) m = m.shadow(listOf(0, 3, 8, 16)[sh.coerceIn(0, 3)].dp, shape, clip = false)
    m = m.then(if (cfg.op < 1f) Modifier.graphicsLayer { alpha = cfg.op } else Modifier).flat(shape)
    val bd = cfg.bd
    if (bd != null && bd > 0f) m = m.border((bd * 0.5f).dp, T.c.edgeHi.copy(alpha = 0.5f), shape)
    return m
}

/** لمعة انعكاس خفيفة فوق الصورة (تُرسم فقط إذا كانت النسبة > 0). */
@Composable
fun CardSheen(rf: Float, modifier: Modifier = Modifier) {
    if (rf > 0f) Box(modifier.background(Brush.linearGradient(0f to Color.White.copy(alpha = 0.22f * rf), 0.4f to Color.Transparent, 1f to Color.Transparent)))
}

@Composable
fun PosterCard(item: Item, onClick: (Rect?) -> Unit, width: Dp = posterWidth(), progress: Float? = null) {
    val ref = remember { CoordRef() }
    val cfg = T.look.card(if (progress != null) "small" else "media")
    val tp = cfg.tp ?: if (T.look.cardStyle == "overlay") 1 else 0
    val overlay = tp == 1
    val shape = cardShapeOf(cfg, T.card)
    val tsty = T.small.copy(fontSize = (12 * cfg.ts).sp, lineHeight = (18 * cfg.ts).sp)
    val pad = (cfg.pd ?: 8f).dp
    val w = if (width == Dp.Unspecified) width else width * cfg.sz
    // Dp.Unspecified = يملأ عرض الخلية (الشبكات)، وإلا عرض ثابت (الصفوف الأفقية)
    Column(if (w == Dp.Unspecified) Modifier.fillMaxWidth() else Modifier.width(w)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.7f).trackBounds(ref)
                .cardSurface(cfg, shape).tvFocus(shape).tap { onClick(ref.bounds()) },
        ) {
            RemoteImage(item.poster ?: item.logo ?: item.backdrop, Modifier.fillMaxSize(), reqWidthPx = 360) {}
            CardSheen(cfg.rf, Modifier.fillMaxSize())
            item.quality?.let {
                Box(Modifier.align(Alignment.TopEnd).padding(6.dp).glassDense(RoundedCornerShape(50)).padding(horizontal = 7.dp, vertical = 1.dp)) {
                    Txt(it, style = T.small)
                }
            }
            if (overlay) {
                val gA = (cfg.gr ?: 80f) / 100f * 0.8f
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = gA)))).padding(start = pad, end = pad, top = 22.dp, bottom = if (progress != null) pad + 6.dp else pad)) {
                    Txt(item.title, Modifier.fillMaxWidth(), tsty, Color.White, maxLines = 2)
                }
            }
            if (progress != null) {
                Box(Modifier.align(Alignment.BottomCenter).padding(8.dp).fillMaxWidth().height(3.dp).background(Color(0x66FFFFFF), RoundedCornerShape(50))) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).background(T.c.accent2, RoundedCornerShape(50)))
                }
            }
        }
        if (tp == 0) Txt(item.title, Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp).fillMaxWidth(), tsty, maxLines = 2, minLines = 2)
    }
}

/** بطاقة قناة بحجم ثابت: الشعار بنسبته الأصلية (Fit) والاسم سطران كحد أقصى. */
@Composable
fun ChannelCard(item: Item, fav: Boolean, onClick: () -> Unit, onFav: () -> Unit, modifier: Modifier = Modifier) {
    val tv = LocalTv.current
    val cfg = T.look.card("channel")
    val shape = cardShapeOf(cfg, T.card)
    val tsty = T.small.copy(fontSize = (12 * cfg.ts).sp, lineHeight = (18 * cfg.ts).sp)
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.35f).cardSurface(cfg, shape).tvFocus(shape).tapHold(onClick, onFav)) {
            CardSheen(cfg.rf, Modifier.fillMaxSize())
            RemoteImage(
                item.logo ?: item.poster,
                Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
                reqWidthPx = 240, scale = ContentScale.Fit,
            ) {}
            if (!item.online) {
                Box(Modifier.align(Alignment.BottomStart).padding(6.dp).glassDense(RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 1.dp)) {
                    Txt(S.offlineChannel, style = T.small, color = T.c.danger)
                }
            }
            // على اللمس: زر قلب. على الريموت: ضغطة مطولة، ويظهر القلب كمؤشر فقط
            if (tv) {
                if (fav) BIcon(Ic.Heart, Modifier.align(Alignment.TopEnd).padding(8.dp), 16.dp, T.c.danger, active = true)
            } else {
                FavoriteButton(fav, onFav, Modifier.align(Alignment.TopEnd), size = 36.dp, container = false)
            }
        }
        if (cfg.tp != 2) Txt(item.title, Modifier.padding(top = 5.dp, start = 2.dp, end = 2.dp).fillMaxWidth(), tsty, maxLines = 2, minLines = 2)
    }
}

@Composable
fun SectionTitle(text: String) =
    Txt(text, Modifier.padding(horizontal = 16.dp, vertical = (8 * T.look.space).dp), T.h2)

/** زر القلب: نبضة نابضة قصيرة عند التبديل (بدون ارتداد مبالغ فيه). */
@Composable
fun FavoriteButton(active: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, size: Dp = 44.dp, container: Boolean = true) {
    val scale = remember { Animatable(1f) }
    var prev by remember { mutableStateOf(active) }
    LaunchedEffect(active) {
        if (prev != active) {
            prev = active
            scale.snapTo(0.72f)
            scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 520f))
        }
    }
    val shape = RoundedCornerShape(50)
    Box(
        modifier.size(size)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .then(if (container) Modifier.glass(shape) else Modifier)
            .tvFocus(shape).tap(onToggle),
        contentAlignment = Alignment.Center,
    ) { BIcon(Ic.Heart, size = size * 0.5f, tint = if (active) T.c.danger else T.c.text, active = active) }
}

@Composable
fun Spinner(dia: Dp = 28.dp, color: Color = T.c.accent2) {
    val t = rememberInfiniteTransition(label = "spinner")
    val a by t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "a")
    Canvas(Modifier.size(dia)) {
        drawArc(color, a, 270f, false, style = Stroke(dia.toPx() * 0.11f, cap = StrokeCap.Round))
    }
}

/** خط تقدم رفيع غير محدد: يُعرض فقط أثناء تحديث المحتوى عند تغيير الفلتر. */
@Composable
fun ProgressLine(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "line")
    val x by t.animateFloat(-0.35f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "x")
    val c = T.c.accent2
    Canvas(modifier.fillMaxWidth().height(2.dp)) {
        drawRect(c.copy(alpha = 0.9f), Offset(size.width * x, 0f), Size(size.width * 0.35f, size.height))
    }
}

/** هيكل تحميل ثابت (بدون حركة مستمرة): صفوف قائمة وشبكة ملصقات بنفس أسطح النمط الحالي. */
@Composable
fun Skeleton(modifier: Modifier = Modifier) {
    val g = (12 * T.look.space).dp
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(g)) {
        Box(Modifier.fillMaxWidth(0.35f).height(16.dp).flat(RoundedCornerShape(50)))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { Box(Modifier.weight(1f).aspectRatio(0.7f).flat(T.card)) }
        }
        repeat(3) { Box(Modifier.fillMaxWidth().height(52.dp).flat(T.row)) }
    }
}

@Composable
fun MessageBox(text: String, action: String? = null, onAction: () -> Unit = {}, sub: String? = null, ic: Ic? = null) {
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        if (ic != null) Box(Modifier.padding(bottom = 14.dp).size(64.dp).flat(RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
            BIcon(ic, size = 30.dp, tint = T.c.textDim)
        }
        Txt(text, style = T.h2, color = T.c.text)
        sub?.let { Txt(it, Modifier.padding(top = 6.dp), T.small, T.c.textDim) }
        if (action != null) PrimaryButton(action, onAction, Modifier.padding(top = 18.dp))
    }
}

/** تعرض كل حالات الواجهة الموحدة. Refreshing تُعرض كمحتوى (التحديث لا يمسح الشاشة). */
@Composable
fun <T> StateHost(
    state: UiState<T>,
    onRetry: () -> Unit,
    emptyText: String = S.empty,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        UiState.Initial, UiState.Loading -> Skeleton()
        is UiState.Success -> content(state.data)
        is UiState.Refreshing -> content(state.data)
        UiState.Empty -> MessageBox(emptyText, ic = Ic.Search)
        is UiState.Error -> MessageBox(S.loadErr, S.retry, onRetry, ic = Ic.Close)
        UiState.Offline -> MessageBox(S.offline, S.retry, onRetry, sub = S.offlineHint, ic = Ic.Channels)
    }
}

@Composable
fun PullRefresh(refreshing: Boolean, onRefresh: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var pull by remember { mutableFloatStateOf(0f) }
    val thr = with(LocalDensity.current) { 72.dp.toPx() }
    val curRefresh by rememberUpdatedState(onRefresh)
    val isRef by rememberUpdatedState(refreshing)
    val conn = remember(thr) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (pull > 0f && available.y < 0f) {
                    val d = maxOf(available.y, -pull); pull += d; return Offset(0f, d)
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f && !isRef) {
                    pull = (pull + available.y * 0.5f).coerceAtMost(thr * 1.5f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pull >= thr && !isRef) curRefresh()
                pull = 0f
                return Velocity.Zero
            }
        }
    }
    Box(modifier.nestedScroll(conn)) {
        content()
        if (refreshing || pull > 0f) {
            val sweep = if (refreshing) 0f else (pull / thr).coerceIn(0f, 1f)
            Box(
                Modifier.align(Alignment.TopCenter)
                    .offset { IntOffset(0, (if (refreshing) 12.dp.toPx() else pull * 0.6f).roundToInt()) }
                    .size(34.dp).glassDense(RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                if (refreshing) Spinner(18.dp)
                else Canvas(Modifier.size(18.dp)) {
                    drawArc(Color.White.copy(alpha = 0.9f), -90f, sweep * 300f, false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }
    }
}

@Composable
fun OfflinePill(modifier: Modifier = Modifier) {
    Row(
        modifier.glassDense(RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(7.dp).background(T.c.danger, RoundedCornerShape(50)))
        Txt(S.offlineBanner, style = T.small, color = T.c.textDim, maxLines = 1)
    }
}

/** سطح الحوارات/اللوحات حسب "نمط الحوارات": sheet زجاج كثيف | solid معتم | flat مسطح | outlined زجاج بحد. */
@Composable
fun Modifier.dialogSurface(shape: Shape): Modifier = when (T.look.ap["dlgStyle"]) {
    "solid" -> clip(shape).background(T.c.dense)
    "flat" -> flat(shape)
    "outlined" -> glassDense(shape).border(1.dp, T.c.edgeHi.copy(alpha = 0.5f), shape)
    else -> glassDense(shape)
}

/** حوار تأكيد بزجاج كثيف: يدخل بتكبير هادئ مع تعتيم الخلفية. */
@Composable
fun ConfirmDialog(visible: Boolean, title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    BackHandler(visible) { onDismiss() }
    AnimatedVisibility(visible, enter = fadeIn(tween(160)), exit = fadeOut(tween(140))) {
        Box(Modifier.fillMaxSize().background(Color(0x99000000)).tap(onDismiss), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(28.dp)
                    .animateEnterExit(
                        enter = scaleIn(spring(dampingRatio = 0.85f, stiffness = 500f), initialScale = 0.92f) + fadeIn(tween(140)),
                        exit = scaleOut(tween(120), targetScale = 0.96f) + fadeOut(tween(100)),
                    )
                    .dialogSurface(GlassShape).tap {}.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Txt(title, style = T.h2)
                Txt(text, style = T.body, color = T.c.textDim)
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(confirmLabel, onConfirm)
                    Chip(S.cancel, false, onDismiss)
                }
            }
        }
    }
}

/** لوحة سفلية بزجاج كثيف: تنزلق بنابض هادئ. */
@Composable
fun GlassSheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(visible) { onDismiss() }
    AnimatedVisibility(visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(160))) {
        Box(Modifier.fillMaxSize().background(Color(0x80000000)).tap(onDismiss), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier.fillMaxWidth()
                    .animateEnterExit(
                        enter = slideInVertically(spring(dampingRatio = 0.9f, stiffness = 380f)) { it },
                        exit = slideOutVertically(tween(180)) { it },
                    )
                    .dialogSurface(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).tap {}
                    .navigationBarsPadding().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}

/** إضاءة محيطية خافتة جداً تُرسم مرة واحدة (بدون نيون ولا حركة). */
@Composable
fun Modifier.aeroGlow(): Modifier {
    val c = T.c
    return drawBehind {
        val r1 = size.width * 0.9f
        drawCircle(
            Brush.radialGradient(listOf(c.accent.copy(alpha = 0.10f), Color.Transparent), center = Offset(size.width * 0.9f, 0f), radius = r1),
            radius = r1, center = Offset(size.width * 0.9f, 0f),
        )
        val r2 = size.width * 0.8f
        drawCircle(
            Brush.radialGradient(listOf(c.accent2.copy(alpha = 0.05f), Color.Transparent), center = Offset(0f, size.height), radius = r2),
            radius = r2, center = Offset(0f, size.height),
        )
    }
}
