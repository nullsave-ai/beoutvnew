package com.beouttv.app.ui

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beouttv.app.core.UiState
import com.beouttv.app.core.dataOrNull
import com.beouttv.app.data.Detail
import com.beouttv.app.data.HistoryEntry
import com.beouttv.app.data.Item
import com.beouttv.app.data.RemoteImage
import kotlin.math.roundToInt

private val HeroH = 380.dp

/**
 * صفحة التفاصيل. p هو تقدم الانتقال [0..1].
 * انتقال شبيه بالعنصر المشترك: صورة البطاقة (ghost) تتمدد من حدود البطاقة المضغوطة إلى مستطيل الغلاف
 * وتنكمش عائدة عند الإغلاق، بينما بقية المحتوى يظهر تدريجياً. بدون بذرة أو حدود يكتفي بالظهور التدريجي.
 */
@Composable
fun DetailScreen(id: String, seed: Item?, origin: Rect?, p: () -> Float, vm: DetailVM, nav: NavVM) {
    val loader = remember(id) { vm.loader(id) }
    LaunchedEffect(id) { loader.load() }
    val st by loader.state.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val hist by vm.history.collectAsStateWithLifecycle()

    val data = st.dataOrNull()
    val item = data?.item ?: seed
    val listState = rememberLazyListState()
    val atTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 } }
    // الـ ghost يعمل فقط والصفحة في أعلاها (وإلا نكتفي بالتلاشي حتى لا يقفز الغلاف)
    val ghost by remember(origin, item != null) { derivedStateOf { origin != null && item != null && atTop && p() < 0.999f } }
    val heroPx = with(LocalDensity.current) { HeroH.toPx() }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = (p() * 1.6f).coerceIn(0f, 1f) }.screenBackground())

        Box(Modifier.fillMaxSize().graphicsLayer { alpha = ((p() - 0.2f) / 0.8f).coerceIn(0f, 1f) }) {
            if (item == null) {
                StateHost(st, { loader.refresh() }) { }
            } else {
                DetailBody(
                    item, data, st, favs.any { it.id == item.id }, hist, listState, vm, nav,
                    heroHidden = { ghost }, onRetry = { loader.refresh() },
                )
            }
        }

        if (ghost && origin != null && item != null) {
            Box(
                Modifier.fillMaxSize().layout { m, c ->
                    val v = p().coerceIn(0f, 1f)
                    val w = lerp(origin.width, c.maxWidth.toFloat(), v).roundToInt().coerceAtLeast(1)
                    val h = lerp(origin.height, heroPx, v).roundToInt().coerceAtLeast(1)
                    val x = lerp(origin.left, 0f, v).roundToInt()
                    val y = lerp(origin.top, 0f, v).roundToInt()
                    val pl = m.measure(Constraints.fixed(w, h))
                    layout(c.maxWidth, c.maxHeight) { pl.place(x, y) }
                },
            ) {
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        shape = RoundedCornerShape(lerp(14.dp.toPx(), 0f, p().coerceIn(0f, 1f)))
                        clip = true
                    },
                ) { RemoteImage(item.poster ?: item.backdrop, Modifier.fillMaxSize(), reqWidthPx = 360) {} }
            }
        }

        GlassIconButton(
            Ic.Back, { nav.pop() },
            Modifier.statusBarsPadding().padding(12.dp).graphicsLayer { alpha = p().coerceIn(0f, 1f) },
        )
    }
}

