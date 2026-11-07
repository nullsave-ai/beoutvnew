package com.beouttv.app.data

import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.serializer

/** كل بيانات المحتوى تأتي من الـ Backend. المحلي فقط: مفضلة، سجل، مواضع، إعدادات. */
object Repo {
    private fun q(vararg p: Pair<String, String?>): String =
        p.filter { !it.second.isNullOrBlank() }
            .joinToString("&", prefix = "?") { "${it.first}=${Api.enc(it.second!!)}" }
            .let { if (it == "?") "" else it }

    suspend fun home(): HomeResponse = Api.getJson("home")
    suspend fun config(): AppConfig = Api.getJson("config")

    // نسخ محفوظة على القرص (تُعرض فوراً عند بدء التشغيل ثم يُحدَّث من الخادم)
    suspend fun homeCached(): HomeResponse? = cachedOf("home")
    suspend fun configCached(): AppConfig? = cachedOf("config")

    private suspend inline fun <reified T> cachedOf(path: String): T? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            Api.readCache(path)?.let { runCatching { Api.json.decodeFromString<T>(it) }.getOrNull() }
        }

    suspend fun channelCategories(): List<Category> = Api.getJson("categories${q("kind" to "channel")}")
    suspend fun catalogCategories(kind: String): List<Category> = Api.getJson("categories${q("kind" to kind)}")

    suspend fun channels(category: String?, cursor: String?): Page =
        Api.getJson("channels${q("category" to category, "cursor" to cursor)}")

    suspend fun catalog(kind: String, category: String?, cursor: String?, sort: String? = null): Page =
        Api.getJson("catalog${q("kind" to kind, "category" to category, "cursor" to cursor, "sort" to sort)}")

    suspend fun detail(id: String): Detail = Api.getJson("items/${Api.enc(id)}")

    suspend fun search(query: String, type: String?, cursor: String?): Page =
        Api.getJson("search${q("q" to query, "type" to type, "cursor" to cursor)}", dedupe = false)

    suspend fun suggest(query: String): List<String> =
        Api.getJson("search/suggest${q("q" to query)}", dedupe = false)

    /** رابط التشغيل يُطلب لحظة التشغيل فقط (الروابط محمية ومؤقتة من الخادم). */
    suspend fun play(kind: String, id: String): PlayInfo =
        Api.getJson("play/${Api.enc(kind)}/${Api.enc(id)}", dedupe = false)

    // ---- محلي + مزامنة best-effort ----
    suspend fun toggleFavorite(item: Item) {
        val wasFav = Prefs.favorites.first().any { it.id == item.id }
        Prefs.toggleFavorite(item)
        Api.post("sync/favorites", """{"itemId":${Api.json.encodeToString(String.serializer(), item.id)},"on":${!wasFav}}""")
    }

    suspend fun saveProgress(e: HistoryEntry) {
        Prefs.saveProgress(e)
        if (e.item.kind != "channel")
            Api.post("sync/history", Api.json.encodeToString(HistoryEntry.serializer(), e))
    }
}
