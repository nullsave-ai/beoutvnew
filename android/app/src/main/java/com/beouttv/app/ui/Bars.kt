package com.beouttv.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min

class TopAction(val ic: Ic, val onClick: () -> Unit, val active: Boolean = false)

private fun labelOf(ic: Ic) = when (ic) { Ic.Search -> S.search; Ic.Settings -> S.settings; else -> "" }

/** نقر مع إبلاغ لحظي بالضغط/الإفلات (للتأثير السائل). */
@Composable
private fun Modifier.pressTap(onPress: (Boolean) -> Unit, onClick: () -> Unit): Modifier {
    val cb by rememberUpdatedState(onClick)
    val pr by rememberUpdatedState(onPress)
    return pointerInput(Unit) {
        detectTapGestures(
            onPress = { pr(true); tryAwaitRelease(); pr(false) },
            onTap = { cb() },
        )
    }
}

/**
 * الشريط العلوي العائم. الأنماط:
 * pill كبسولة | circles دوائر | square مربعات | bar شريط بعرض كامل | ghost أيقونات بلا حاوية
 * | island جزيرة بحقل بحث | stack عمود جانبي | chips أزرار بأسماء.
 */
@Composable
fun TopBar(style: String, actions: List<TopAction>, modifier: Modifier = Modifier) {
    val ap = T.look.ap
    val sz = (ap["tbSize"]?.toFloatOrNull() ?: 100f) / 100f
    val al = (ap["tbAlpha"]?.toFloatOrNull() ?: 100f) / 100f
    val wide = style != "pill"
    val k = if (wide) min(sz, 1f) else sz
    TopBarImpl(style, actions, if (k == 1f && al == 1f) modifier else modifier.graphicsLayer { scaleX = k; scaleY = k; alpha = al; transformOrigin = TransformOrigin(0.5f, 0f) })
}

