package com.beouttv.app.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beouttv.app.core.Loader
import com.beouttv.app.core.PagedLoader
import com.beouttv.app.core.UiState
import com.beouttv.app.data.Api
import com.beouttv.app.data.AppConfig
import com.beouttv.app.data.Category
import com.beouttv.app.data.Detail
import com.beouttv.app.data.HomeResponse
import com.beouttv.app.data.Img
import com.beouttv.app.data.Item
import com.beouttv.app.data.AP
import com.beouttv.app.data.Prefs
import com.beouttv.app.data.Repo
import com.beouttv.app.data.Settings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppVM : ViewModel() {
    val settings: StateFlow<Settings> = Prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    /** false حتى تُقرأ الإعدادات المحفوظة فعلياً (يمنع وميض سمة افتراضية). */
    val settingsLoaded = MutableStateFlow(false)

    val config = Loader<AppConfig>(viewModelScope, cached = { Repo.configCached() }) { Repo.config() }

    private var cfgAt = 0L

    init {
        viewModelScope.launch {
            Prefs.settings.first()
            settingsLoaded.value = true
        }
    }

    /** عند العودة للتطبيق: لا يُعاد الطلب أكثر من مرة كل 5 دقائق (ولا يُلغى طلب جارٍ). */
    fun softRefreshConfig() {
        val now = System.currentTimeMillis()
        if (config.state.value is UiState.Initial) { config.load(); cfgAt = now }
        else if (now - cfgAt > 5 * 60_000L) { config.refresh(); cfgAt = now }
    }

    fun setTheme(v: String) = viewModelScope.launch { Prefs.setTheme(v) }
    fun setUiMode(v: String) = viewModelScope.launch { Prefs.setUiMode(v) }
    fun setVideoFit(v: String) = viewModelScope.launch { Prefs.setVideoFit(v) }
    fun setLongPressBoost(v: Boolean) = viewModelScope.launch { Prefs.setLongPressBoost(v) }
    fun setStr(k: String, v: String) = viewModelScope.launch { Prefs.setStr(k, v) }
    // ---- المظهر ----
    val presets: StateFlow<Map<String, Map<String, String>>> = Prefs.presets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    fun applyAp(m: Map<String, String>) = viewModelScope.launch { Prefs.applyAp(m) }
    fun undoAp() = viewModelScope.launch { Prefs.undoAp() }
    fun resetGroup(g: String) = viewModelScope.launch {
        val keys = if (g == "all") AP.allKeys() else (AP.groups[g] ?: emptyList())
        Prefs.applyAp(keys.associateWith { AP.def.getValue(it) })
    }
    private val pending = HashMap<String, String>()
    private var flushJob: kotlinx.coroutines.Job? = null
    /** كتابة مؤجلة قصيرة للمنزلقات: المعاينة فورية والتخزين مرة واحدة بعد توقف الحركة. */
    fun setNum(k: String, v: String) {
        pending[k] = v
        flushJob?.cancel()
        flushJob = viewModelScope.launch {
            kotlinx.coroutines.delay(90)
            val m = HashMap(pending); pending.clear()
            Prefs.setMany(m)
        }
    }
    fun setMany(m: Map<String, String>) = viewModelScope.launch { Prefs.setMany(m) }
    fun renamePreset(old: String, new: String) = viewModelScope.launch { Prefs.renamePreset(old, new) }
    fun applyStyle(ui: String) = applyAp(AP.styleBundle(ui))
    fun applyDepth(d: String) = applyAp(AP.depthBundle(d))
    fun savePreset(name: String) = viewModelScope.launch { Prefs.savePreset(name, Prefs.snapshot()) }
    fun savePresetMap(name: String, m: Map<String, String>) = viewModelScope.launch { Prefs.savePreset(name, m) }
    fun deletePreset(name: String) = viewModelScope.launch { Prefs.deletePreset(name) }
    suspend fun exportAp(): String = Prefs.exportAp(Prefs.snapshot())
    fun importAp(text: String): Boolean {
        val m = Prefs.parseAp(text) ?: return false
        applyAp(m); return true
    }
    fun setAutoPlay(v: Boolean) = viewModelScope.launch { Prefs.setAutoPlay(v) }
    fun setAutoFullscreen(v: Boolean) = viewModelScope.launch { Prefs.setAutoFullscreen(v) }
    fun setMaxHeight(v: Int) = viewModelScope.launch { Prefs.setMaxHeight(v) }
    fun setResume(v: Boolean) = viewModelScope.launch { Prefs.setResume(v) }
    fun setKeepScreen(v: Boolean) = viewModelScope.launch { Prefs.setKeepScreenOn(v) }
    fun setNotifications(v: Boolean, apply: (Boolean) -> Unit) = viewModelScope.launch {
        Prefs.setNotifications(v); apply(v)
    }
    fun clearCache() { Img.clear(); Api.clearCache() }
    fun clearHistory() = viewModelScope.launch { Prefs.clearHistory() }
}

class HomeVM : ViewModel() {
    val home = Loader<HomeResponse>(
        viewModelScope,
        isEmpty = { it.sections.isEmpty() },
        cached = { Repo.homeCached() },
    ) { Repo.home() }
    val history = Prefs.history.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val favorites = Prefs.favorites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    fun toggleFav(item: Item) { viewModelScope.launch { Repo.toggleFavorite(item) } }

    /** القسم المختار في لوحة الأقسام، وحالة تمرير قائمة القنوات؛ تبقيان عند التنقل بين الصفحات. */
    val sectionKey = MutableStateFlow<String?>(null)
    val listState = LazyListState()
    private var last = 0L

