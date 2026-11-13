package com.beouttv.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * محمّل صور صغير: ذاكرة LRU + قرص + تصغير أثناء فك الترميز.
 * - peek(): قراءة متزامنة من الذاكرة (تمنع إطاراً فارغاً عند عودة البطاقة للشاشة).
 * - قفل لكل رابط: تنزيل واحد فقط لنفس الصورة حتى لو طلبتها بطاقتان معاً.
 * - سقف 6 مهام متوازية: التمرير السريع لا يغرق الشبكة، وإلغاء الطلب يخرج من الطابور فوراً.
 */
object Img {
    private lateinit var dir: File
    private val mem = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 10).toInt().coerceAtMost(48 shl 20)) {
        override fun sizeOf(k: String, v: Bitmap) = v.byteCount
    }
    private val sem = Semaphore(6)
    private val locks = ConcurrentHashMap<String, Mutex>()

    fun init(c: Context) {
        dir = File(c.cacheDir, "img").apply { mkdirs() }
    }

    private fun name(url: String) =
        MessageDigest.getInstance("MD5").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }

    fun peek(url: String, reqW: Int): Bitmap? = mem.get("$url@$reqW")

    suspend fun load(url: String, reqW: Int): Bitmap? {
        val key = "$url@$reqW"
        mem.get(key)?.let { return it }
        return sem.withPermit {
            withContext(Dispatchers.IO) {
                mem.get(key)?.let { return@withContext it }
                val n = name(url)
                val f = File(dir, n)
                if (!f.exists()) {
                    locks.getOrPut(n) { Mutex() }.withLock {
                        if (!f.exists()) runCatching {
                            val c = URL(url).openConnection() as HttpURLConnection
                            c.connectTimeout = 8000; c.readTimeout = 12000
                            if (c.responseCode in 200..299) {
                                val tmp = File(dir, "$n.tmp")
                                c.inputStream.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
                                tmp.renameTo(f)
                            }
                            c.disconnect()
                        }
                        Unit
                    }
                }
                if (!f.exists()) return@withContext null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(f.path, bounds)
                var s = 1
                while (bounds.outWidth / (s * 2) >= reqW) s *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = s; inPreferredConfig = Bitmap.Config.RGB_565 }
                BitmapFactory.decodeFile(f.path, opts)?.also { mem.put(key, it) }
            }
        }
    }

    fun clear() {
        mem.evictAll()
        dir.listFiles()?.forEach { it.delete() }
    }

    fun sizeBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L
}

@Composable
fun RemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
    reqWidthPx: Int = 480,
    scale: ContentScale = ContentScale.Crop,
    alignment: androidx.compose.ui.Alignment = androidx.compose.ui.Alignment.Center,
    placeholder: @Composable () -> Unit = {},
) {
    // عناصر المعاينة في الاستوديو: "gradient:<id>" يرسم تدرجاً ثابتاً بدل صورة (لا شبكة)
    if (url != null && url.startsWith("gradient:")) {
        val h = url.hashCode()
        val c1 = androidx.compose.ui.graphics.Color.hsv(((h ushr 0) and 0xFF) / 255f * 360f, 0.55f, 0.62f)
        val c2 = androidx.compose.ui.graphics.Color.hsv(((h ushr 8) and 0xFF) / 255f * 360f, 0.6f, 0.4f)
        Box(modifier.clipToBounds().background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(c1, c2))))
        return
    }
    // إن كانت الصورة في الذاكرة تظهر فوراً بلا تلاشٍ، وإلا تظهر بتلاشٍ قصير عند وصولها
    val cached = remember(url, reqWidthPx) {
        if (url.isNullOrBlank()) null else Img.peek(url, reqWidthPx)?.asImageBitmap()
    }
    val bmp: ImageBitmap? by produceState<ImageBitmap?>(cached, url, reqWidthPx) {
        if (value == null && !url.isNullOrBlank()) value = Img.load(url, reqWidthPx)?.asImageBitmap()
    }
    Box(modifier.clipToBounds()) {
        val b = bmp
        if (b == null) placeholder() else {
            val a = remember(b) { Animatable(if (cached != null) 1f else 0f) }
            LaunchedEffect(b) { if (a.value < 1f) a.animateTo(1f, tween(180)) }
            Image(b, null, Modifier.matchParentSize().graphicsLayer { alpha = a.value }, alignment = alignment, contentScale = scale)
        }
    }
}
