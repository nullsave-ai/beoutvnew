package com.beouttv.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beouttv.app.core.UiState
import com.beouttv.app.data.HistoryEntry
import com.beouttv.app.data.HomeResponse
import com.beouttv.app.data.Item
import com.beouttv.app.data.RemoteImage
import com.beouttv.app.data.Section
import com.beouttv.app.data.Settings
import kotlin.math.absoluteValue

/** ما يمكن متابعته: أفلام وحلقات بدأت ولم تنتهِ (لا قنوات). تستخدمها الرئيسية وصفحة الأفلام. */
internal fun inProgress(h: List<HistoryEntry>): List<HistoryEntry> =
    h.filter { it.item.kind != "channel" && it.durationMs > 0 && it.positionMs in 1 until (it.durationMs * 95 / 100) }
        .distinctBy { it.item.id }

private val ShadeBrush = Brush.verticalGradient(0.42f to Color.Transparent, 1f to Color(0xE6000000))
private val BigTitle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)

private class HomeModel(val hero: List<Item>, val cont: List<HistoryEntry>, val rows: List<Section>)

/** يبني محتوى الرئيسية: بطاقات العرض، المتابعة، والصفوف؛ بلا فارغ ولا مكرر، ومرشّحاً بنوع المحتوى. */
private fun buildHome(data: HomeResponse, hist: List<HistoryEntry>, kind: String?, heroN: Int = 6): HomeModel {
    fun ok(i: Item) = kind == null || i.kind == kind
    val sections = data.sections.filter { it.type != "continue" && it.type != "categories" }
    val hero = (sections.firstOrNull { it.type == "hero" }?.items.orEmpty().ifEmpty {
        sections.filter { it.type != "channels" && it.type != "hero" }.flatMap { it.items }
    }).filter { !it.isChannel && ok(it) && (it.poster != null || it.backdrop != null) }.distinctBy { it.id }.take(heroN)

    val seenIds = HashSet<String>()
    val seenSets = HashSet<List<String>>()
    val rows = ArrayList<Section>()
    for (s in sections) {
        if (s.type == "hero") continue
        val items = s.items.filter(::ok).distinctBy { it.id }
        if (items.isEmpty() || !seenIds.add(s.id) || !seenSets.add(items.map { it.id })) continue
        rows += s.copy(items = items)
    }
    val cont = inProgress(hist).filter { ok(it.item) }
    return HomeModel(hero, cont, rows)
}

private fun openFrom(nav: NavVM, live: LiveVM, item: Item, siblings: List<Item>, origin: Rect?) {
    if (item.isChannel) { live.play(item, siblings); nav.tab(1) }
    else openItem(nav, item, siblings, origin)
}

