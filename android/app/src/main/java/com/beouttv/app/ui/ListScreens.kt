package com.beouttv.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beouttv.app.core.UiState
import com.beouttv.app.core.dataOrNull
import com.beouttv.app.data.Item

private val GridGap = 10.dp
private val ChannelMinCell = 100.dp
private val PosterMinCell = 100.dp


// ===================== الأفلام والمسلسلات =====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CatalogScreen(vm: CatalogVM, nav: NavVM) {
    LaunchedEffect(Unit) { vm.start() }
    val kind by vm.kind.collectAsStateWithLifecycle()
    val sel by vm.category.collectAsStateWithLifecycle()
    val cats by vm.catsUi.collectAsStateWithLifecycle()
    val recent by vm.recentUi.collectAsStateWithLifecycle()
    val ps by vm.paged.state.collectAsStateWithLifecycle()
    val hist by vm.history.collectAsStateWithLifecycle()

    val catList = cats.dataOrNull() ?: emptyList()
    val recentList = recent.dataOrNull() ?: emptyList()
    val cont = remember(hist, kind) { inProgress(hist).filter { it.item.kind == kind } }
    val list = ps.ui.dataOrNull()
    val listTitle = sel?.let { id -> catList.firstOrNull { it.id == id }?.name }
        ?: if (kind == "movie") S.allMovies else S.allSeries

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(S.movies) { IconAction(Ic.Search, { nav.push(Route.Search(kind)) }) }
        SegmentedControl(
            listOf(S.movie, S.series), if (kind == "movie") 0 else 1,
            { vm.setKind(if (it == 0) "movie" else "series") },
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Box(Modifier.fillMaxWidth().height(2.dp)) { if (ps.ui is UiState.Refreshing<*> && !ps.pulling) ProgressLine() }

        PullRefresh(ps.pulling, { vm.refresh() }, Modifier.weight(1f)) {
            LoadMoreTrigger(vm.gridState, vm.paged)
            LazyVerticalGrid(
                (if (T.look.lay.cols > 0) GridCells.Fixed(T.look.lay.cols) else GridCells.Adaptive(PosterMinCell)), Modifier.fillMaxSize(), vm.gridState,
                PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = LocalBottomPad.current),
                verticalArrangement = Arrangement.spacedBy(GridGap), horizontalArrangement = Arrangement.spacedBy(GridGap),
            ) {
                // كاروسيل "أضيف حديثاً" مع التقاط عند التمرير
                if (recentList.isNotEmpty()) item(key = "recent", span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        SectionHeader(S.recentlyAdded, hPad = 0.dp)
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val cardW: Dp = (maxWidth * 0.8f).coerceAtMost(420.dp)
                            val st = rememberLazyListState()
                            LazyRow(
                                state = st, flingBehavior = rememberSnapFlingBehavior(st),
                                horizontalArrangement = Arrangement.spacedBy(GridGap),
                            ) {
                                items(recentList, key = { it.id }) { it: Item ->
                                    WideCard(it, { origin -> openItem(nav, it, recentList, origin) }, cardW)
                                }
                            }
                        }
                    }
                }
                if (cont.isNotEmpty()) item(key = "cont", span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        SectionHeader(S.continueWatching, hPad = 0.dp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(GridGap)) {
                            items(cont, key = { it.key }) { e ->
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
                if (catList.isNotEmpty()) item(key = "chips", span = { GridItemSpan(maxLineSpan) }) {
                    // الشرائح بدون حشوة جانبية هنا لأن الشبكة نفسها تحمل الحشوة
                    CategoryChips(catList, sel, { vm.select(it) }, Modifier.padding(vertical = 2.dp), hPad = 0.dp)
                }
                item(key = "title", span = { GridItemSpan(maxLineSpan) }) { SectionHeader(listTitle, hPad = 0.dp) }

                when {
                    list != null -> items(list, key = { it.id }) { it: Item ->
                        PosterCard(it, { origin -> openItem(nav, it, list, origin) }, Dp.Unspecified)
                    }
                    ps.ui is UiState.Empty -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().height(240.dp)) { MessageBox(S.empty) }
                    }
                    ps.ui is UiState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().height(260.dp)) { MessageBox(S.loadErr, S.retry, { vm.paged.reset() }) }
                    }
                    ps.ui is UiState.Offline -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().height(260.dp)) { MessageBox(S.offline, S.retry, { vm.paged.reset() }, sub = S.offlineHint) }
                    }
                    else -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Spinner(30.dp) }
                    }
                }
                if (ps.loadingMore) item(span = { GridItemSpan(maxLineSpan) }) { LoadingFooter() }
            }
        }
    }
}

@Composable
private fun LoadingFooter() =
    Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) { Spinner(22.dp) }
