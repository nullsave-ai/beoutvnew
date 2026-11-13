package com.beouttv.app.optical

import android.graphics.Bitmap

/**
 * جسر JNI للمحرك البصري. المحرك اختياري: إن لم تُبنَ المكتبة (لا NDK) أو فشل تحميلها
 * تُستخدم نسخة Kotlin المكافئة تلقائياً، فلا ينكسر أي شيء.
 * الاستدعاءات قليلة عمداً: تمويه واحد لكل صورة مصغرة.
 */
object NativeEngine {
    val available: Boolean = try {
        System.loadLibrary("beout_optical"); true
    } catch (_: Throwable) { false }

    @JvmStatic private external fun nativeCaps(): Int
    @JvmStatic private external fun nativeBlur(bmp: Bitmap, radius: Int, passes: Int): Boolean
    @JvmStatic private external fun nativeBench(): Long

    /** بتات: 1 NEON، 2 SSE4.2، 4 AVX2، 8 معالج 64 بت. */
    val caps: Int by lazy { if (available) runCatching { nativeCaps() }.getOrDefault(0) else 0 }

    /** زمن المعيار بالميكروثانية؛ -1 إن لم يتوفر المحرك. */
    val bench: Long by lazy { if (available) runCatching { nativeBench() }.getOrDefault(-1L) else -1L }

    /** يمزج الصورة في مكانها. يجب أن تكون قابلة للتعديل وRGBA_8888. */
    fun blur(bmp: Bitmap, radius: Int, passes: Int = 2): Boolean {
        if (radius < 1) return true
        if (available && bmp.config == Bitmap.Config.ARGB_8888 && bmp.isMutable) {
            val ok = runCatching { nativeBlur(bmp, radius, passes) }.getOrDefault(false)
            if (ok) return true
        }
        return KBlur.box(bmp, radius, passes)
    }
}

/** نسخة Kotlin المكافئة (احتياطية). نفس الخوارزمية: Box بنافذة منزلقة مع تمديد الحواف. */
object KBlur {
    fun box(bmp: Bitmap, radius: Int, passes: Int): Boolean {
        if (!bmp.isMutable) return false
        val w = bmp.width; val h = bmp.height
        if (w < 2 || h < 2) return true
        val r = radius.coerceIn(1, 64)
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h)
        repeat(passes.coerceIn(1, 4)) {
            line(px, tmp, w, h, r, true)
            line(tmp, px, w, h, r, false)
        }
        bmp.setPixels(px, 0, w, 0, 0, w, h)
        return true
    }

    private fun line(s: IntArray, d: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val win = 2 * r + 1
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        for (l in 0 until lines) {
            fun idx(i: Int) = if (horizontal) l * w + i else i * w + l
            var a = 0; var rr = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val p = s[idx(i.coerceIn(0, len - 1))]
                a += p ushr 24; rr += (p shr 16) and 255; g += (p shr 8) and 255; b += p and 255
            }
            for (i in 0 until len) {
                d[idx(i)] = ((a / win) shl 24) or ((rr / win) shl 16) or ((g / win) shl 8) or (b / win)
                val po = s[idx((i - r).coerceAtLeast(0))]
                val pn = s[idx((i + r + 1).coerceAtMost(len - 1))]
                a += (pn ushr 24) - (po ushr 24)
                rr += ((pn shr 16) and 255) - ((po shr 16) and 255)
                g += ((pn shr 8) and 255) - ((po shr 8) and 255)
                b += (pn and 255) - (po and 255)
            }
        }
    }
}
