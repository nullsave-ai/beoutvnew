package com.beouttv.app.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import com.beouttv.app.data.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Immutable
data class PlayEntry(val item: Item, val episodeId: String? = null, val label: String? = null) {
    val key get() = episodeId ?: item.id
    val kind get() = if (episodeId != null) "episode" else item.kind
}

@Immutable
data class PlayReq(val entries: List<PlayEntry>, val index: Int = 0)

sealed interface Route {
    data class Detail(val id: String, val seed: Item? = null) : Route
    data class Search(val type: String? = null) : Route
    data object Settings : Route
    data class Player(val req: PlayReq) : Route
}

/**
 * عنصر في مكدس التنقل. closing=true يعني أن شاشة الخروج تعمل حركتها العكسية،
 * ولا يُزال العنصر فعلياً إلا بعد انتهائها (يعطي انتقالاً متصلاً بدل الاختفاء المفاجئ).
 * origin: حدود البطاقة المضغوطة (إحداثيات النافذة) لانتقال الغلاف من البطاقة إلى التفاصيل.
 */
@Immutable
data class NavEntry(val id: Long, val route: Route, val origin: Rect? = null, val closing: Boolean = false)

/** تنقل مخصص: 3 أقسام رئيسية + مكدس صفحات. يعيش في ViewModel فيبقى عبر تغيير الاتجاه. */
class NavVM : ViewModel() {
    private val _tab = MutableStateFlow(0)
    val tab: StateFlow<Int> = _tab
    private val _stack = MutableStateFlow<List<NavEntry>>(emptyList())
    val stack: StateFlow<List<NavEntry>> = _stack
    private var seq = 0L

    fun tab(i: Int) { _tab.value = i }

    fun push(r: Route, origin: Rect? = null) {
        // نقرتان سريعتان على البطاقة نفسها لا تفتحان صفحتين
        if (_stack.value.lastOrNull { !it.closing }?.route == r) return
        _stack.value = _stack.value + NavEntry(++seq, r, origin)
    }

    /** يبدأ إغلاق أعلى عنصر غير مغلق. يرجع false إن لم يوجد شيء لإغلاقه. */
    fun pop(): Boolean {
        val s = _stack.value
        val i = s.indexOfLast { !it.closing }
        if (i < 0) return false
        _stack.value = s.toMutableList().also { it[i] = it[i].copy(closing = true) }
        return true
    }

    fun remove(id: Long) { _stack.value = _stack.value.filter { it.id != id } }

    val hasOpen: Boolean get() = _stack.value.any { !it.closing }
}

/** يفتح عنصراً: القناة تشغَّل مباشرة، وغيرها يفتح صفحة التفاصيل (origin = حدود البطاقة للانتقال). */
fun openItem(nav: NavVM, item: Item, siblings: List<Item> = emptyList(), origin: Rect? = null) {
    when {
        item.isChannel -> {
            val list = siblings.filter { it.isChannel }.ifEmpty { listOf(item) }
            nav.push(Route.Player(PlayReq(list.map { PlayEntry(it) }, list.indexOfFirst { it.id == item.id }.coerceAtLeast(0))))
        }
        item.kind == "episode" && item.parentId != null -> nav.push(Route.Detail(item.parentId, item), origin)
        else -> nav.push(Route.Detail(item.id, item), origin)
    }
}
