package com.beouttv.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beouttv.app.core.UiState
import com.beouttv.app.core.dataOrNull
import com.beouttv.app.data.HomeResponse
import com.beouttv.app.data.Item
import kotlinx.coroutines.launch

private val PanesSpring = spring<IntOffset>(dampingRatio = 0.92f, stiffness = 420f)

/**
 * هيكل الصفحتين المباشرتين (الرئيسية والقنوات): ترويسة، فتحة المشغل، شريط "يُعرض الآن"،
 * اقتراحات (الرئيسية فقط)، ثم لوحتا القنوات والأقسام. المشغل نفسه لا يُرسم هنا بل في PlayerLayer
 * الذي يتبع مستطيل الفتحة (slot).
 * التبديل بين الصفحتين يحرّك اللوحتين فقط؛ الترويسة والمشغل ثابتان فلا ينقطع البث.
 */
@Composable
fun LiveScaffold(
    tab: Int,
    split: Boolean,
    nav: NavVM,
    home: HomeVM,
    channels: ChannelsVM,
    live: LiveVM,
    slot: MutableState<Rect>,
    modifier: Modifier = Modifier,
) {
    val bottomPad = LocalBottomPad.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val homeState by home.home.state.collectAsStateWithLifecycle()
    val suggestions = remember(homeState) { suggestionsOf(homeState.dataOrNull()) }
    val cur by live.current.collectAsStateWithLifecycle()
    val canStep by live.canStep.collectAsStateWithLifecycle()
    val favs by live.favorites.collectAsStateWithLifecycle()
    val isFav = cur?.let { c -> favs.any { it.id == c.id } } == true

    BoxWithConstraints(modifier.fillMaxSize()) {
        val usableH = maxHeight - statusTop - bottomPad
        val d = liveDims(maxWidth, usableH, wantSuggest = tab == 0 && suggestions.isNotEmpty(), split = split)

        val header: @Composable () -> Unit = {
            Crossfade(tab, animationSpec = tween(180), label = "liveHeader") { t ->
                if (t == 0) ScreenHeader(S.app, brand = true) {
                    IconAction(Ic.Search, { nav.push(Route.Search()) })
                    IconAction(Ic.Settings, { nav.push(Route.Settings) })
                } else ScreenHeader(S.channels) {
                    IconAction(Ic.Settings, { nav.push(Route.Settings) })
                }
            }
        }
        val player: @Composable () -> Unit = {
            PlayerSlot(d.playerW, d.playerH, slot)
            NowPlayingBar(
                item = cur, engine = live.engine, fav = isFav, canStep = canStep,
                onFav = { cur?.let { live.toggleFav(it) } },
                onPrev = { live.step(-1) }, onNext = { live.step(1) },
                onFullscreen = { live.enterFullscreen(false) },
            )
            if (d.suggest && tab == 0) {
                SuggestStrip(suggestions, { item, origin -> nav.push(Route.Detail(item.id, item), origin) })
            }
        }
        val panes: @Composable (Modifier) -> Unit = { m ->
            AnimatedContent(
                targetState = tab,
                modifier = m,
                transitionSpec = {
                    val sign = (if (targetState > initialState) 1 else -1) * (if (rtl) -1 else 1)
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(PanesSpring) { sign * it / 8 } + fadeIn(tween(200)),
                        initialContentExit = slideOutHorizontally(PanesSpring) { -sign * it / 8 } + fadeOut(tween(120)),
                        targetContentZIndex = 0f,
                        sizeTransform = null,
                    )
                },
                label = "livePanes",
            ) { t ->
                if (t == 0) HomePanes(d, home, live, nav, bottomPad)
                else ChannelsPanes(d, channels, live, bottomPad)
            }
        }

        if (!d.split) {
            Column(Modifier.fillMaxSize()) {
                header()
                player()
                panes(Modifier.weight(1f).fillMaxSize())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                header()
                Row(Modifier.weight(1f).fillMaxSize()) {
                    Column(Modifier.width(d.colW).fillMaxHeight()) { player() }
                    panes(Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

/** فتحة سوداء بمقاس المشغل؛ تُبلغ عن مستطيلها فتتبعه طبقة المشغل. */
@Composable
private fun PlayerSlot(w: Dp, h: Dp, slot: MutableState<Rect>) {
    Box(
        Modifier.width(w).height(h).background(Color.Black).onGloballyPositioned {
            val r = it.boundsInRoot()
            if (r != slot.value) slot.value = r
        },
    )
}

// ===================== الرئيسية: لوحتا الأقسام والقنوات =====================

/**
 * الأقسام: آخر ما شاهدت، المفضلة، ثم أقسام القنوات من الخادم (بلا مكرر بالمعرّف أو بنفس العناصر).
 * أفلام ومسلسلات لا تدخل هنا إطلاقاً (تظهر في شريط الاقتراحات المنفصل).
 */
internal fun buildLiveSections(data: HomeResponse?, favs: List<Item>, recents: List<Item>): List<LiveSection> {
    val out = ArrayList<LiveSection>()
    val r = recents.filter { it.isChannel }.distinctBy { it.id }
    if (r.isNotEmpty()) out += LiveSection("~recent", S.recentChannels, r)
    val f = favs.filter { it.isChannel }.distinctBy { it.id }
    if (f.isNotEmpty()) out += LiveSection("~fav", S.favorites, f)
    val seenIds = HashSet<String>()
    val seenSets = HashSet<List<String>>()
    data?.sections?.forEach { s ->
        if (s.type != "channels") return@forEach
        val items = s.items.filter { it.isChannel }.distinctBy { it.id }
        if (items.isEmpty() || !seenIds.add(s.id) || !seenSets.add(items.map { it.id })) return@forEach
        out += LiveSection(s.id, s.title, items)
    }
    return out
}

/** اقتراحات الرئيسية: قسم suggest من الخادم، وإن لم يوجد فعناصر أقسام row (أفلام/مسلسلات فقط). */
internal fun suggestionsOf(data: HomeResponse?): List<Item> {
    val s = data?.sections ?: return emptyList()
    val direct = s.filter { it.type == "suggest" }.flatMap { it.items }.filter { !it.isChannel }
    val src = direct.ifEmpty { s.filter { it.type == "row" }.flatMap { it.items }.filter { !it.isChannel } }
    return src.distinctBy { it.id }.take(12)
}

@Composable
private fun HomePanes(d: LiveDims, vm: HomeVM, live: LiveVM, nav: NavVM, bottomPad: Dp) {
    val st by vm.home.state.collectAsStateWithLifecycle()
    val favs by live.favorites.collectAsStateWithLifecycle()
    val recents by live.recents.collectAsStateWithLifecycle()
    val cur by live.current.collectAsStateWithLifecycle()
    val key by vm.sectionKey.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    PullRefresh(st is UiState.Refreshing<*>, { vm.home.refresh() }, Modifier.fillMaxSize()) {
        StateHost(st, { vm.home.refresh() }) { data ->
            val sections = remember(data, favs, recents) { buildLiveSections(data, favs, recents) }
            if (sections.isEmpty()) {
                MessageBox(S.noChannels, S.browseChannels, { nav.tab(1) })
            } else {
                val selected = sections.firstOrNull { it.key == key } ?: sections.first()
                val items = selected.items.orEmpty()
                val favIds = remember(favs) { favs.mapTo(HashSet()) { it.id } }
                LaunchedEffect(sections.first().key) { live.autoStart(sections.first { it.items?.isNotEmpty() == true }.items.orEmpty()) }
                PanesRow(
                    sectionsW = d.sectionsW,
                    modifier = Modifier.fillMaxSize(),
                    sections = { m ->
                        SectionsPane(sections, selected.key, { k ->
                            vm.sectionKey.value = k
                            scope.launch { vm.listState.scrollToItem(0) }
                        }, m, bottomPad)
                    },
                    channels = { m ->
                        ChannelsPane(
                            items, cur?.id, live.engine.isPlaying, favIds,
                            onPick = { live.play(it, items) },
                            onFav = { live.toggleFav(it) },
                            modifier = m, state = vm.listState, scrollKey = selected.key, bottomPad = bottomPad,
                        )
                    },
                )
            }
        }
    }
}

// ===================== القنوات: أقسام من الخادم وبحث داخل اللوحة =====================

@Composable
private fun ChannelsPanes(d: LiveDims, vm: ChannelsVM, live: LiveVM, bottomPad: Dp) {
    LaunchedEffect(Unit) { vm.start() }
    val cats by vm.cats.state.collectAsStateWithLifecycle()
    val ps by vm.paged.state.collectAsStateWithLifecycle()
    val fs by vm.found.state.collectAsStateWithLifecycle()
    val sel by vm.category.collectAsStateWithLifecycle()
    val favOnly by vm.favOnly.collectAsStateWithLifecycle()
    val q by vm.query.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val cur by live.current.collectAsStateWithLifecycle()
    val favIds = remember(favs) { favs.mapTo(HashSet()) { it.id } }

    val searching = q.trim().length >= 2
    val src = if (searching) fs else ps
    val showFav = favOnly && !searching
    val favList = remember(favs) { favs.filter { it.isChannel } }
    val ui: UiState<List<Item>> =
        if (showFav) (if (favList.isEmpty()) UiState.Empty else UiState.Success(favList)) else src.ui

    LaunchedEffect(ps) { ps.ui.dataOrNull()?.let { live.autoStart(it) } }
    LaunchedEffect(searching, showFav) { vm.listState.scrollToItem(0) }

    val sections = remember(cats) {
        buildList {
            add(LiveSection("~all", S.all))
            add(LiveSection("~fav", S.favorites))
            cats.dataOrNull()?.forEach { add(LiveSection(it.id, it.name)) }
        }
    }
    val selectedKey = if (favOnly) "~fav" else sel ?: "~all"

    PanesRow(
        sectionsW = d.sectionsW,
        modifier = Modifier.fillMaxSize(),
        sections = { m ->
            SectionsPane(sections, selectedKey, { k ->
                vm.query.value = ""
                when (k) {
                    "~all" -> { vm.favOnly.value = false; vm.select(null) }
                    "~fav" -> vm.favOnly.value = true
                    else -> { vm.favOnly.value = false; vm.select(k) }
                }
            }, m, bottomPad, dim = searching)
        },
        channels = { m ->
            Column(m) {
                SearchField(q, { vm.query.value = it }, S.searchChannels, Modifier.padding(bottom = 6.dp))
                PullRefresh(src.pulling, { vm.refresh() }, Modifier.weight(1f)) {
                    if (showFav && favList.isEmpty()) MessageBox(S.noFavorites, sub = S.noFavoritesHint)
                    else StateHost(
                        ui,
                        { if (searching) vm.found.reset() else vm.paged.reset() },
                        if (searching) S.noResults else S.empty,
                    ) { list ->
                        ChannelsPane(
                            list, cur?.id, live.engine.isPlaying, favIds,
                            onPick = { live.play(it, list) },
                            onFav = { vm.toggleFav(it) },
                            modifier = Modifier.fillMaxSize(), state = vm.listState,
                            scrollKey = selectedKey, bottomPad = bottomPad,
                            loadingMore = src.loadingMore && !showFav,
                            onEnd = if (showFav) null else ({ (if (searching) vm.found else vm.paged).loadMore() }),
                        )
                    }
                }
            }
        },
    )
}