    fun softRefresh() {
        val now = System.currentTimeMillis()
        if (home.state.value is UiState.Initial) { home.load(); last = now }
        else if (now - last > 5 * 60_000L) { home.refresh(); last = now }
    }
}

@OptIn(FlowPreview::class)
class ChannelsVM : ViewModel() {
    val cats = Loader<List<Category>>(viewModelScope) { Repo.channelCategories() }
    val category = MutableStateFlow<String?>(null)
    val favOnly = MutableStateFlow(false)
    val query = MutableStateFlow("")
    val paged = PagedLoader(viewModelScope) { Repo.channels(category.value, it) }
    val favorites = Prefs.favorites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    /** نتائج البحث في القنوات من الخادم (لا يقتصر على الصفحات المحمّلة). */
    val found = PagedLoader(viewModelScope) { Repo.search(query.value.trim(), "channel", it) }
    val listState = LazyListState()
    private var started = false

    init {
        viewModelScope.launch {
            query.debounce(250).map { it.trim() }.distinctUntilChanged().collect { q ->
                if (q.length < 2) found.clear() else found.reset(keepOld = true)
            }
        }
    }

    fun start() {
        if (started) return
        started = true
        cats.load(); paged.reset()
    }

    /** تغيير الفلتر يُبقي المحتوى الحالي ظاهراً ويستبدله عند وصول الجديد (بدون إعادة تحميل الشاشة). */
    fun select(c: String?) {
        if (category.value == c) return
        category.value = c
        paged.reset(keepOld = true)
        viewModelScope.launch { listState.scrollToItem(0) }
    }

    fun refresh() { cats.refresh(); paged.reset(keepOld = true, pull = true) }
    fun toggleFav(item: Item) { viewModelScope.launch { Repo.toggleFavorite(item) } }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogVM : ViewModel() {
    val kind = MutableStateFlow("movie")
    val category = MutableStateFlow<String?>(null)
    private val cats = MutableStateFlow<Loader<List<Category>>?>(null)
    private val recent = MutableStateFlow<Loader<List<Item>>?>(null)
    /** حالتا التصنيفات و"أضيف حديثاً" تتبعان المحمّل الحالي عند تبديل النوع. */
    val catsUi: StateFlow<UiState<List<Category>>> =
        cats.flatMapLatest { it?.state ?: flowOf(UiState.Initial) }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Initial)
    val recentUi: StateFlow<UiState<List<Item>>> =
        recent.flatMapLatest { it?.state ?: flowOf(UiState.Initial) }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Initial)
    val paged = PagedLoader(viewModelScope) { Repo.catalog(kind.value, category.value, it) }
    val history = Prefs.history.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val gridState = LazyGridState()
    private var started = false

    private fun catsFor(k: String) = Loader<List<Category>>(viewModelScope) { Repo.catalogCategories(k) }
    private fun recentFor(k: String) =
        Loader<List<Item>>(viewModelScope, isEmpty = { it.isEmpty() }) { Repo.catalog(k, null, null, "recent").items.take(12) }

    fun start() {
        if (started) return
        started = true
        cats.value = catsFor(kind.value).also { it.load() }
        recent.value = recentFor(kind.value).also { it.load() }
        paged.reset()
    }

    fun setKind(k: String) {
        if (kind.value == k) return
        kind.value = k; category.value = null
        cats.value = catsFor(k).also { it.load() }
        recent.value = recentFor(k).also { it.load() }
        paged.reset(keepOld = true)
        viewModelScope.launch { gridState.scrollToItem(0) }
    }

    fun select(c: String?) {
        if (category.value == c) return
        category.value = c
        paged.reset(keepOld = true)
        viewModelScope.launch { gridState.scrollToItem(0) }
    }

    fun refresh() {
        cats.value?.refresh(); recent.value?.refresh(); paged.reset(keepOld = true, pull = true)
    }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchVM : ViewModel() {
    val query = MutableStateFlow("")
    val type = MutableStateFlow<String?>(null)
    val suggestions = MutableStateFlow<List<String>>(emptyList())
    val paged = PagedLoader(viewModelScope) { Repo.search(query.value.trim(), type.value, it) }
    val recent = Prefs.searchHistory.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val favorites = Prefs.favorites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    fun toggleFav(item: Item) { viewModelScope.launch { Repo.toggleFavorite(item) } }

    init {
        viewModelScope.launch {
            combine(query.debounce(250), type) { q, t -> q.trim() to t }
                .distinctUntilChanged()
                .collect { (q, _) ->
                    if (q.length < 2) paged.clear() else paged.reset(keepOld = true)
                }
        }
        viewModelScope.launch {
            query.debounce(150).distinctUntilChanged().collect { q ->
                val t = q.trim()
                suggestions.value = if (t.length < 2) emptyList()
                else runCatching { Repo.suggest(t) }.getOrDefault(emptyList())
            }
        }
    }

    fun commit() { val q = query.value.trim(); if (q.length >= 2) viewModelScope.launch { Prefs.addSearch(q) } }
    fun clearRecent() { viewModelScope.launch { Prefs.clearSearch() } }
}

class DetailVM : ViewModel() {
    private val loaders = HashMap<String, Loader<Detail>>()
    val favorites = Prefs.favorites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val history = Prefs.history.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun loader(id: String): Loader<Detail> =
        loaders.getOrPut(id) { Loader(viewModelScope) { Repo.detail(id) } }

    fun toggleFav(item: Item) { viewModelScope.launch { Repo.toggleFavorite(item) } }
}
