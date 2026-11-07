package com.beouttv.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
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
import com.beouttv.app.data.Item

/** البحث: type يحدد النوع المبدئي (أفلام/مسلسلات من صفحة الأفلام، أو الكل من الرئيسية). */
@Composable
fun SearchScreen(vm: SearchVM, nav: NavVM, live: LiveVM, type: String?) {
    val q by vm.query.collectAsStateWithLifecycle()
    val curType by vm.type.collectAsStateWithLifecycle()
    val ps by vm.paged.state.collectAsStateWithLifecycle()
    val sug by vm.suggestions.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val favIds = remember(favs) { favs.mapTo(HashSet()) { it.id } }
    val grid = rememberLazyGridState()
    val focus = remember { FocusRequester() }

    // كل فتح للبحث يبدأ نظيفاً بالنوع المطلوب
    LaunchedEffect(Unit) {
        vm.query.value = ""
        vm.type.value = type
        runCatching { focus.requestFocus() }
    }

    Column(
        Modifier.fillMaxSize().screenBackground().statusBarsPadding().navigationBarsPadding().imePadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.padding(top = 8.dp, start = 12.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassIconButton(Ic.Back, { nav.pop() })
            SearchField(q, { vm.query.value = it }, S.searchHint, Modifier.weight(1f), onSearch = { vm.commit() }, focus = focus)
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Chip(S.all, curType == null, { vm.type.value = null }) }
            item { Chip(S.movie, curType == "movie", { vm.type.value = "movie" }) }
            item { Chip(S.series, curType == "series", { vm.type.value = "series" }) }
            item { Chip(S.episode, curType == "episode", { vm.type.value = "episode" }) }
            item { Chip(S.channel, curType == "channel", { vm.type.value = "channel" }) }
        }
        if (q.trim().length < 2) {
            if (recent.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Txt(S.searchHint, style = T.body, color = T.c.textDim)
                }
            } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "recent-h") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Txt(S.recent, style = T.h2)
                        Txt(S.clear, Modifier.tap { vm.clearRecent() }.padding(8.dp), T.small, T.c.accent2)
                    }
                }
                items(recent, key = { it }) { r ->
                    Row(
                        Modifier.fillMaxWidth().glass(CardShape).tvFocus(CardShape).tap { vm.query.value = r }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BIcon(Ic.Search, size = 18.dp, tint = T.c.textDim)
                        Txt(r, style = T.body, maxLines = 1)
                    }
                }
            }
        } else {
            if (sug.isNotEmpty() && ps.ui !is UiState.Success<*>) {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sug, key = { it }) { s -> Chip(s, false, { vm.query.value = s; vm.commit() }) }
                }
            }
            StateHost(ps.ui, { vm.paged.reset() }, S.noResults) { list ->
                LoadMoreTrigger(grid, vm.paged)
                LazyVerticalGrid(
                    GridCells.Adaptive(100.dp), Modifier.fillMaxSize(), grid,
                    PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(list, key = { it.id }) { item: Item ->
                        if (item.isChannel) ChannelCard(
                            item, item.id in favIds,
                            {
                                vm.commit()
                                live.play(item, list)
                                if (nav.tab.value == 2) nav.tab(0)
                                nav.pop()
                            },
                            { vm.toggleFav(item) },
                        )
                        else PosterCard(item, { origin -> vm.commit(); openItem(nav, item, list, origin) }, Dp.Unspecified)
                    }
                    if (ps.loadingMore) item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { Spinner(22.dp) }
                    }
                }
            }
        }
    }
}