@Composable
private fun DetailBody(
    item: Item,
    d: Detail?,
    st: UiState<Detail>,
    isFav: Boolean,
    hist: List<HistoryEntry>,
    listState: LazyListState,
    vm: DetailVM,
    nav: NavVM,
    heroHidden: () -> Boolean,
    onRetry: () -> Unit,
) {
    var seasonIdx by remember(item.id) { mutableIntStateOf(0) }
    var expanded by remember(item.id) { mutableStateOf(false) }
    val seasons = d?.seasons ?: emptyList()
    val season = seasons.getOrNull(seasonIdx)
    val lastEntry = remember(hist, item.id) { hist.firstOrNull { it.item.id == item.id } }
    val entries = remember(d) {
        d?.seasons?.flatMap { s ->
            s.episodes.map { e ->
                PlayEntry(item, e.id, "${S.season} ${s.number} - ${S.episodeWord} ${e.number}" + (if (e.title.isNotBlank()) ": ${e.title}" else ""))
            }
        } ?: emptyList()
    }
    val blend = colorLerp(T.c.bgTop, T.c.bgBottom, 0.45f)

    LazyColumn(Modifier.fillMaxSize(), listState, contentPadding = PaddingValues(bottom = 32.dp)) {
        item(key = "hero") {
            Box(Modifier.fillMaxWidth().height(HeroH).graphicsLayer { alpha = if (heroHidden()) 0f else 1f }) {
                // المصغّرة المخزنة من البطاقة تظهر فوراً ثم تُستبدل بالغلاف عالي الدقة
                RemoteImage(item.backdrop ?: item.poster, Modifier.fillMaxSize(), reqWidthPx = 1080) {
                    RemoteImage(item.poster ?: item.backdrop, Modifier.fillMaxSize(), reqWidthPx = 360) {}
                }
                Box(Modifier.fillMaxWidth().height(110.dp).background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent))))
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(colorStops = arrayOf(0.45f to Color.Transparent, 1f to blend)),
                    ),
                )
            }
        }
        item(key = "info") {
            Column(Modifier.padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Txt(item.title, style = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, lineHeight = 38.sp), maxLines = 3, modifier = Modifier.fillMaxWidth())
                val metas = listOfNotNull(
                    item.year?.toString(), item.category, item.quality,
                    item.durationMin?.let { m -> "$m ${S.minutes}" },
                )
                if (metas.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(metas) { t -> Box(Modifier.flat(RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 7.dp)) { Txt(t, style = T.small, maxLines = 1) } }
                }
                if (lastEntry != null && lastEntry.durationMs > 0 && !item.isSeries)
                    ThinProgress(lastEntry.positionMs.toFloat() / lastEntry.durationMs, Modifier.padding(top = 2.dp))
                val canPlay = !item.isSeries || entries.isNotEmpty()
                if (canPlay) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    val label = if (lastEntry != null && lastEntry.positionMs > 0) S.resume else S.play
                    PlayPill(label, {
                        if (item.isSeries) {
                            val idx = lastEntry?.episodeId?.let { eid -> entries.indexOfFirst { e -> e.episodeId == eid } }?.takeIf { x -> x >= 0 } ?: 0
                            nav.push(Route.Player(PlayReq(entries, idx)))
                        } else nav.push(Route.Player(PlayReq(listOf(PlayEntry(item)))))
                    }, Modifier.weight(1f))
                    FavoriteButton(isFav, { vm.toggleFav(item) }, size = 56.dp)
                }
                item.description?.takeIf { it.isNotBlank() }?.let { t ->
                    Txt(
                        t, Modifier.tap { expanded = !expanded }, T.body, T.c.textDim,
                        maxLines = if (expanded) Int.MAX_VALUE else 4,
                    )
                }
                if (d == null) LoadStatus(st, onRetry)
            }
        }
        if (item.isSeries && seasons.isNotEmpty()) {
            item(key = "seasons") {
                LazyRow(Modifier.padding(top = 14.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(seasons.size) { i -> Chip("${S.season} ${seasons[i].number}", i == seasonIdx, { seasonIdx = i }) }
                }
            }
            season?.let { s ->
                items(s.episodes, key = { e -> "ep-" + e.id }) { e ->
                    val progress = hist.firstOrNull { h -> h.episodeId == e.id }
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth().flat(T.card).tvFocus(T.card)
                            .tap {
                                val idx = entries.indexOfFirst { x -> x.episodeId == e.id }.coerceAtLeast(0)
                                nav.push(Route.Player(PlayReq(entries, idx)))
                            }.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(96.dp, 56.dp).flat(RoundedCornerShape(10.dp))) {
                            RemoteImage(e.poster ?: item.backdrop, Modifier.fillMaxSize(), reqWidthPx = 240) {}
                        }
                        Column(Modifier.weight(1f)) {
                            Txt("${S.episodeWord} ${e.number}" + if (e.title.isNotBlank()) " - ${e.title}" else "", style = T.body, maxLines = 1)
                            e.durationMin?.let { m -> Txt("$m ${S.minutes}", style = T.small, color = T.c.textDim) }
                            if (progress != null && progress.durationMs > 0)
                                ThinProgress(progress.positionMs.toFloat() / progress.durationMs, Modifier.padding(top = 4.dp))
                        }
                        BIcon(Ic.Play, size = 22.dp, tint = T.c.accent2)
                    }
                }
            }
        }
        val related = d?.related?.filter { it.id != item.id }.orEmpty()
        if (related.isNotEmpty()) {
            item(key = "related") {
                Column(Modifier.padding(top = 14.dp)) {
                    SectionHeader(S.related)
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(related, key = { r -> r.id }) { r -> PosterCard(r, { origin -> openItem(nav, r, related, origin) }) }
                    }
                }
            }
        }
    }
}

/** حالة تحميل التفاصيل تحت العنوان المعروض من البذرة. */
@Composable
private fun LoadStatus(st: UiState<Detail>, onRetry: () -> Unit) {
    when (st) {
        UiState.Initial, UiState.Loading -> Box(Modifier.padding(top = 8.dp)) { Spinner(24.dp) }
        UiState.Offline -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(S.offline, style = T.small, color = T.c.textDim)
            Chip(S.retry, false, onRetry, compact = true)
        }
        is UiState.Error, UiState.Empty -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(S.loadErr, style = T.small, color = T.c.textDim)
            Chip(S.retry, false, onRetry, compact = true)
        }
        else -> {}
    }
}
