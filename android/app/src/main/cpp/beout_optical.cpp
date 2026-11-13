// BEOUT Optical Native Engine — محرك خفيف: كشف قدرات المعالج + تمويه Box متعدد التمريرات (≈ Gaussian)
// على Bitmap بصيغة RGBA_8888. مصدر واحد لكل المعماريات (arm64-v8a / armeabi-v7a / x86_64 / x86).
// بلا STL ولا استثناءات. التمويه يعمل على صور مصغرة (≤ 128px) فيبقى رخيصاً على الأجهزة الاقتصادية.
#include <jni.h>
#include <android/bitmap.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#if defined(__arm__) || defined(__aarch64__)
#include <sys/auxv.h>
#if defined(__arm__)
#include <asm/hwcap.h>
#endif
#endif

// ---------- طبقة تجريد المنصة: قدرات المعالج عند التشغيل ----------
enum { CAP_NEON = 1, CAP_SSE42 = 2, CAP_AVX2 = 4, CAP_64 = 8 };

static int detect_caps() {
    int caps = 0;
#if defined(__aarch64__)
    caps |= CAP_NEON | CAP_64;           // NEON إلزامي في ARMv8
#elif defined(__arm__)
    unsigned long hw = getauxval(AT_HWCAP);
    if (hw & HWCAP_NEON) caps |= CAP_NEON;
#elif defined(__x86_64__) || defined(__i386__)
    __builtin_cpu_init();
    if (__builtin_cpu_supports("sse4.2")) caps |= CAP_SSE42;
    if (__builtin_cpu_supports("avx2")) caps |= CAP_AVX2;
#if defined(__x86_64__)
    caps |= CAP_64;
#endif
#endif
    return caps;
}

// ---------- تمويه Box بنافذة منزلقة O(n) لكل قناة، مع تمديد الحواف ----------
static void blur_h(const uint8_t* s, uint8_t* d, int w, int h, int stride, int r) {
    const int win = 2 * r + 1;
    for (int y = 0; y < h; y++) {
        const uint8_t* row = s + (size_t)y * stride;
        uint8_t* out = d + (size_t)y * stride;
        int sr = 0, sg = 0, sb = 0, sa = 0;
        for (int i = -r; i <= r; i++) {
            int xi = i < 0 ? 0 : (i >= w ? w - 1 : i);
            const uint8_t* p = row + xi * 4;
            sr += p[0]; sg += p[1]; sb += p[2]; sa += p[3];
        }
        for (int x = 0; x < w; x++) {
            uint8_t* o = out + x * 4;
            o[0] = (uint8_t)(sr / win); o[1] = (uint8_t)(sg / win); o[2] = (uint8_t)(sb / win); o[3] = (uint8_t)(sa / win);
            int xo = x - r; if (xo < 0) xo = 0;
            int xn = x + r + 1; if (xn >= w) xn = w - 1;
            const uint8_t* po = row + xo * 4; const uint8_t* pn = row + xn * 4;
            sr += pn[0] - po[0]; sg += pn[1] - po[1]; sb += pn[2] - po[2]; sa += pn[3] - po[3];
        }
    }
}

static void blur_v(const uint8_t* s, uint8_t* d, int w, int h, int stride, int r) {
    const int win = 2 * r + 1;
    for (int x = 0; x < w; x++) {
        int sr = 0, sg = 0, sb = 0, sa = 0;
        for (int i = -r; i <= r; i++) {
            int yi = i < 0 ? 0 : (i >= h ? h - 1 : i);
            const uint8_t* p = s + (size_t)yi * stride + x * 4;
            sr += p[0]; sg += p[1]; sb += p[2]; sa += p[3];
        }
        for (int y = 0; y < h; y++) {
            uint8_t* o = d + (size_t)y * stride + x * 4;
            o[0] = (uint8_t)(sr / win); o[1] = (uint8_t)(sg / win); o[2] = (uint8_t)(sb / win); o[3] = (uint8_t)(sa / win);
            int yo = y - r; if (yo < 0) yo = 0;
            int yn = y + r + 1; if (yn >= h) yn = h - 1;
            const uint8_t* po = s + (size_t)yo * stride + x * 4; const uint8_t* pn = s + (size_t)yn * stride + x * 4;
            sr += pn[0] - po[0]; sg += pn[1] - po[1]; sb += pn[2] - po[2]; sa += pn[3] - po[3];
        }
    }
}

static bool blur_pixels(uint8_t* px, int w, int h, int stride, int radius, int passes) {
    if (radius < 1 || w < 2 || h < 2) return true;
    if (radius > 64) radius = 64;
    if (passes < 1) passes = 1;
    if (passes > 4) passes = 4;
    uint8_t* tmp = (uint8_t*)malloc((size_t)stride * h);
    if (!tmp) return false;
    for (int i = 0; i < passes; i++) {
        blur_h(px, tmp, w, h, stride, radius);
        blur_v(tmp, px, w, h, stride, radius);
    }
    free(tmp);
    return true;
}

extern "C" {

JNIEXPORT jint JNICALL Java_com_beouttv_app_optical_NativeEngine_nativeCaps(JNIEnv*, jclass) {
    static int caps = -1;
    if (caps < 0) caps = detect_caps();
    return caps;
}

JNIEXPORT jboolean JNICALL Java_com_beouttv_app_optical_NativeEngine_nativeBlur(JNIEnv* env, jclass, jobject bitmap, jint radius, jint passes) {
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) return JNI_FALSE;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) return JNI_FALSE;
    void* px = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &px) != ANDROID_BITMAP_RESULT_SUCCESS || !px) return JNI_FALSE;
    bool ok = blur_pixels((uint8_t*)px, (int)info.width, (int)info.height, (int)info.stride, radius, passes);
    AndroidBitmap_unlockPixels(env, bitmap);
    return ok ? JNI_TRUE : JNI_FALSE;
}

// معيار سريع (≈ بضعة ميلي ثانية): زمن تمويه صورة 96x96 بالميكروثانية، يدخل في اختيار مستوى الأداء التلقائي
JNIEXPORT jlong JNICALL Java_com_beouttv_app_optical_NativeEngine_nativeBench(JNIEnv*, jclass) {
    const int w = 96, h = 96, stride = w * 4;
    uint8_t* buf = (uint8_t*)malloc((size_t)stride * h);
    if (!buf) return -1;
    for (int i = 0; i < stride * h; i++) buf[i] = (uint8_t)(i * 31);
    struct timespec a, b;
    clock_gettime(CLOCK_MONOTONIC, &a);
    for (int k = 0; k < 6; k++) blur_pixels(buf, w, h, stride, 8, 3);
    clock_gettime(CLOCK_MONOTONIC, &b);
    free(buf);
    return (jlong)((b.tv_sec - a.tv_sec) * 1000000LL + (b.tv_nsec - a.tv_nsec) / 1000);
}

}
