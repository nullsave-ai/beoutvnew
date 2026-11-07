package com.beouttv.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beouttv.app.core.PagedLoader
import com.beouttv.app.data.Item
import com.beouttv.app.data.RemoteImage

/**
 * قسم في لوحة الأقسام (يسار الصفحة). items: عناصر القسم إن كانت معروفة مسبقاً (الرئيسية)،
 * أو null إن كانت تُحمَّل من الخادم عند اختياره (صفحة القنوات).
 */
@Immutable
data class LiveSection(val key: String, val title: String, val items: List<Item>? = null)

private val RowShape = RoundedCornerShape(12.dp)

// ===================== التخطيط: القنوات يمين والأقسام يسار =====================

/**
 * صف اللوحتين. الواجهة RTL، لذلك أول عنصر (القنوات) يقع في اليمين، والأقسام في اليسار.
 */
@Composable
fun PanesRow(
    sectionsW: Dp,
    modifier: Modifier = Modifier,
    sections: @Composable (Modifier) -> Unit,
    channels: @Composable (Modifier) -> Unit,
) {
    Row(modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        channels(Modifier.weight(1f).fillMaxHeight())
        sections(Modifier.width(sectionsW).fillMaxHeight())
    }
}

// ===================== لوحة الأقسام =====================

@Composable
fun SectionsPane(
    sections: List<LiveSection>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomPad: Dp = 0.dp,
    dim: Boolean = false,
) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(top = 4.dp, bottom = bottomPad),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(sections, key = { it.key }) { s ->
            SectionItem(s, s.key == selected && !dim) { onSelect(s.key) }
        }
    }
}

@Composable
private fun SectionItem(s: LiveSection, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp)
            .then(if (selected) Modifier.accentFill(shape) else Modifier.glass(shape))
            .tvFocus(shape).tap(onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Txt(s.title, Modifier.weight(1f), T.small, if (selected) Color.White else T.c.text, maxLines = 2)
        val n = s.items?.size
        if (n != null) Txt(n.toString(), style = T.small, color = if (selected) Color(0xCCFFFFFF) else T.c.textDim, maxLines = 1)
    }
}

// ===================== لوحة القنوات =====================

/**
 * قائمة القنوات. القناة الجارية مميزة وتُمرَّر إلى مجال الرؤية تلقائياً عند تغيّرها.
 * header يُعرض فوق القائمة (عنوان القسم في الرئيسية، حقل البحث في صفحة القنوات).
 * onEnd: يُستدعى عند الاقتراب من نهاية القائمة (التحميل التدريجي).
 */
@Composable
fun ChannelsPane(
    items: List<Item>,
    currentId: String?,
    playing: Boolean,
    favIds: Set<String>,
    onPick: (Item) -> Unit,
    onFav: (Item) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    scrollKey: Any? = null,
    bottomPad: Dp = 0.dp,
    loadingMore: Boolean = false,
    onEnd: (() -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
) {
    LaunchedEffect(currentId, scrollKey) {
        val i = items.indexOfFirst { it.id == currentId }
        if (i >= 0 && state.layoutInfo.visibleItemsInfo.none { it.index == i }) state.animateScrollToItem(i)
    }
    if (onEnd != null) {
        val near by remember(state) {
            derivedStateOf {
                val info = state.layoutInfo
                val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                info.totalItemsCount > 0 && last >= info.totalItemsCount - 6
            }
        }
        LaunchedEffect(near) { if (near) onEnd() }
    }
    Column(modifier) {
        header?.invoke()
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f), state,
            contentPadding = PaddingValues(top = 4.dp, bottom = bottomPad),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(items, key = { it.id }) { ch ->
                ChannelRow(ch, ch.id == currentId, playing && ch.id == currentId, ch.id in favIds, { onPick(ch) }, { onFav(ch) })
            }
            if (loadingMore) item(key = "more") {
                Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) { Spinner(22.dp) }
            }
        }
    }
}

@Composable
private fun Modifier.playingFill(shape: Shape): Modifier {
    val c = T.c
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(c.accent.copy(alpha = 0.34f), c.accentDeep.copy(alpha = 0.22f))))
        .border(1.dp, c.accent2.copy(alpha = 0.75f), shape)
}

/** صف قناة: شعار، اسم، حالة، قلب. الجارية مميزة بتدرج أزرق ومؤشر أعمدة متحرك. */
@Composable
fun ChannelRow(item: Item, current: Boolean, animating: Boolean, fav: Boolean, onClick: () -> Unit, onFav: () -> Unit) {
    val tv = LocalTv.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .then(if (current) Modifier.playingFill(RowShape) else Modifier.glass(RowShape))
            .tvFocus(RowShape).tapHold(onClick, onFav)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(width = 48.dp, height = 36.dp), contentAlignment = Alignment.Center) {
            RemoteImage(item.logo ?: item.poster, Modifier.fillMaxSize(), reqWidthPx = 160, scale = ContentScale.Fit) {}
        }
        Column(Modifier.weight(1f)) {
            Txt(item.title, style = T.small, color = if (current) T.c.accent2 else T.c.text, maxLines = 2)
            if (!item.online) Txt(S.offlineChannel, style = T.small, color = T.c.danger, maxLines = 1)
        }
        if (current) EqBars(T.c.accent2, animating)
        if (tv) {
            if (fav) BIcon(Ic.Heart, size = 16.dp, tint = T.c.danger, active = true)
        } else {
            FavoriteButton(fav, onFav, size = 36.dp, container = false)
        }
    }
}

