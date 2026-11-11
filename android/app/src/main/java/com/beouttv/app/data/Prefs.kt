package com.beouttv.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

private val Context.store: DataStore<Preferences> by preferencesDataStore("beout")

/**
 * التخزين المحلي (DataStore). اخترنا DataStore بدل Room لأن البيانات المحلية صغيرة
 * (إعدادات، مفضلة، سجل، مواضع) وهذا يوفر مئات الكيلوبايت من الحجم.
 */
object Prefs {
    private lateinit var ds: DataStore<Preferences>
    private val K_THEME = stringPreferencesKey("theme")
    private val K_MAXH = intPreferencesKey("maxH")
    private val K_RESUME = booleanPreferencesKey("resume")
    private val K_NOTIF = booleanPreferencesKey("notif")
    private val K_SCREEN = booleanPreferencesKey("screen")
    private val K_HISTORY = stringPreferencesKey("history")
    private val K_FAV = stringPreferencesKey("fav")
    private val K_SEARCH = stringPreferencesKey("search")
    private val K_LAST_CH = stringPreferencesKey("lastCh")
    private val K_MSG = intPreferencesKey("msgIdx")
    private val K_UIMODE = stringPreferencesKey("uiMode")
    private val K_FIT = stringPreferencesKey("videoFit")
    private val K_BOOST = booleanPreferencesKey("boost")
    private val K_AUTOPLAY = booleanPreferencesKey("autoPlay")
    private val K_AUTOFS = booleanPreferencesKey("autoFs")
    private val K_RECENT = stringPreferencesKey("recentCh")

    private lateinit var app: Context

    fun init(c: Context) {
        app = c.applicationContext
        ds = app.store
    }

    val settings: Flow<Settings> = kotlinx.coroutines.flow.flow { emitAll(ds.data) }.map {
        val m = AP.keys.associateWith { k -> it[stringPreferencesKey(k)] ?: AP.def.getValue(k) }
        Settings(
            theme = m.getValue("theme"),
            maxHeight = it[K_MAXH] ?: 0,
            resume = it[K_RESUME] ?: true,
            notifications = it[K_NOTIF] ?: true,
            keepScreenOn = it[K_SCREEN] ?: true,
            uiMode = it[K_UIMODE] ?: "auto",
            videoFit = it[K_FIT] ?: "fit",
            longPressBoost = it[K_BOOST] ?: true,
            autoPlay = it[K_AUTOPLAY] ?: true,
            autoFullscreen = it[K_AUTOFS] ?: true,
            navStyle = m.getValue("navStyle"), toolbarStyle = m.getValue("toolbarStyle"),
            homeLayout = m.getValue("homeLayout"), chLayout = m.getValue("chLayout"),
            accent = m.getValue("accent"), radius = m.getValue("radius"), glass = m.getValue("glass"),
            iconStyle = m.getValue("iconStyle"), fontScale = m.getValue("fontScale"), posterSize = m.getValue("posterSize"),
            ui = m.getValue("ui"), depth = m.getValue("depth"), shadow = m.getValue("shadow"), borderW = m.getValue("borderW"),
            cardDepth = m.getValue("cardDepth"), clarity = m.getValue("clarity"), space = m.getValue("space"),
            density = m.getValue("density"), listStyle = m.getValue("listStyle"), cardStyle = m.getValue("cardStyle"),
            playerLayout = m.getValue("playerLayout"),
        )
    }.distinctUntilChanged()

    suspend fun settingsNow() = settings.first()

    /** نسخة متزامنة سريعة من السمة لتلوين النافذة قبل تحميل DataStore (تمنع وميض الأبيض). */
    fun themeMirror(c: Context): String =
        c.getSharedPreferences("core", Context.MODE_PRIVATE).getString("theme", "dark") ?: "dark"