@Composable
private fun TopBarImpl(style: String, actions: List<TopAction>, modifier: Modifier) {
    when (style) {
        "circles" -> Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            actions.forEach { GlassIconButton(it.ic, it.onClick, size = 44.dp, active = it.active) }
        }
        "square" -> Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            actions.forEach { a ->
                val shape = RoundedCornerShape(14.dp)
                Box(Modifier.size(44.dp).glass(shape).tvFocus(shape).tap(a.onClick), contentAlignment = Alignment.Center) {
                    BIcon(a.ic, size = 22.dp, tint = if (a.active) T.c.accent2 else T.c.text, active = a.active)
                }
            }
        }
        "bar" -> Row(
            modifier.padding(horizontal = 12.dp).fillMaxWidth().glassDense(RoundedCornerShape(18.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically,
        ) { actions.forEach { IconAction(it.ic, it.onClick, active = it.active) } }
        "ghost" -> Row(modifier.fillMaxWidth().padding(horizontal = 14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            actions.forEach { a ->
                val shape = RoundedCornerShape(50)
                Box(Modifier.size(42.dp).clip(shape).background(Color.Black.copy(alpha = 0.28f)).tvFocus(shape).tap(a.onClick), contentAlignment = Alignment.Center) {
                    BIcon(a.ic, size = 22.dp, tint = Color.White, active = a.active)
                }
            }
        }
        "island" -> {
            val shape = RoundedCornerShape(50)
            Row(
                modifier.padding(horizontal = 14.dp).fillMaxWidth().glassDense(shape).padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions.forEachIndexed { i, a ->
                    if (i == 0) {
                        Row(
                            Modifier.weight(1f).height(40.dp).clip(shape).tvFocus(shape).tap(a.onClick).padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BIcon(a.ic, size = 20.dp, tint = T.c.textDim)
                            Txt(labelOf(a.ic), style = T.body, color = T.c.textDim, maxLines = 1)
                        }
                    } else IconAction(a.ic, a.onClick, active = a.active)
                }
            }
        }
        "stack" -> Row(modifier.fillMaxWidth().padding(horizontal = 14.dp), Arrangement.End) {
            Column(Modifier.glassDense(RoundedCornerShape(26.dp)).padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                actions.forEach { IconAction(it.ic, it.onClick, active = it.active) }
            }
        }
        "chips" -> Row(modifier.fillMaxWidth().padding(horizontal = 14.dp), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
            actions.forEach { a ->
                val shape = RoundedCornerShape(50)
                Row(
                    Modifier.height(38.dp).glassDense(shape).tvFocus(shape).tap(a.onClick).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BIcon(a.ic, size = 18.dp, tint = if (a.active) T.c.accent2 else T.c.text, active = a.active)
                    Txt(labelOf(a.ic), style = T.small, color = T.c.text, maxLines = 1)
                }
            }
        }
        else -> Row(
            modifier.glassDense(RoundedCornerShape(50)).padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) { actions.forEach { IconAction(it.ic, it.onClick, active = it.active) } }
    }
}

private val NavItems = listOf(Ic.Home to S.home, Ic.Channels to S.channels, Ic.Movies to S.moviesShort)

/**
 * شريط التنقل العائم. الأنماط:
 * labels أسماء دائمة + مؤشر سائل يتفاعل مع الضغط | compact النشط كبسولة بالاسم | icons أيقونات فقط
 * | dock لوح بعرض كامل بخط سفلي | ios شريط تبويب كلاسيكي | round دوائر منفصلة | text أسماء فقط | bubble فقاعة بارزة.
 * لا يوجد أي تأثير حجم/حركة عند الضغط إلا في labels (السائل) بطلب المستخدم.
 */
@Composable
fun NavBar(style: String, tab: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    val ap = T.look.ap
    val sz = (ap["navSize"]?.toFloatOrNull() ?: 100f) / 100f
    val al = (ap["navAlpha"]?.toFloatOrNull() ?: 100f) / 100f
    val k = if (style == "ios" || style == "dock") 1f else sz
    NavBarImpl(style, tab, onTab, if (k == 1f && al == 1f) modifier else modifier.graphicsLayer { scaleX = k; scaleY = k; alpha = al; transformOrigin = TransformOrigin(0.5f, 1f) })
}

@Composable
private fun NavBarImpl(style: String, tab: Int, onTab: (Int) -> Unit, modifier: Modifier) {
    val pill = RoundedCornerShape(50)
    when (style) {
        // زجاجي (المراجع البصرية): كبسولة زجاجية، الأيقونات غير النشطة دوائر زجاجية والنشطة كبسولة فاتحة بالاسم
        "aero" -> {
            val dark = T.c.dark
            val actBg = if (dark) Color(0xFFF3F5F9) else Color(0xFF182033)
            val actFg = if (dark) Color(0xFF141824) else Color(0xFFF3F5F9)
            Row(modifier.glassDense(pill).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                NavItems.forEachIndexed { i, (ic, label) ->
                    val sel = tab == i
                    if (sel) Row(
                        Modifier.height(48.dp).clip(pill).background(actBg, pill).tvFocus(pill).tap { onTab(i) }.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BIcon(ic, size = 22.dp, tint = actFg, active = true)
                        Txt(label, style = T.small, color = actFg, maxLines = 1)
                    } else Box(
                        Modifier.size(48.dp).clip(pill).background(T.c.text.copy(alpha = 0.08f), pill).tvFocus(pill).tap { onTab(i) },
                        contentAlignment = Alignment.Center,
                    ) { BIcon(ic, size = 22.dp, tint = T.c.textDim, active = false) }
                }
            }
        }
        "compact" -> Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            NavItems.forEachIndexed { i, (ic, label) ->
                val sel = tab == i
                Row(
                    Modifier.height(52.dp)
                        .then(if (sel) Modifier.tinted(pill).background(T.c.dense.copy(alpha = 0.6f), pill) else Modifier.glassDense(pill))
                        .tvFocus(pill).tap { onTab(i) }
                        .padding(horizontal = if (sel) 18.dp else 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BIcon(ic, size = 22.dp, tint = if (sel) T.c.accent2 else T.c.textDim, active = sel)
                    if (sel) Txt(label, style = T.small, color = T.c.accent2, maxLines = 1)
                }
            }
        }
        "icons" -> Row(modifier.glassDense(pill).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NavItems.forEachIndexed { i, (ic, _) ->
                val sel = tab == i
                Column(
                    Modifier.clip(pill).tvFocus(pill).tap { onTab(i) }.padding(horizontal = 22.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    BIcon(ic, size = 26.dp, tint = if (sel) T.c.text else T.c.textDim, active = sel)
                    Box(Modifier.size(4.dp).background(if (sel) T.c.accent2 else Color.Transparent, pill))
                }
            }
        }
        "dock" -> {
            val shape = RoundedCornerShape(22.dp)
            Row(modifier.padding(horizontal = 14.dp).fillMaxWidth().glassDense(shape).padding(horizontal = 4.dp, vertical = 4.dp)) {
                NavItems.forEachIndexed { i, (ic, label) ->
                    val sel = tab == i
                    val s = RoundedCornerShape(18.dp)
                    Column(
                        Modifier.weight(1f).height(54.dp).clip(s).tvFocus(s).tap { onTab(i) },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        BIcon(ic, size = 22.dp, tint = if (sel) T.c.accent2 else T.c.textDim, active = sel)
                        Txt(label, style = T.small, color = if (sel) T.c.accent2 else T.c.textDim, maxLines = 1)
                        Box(Modifier.padding(top = 2.dp).width(18.dp).height(3.dp).background(if (sel) T.c.accent2 else Color.Transparent, pill))
                    }
                }
            }
        }
        "ios" -> Column(modifier.fillMaxWidth().background(T.c.dense.copy(alpha = 0.94f))) {
            Box(Modifier.fillMaxWidth().height(0.6.dp).background(T.c.edgeLo))
            Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp)) {
                NavItems.forEachIndexed { i, (ic, label) ->
                    val sel = tab == i
                    val s = RoundedCornerShape(10.dp)
                    Column(
                        Modifier.weight(1f).clip(s).tvFocus(s).tap { onTab(i) }.padding(vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        BIcon(ic, size = 24.dp, tint = if (sel) T.c.accent else T.c.textDim, active = sel)
                        Txt(label, style = T.small, color = if (sel) T.c.accent else T.c.textDim, maxLines = 1)
                    }
                }
            }
        }
        "round" -> Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            NavItems.forEachIndexed { i, (ic, _) ->
                val sel = tab == i
                Box(
                    Modifier.size(54.dp)
                        .then(if (sel) Modifier.accentFill(pill) else Modifier.glassDense(pill))
                        .tvFocus(pill).tap { onTab(i) },
                    contentAlignment = Alignment.Center,
                ) { BIcon(ic, size = 24.dp, tint = if (sel) Color.White else T.c.textDim, active = sel) }
            }
        }
        "text" -> Row(modifier.glassDense(pill).padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            NavItems.forEachIndexed { i, (_, label) ->
                val sel = tab == i
                Column(
                    Modifier.clip(pill).tvFocus(pill).tap { onTab(i) }.padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Txt(label, style = T.h2, color = if (sel) T.c.text else T.c.textDim, maxLines = 1)
                    Box(Modifier.width(16.dp).height(3.dp).background(if (sel) T.c.accent else Color.Transparent, pill))
                }
            }
        }
        "bubble" -> Row(modifier.glassDense(pill).padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            NavItems.forEachIndexed { i, (ic, label) ->
                val sel = tab == i
                Row(
                    Modifier.height(46.dp).clip(pill).then(if (sel) Modifier.accentFill(pill) else Modifier)
                        .tvFocus(pill).tap { onTab(i) }.padding(horizontal = if (sel) 18.dp else 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    BIcon(ic, size = 22.dp, tint = if (sel) Color.White else T.c.textDim, active = sel)
                    if (sel) Txt(label, style = T.small, color = Color.White, maxLines = 1)
                }
            }
        }
        else -> LabelsNav(tab, onTab, modifier)
    }
}

/** أسماء دائمة: المؤشر السائل ينزلق إلى الزر لحظة الضغط (قبل تغيّر الصفحة) ويتمدد/ينضغط أثناء الحركة. */
@Composable
private fun LabelsNav(tab: Int, onTab: (Int) -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(50)
    val itemW = 98.dp
    val itemH = 52.dp
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var pressed by remember { mutableIntStateOf(-1) }
    val target = if (pressed >= 0) pressed else tab
    val pos = remember { Animatable(tab.toFloat()) }
    LaunchedEffect(target) { pos.animateTo(target.toFloat(), spring(dampingRatio = 0.62f, stiffness = 380f)) }
    val squish by animateFloatAsState(if (pressed >= 0) 1f else 0f, tween(120), label = "sq")
    val accent = T.c.accent
    Box(modifier.glassDense(shape).padding(6.dp)) {
        Box(
            Modifier.width(itemW).height(itemH)
                .graphicsLayer {
                    val p = pos.value
                    val slot = if (rtl) (NavItems.size - 1 - p) else p
                    val move = min(1f, abs(p - target))
                    translationX = slot * itemW.toPx()
                    scaleX = 1f + 0.22f * move + 0.08f * squish
                    scaleY = 1f - 0.06f * move - 0.05f * squish
                }
                .background(accent.copy(alpha = 0.3f + 0.08f * squish), shape),
        )
        Row {
            NavItems.forEachIndexed { i, (ic, label) ->
                val sel = target == i
                Column(
                    Modifier.width(itemW).height(itemH).clip(shape).tvFocus(shape)
                        .pressTap({ pressed = if (it) i else if (pressed == i) -1 else pressed }) { onTab(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    BIcon(ic, size = 22.dp, tint = if (sel) T.c.accent2 else T.c.textDim, active = sel)
                    Txt(label, style = T.small, color = if (sel) T.c.accent2 else T.c.textDim, maxLines = 1)
                }
            }
        }
    }
}
