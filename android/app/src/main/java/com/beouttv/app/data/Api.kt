package com.beouttv.app.data

import com.beouttv.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

class HttpException(val code: Int) : IOException("HTTP $code")

/**
 * عميل شبكة خفيف: HttpURLConnection + ETag + دمج الطلبات المتطابقة + إلغاء فعلي للاتصال
 * + كاش قرص لكل استجابة GET ناجحة يُستخدم عند انقطاع الشبكة أو أخطاء الخادم (5xx).
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
object Api {
    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    @Volatile var deviceId: String = ""

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inflight = HashMap<String, Deferred<String>>()
    private val etags = HashMap<String, Pair<String, String>>()
    @Volatile private var cacheDir: File? = null

    fun init(cache: File) {
        val d = File(cache, "api").apply { mkdirs() }
        cacheDir = d
        // تقليم: نبقي أحدث 300 ملف
        d.listFiles()?.sortedByDescending { it.lastModified() }?.drop(300)?.forEach { it.delete() }
    }

    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun cacheFile(path: String): File? {
        val d = cacheDir ?: return null
        val name = MessageDigest.getInstance("MD5").digest(path.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(d, name)
    }

    /** آخر استجابة محفوظة لهذا المسار (بدون شبكة). */
    fun readCache(path: String): String? = runCatching {
        cacheFile(path)?.takeIf { it.exists() }?.readText()
    }.getOrNull()

    private fun writeCache(path: String, body: String) {
        runCatching {
            val f = cacheFile(path) ?: return
            val tmp = File(f.path + ".tmp")
            tmp.writeText(body)
            tmp.renameTo(f)
        }
    }

    suspend fun get(path: String, dedupe: Boolean = true): String {
        if (!dedupe) return fetch(path, store = false)
        val d = synchronized(inflight) {
            inflight.getOrPut(path) {
                scope.async {
                    try { fetchWithFallback(path) } finally { synchronized(inflight) { inflight.remove(path) } }
                }
            }
        }
        return d.await()
    }

    suspend inline fun <reified T> getJson(path: String, dedupe: Boolean = true): T =
        json.decodeFromString<T>(get(path, dedupe))

    private suspend fun fetchWithFallback(path: String): String =
        try {
            fetch(path, store = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // أخطاء 4xx تعني طلباً خاطئاً فلا نخفيها بالكاش. الشبكة و5xx نعوّضها بآخر نسخة.
            val recoverable = e !is HttpException || e.code >= 500
            (if (recoverable) readCache(path) else null) ?: throw e
        }

    private suspend fun fetch(path: String, store: Boolean): String = withContext(Dispatchers.IO) {
        val conn = (URL(BuildConfig.API_BASE + path).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Device", deviceId)
            synchronized(etags) { etags[path] }?.let { setRequestProperty("If-None-Match", it.first) }
        }
        val h = currentCoroutineContext()[Job]?.invokeOnCompletion { conn.disconnect() }
        try {
            when (val code = conn.responseCode) {
                304 -> synchronized(etags) { etags[path]?.second } ?: throw HttpException(code)
                in 200..299 -> {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.getHeaderField("ETag")?.let { synchronized(etags) { etags[path] = it to body } }
                    if (store) writeCache(path, body)
                    body
                }
                else -> throw HttpException(code)
            }
        } finally {
            h?.dispose()
            conn.disconnect()
        }
    }

    /** إرسال بدون انتظار نتيجة (مزامنة السجل والمفضلة). */
    fun post(path: String, body: String) {
        scope.async {
            runCatching {
                val c = (URL(BuildConfig.API_BASE + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; doOutput = true
                    connectTimeout = 8_000; readTimeout = 8_000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-Device", deviceId)
                }
                c.outputStream.use { it.write(body.toByteArray()) }
                c.responseCode
                c.disconnect()
            }
        }
    }

    fun clearCache() {
        synchronized(etags) { etags.clear() }
        cacheDir?.listFiles()?.forEach { it.delete() }
    }
}
