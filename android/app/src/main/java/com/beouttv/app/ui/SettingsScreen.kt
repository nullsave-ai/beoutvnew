package com.beouttv.app.ui

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beouttv.app.BuildConfig
import com.beouttv.app.data.Img
import com.beouttv.app.notify.Reminders

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Txt(title, Modifier.padding(horizontal = 4.dp), T.small, T.c.accent2)
        Column(Modifier.fillMaxWidth().flat(GlassShape).padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
    }
}

@Composable
private fun Toggle(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val x by animateDpAsState(if (on) 20.dp else 0.dp, spring(dampingRatio = 0.85f, stiffness = 600f), label = "knob")
    Row(Modifier.fillMaxWidth().tvFocus().tap { onChange(!on) }, verticalAlignment = Alignment.CenterVertically) {
        Txt(label, Modifier.weight(1f), T.body)
        val shape = RoundedCornerShape(50)
        Box(Modifier.size(46.dp, 26.dp).then(if (on) Modifier.accentFill(shape) else Modifier.glass(shape)).padding(3.dp)) {
            Box(Modifier.align(Alignment.CenterStart).offset(x = x).size(20.dp).clip(shape).background(Color.White))
        }
    }
}

@Composable
private fun Labeled(label: String, hint: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Txt(label, style = T.body)
        content()
        hint?.let { Txt(it, style = T.small, color = T.c.textDim) }
    }
}

@Composable
private fun ChipsRow(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) =
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)

@Composable
private fun Opts(vm: AppVM, key: String, cur: String, list: List<Pair<String, String>>) =
    ChipsRow { list.forEach { (k, l) -> item { Chip(l, cur == k, { vm.setStr(key, k) }) } } }