/** ثلاثة أعمدة تتحرك كمؤشر موازن صوتي؛ تتجمد عند الإيقاف. الحركة تُقرأ في طور الرسم فقط. */
@Composable
fun EqBars(color: Color, animate: Boolean, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "eq")
    val a by t.animateFloat(0.30f, 1f, infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by t.animateFloat(1f, 0.35f, infiniteRepeatable(tween(430, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    val c by t.animateFloat(0.50f, 0.90f, infiniteRepeatable(tween(610, easing = LinearEasing), RepeatMode.Reverse), label = "c")
    Canvas(modifier.size(18.dp)) {
        val w = size.width / 5f
        val hs = if (animate) floatArrayOf(a, b, c) else floatArrayOf(0.4f, 0.4f, 0.4f)
        for (i in 0 until 3) {
            val h = size.height * hs[i]
            drawRoundRect(color, Offset(w * (2 * i), size.height - h), Size(w, h), CornerRadius(w / 2))
        }
    }
}

// ===================== شريط "يُعرض الآن" =====================

/**
 * تحت المشغل مباشرة: شعار القناة واسمها وحالتها، وأزرار السابق/التالي والمفضلة وملء الشاشة.
 * على التلفاز هو الطريق الأقصر إلى ملء الشاشة بالريموت.
 */
@Composable
fun NowPlayingBar(
    item: Item?,
    engine: PlaybackEngine,
    fav: Boolean,
    canStep: Boolean,
    onFav: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().height(Dm.NowPlaying).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (item == null) {
            Txt(S.pickChannel, Modifier.weight(1f), T.body, T.c.textDim, maxLines = 1)
            return@Row
        }
        Box(Modifier.size(width = 46.dp, height = 36.dp).glass(RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            RemoteImage(item.logo ?: item.poster, Modifier.fillMaxSize().padding(4.dp), reqWidthPx = 160, scale = ContentScale.Fit) {}
        }
        Column(Modifier.weight(1f)) {
            Txt(item.title, style = T.h2, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).background(if (engine.fatal) T.c.textDim else T.c.danger, RoundedCornerShape(50)))
                val status = when {
                    engine.fatal -> S.playbackErr
                    engine.reconnecting -> S.reconnecting
                    else -> item.category?.let { "${S.live} · $it" } ?: S.live
                }
                Txt(status, style = T.small, color = T.c.textDim, maxLines = 1)
            }
        }
        if (canStep) IconAction(Ic.Prev, onPrev, size = 40.dp)
        if (canStep) IconAction(Ic.Next, onNext, size = 40.dp)
        FavoriteButton(fav, onFav, size = 40.dp, container = false)
        IconAction(Ic.Fullscreen, onFullscreen, size = 40.dp)
    }
}

// ===================== شريط الاقتراحات =====================

/**
 * شريط واحد منفصل عن أقسام القنوات: أفلام ومسلسلات مقترحة بعناصر مضغوطة (غلاف + عنوان).
 * الضغط يفتح التفاصيل بانتقال الغلاف من مكان البطاقة.
 */
@Composable
fun SuggestStrip(items: List<Item>, onOpen: (Item, Rect?) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().height(Dm.Suggest)) {
        Txt(S.suggested, Modifier.padding(horizontal = 12.dp, vertical = 2.dp), T.small, T.c.accent2, maxLines = 1)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.id }) { it -> SuggestCard(it) { r -> onOpen(it, r) } }
        }
    }
}

@Composable
private fun SuggestCard(item: Item, onClick: (Rect?) -> Unit) {
    val ref = remember { CoordRef() }
    Row(
        Modifier.width(196.dp).height(66.dp).trackBounds(ref)
            .glass(CardShape).tvFocus(CardShape).tap { onClick(ref.bounds()) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(46.dp).fillMaxHeight()) {
            RemoteImage(item.poster ?: item.backdrop ?: item.logo, Modifier.fillMaxSize(), reqWidthPx = 160) {}
        }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(item.title, style = T.small, maxLines = 2)
            val tag = if (item.isSeries) S.seriesTag else S.movieTag
            Txt(if (item.year != null) "$tag · ${item.year}" else tag, style = T.small, color = T.c.textDim, maxLines = 1)
        }
    }
}

// ===================== تحميل تدريجي لقائمة عمودية =====================

@Composable
fun ListLoadMore(state: LazyListState, paged: PagedLoader) {
    val near by remember(state) {
        derivedStateOf {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(near) { if (near) paged.loadMore() }
}
