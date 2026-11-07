package com.beouttv.app.core

import com.beouttv.app.data.AppConfig
import com.beouttv.app.data.HomeResponse

/**
 * مراحل بدء التشغيل. تُشتق من حالات التحميل الفعلية (لا مؤقتات ولا تأخير اصطناعي):
 *  Initializing  -> تهيئة الخدمات المحلية (الإعدادات)
 *  LoadingConfig -> الإعدادات الأساسية (محفوظة ثم بعيدة)
 *  LoadingCache  -> قراءة المحتوى المحفوظ
 *  Refreshing    -> طلب المحتوى من الخادم (لا يوجد محفوظ)
 *  Ready         -> واجهة قابلة للاستخدام (قد يستمر التحديث بالخلفية)
 *  OfflineReady  -> واجهة من المحفوظ بدون اتصال
 *  Error         -> فشل قابل للإعادة ولا يوجد أي محتوى
 */
sealed interface Startup {
    data object Initializing : Startup
    data object LoadingConfig : Startup
    data object LoadingCache : Startup
    data object Refreshing : Startup
    data object Ready : Startup
    data object OfflineReady : Startup
    data class Error(val offline: Boolean) : Startup

    val showsApp: Boolean get() = this is Ready || this is OfflineReady
}

fun deriveStartup(
    settingsLoaded: Boolean,
    config: UiState<AppConfig>,
    home: UiState<HomeResponse>,
    online: Boolean,
): Startup {
    if (!settingsLoaded) return Startup.Initializing
    return when (home) {
        is UiState.Success, is UiState.Refreshing ->
            if (online) Startup.Ready else Startup.OfflineReady
        // خادم بلا أقسام: واجهة جاهزة بحالة فارغة مصممة (ليست خطأ)
        UiState.Empty -> if (online) Startup.Ready else Startup.OfflineReady
        is UiState.Error -> Startup.Error(offline = !online)
        UiState.Offline -> Startup.Error(offline = true)
        UiState.Loading -> Startup.Refreshing
        UiState.Initial ->
            if (config is UiState.Initial || config is UiState.Loading) Startup.LoadingConfig else Startup.LoadingCache
    }
}
