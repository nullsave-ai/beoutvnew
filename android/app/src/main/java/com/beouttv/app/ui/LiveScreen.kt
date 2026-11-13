package com.beouttv.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beouttv.app.core.UiState
import com.beouttv.app.core.dataOrNull
import com.beouttv.app.data.Item
import com.beouttv.app.data.Settings

/**
 * الصفحة المباشرة الوحيدة (الرئيسية): شريط أدوات عائم، فتحة المشغل، شريط "يُعرض الآن"،
 * ثم لوحتا القنوات (يمين) والأقسام (يسار). المشغل نفسه يُرسم في PlayerLayer ويتبع مستطيل الفتحة.
 */
@Composable
fun LiveScaffold(
    split: Boolean,
    nav: NavVM,
    channels: ChannelsVM,
    live: LiveVM,
    slot: MutableState<Rect>,
    settings: Settings,
    modifier: Modifier = Modifier,
) {
    val bottomPad = LocalBottomPad.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cur by live.current.collectAsStateWithLifecycle()
    val q by channels.query.collectAsStateWithLifecycle()
    var searchOpen by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val usableH = maxHeight - statusTop - bottomPad
        val d = liveDims(maxWidth, usableH, wantSuggest = false, split = split)

        val player: @Composable () -> Unit = {
            PlayerSlot(d.playerW, d.playerH, slot)
            NowPlayingBar(item = cur, engine = live.engine)
        }
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(statusTop + Dm.Header))
            if (!d.split) {
                player()
                ChannelsPanes(d, channels, live, bottomPad, Modifier.weight(1f).fillMaxWidth())
            } else {
                Row(Modifier.weight(1f).fillMaxSize()) {
                    Column(Modifier.width(d.colW).fillMaxHeight()) { player() }
                    ChannelsPanes(d, channels, live, bottomPad, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
        // شريط الأدوات العائم: نفس مادة شريط التنقل، يتمدد إلى حقل بحث ثم ينكمش
        val tbMod = Modifier.align(Alignment.TopCenter).padding(top = statusTop + 4.dp)
        if (searchOpen) SearchBarOpen(q, { channels.query.value = it }, { searchOpen = false; channels.query.value = "" }, tbMod)
        else TopBar(
            settings.toolbarStyle,
            listOf(TopAction(Ic.Search, { searchOpen = true }), TopAction(Ic.Settings, { nav.push(Route.Settings) })),
            tbMod,
        )
    }
}

/** حالة البحث المفتوح: الشريط يتمدد إلى حقل بحث بنابض. */
@Composable
private fun SearchBarOpen(query: String, onQuery: (String) -> Unit, onClose: () -> Unit, modifier: Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Row(
        modifier.padding(horizontal = 12.dp).fillMaxWidth()
            .glassDense(RoundedCornerShape(50)).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchField(query, onQuery, S.searchChannels, Modifier.weight(1f), focus = focus)
        IconAction(Ic.Close, onClose, active = true)
    }
}

/**
 * فتحة المشغل: مستطيل مدوّر بهامش من الجانبين (لا يلتصق بالحواف). المستطيل المُبلَغ عنه هو
 * ما يتبعه المشغل، فالزوايا المدورة تُطبَّق على المشغل نفسه في PlayerLayer.
 */
@Composable
private fun PlayerSlot(w: Dp, h: Dp, slot: MutableState<Rect>) {
    val m = 12.dp
    val iw = (w - m * 2).coerceAtLeast(1.dp)
    val ih = minOf(h, iw * 9f / 16f)
    Box(Modifier.width(w).height(ih + 8.dp).padding(horizontal = m, vertical = 4.dp)) {
        Box(
            Modifier.fillMaxSize().clip(RoundedCornerShape((16 * T.look.radius).dp)).background(Color.Black).onGloballyPositioned {
                val r = it.boundsInRoot()
                if (r != slot.value) slot.value = r
            },
        )
    }
}

// ===================== القنوات: أقسام من الخادم وبحث =====================

@Composable
private fun ChannelsPanes(d: LiveDims, vm: ChannelsVM, live: LiveVM, bottomPad: Dp, modifier: Modifier) {
    LaunchedEffect(Unit) { vm.start() }
    val cats by vm.cats.state.collectAsStateWithLifecycle()
    val ps by vm.paged.state.collectAsStateWithLifecycle()
    val fs by vm.found.state.collectAsStateWithLifecycle()
    val sel by vm.category.collectAsStateWithLifecycle()
    val favOnly by vm.favOnly.collectAsStateWithLifecycle()
    val q by vm.query.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val recents by live.recents.collectAsStateWithLifecycle()
    val cur by live.current.collectAsStateWithLifecycle()
    val favIds = remember(favs) { favs.mapTo(HashSet()) { it.id } }
    var recentSel by rememberSaveable { mutableStateOf(false) }

    val searching = q.trim().length >= 2
    val src = if (searching) fs else ps
    val showRecent = recentSel && !searching
    val showFav = favOnly && !searching && !showRecent
    val favList = remember(favs) { favs.filter { it.isChannel } }
    val recentList = remember(recents) { recents.filter { it.isChannel } }
    val ui: UiState<List<Item>> = when {
        showRecent -> if (recentList.isEmpty()) UiState.Empty else UiState.Success(recentList)
        showFav -> if (favList.isEmpty()) UiState.Empty else UiState.Success(favList)
        else -> src.ui
    }
    val local = showRecent || showFav

    LaunchedEffect(ps) { ps.ui.dataOrNull()?.let { live.autoStart(it) } }
    LaunchedEffect(searching, showFav, showRecent) { vm.listState.scrollToItem(0) }

    val sections = remember(cats, recentList.isEmpty()) {
        buildList {
            add(LiveSection("~all", S.all))
            if (recentList.isNotEmpty()) add(LiveSection("~recent", S.recentChannels))
            add(LiveSection("~fav", S.favorites))
            cats.dataOrNull()?.forEach { add(LiveSection(it.id, it.name)) }
        }
    }
    val selectedKey = when {
        recentSel -> "~recent"
        favOnly -> "~fav"
        else -> sel ?: "~all"
    }

    PanesRow(
        sectionsW = d.sectionsW,
        modifier = modifier,
        sections = { m ->
            SectionsPane(sections, selectedKey, { k ->
                vm.query.value = ""
                recentSel = k == "~recent"
                when (k) {
                    "~all" -> { vm.favOnly.value = false; vm.select(null) }
                    "~recent" -> vm.favOnly.value = false
                    "~fav" -> vm.favOnly.value = true
                    else -> { vm.favOnly.value = false; vm.select(k) }
                }
            }, m, bottomPad, dim = searching)
        },
        channels = { m ->
            PullRefresh(src.pulling, { vm.refresh() }, m) {
                if (showFav && favList.isEmpty()) MessageBox(S.noFavorites, sub = S.noFavoritesHint, ic = Ic.Heart)
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
                        loadingMore = src.loadingMore && !local,
                        onEnd = if (local) null else ({ (if (searching) vm.found else vm.paged).loadMore() }),
                    )
                }
            }
        },
    )
}
