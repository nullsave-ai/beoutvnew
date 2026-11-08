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
        Settings(
            theme = it[K_THEME] ?: "dark",
            maxHeight = it[K_MAXH] ?: 0,
            resume = it[K_RESUME] ?: true,
            notifications = it[K_NOTIF] ?: true,
            keepScreenOn = it[K_SCREEN] ?: true,
            uiMode = it[K_UIMODE] ?: "auto",
            videoFit = it[K_FIT] ?: "fit",
            longPressBoost = it[K_BOOST] ?: true,
            autoPlay = it[K_AUTOPLAY] ?: true,
            autoFullscreen = it[K_AUTOFS] ?: true,
            navStyle = it[stringPreferencesKey("navStyle")] ?: "labels",
            toolbarStyle = it[stringPreferencesKey("toolbarStyle")] ?: "pill",
            homeLayout = it[stringPreferencesKey("homeLayout")] ?: "hero",
            chLayout = it[stringPreferencesKey("chLayout")] ?: "auto",
            accent = it[stringPreferencesKey("accent")] ?: "theme",
            radius = it[stringPreferencesKey("radius")] ?: "m",
            glass = it[stringPreferencesKey("glass")] ?: "m",
            iconStyle = it[stringPreferencesKey("iconStyle")] ?: "duo",
            fontScale = it[stringPreferencesKey("fontScale")] ?: "m",
            posterSize = it[stringPreferencesKey("posterSize")] ?: "m",
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