@Composable
fun HomeScreen(vm: HomeVM, nav: NavVM, live: LiveVM, settings: Settings) {
    val st by vm.home.state.collectAsStateWithLifecycle()
    val hist by vm.history.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    var kind by rememberSaveable { mutableStateOf<String?>(null) }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(Modifier.fillMaxSize()) {
        PullRefresh(st is UiState.Refreshing<*>, { vm.home.refresh() }, Modifier.fillMaxSize()) {
            StateHost(st, { vm.home.refresh() }) { data ->
                val carouselHome = settings.homeLayout == "carousel"
                val m = remember(data, hist, kind, carouselHome) { buildHome(data, hist, kind, if (carouselHome) 10 else 6) }
                val favIds = remember(favs) { favs.mapTo(HashSet()) { it.id } }
                LazyColumn(
                    Modifier.fillMaxSize(), vm.listState,
                    contentPadding = PaddingValues(top = statusTop + Dm.Header + 6.dp, bottom = LocalBottomPad.current),
                    verticalArrangement = Arrangement.spacedBy((T.look.lay.secGap * T.look.space).dp),
                ) {
                    if (m.hero.isNotEmpty()) item(key = "hero") {
                        if (carouselHome) HeroCarousel(m.hero, settings, nav, live) else HeroPager(m.hero, settings.homeLayout == "hero", nav, live)
                    }
                    item(key = "kinds") { KindChips(kind) { kind = it } }
                    if (m.cont.isNotEmpty()) item(key = "cont") {
                        Column {
                            SectionHeader(S.continueWatching)
                            LazyRow(contentPadding = PaddingValues(horizontal = T.look.lay.hPad.dp), horizontalArrangement = Arrangement.spacedBy(T.look.lay.cardGap.dp)) {
                                items(m.cont, key = { it.key }) { e ->
                                    PosterCard(
                                        e.item,
                                        { origin ->
                                            if (e.item.kind == "movie") nav.push(Route.Player(PlayReq(listOf(PlayEntry(e.item)))))
                                            else nav.push(Route.Detail(e.item.id, e.item), origin)
                                        },
                                        progress = e.positionMs.toFloat() / e.durationMs,
                                    )
                                }
                            }
                        }
                    }
                    items(m.rows, key = { it.id }) { s ->
                        val first = settings.homeLayout == "hero" && s.id == m.rows.firstOrNull { r -> r.type != "channels" }?.id
                        Column {
                            SectionHeader(s.title)
                            LazyRow(contentPadding = PaddingValues(horizontal = T.look.lay.hPad.dp), horizontalArrangement = Arrangement.spacedBy(T.look.lay.cardGap.dp)) {
                                items(s.items, key = { it.id }) { it ->
                                    when {
                                        it.isChannel -> ChannelCard(
                                            it, it.id in favIds, { openFrom(nav, live, it, s.items, null) }, { vm.toggleFav(it) },
                                            Modifier.width(posterWidth() * 1.2f),
                                        )
                                        first -> FeatureCard(it) { o -> openFrom(nav, live, it, s.items, o) }
                                        else -> PosterCard(it, { o -> openFrom(nav, live, it, s.items, o) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        TopBar(
            settings.toolbarStyle,
            listOf(TopAction(Ic.Search, { nav.push(Route.Search()) }), TopAction(Ic.Settings, { nav.push(Route.Settings) })),
            Modifier.align(Alignment.TopCenter).padding(top = statusTop + 4.dp),
        )
    }
}

@Composable
private fun KindChips(kind: String?, onKind: (String?) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = T.look.lay.hPad.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Chip(S.kindAll, kind == null, { onKind(null) }) }
        item { Chip(S.movie, kind == "movie", { onKind("movie") }) }
        item { Chip(S.series, kind == "series", { onKind("series") }) }
        item { Chip(S.channel, kind == "channel", { onKind("channel") }) }
    }
}

/**
 * بطاقات العرض. big=true (المرجع الثاني): بطاقة كبيرة بالعنوان والوصف فوق الصورة وشارة في الزاوية.
 * big=false (المرجع الأول): بطاقة وسطية بزر تشغيل زجاجي، والعنوان في الوسط تحتها مع نقاط الصفحات.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroPager(items: List<Item>, big: Boolean, nav: NavVM, live: LiveVM) {
    val pager = rememberPagerState(pageCount = { items.size })
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            pager, Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = if (big) 36.dp else 72.dp),
            pageSpacing = 12.dp,
        ) { i ->
            val it = items[i]
            val ref = remember { CoordRef() }
            val off = ((pager.currentPage - i) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
            val lcfg = T.look.card("large")
            val hshape = cardShapeOf(lcfg, T.hero)
            Box(
                Modifier.fillMaxWidth().aspectRatio(if (big) 0.8f else 0.74f)
                    .graphicsLayer { val sc = 1f - 0.07f * off; scaleX = sc; scaleY = sc; alpha = 1f - 0.4f * off }
                    .trackBounds(ref).cardSurface(lcfg, hshape).tvFocus(hshape)
                    .tap { openFrom(nav, live, it, items, ref.bounds()) },
            ) {
                RemoteImage(it.poster ?: it.backdrop, Modifier.fillMaxSize(), reqWidthPx = 720) {}
                val badge = it.quality ?: it.year?.toString()
                if (big) {
                    Box(Modifier.fillMaxSize().background(ShadeBrush))
                    if (badge != null) Txt(
                        badge, Modifier.align(Alignment.TopEnd).padding(14.dp).glassDense(RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp),
                        T.small, Color.White, maxLines = 1,
                    )
                    Column(Modifier.align(Alignment.BottomStart).padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Txt(it.title, style = BigTitle, color = Color.White, maxLines = 2)
                        it.description?.let { d -> Txt(d, style = T.small, color = Color(0xCCFFFFFF), maxLines = 2) }
                    }
                } else {
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color(0x99000000))))
                    Box(Modifier.align(Alignment.Center).size(62.dp).glassDense(RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                        BIcon(Ic.Play, size = 28.dp, tint = Color.White, active = true)
                    }
                }
            }
        }
        if (!big) {
            val cur = items[pager.currentPage.coerceIn(0, items.lastIndex)]
            Txt(if (cur.isSeries) S.seriesTag else S.movieTag, Modifier.padding(top = 12.dp), T.small, T.c.textDim, maxLines = 1)
            Txt(cur.title, Modifier.padding(horizontal = 24.dp), BigTitle, T.c.text, maxLines = 1)
        }
        if (items.size > 1) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items.indices.forEach { d ->
                val on = d == pager.currentPage
                Box(Modifier.size(width = if (on) 16.dp else 6.dp, height = 6.dp).background(if (on) T.c.accent2 else T.c.textDim.copy(alpha = 0.4f), RoundedCornerShape(50)))
            }
        }
    }
}

/** الكاروسيل ثلاثي الأبعاد في الرئيسية: النمط والمعاملات من الإعدادات؛ والغلاف المركزي يغذي الخلفية المموهة. */
@Composable
private fun HeroCarousel(items: List<Item>, settings: Settings, nav: NavVM, live: LiveVM) {
    val cfg = remember(settings.ap) { carCfg(settings.ap) }
    var sel by remember { mutableStateOf(0) }
    val cur = items.getOrNull(sel.coerceIn(0, items.lastIndex))
    androidx.compose.runtime.LaunchedEffect(cur?.id) { BackdropHub.url.value = cur?.poster ?: cur?.backdrop }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { BackdropHub.url.value = null } }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Carousel3D(items, cfg, Modifier, onSelect = { sel = it }, onOpen = { openFrom(nav, live, it, items, null) }, onLong = { openFrom(nav, live, it, items, null) })
        if (cfg.f("textPos").toInt() == 3 && cur != null) {
            Txt(cur.title, Modifier.padding(horizontal = 24.dp), BigTitle, T.c.text, maxLines = 1)
        }
        if (items.size > 1) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items.indices.forEach { d ->
                val on = d == sel
                Box(Modifier.size(width = if (on) 16.dp else 6.dp, height = 6.dp).background(if (on) T.c.accent2 else T.c.textDim.copy(alpha = 0.4f), RoundedCornerShape(50)))
            }
        }
    }
}

/** بطاقة "الأكثر رواجاً" (المرجع الثاني): صورة كبيرة، عنوان ووصف فوقها، وشارة. */
@Composable
private fun FeatureCard(item: Item, onClick: (Rect?) -> Unit) {
    val ref = remember { CoordRef() }
    val cfg = T.look.card("featured")
    val shape = cardShapeOf(cfg, RoundedCornerShape(22.dp))
    Box(Modifier.width((250 * cfg.sz).dp).aspectRatio(0.82f).trackBounds(ref).cardSurface(cfg, shape).tvFocus(shape).tap { onClick(ref.bounds()) }) {
        RemoteImage(item.poster ?: item.backdrop ?: item.logo, Modifier.fillMaxSize(), reqWidthPx = 600) {}
        Box(Modifier.fillMaxSize().background(ShadeBrush))
        CardSheen(cfg.rf, Modifier.fillMaxSize())
        val badge = item.quality ?: item.year?.toString()
        if (badge != null) Txt(
            badge, Modifier.align(Alignment.TopEnd).padding(10.dp).glassDense(RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 2.dp),
            T.small, Color.White, maxLines = 1,
        )
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(item.title, style = T.h2, color = Color.White, maxLines = 1)
            item.description?.let { Txt(it, style = T.small, color = Color(0xCCFFFFFF), maxLines = 2) }
        }
    }
}
