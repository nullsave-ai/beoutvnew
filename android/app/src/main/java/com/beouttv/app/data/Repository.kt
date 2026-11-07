package com.beouttv.app.data

import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.serializer

/**
 * مستودع البيانات: يطلب البيانات من الخادم، وفي حال عدم توفر الخادم أو كونه في وضع المعاينة
 * يعود تلقائياً وبسلاسة إلى البيانات التجريبية (DemoData) لاختبار التطبيق والمشغل.
 */
object Repo {
    private fun q(vararg p: Pair<String, String?>): String =
        p.filter { !it.second.isNullOrBlank() }
            .joinToString("&", prefix = "?") { "${it.first}=${Api.enc(it.second!!)}" }
            .let { if (it == "?") "" else it }

    suspend fun home(): HomeResponse =
        runCatching { Api.getJson<HomeResponse>("home") }.getOrElse { DemoData.home() }

    suspend fun config(): AppConfig =
        runCatching { Api.getJson<AppConfig>("config") }.getOrElse { DemoData.config() }

    // نسخ محفوظة على القرص (تُعرض فوراً عند بدء التشغيل، مع تقديم البيانات التجريبية كقاعدة فورية)
    suspend fun homeCached(): HomeResponse? =
        cachedOf<HomeResponse>("home") ?: DemoData.home()

    suspend fun configCached(): AppConfig? =
        cachedOf<AppConfig>("config") ?: DemoData.config()

    private suspend inline fun <reified T> cachedOf(path: String): T? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            Api.readCache(path)?.let { runCatching { Api.json.decodeFromString<T>(it) }.getOrNull() }
        }

    suspend fun channelCategories(): List<Category> =
        runCatching { Api.getJson<List<Category>>("categories${q("kind" to "channel")}") }.getOrElse { DemoData.channelCategories() }

    suspend fun catalogCategories(kind: String): List<Category> =
        runCatching { Api.getJson<List<Category>>("categories${q("kind" to kind)}") }.getOrElse { DemoData.catalogCategories(kind) }

    suspend fun channels(category: String?, cursor: String?): Page =
        runCatching { Api.getJson<Page>("channels${q("category" to category, "cursor" to cursor)}") }.getOrElse { DemoData.channels(category, cursor) }

    suspend fun catalog(kind: String, category: String?, cursor: String?, sort: String? = null): Page =
        runCatching { Api.getJson<Page>("catalog${q("kind" to kind, "category" to category, "cursor" to cursor, "sort" to sort)}") }.getOrElse { DemoData.catalog(kind, category, cursor, sort) }

    suspend fun detail(id: String): Detail =
        runCatching { Api.getJson<Detail>("items/${Api.enc(id)}") }.getOrElse { DemoData.detail(id) }

    suspend fun search(query: String, type: String?, cursor: String?): Page =
        runCatching { Api.getJson<Page>("search${q("q" to query, "type" to type, "cursor" to cursor)}", dedupe = false) }.getOrElse { DemoData.search(query, type, cursor) }

    suspend fun suggest(query: String): List<String> =
        runCatching { Api.getJson<List<String>>("search/suggest${q("q" to query)}", dedupe = false) }.getOrElse { DemoData.suggest(query) }

    /** رابط التشغيل يُطلب لحظة التشغيل فقط. */
    suspend fun play(kind: String, id: String): PlayInfo =
        runCatching { Api.getJson<PlayInfo>("play/${Api.enc(kind)}/${Api.enc(id)}", dedupe = false) }.getOrElse { DemoData.play(kind, id) }

    // ---- محلي + مزامنة best-effort ----
    suspend fun toggleFavorite(item: Item) {
        val wasFav = Prefs.favorites.first().any { it.id == item.id }
        Prefs.toggleFavorite(item)
        runCatching {
            Api.post("sync/favorites", """{"itemId":${Api.json.encodeToString(String.serializer(), item.id)},"on":${!wasFav}}""")
        }
    }

    suspend fun saveProgress(e: HistoryEntry) {
        Prefs.saveProgress(e)
        if (e.item.kind != "channel")
            runCatching {
                Api.post("sync/history", Api.json.encodeToString(HistoryEntry.serializer(), e))
            }
    }
}
