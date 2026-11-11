package com.beouttv.app.data

/**
 * نظام المظهر: كل خيار مفتاح نصي واحد له قيمة افتراضية. الأنماط (ui) حزم قيم جاهزة تُكتب على المفاتيح
 * ثم يعدّلها المستخدم بحرية. الملفات الشخصية (Presets) لقطات كاملة من هذه المفاتيح.
 */
object AP {
    val def = linkedMapOf(
        "theme" to "dark", "accent" to "theme", "ui" to "glass",
        "depth" to "bal", "shadow" to "1", "borderW" to "0", "cardDepth" to "1", "clarity" to "m", "glass" to "m",
        "space" to "m", "density" to "m", "radius" to "m", "posterSize" to "m",
        "listStyle" to "cards", "cardStyle" to "below", "homeLayout" to "hero", "chLayout" to "auto",
        "navStyle" to "labels", "toolbarStyle" to "pill", "playerLayout" to "classic",
        "iconStyle" to "duo", "fontScale" to "m",
    )
    val keys get() = def.keys

    /** مجموعات إعادة الضبط. */
    val groups = mapOf(
        "theme" to listOf("theme", "accent"),
        "style" to listOf("ui", "depth", "shadow", "borderW", "cardDepth", "clarity", "glass", "space", "density", "radius", "posterSize", "listStyle", "cardStyle", "homeLayout", "navStyle", "toolbarStyle", "playerLayout", "iconStyle"),
        "nav" to listOf("navStyle", "toolbarStyle"),
        "depth" to listOf("depth", "shadow", "borderW", "cardDepth", "clarity", "glass"),
        "layout" to listOf("homeLayout", "chLayout", "listStyle", "cardStyle", "space", "density", "radius", "posterSize"),
        "player" to listOf("playerLayout"),
        "text" to listOf("iconStyle", "fontScale"),
    )

    /** القيم الافتراضية لكل نمط واجهة: تغيّر الشكل والتخطيط وطريقة عرض العناصر لا الألوان فقط. */
    fun styleBundle(ui: String): Map<String, String> = when (ui) {
        // سينمائي بسيط: صور كبيرة، صفوف بلا حاويات، عناصر تحكم قليلة
        "cinema" -> mapOf(
            "ui" to "cinema", "depth" to "min", "shadow" to "0", "borderW" to "0", "cardDepth" to "0", "clarity" to "s", "glass" to "s",
            "space" to "l", "density" to "m", "radius" to "m", "posterSize" to "l", "listStyle" to "plain", "cardStyle" to "overlay",
            "homeLayout" to "hero", "navStyle" to "icons", "toolbarStyle" to "ghost", "playerLayout" to "bar", "iconStyle" to "line",
        )
        // زجاج انتقائي: شفافية خفيفة وعمق ناعم
        "glass" -> mapOf(
            "ui" to "glass", "depth" to "bal", "shadow" to "1", "borderW" to "0", "cardDepth" to "1", "clarity" to "m", "glass" to "m",
            "space" to "m", "density" to "m", "radius" to "m", "posterSize" to "m", "listStyle" to "glass", "cardStyle" to "below",
            "homeLayout" to "hero", "navStyle" to "labels", "toolbarStyle" to "pill", "playerLayout" to "classic", "iconStyle" to "duo",
        )
        // مستوحى من iOS: مجموعات مرتبة، مساحات مريحة، شريط تبويب كلاسيكي
        "ios" -> mapOf(
            "ui" to "ios", "depth" to "min", "shadow" to "0", "borderW" to "0", "cardDepth" to "0", "clarity" to "m", "glass" to "s",
            "space" to "l", "density" to "m", "radius" to "m", "posterSize" to "m", "listStyle" to "grouped", "cardStyle" to "below",
            "homeLayout" to "rows", "navStyle" to "ios", "toolbarStyle" to "bar", "playerLayout" to "bar", "iconStyle" to "line",
        )
        // مسطح حديث: كثافة أعلى، زوايا صغيرة، بطاقات خفيفة
        "flat" -> mapOf(
            "ui" to "flat", "depth" to "min", "shadow" to "0", "borderW" to "0", "cardDepth" to "0", "clarity" to "l", "glass" to "s",
            "space" to "s", "density" to "s", "radius" to "s", "posterSize" to "s", "listStyle" to "compact", "cardStyle" to "below",
            "homeLayout" to "rows", "navStyle" to "dock", "toolbarStyle" to "square", "playerLayout" to "classic", "iconStyle" to "solid",
        )
        // عمق محيطي: طبقات وظلال ناعمة وعناصر عائمة
        else -> mapOf(
            "ui" to "ambient", "depth" to "rich", "shadow" to "3", "borderW" to "0", "cardDepth" to "2", "clarity" to "l", "glass" to "l",
            "space" to "m", "density" to "m", "radius" to "l", "posterSize" to "m", "listStyle" to "cards", "cardStyle" to "overlay",
            "homeLayout" to "hero", "navStyle" to "bubble", "toolbarStyle" to "island", "playerLayout" to "classic", "iconStyle" to "duo",
        )
    }

    /** درجات عمق الواجهة: تكتب المفاتيح المرتبطة بالعمق دفعة واحدة. */
    fun depthBundle(d: String): Map<String, String> = when (d) {
        "min" -> mapOf("depth" to "min", "shadow" to "0", "cardDepth" to "0", "clarity" to "s", "glass" to "s")
        "ref" -> mapOf("depth" to "ref", "shadow" to "2", "cardDepth" to "2", "clarity" to "m", "glass" to "m")
        "rich" -> mapOf("depth" to "rich", "shadow" to "3", "cardDepth" to "2", "clarity" to "l", "glass" to "l")
        else -> mapOf("depth" to "bal", "shadow" to "1", "cardDepth" to "1", "clarity" to "m", "glass" to "m")
    }
}