    suspend fun setTheme(v: String) {
        app.getSharedPreferences("core", Context.MODE_PRIVATE).edit().putString("theme", v).apply()
        ds.edit { it[K_THEME] = v }
    }
    // ---- الملفات الشخصية للتصميم (Presets) ----
    private val K_PRESETS = stringPreferencesKey("apPresets")
    private val K_PREV = stringPreferencesKey("apPrev")
    private val mapSer = kotlinx.serialization.builtins.MapSerializer(String.serializer(), String.serializer())
    private val presetsSer = kotlinx.serialization.builtins.MapSerializer(String.serializer(), mapSer)
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    val presets: Flow<Map<String, Map<String, String>>> = kotlinx.coroutines.flow.flow { emitAll(ds.data) }
        .map { p -> p[K_PRESETS]?.let { runCatching { json.decodeFromString(presetsSer, it) }.getOrNull() } ?: emptyMap() }
        .distinctUntilChanged()

    /** لقطة المظهر الحالي كاملة. */
    suspend fun snapshot(): Map<String, String> {
        val p = ds.data.first()
        return AP.keys.associateWith { k -> p[stringPreferencesKey(k)] ?: AP.def.getValue(k) }
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.writeAp(m: Map<String, String>) {
        m.forEach { (k, v) -> if (k in AP.def) this[stringPreferencesKey(k)] = v }
    }

    /** يطبّق مجموعة مفاتيح مع حفظ المظهر السابق للتراجع. */
    suspend fun applyAp(m: Map<String, String>) {
        val prev = snapshot()
        m["theme"]?.let { app.getSharedPreferences("core", Context.MODE_PRIVATE).edit().putString("theme", it).apply() }
        ds.edit { it[K_PREV] = json.encodeToString(mapSer, prev); it.writeAp(m) }
    }

    /** يعيد المظهر الذي كان قبل آخر تطبيق (ويحفظ الحالي ليُعاد إليه). */
    suspend fun undoAp() {
        val cur = snapshot()
        val prev = ds.data.first()[K_PREV]?.let { runCatching { json.decodeFromString(mapSer, it) }.getOrNull() } ?: return
        prev["theme"]?.let { app.getSharedPreferences("core", Context.MODE_PRIVATE).edit().putString("theme", it).apply() }
        ds.edit { it[K_PREV] = json.encodeToString(mapSer, cur); it.writeAp(prev) }
    }

    suspend fun hasPrev(): Boolean = ds.data.first()[K_PREV] != null

    suspend fun savePreset(name: String, m: Map<String, String>) =
        ds.edit { p ->
            val cur = p[K_PRESETS]?.let { runCatching { json.decodeFromString(presetsSer, it) }.getOrNull() } ?: emptyMap()
            p[K_PRESETS] = json.encodeToString(presetsSer, cur + (name to m))
        }

    suspend fun deletePreset(name: String) =
        ds.edit { p ->
            val cur = p[K_PRESETS]?.let { runCatching { json.decodeFromString(presetsSer, it) }.getOrNull() } ?: emptyMap()
            p[K_PRESETS] = json.encodeToString(presetsSer, cur - name)
        }

    fun exportAp(m: Map<String, String>): String = json.encodeToString(mapSer, m)
    fun parseAp(s: String): Map<String, String>? =
        runCatching { json.decodeFromString(mapSer, s.trim()) }.getOrNull()?.filterKeys { it in AP.def }?.takeIf { it.isNotEmpty() }

    /** إعدادات نصية بسيطة (أنماط الواجهة) بدل دالة لكل واحد: تقلل الكود. */
    suspend fun setStr(k: String, v: String) = ds.edit { it[stringPreferencesKey(k)] = v }
    suspend fun setUiMode(v: String) = ds.edit { it[K_UIMODE] = v }
    suspend fun setVideoFit(v: String) = ds.edit { it[K_FIT] = v }
    suspend fun setLongPressBoost(v: Boolean) = ds.edit { it[K_BOOST] = v }
    suspend fun setAutoPlay(v: Boolean) = ds.edit { it[K_AUTOPLAY] = v }
    suspend fun setAutoFullscreen(v: Boolean) = ds.edit { it[K_AUTOFS] = v }
    suspend fun setMaxHeight(v: Int) = ds.edit { it[K_MAXH] = v }
    suspend fun setResume(v: Boolean) = ds.edit { it[K_RESUME] = v }
    suspend fun setNotifications(v: Boolean) = ds.edit { it[K_NOTIF] = v }
    suspend fun setKeepScreenOn(v: Boolean) = ds.edit { it[K_SCREEN] = v }

    // ---- السجل ومواضع المشاهدة ----
    private val histSer = ListSerializer(HistoryEntry.serializer())
    private fun decodeHist(s: String?): List<HistoryEntry> =
        s?.let { runCatching { Api.json.decodeFromString(histSer, it) }.getOrNull() } ?: emptyList()

    val history: Flow<List<HistoryEntry>> = kotlinx.coroutines.flow.flow { emitAll(ds.data) }
        .map { decodeHist(it[K_HISTORY]) }.distinctUntilChanged()

    suspend fun historyEntry(key: String): HistoryEntry? = history.first().firstOrNull { it.key == key }

    suspend fun saveProgress(e: HistoryEntry) = ds.edit { p ->
        val list = decodeHist(p[K_HISTORY]).filter { it.key != e.key }
        p[K_HISTORY] = Api.json.encodeToString(histSer, (listOf(e) + list).take(100))
    }

    /** مسح السجل يشمل آخر القنوات المشاهدة أيضاً. */
    suspend fun clearHistory() = ds.edit { it.remove(K_HISTORY); it.remove(K_RECENT) }

    // ---- المفضلة ----
    private val favSer = ListSerializer(Item.serializer())
    private fun decodeFav(s: String?): List<Item> =
        s?.let { runCatching { Api.json.decodeFromString(favSer, it) }.getOrNull() } ?: emptyList()

    val favorites: Flow<List<Item>> = kotlinx.coroutines.flow.flow { emitAll(ds.data) }
        .map { decodeFav(it[K_FAV]) }.distinctUntilChanged()

    suspend fun toggleFavorite(item: Item) = ds.edit { p ->
        val list = decodeFav(p[K_FAV])
        val next = if (list.any { it.id == item.id }) list.filter { it.id != item.id } else listOf(item) + list
        p[K_FAV] = Api.json.encodeToString(favSer, next)
    }

    // ---- سجل البحث ----
    private val strSer = ListSerializer(String.serializer())
    val searchHistory: Flow<List<String>> = kotlinx.coroutines.flow.flow { emitAll(ds.data) }
        .map { p -> p[K_SEARCH]?.let { runCatching { Api.json.decodeFromString(strSer, it) }.getOrNull() } ?: emptyList() }
        .distinctUntilChanged()

    suspend fun addSearch(q: String) = ds.edit { p ->
        val cur = p[K_SEARCH]?.let { runCatching { Api.json.decodeFromString(strSer, it) }.getOrNull() } ?: emptyList()
        p[K_SEARCH] = Api.json.encodeToString(strSer, (listOf(q) + cur.filter { it != q }).take(20))
    }

    suspend fun clearSearch() = ds.edit { it.remove(K_SEARCH) }

    // ---- القنوات ----
    suspend fun lastChannel(): String? = ds.data.first()[K_LAST_CH]
    suspend fun setLastChannel(id: String) = ds.edit { it[K_LAST_CH] = id }

    /** آخر القنوات التي شُغّلت (الأحدث أولاً، حتى 20). تُخزَّن كاملة فتعمل القائمة بلا اتصال. */
    val recentChannels: Flow<List<Item>> = kotlinx.coroutines.flow.flow { emitAll(ds.data) }
        .map { decodeFav(it[K_RECENT]) }.distinctUntilChanged()

    suspend fun pushRecentChannel(item: Item) = ds.edit { p ->
        val cur = decodeFav(p[K_RECENT])
        if (cur.firstOrNull()?.id == item.id) return@edit
        p[K_RECENT] = Api.json.encodeToString(favSer, (listOf(item) + cur.filter { it.id != item.id }).take(20))
    }

    // ---- الإشعارات المحلية ----
    suspend fun nextMessageIndex(size: Int): Int {
        var idx = 0
        ds.edit { p ->
            idx = ((p[K_MSG] ?: -1) + 1) % size
            p[K_MSG] = idx
        }
        return idx
    }
}

private suspend fun <T> kotlinx.coroutines.flow.FlowCollector<T>.emitAll(f: Flow<T>) = f.collect { emit(it) }
