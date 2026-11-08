package com.beouttv.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.beouttv.app.core.PagedLoader
import com.beouttv.app.data.Category
import com.beouttv.app.data.Item
import com.beouttv.app.data.RemoteImage

/** ترويسة صفحة مثبتة: العنوان في بداية السطر والإجراءات في نهايته، وتحترم شريط الحالة. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    brand: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(title, Modifier.weight(1f), T.title, if (brand) T.c.accent2 else T.c.text, maxLines = 1)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, hPad: Dp = 16.dp) =
    Txt(text, modifier.padding(horizontal = hPad, vertical = 8.dp), T.h2, maxLines = 1)

/** حقل بحث زجاجي: أيقونة، نص، وزر مسح يظهر عند وجود نص. */
@Composable
fun SearchField(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
    focus: FocusRequester? = null,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier.fillMaxWidth().glass(shape).padding(start = 14.dp, end = 4.dp).heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BIcon(Ic.Search, size = 18.dp, tint = T.c.textDim)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value, onChange,
                Modifier.fillMaxWidth().then(if (focus != null) Modifier.focusRequester(focus) else Modifier),
                singleLine = true,
                textStyle = T.body.copy(color = T.c.text, textDirection = androidx.compose.ui.text.style.TextDirection.Content),
                cursorBrush = SolidColor(T.c.accent2),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            )
            if (value.isEmpty()) Txt(hint, style = T.body, color = T.c.textDim, maxLines = 1)
        }
        if (value.isNotEmpty()) IconAction(Ic.Close, { onChange("") }, size = 36.dp, tint = T.c.textDim)
        else Box(Modifier.width(10.dp))
    }
}

/** محفّز التحميل التدريجي عند الاقتراب من نهاية الشبكة. */
@Composable
fun LoadMoreTrigger(state: LazyGridState, paged: PagedLoader) {
    val near by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 8
        }
    }
    LaunchedEffect(near) { if (near) paged.loadMore() }
}

/** صف تصنيفات: "الكل" ثم التصنيفات. extra يُعرض قبلها (مثل زر المفضلة). */
@Composable
fun CategoryChips(
    cats: List<Category>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    hPad: Dp = 16.dp,
    extra: (@Composable () -> Unit)? = null,
) {
    LazyRow(modifier, contentPadding = PaddingValues(horizontal = hPad), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (extra != null) item(key = "extra") { extra() }
        item(key = "all") { Chip(S.all, selected == null, { onSelect(null) }) }
        items(cats, key = { it.id }) { Chip(it.name, selected == it.id, { onSelect(it.id) }) }
    }
}

/** تحكم مقطعي: المؤشر ينزلق بنابض (يتبع اتجاه الواجهة). */
@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    val pos by animateFloatAsState(selected.toFloat(), spring(dampingRatio = 0.9f, stiffness = 500f), label = "seg")
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val a1 = T.c.accent
    val a2 = T.c.accentDeep
    Row(
        modifier.glass(shape).drawBehind {
            val w = size.width / options.size
            val x = if (rtl) size.width - (pos + 1f) * w else pos * w
            val inset = 3.dp.toPx()
            drawRoundRect(
                Brush.verticalGradient(listOf(a1, a2)),
                Offset(x + inset, inset), Size(w - 2 * inset, size.height - 2 * inset),
                CornerRadius(size.height),
            )
        },
    ) {
        options.forEachIndexed { i, label ->
            Box(
                Modifier.weight(1f).tvFocus(shape).tap { onSelect(i) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Txt(label, style = T.small, color = if (i == selected) Color.White else T.c.textDim, maxLines = 1) }
        }
    }
}

/** بطاقة عريضة (غلاف أفقي) للكاروسيل. */
@Composable
fun WideCard(item: Item, onClick: (Rect?) -> Unit, width: Dp) {
    val ref = remember { CoordRef() }
    Box(
        Modifier.width(width).aspectRatio(1.7f).trackBounds(ref)
            .flat(T.card).tvFocus(T.card).tap { onClick(ref.bounds()) },
    ) {
        RemoteImage(item.backdrop ?: item.poster, Modifier.fillMaxSize(), reqWidthPx = 640) {}
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB3000000)))))
        Txt(item.title, Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 10.dp), T.h2, Color.White, maxLines = 2)
        item.quality?.let {
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp).glassDense(RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 1.dp)) {
                Txt(it, style = T.small)
            }
        }
    }
}

/** شريط تقدم رفيع. */
@Composable
fun ThinProgress(frac: Float, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(modifier.fillMaxWidth().height(3.dp).background(Color(0x33FFFFFF), shape)) {
        Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).height(3.dp).background(T.c.accent2, shape))
    }
}