@Composable
fun SettingsScreen(vm: AppVM, nav: NavVM) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var cacheMb by remember { mutableStateOf(Img.sizeBytes() / 1_048_576f) }
    var askCache by remember { mutableStateOf(false) }
    var askHistory by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().screenBackground()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassIconButton(Ic.Back, { nav.pop() })
                Txt(S.settings, style = T.title)
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item(key = "appearance") {
                    Group(S.appearance) {
                        Labeled(S.appearance) {
                            ChipsRow {
                                item { Chip(S.light, s.theme == "light", { vm.setTheme("light") }) }
                                item { Chip(S.dark, s.theme == "dark", { vm.setTheme("dark") }) }
                                item { Chip(S.blue, s.theme == "blue", { vm.setTheme("blue") }) }
                                item { Chip(S.slate, s.theme == "slate", { vm.setTheme("slate") }) }
                                item { Chip(S.violet, s.theme == "violet", { vm.setTheme("violet") }) }
                            }
                        }
                        Labeled(S.displayMode, S.displayModeHint) {
                            val modes = listOf("auto", "phone", "tv")
                            SegmentedControl(
                                listOf(S.modeAuto, S.modePhone, S.modeTv),
                                modes.indexOf(s.uiMode).coerceAtLeast(0),
                                { vm.setUiMode(modes[it]) },
                            )
                        }
                    }
                }
                item(key = "custom") {
                    Group(S.customize) {
                        Labeled(S.navStyle) { Opts(vm, "navStyle", s.navStyle, listOf("labels" to S.navLabels, "compact" to S.navCompact, "icons" to S.navIcons)) }
                        Labeled(S.toolbarStyle) { Opts(vm, "toolbarStyle", s.toolbarStyle, listOf("pill" to S.tbPill, "circles" to S.tbCircles, "square" to S.tbSquare)) }
                        Labeled(S.homeLayout) { Opts(vm, "homeLayout", s.homeLayout, listOf("hero" to S.hlHero, "rows" to S.hlRows)) }
                        val sml = listOf("s" to S.sSmall, "m" to S.sMed, "l" to S.sLarge)
                        Labeled(S.accentLbl) { Opts(vm, "accent", s.accent, listOf("theme" to S.accTheme, "blue" to S.blue, "violet" to S.violet, "teal" to S.accTeal, "rose" to S.accRose, "amber" to S.accAmber, "green" to S.accGreen)) }
                        Labeled(S.radiusLbl) { Opts(vm, "radius", s.radius, sml) }
                        Labeled(S.glassLbl) { Opts(vm, "glass", s.glass, sml) }
                        Labeled(S.iconLbl) { Opts(vm, "iconStyle", s.iconStyle, listOf("line" to S.iconLine, "duo" to S.iconDuo, "solid" to S.iconSolid)) }
                        Labeled(S.fontLbl) { Opts(vm, "fontScale", s.fontScale, sml) }
                        Labeled(S.posterLbl) { Opts(vm, "posterSize", s.posterSize, sml) }
                        Labeled(S.chLayout) { Opts(vm, "chLayout", s.chLayout, listOf("auto" to S.chAuto, "stacked" to S.chStacked, "split" to S.chSplit)) }
                    }
                }
                item(key = "player") {
                    Group(S.playerSettings) {
                        Labeled(S.videoDisplay) {
                            val fits = listOf("fit" to S.fitFit, "fill" to S.fitFill, "zoom" to S.fitZoom, "stretch" to S.fitStretch, "auto" to S.fitAuto)
                            ChipsRow { fits.forEach { (k, l) -> item { Chip(l, s.videoFit == k, { vm.setVideoFit(k) }) } } }
                        }
                        Labeled(S.videoQuality) {
                            ChipsRow {
                                listOf(0 to S.auto, 480 to "480p", 720 to "720p", 1080 to "1080p").forEach { (h, l) ->
                                    item { Chip(l, s.maxHeight == h, { vm.setMaxHeight(h) }) }
                                }
                            }
                        }
                        Toggle(S.resumePlayback, s.resume) { vm.setResume(it) }
                        Toggle(S.keepScreen, s.keepScreenOn) { vm.setKeepScreen(it) }
                        Toggle(S.longPressBoost, s.longPressBoost) { vm.setLongPressBoost(it) }
                        Toggle(S.autoPlayLive, s.autoPlay) { vm.setAutoPlay(it) }
                        Toggle(S.autoFullscreen, s.autoFullscreen) { vm.setAutoFullscreen(it) }
                    }
                }
                item(key = "notif") {
                    Group(S.notifications) {
                        Toggle(S.notifications, s.notifications) { on -> vm.setNotifications(on) { Reminders.apply(ctx, it) } }
                    }
                }
                item(key = "storage") {
                    Group(S.cache) {
                        Txt("%.1f MB".format(cacheMb), style = T.body, color = T.c.textDim)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip(S.clearCache, false, { askCache = true })
                            Chip(S.clearHistory, false, { askHistory = true })
                        }
                    }
                }
                item(key = "about") {
                    Group(S.about) {
                        Row(Modifier.fillMaxWidth()) {
                            Txt(S.version, Modifier.weight(1f), T.body)
                            Txt(BuildConfig.VERSION_NAME, style = T.body, color = T.c.textDim)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip(S.privacy, false, { showPrivacy = true })
                            Chip(S.terms, false, { showTerms = true })
                        }
                    }
                }
            }
        }

        ConfirmDialog(askCache, S.clearCache, S.clearCacheConfirm, S.delete, {
            vm.clearCache(); cacheMb = 0f; askCache = false
        }, { askCache = false })
        ConfirmDialog(askHistory, S.clearHistory, S.clearHistoryConfirm, S.delete, {
            vm.clearHistory(); askHistory = false
        }, { askHistory = false })
        GlassSheet(showPrivacy, { showPrivacy = false }) {
            Txt(S.privacy, style = T.h2)
            Txt(S.privacyText, style = T.body, color = T.c.textDim)
        }
        GlassSheet(showTerms, { showTerms = false }) {
            Txt(S.terms, style = T.h2)
            Txt(S.termsText, style = T.body, color = T.c.textDim)
        }
    }
}
