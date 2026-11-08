package com.beouttv.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min

class TopAction(val ic: Ic, val onClick: () -> Unit, val active: Boolean = false)

/**
 * الشريط العلوي العائم. style: pill (كبسولة واحدة مثل شريط التنقل) | circles (دوائر زجاجية، المرجع الثاني)
 * | square (مربعات بزوايا مدورة، المرجع الأول). في دوائر/مربعات تتوزع الأزرار على طرفي الشريط.
 */
@Composable
fun TopBar(style: String, actions: List<TopAction>, modifier: Modifier = Modifier) {
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
        else -> Row(
            modifier.glassDense(RoundedCornerShape(50)).padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) { actions.forEach { IconAction(it.ic, it.onClick, active = it.active) } }
    }
}

private val NavItems = listOf(Ic.Home to S.home, Ic.Channels to S.channels, Ic.Movies to S.moviesShort)

/**
 * شريط التنقل العائم. style:
 * labels  — أسماء ظاهرة دائماً ومؤشر كبسولة ينزلق بنابض (أثر سائل).
 * compact — النشط كبسولة بأيقونة واسم، والبقية دوائر (المرجع الثاني).
 * icons   — أيقونات فقط داخل كبسولة زجاجية (المرجع الأول).
 */
@Composable
fun NavBar(style: String, tab: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    when (style) {
        "compact" -> Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            NavItems.forEachIndexed { i, (ic, label) ->
                val sel = tab == i
                val s = RoundedCornerShape(50)
                Row(
                    Modifier.animateContentSize(spring(dampingRatio = 0.85f, stiffness = 500f)).height(52.dp)
                        .then(if (sel) Modifier.tinted(s).background(T.c.dense.copy(alpha = 0.6f), s) else Modifier.glassDense(s))
                        .tvFocus(s).tap { onTab(i) }
                        .padding(horizontal = if (sel) 18.dp else 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BIcon(ic, size = 22.dp, tint = if (sel) T.c.accent2 else T.c.textDim, active = sel)
                    if (sel) Txt(label, style = T.small, color = T.c.accent2, maxLines = 1)
                }
            }
        }
        "icons" -> Row(modifier.glassDense(RoundedCornerShape(50)).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NavItems.forEachIndexed { i, (ic, _) ->
                val sel = tab == i
                val s = RoundedCornerShape(50)
                Column(
                    Modifier.clip(s).tvFocus(s).tap { onTab(i) }.padding(horizontal = 22.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    BIcon(ic, size = 26.dp, tint = if (sel) T.c.text else T.c.textDim, active = sel)
                    Box(Modifier.size(4.dp).background(if (sel) T.c.accent2 else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(50)))
                }
            }
        }
        else -> LabelsNav(tab, onTab, modifier)
    }
}

@Composable
private fun LabelsNav(tab: Int, onTab: (Int) -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(50)
    val itemW = 98.dp
    val itemH = 52.dp
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pos = remember { Animatable(tab.toFloat()) }
    LaunchedEffect(tab) { pos.animateTo(tab.toFloat(), spring(dampingRatio = 0.62f, stiffness = 380f)) }
    val accent = T.c.accent
    val edge = T.c.accent2.copy(alpha = 0.5f)
    Box(modifier.glassDense(shape).padding(6.dp)) {
        Box(
            Modifier.width(itemW).height(itemH)
                .graphicsLayer {
                    val p = pos.value
                    val slot = if (rtl) (NavItems.size - 1 - p) else p
                    translationX = slot * itemW.toPx()
                    scaleX = 1f + 0.22f * min(1f, abs(p - tab))
                    scaleY = 1f - 0.06f * min(1f, abs(p - tab))
                }
                .background(accent.copy(alpha = 0.3f), shape),
        )
        Row {
            NavItems.forEachIndexed { i, (ic, label) ->
                val sel = tab == i
                val s = RoundedCornerShape(50)
                Column(
                    Modifier.width(itemW).height(itemH).clip(s).tvFocus(s).tap { onTab(i) },
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
