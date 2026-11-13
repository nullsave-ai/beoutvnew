package com.beouttv.app.data

/**
 * نظام المظهر: كل خيار مفتاح نصي واحد له قيمة افتراضية. الأنماط (ui) حزم قيم جاهزة تُكتب على المفاتيح
 * ثم يعدّلها المستخدم بحرية. الملفات الشخصية (Presets) لقطات كاملة من هذه المفاتيح.
 */
/** وصف خيار رقمي/اختياري: يُستخدم لتوليد المفتاح وقيمته الافتراضية وواجهة الاستوديو معاً. */
class Spec(val key: String, val label: String, val group: String, val min: Float, val max: Float, val step: Float = 1f, val opts: List<String>? = null) {
    val toggle get() = opts == null && min == 0f && max == 1f && step == 1f
}

object AP {
    val def: LinkedHashMap<String, String> = linkedMapOf(
        "theme" to "dark", "accent" to "theme", "ui" to "glass",
        "depth" to "bal", "shadow" to "1", "borderW" to "0", "cardDepth" to "1", "clarity" to "m", "glass" to "m",
        "space" to "m", "density" to "m", "radius" to "m", "posterSize" to "m",
        "listStyle" to "cards", "cardStyle" to "below", "homeLayout" to "hero", "chLayout" to "auto",
        "navStyle" to "labels", "toolbarStyle" to "pill", "playerLayout" to "classic",
        "iconStyle" to "duo", "fontScale" to "m",
        // وضع التصميم: original (الحالي) | optical (التجربة البصرية)
        "mode" to "original", "fullOptical" to "0", "glassPreset" to "pure", "perf" to "auto",
        "carPreset" to "classic3d", "cardTarget" to "all",
        "motion" to "smooth", "mSpeed" to "100", "mIntensity" to "100", "mDamp" to "100", "reduceMotion" to "0",
        "bgMode" to "gradient", "bgC1" to "", "bgC2" to "", "bgDim" to "0", "bgImg" to "", "bgBlur" to "40",
        "fontFam" to "default", "fontW" to "0", "fontTitle" to "100", "fontBody" to "100", "fontSmall" to "100",
        "letterSp" to "0", "lineH" to "100", "textA" to "100",
        "iconSize" to "m", "iconW" to "m", "iconActive" to "accent",
        "layoutMode" to "balanced", "hPad" to "16", "secGap" to "14", "cardGap" to "10", "gridCols" to "0", "contentW" to "0",
        "btnStyle" to "default", "dlgStyle" to "sheet", "srchStyle" to "glass",
        "navSize" to "100", "navAlpha" to "100", "tbSize" to "100", "tbAlpha" to "100",
        "ct_bg" to "", "ct_bg2" to "", "ct_surface" to "", "ct_text" to "", "ct_accent" to "", "ct_glow" to "", "ct_refl" to "", "ct_border" to "",
    ).also { m ->
        GLASS_SPECS.forEach { m[it.key] = "" }
        CAROUSEL_SPECS.forEach { m[it.key] = "" }
        CARD_TYPES.forEach { t -> CARD_SPECS.forEach { m["card_${t}_${it.key}"] = "" } }
    }
    val keys get() = def.keys

    /** مجموعات إعادة الضبط. */
    val groups: Map<String, List<String>> get() = _groups
    private val _groups = mapOf(
        "theme" to listOf("theme", "accent"),
        "style" to listOf("ui", "depth", "shadow", "borderW", "cardDepth", "clarity", "glass", "space", "density", "radius", "posterSize", "listStyle", "cardStyle", "homeLayout", "navStyle", "toolbarStyle", "playerLayout", "iconStyle"),
        "nav" to listOf("navStyle", "toolbarStyle"),
        "depth" to listOf("depth", "shadow", "borderW", "cardDepth", "clarity", "glass"),
        "layout" to listOf("homeLayout", "chLayout", "listStyle", "cardStyle", "space", "density", "radius", "posterSize"),
        "player" to listOf("playerLayout"),
        "text" to listOf("iconStyle", "fontScale", "fontFam", "fontW", "fontTitle", "fontBody", "fontSmall", "letterSp", "lineH", "textA", "iconSize", "iconW", "iconActive"),
        "mode" to listOf("mode", "fullOptical", "perf"),
        "glass" to listOf("glassPreset") + GLASS_SPECS.map { it.key },
        "carousel" to listOf("carPreset") + CAROUSEL_SPECS.map { it.key },
        "cards" to CARD_TYPES.flatMap { t -> CARD_SPECS.map { "card_${t}_${it.key}" } },
        "anim" to listOf("motion", "mSpeed", "mIntensity", "mDamp", "reduceMotion"),
        "bg" to listOf("bgMode", "bgC1", "bgC2", "bgDim", "bgImg", "bgBlur"),
        "buttons" to listOf("btnStyle", "dlgStyle", "srchStyle"),
        "layoutx" to listOf("layoutMode", "hPad", "secGap", "cardGap", "gridCols", "contentW", "space", "density"),
        "navx" to listOf("navStyle", "toolbarStyle", "navSize", "navAlpha", "tbSize", "tbAlpha"),
        "ctheme" to listOf("ct_bg", "ct_bg2", "ct_surface", "ct_text", "ct_accent", "ct_glow", "ct_refl", "ct_border"),
        "all" to emptyList(),
    )

    /** مجموعة مفاتيح إعادة الضبط الكلية للتصميم (كل المفاتيح). */
    fun allKeys(): List<String> = def.keys.toList()

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

// ===================== مواصفات الزجاج البصري =====================

private fun g(k: String, l: String, grp: String, mn: Float = 0f, mx: Float = 100f, st: Float = 1f) = Spec("g_$k", l, grp, mn, mx, st)

val GLASS_SPECS: List<Spec> = listOf(
    g("opacity", "شفافية الزجاج", "الشفافية"), g("surface", "شفافية السطح", "الشفافية"),
    g("layer", "عدد الطبقات", "الشفافية", 1f, 4f), g("bgT", "شفافية الخلفية", "الشفافية"),
    g("refr", "شدة الانكسار", "الانكسار"), g("refrR", "نصف قطر الانكسار", "الانكسار"),
    g("refrDir", "اتجاه الانكسار (درجة)", "الانكسار", 0f, 360f, 5f), g("edgeRefr", "انكسار الحواف", "الانكسار"),
    g("refl", "شدة الانعكاس", "الانعكاس"), g("spec", "اللمعان المركّز", "الانعكاس"),
    g("sharp", "حدة الانعكاس", "الانعكاس"), g("reflDir", "اتجاه الانعكاس (درجة)", "الانعكاس", 0f, 180f, 5f),
    g("blur", "نصف قطر التمويه", "التمويه والانتشار"), g("diff", "انتشار السطح", "التمويه والانتشار"), g("frost", "التصنيف (Frost)", "التمويه والانتشار"),
    g("chroma", "الزيغ اللوني", "التأثيرات البصرية"), g("disp", "التشتت", "التأثيرات البصرية"), g("distort", "تشويه الضوء", "التأثيرات البصرية"),
    g("fres", "تأثير فرينل", "التأثيرات البصرية"), g("glow", "توهج الحواف", "التأثيرات البصرية"), g("amb", "الإضاءة المحيطية", "التأثيرات البصرية"),
    g("bw", "سماكة الحد", "تفاصيل السطح"), g("ba", "شفافية الحد", "تفاصيل السطح"), g("corner", "انحناء الزوايا", "تفاصيل السطح"),
    g("depth", "عمق السطح", "تفاصيل السطح"), g("hiPos", "موضع اللمعة", "تفاصيل السطح"),
    g("shadow", "شدة الظل", "تفاصيل السطح"), g("inner", "الظل الداخلي", "تفاصيل السطح"), g("outer", "الظل الخارجي", "تفاصيل السطح"),
)

val GLASS_PRESETS = listOf(
    "pure" to "Pure Aero Glass", "crystal" to "Crystal Glass", "frosted" to "Frosted Glass", "liquid" to "Liquid Glass",
    "optical" to "Optical Glass", "deep" to "Deep Glass", "dark" to "Dark Glass", "soft" to "Soft Glass",
    "chromatic" to "Chromatic Glass", "custom" to "Custom Glass",
)

private val glassBase = mapOf(
    "g_opacity" to 40f, "g_surface" to 30f, "g_layer" to 2f, "g_bgT" to 50f, "g_refr" to 45f, "g_refrR" to 40f, "g_refrDir" to 135f, "g_edgeRefr" to 40f,
    "g_refl" to 40f, "g_spec" to 40f, "g_sharp" to 50f, "g_reflDir" to 60f, "g_blur" to 40f, "g_diff" to 30f, "g_frost" to 30f,
    "g_chroma" to 15f, "g_disp" to 20f, "g_distort" to 15f, "g_fres" to 45f, "g_glow" to 25f, "g_amb" to 35f,
    "g_bw" to 8f, "g_ba" to 40f, "g_corner" to 50f, "g_depth" to 40f, "g_hiPos" to 30f, "g_shadow" to 35f, "g_inner" to 25f, "g_outer" to 35f,
)

/** القيم الأساسية لنمط زجاج (القيم الناقصة تؤخذ من الأساس). */
fun glassPreset(name: String): Map<String, Float> = glassBase + when (name) {
    "crystal" -> mapOf("g_opacity" to 22f, "g_surface" to 15f, "g_refr" to 70f, "g_refl" to 60f, "g_spec" to 70f, "g_sharp" to 85f, "g_frost" to 8f, "g_fres" to 65f, "g_bw" to 10f, "g_ba" to 60f, "g_chroma" to 25f, "g_glow" to 30f)
    "frosted" -> mapOf("g_opacity" to 55f, "g_surface" to 45f, "g_frost" to 80f, "g_diff" to 75f, "g_blur" to 70f, "g_refr" to 15f, "g_refl" to 18f, "g_spec" to 20f, "g_sharp" to 20f, "g_chroma" to 0f, "g_glow" to 10f)
    "liquid" -> mapOf("g_opacity" to 34f, "g_refr" to 80f, "g_refrR" to 70f, "g_edgeRefr" to 75f, "g_refl" to 55f, "g_spec" to 65f, "g_sharp" to 40f, "g_distort" to 60f, "g_fres" to 60f, "g_corner" to 70f, "g_chroma" to 30f, "g_depth" to 55f)
    "optical" -> mapOf("g_opacity" to 30f, "g_refr" to 85f, "g_refrR" to 60f, "g_edgeRefr" to 80f, "g_refl" to 50f, "g_spec" to 55f, "g_chroma" to 55f, "g_disp" to 55f, "g_fres" to 75f, "g_glow" to 40f, "g_layer" to 3f, "g_ba" to 55f)
    "deep" -> mapOf("g_opacity" to 48f, "g_surface" to 40f, "g_layer" to 4f, "g_depth" to 80f, "g_inner" to 55f, "g_outer" to 60f, "g_shadow" to 60f, "g_amb" to 50f, "g_refr" to 40f, "g_frost" to 45f)
    "dark" -> mapOf("g_opacity" to 62f, "g_surface" to 60f, "g_frost" to 55f, "g_refl" to 30f, "g_spec" to 35f, "g_amb" to 55f, "g_glow" to 35f, "g_ba" to 55f, "g_bw" to 9f, "g_fres" to 55f, "g_chroma" to 8f, "g_depth" to 55f)
    "soft" -> mapOf("g_opacity" to 36f, "g_refr" to 20f, "g_refl" to 22f, "g_spec" to 20f, "g_sharp" to 25f, "g_fres" to 25f, "g_glow" to 12f, "g_chroma" to 0f, "g_bw" to 5f, "g_ba" to 25f, "g_depth" to 25f, "g_shadow" to 20f, "g_inner" to 12f, "g_outer" to 20f)
    "chromatic" -> mapOf("g_chroma" to 85f, "g_disp" to 80f, "g_refr" to 65f, "g_distort" to 45f, "g_fres" to 65f, "g_glow" to 45f, "g_edgeRefr" to 70f, "g_ba" to 55f)
    else -> emptyMap()
}

// ===================== مواصفات الكاروسيل ثلاثي الأبعاد =====================

private fun c(k: String, l: String, grp: String, mn: Float, mx: Float, st: Float = 1f, o: List<String>? = null) = Spec("c_$k", l, grp, mn, mx, st, o)

val CAROUSEL_SPECS: List<Spec> = listOf(
    c("orient", "الاتجاه", "التخطيط", 0f, 1f, 1f, listOf("أفقي", "عمودي")),
    c("visible", "عدد البطاقات الظاهرة", "التخطيط", 1f, 9f), c("side", "عدد البطاقات الجانبية", "التخطيط", 0f, 4f),
    c("spacing", "المسافة بين البطاقات %", "التخطيط", 0f, 140f, 2f), c("overlap", "التداخل %", "التخطيط", 0f, 90f, 2f),
    c("centerPos", "موضع البطاقة المركزية", "التخطيط", 0f, 100f), c("w", "عرض البطاقة", "التخطيط", 110f, 340f, 2f), c("h", "ارتفاع البطاقة", "التخطيط", 150f, 480f, 2f),
    c("align", "المحاذاة", "التخطيط", 0f, 2f, 1f, listOf("بداية", "وسط", "نهاية")),
    c("rotX", "دوران X", "منظور 3D", -60f, 60f), c("rotY", "دوران Y", "منظور 3D", 0f, 85f), c("rotZ", "دوران Z", "منظور 3D", -25f, 25f),
    c("persp", "عمق المنظور", "منظور 3D", 4f, 40f), c("cam", "مسافة الكاميرا", "منظور 3D", 4f, 40f),
    c("zTrans", "إزاحة Z", "منظور 3D", 0f, 100f), c("sideDepth", "عمق البطاقات الجانبية", "منظور 3D", 0f, 100f), c("elev", "ارتفاع المركزية", "منظور 3D", 0f, 40f),
    c("cScale", "حجم المركزية %", "الحجم والرؤية", 80f, 140f), c("sScale", "حجم الجانبية %", "الحجم والرؤية", 40f, 100f), c("fScale", "حجم البعيدة %", "الحجم والرؤية", 20f, 90f),
    c("cAlpha", "شفافية المركزية %", "الحجم والرؤية", 40f, 100f), c("sAlpha", "شفافية الجانبية %", "الحجم والرؤية", 10f, 100f),
    c("blur", "تمويه حسب البعد", "الحجم والرؤية", 0f, 16f), c("dim", "تعتيم حسب الموقع %", "الحجم والرؤية", 0f, 90f),
    c("sens", "حساسية السحب", "الحركة", 0.3f, 3f, 0.1f), c("dur", "مدة الانتقال (ms)", "الحركة", 150f, 1000f, 10f),
    c("stiff", "صلابة النابض", "الحركة", 80f, 1500f, 10f), c("damp", "التخميد %", "الحركة", 30f, 120f),
    c("inertia", "القصور الذاتي", "الحركة", 0f, 100f), c("snap", "قوة الالتصاق", "الحركة", 10f, 100f),
    c("decel", "التباطؤ", "الحركة", 0f, 100f), c("drag", "مقاومة السحب", "الحركة", 0f, 80f),
    c("over", "سلوك تجاوز الحد", "الحركة", 0f, 2f, 1f, listOf("بلا", "مطاطي", "ارتداد")),
    c("tap", "نقر للتوسيط", "التفاعل", 0f, 1f), c("swipe", "التنقل بالسحب", "التفاعل", 0f, 1f), c("long", "ضغطة مطولة (فتح)", "التفاعل", 0f, 1f),
    c("auto", "تشغيل تلقائي", "التفاعل", 0f, 1f), c("autoSpd", "سرعة التشغيل التلقائي (ث)", "التفاعل", 2f, 12f),
    c("pause", "إيقاف عند اللمس", "التفاعل", 0f, 1f), c("loop", "تكرار لا نهائي", "التفاعل", 0f, 1f), c("haptic", "اهتزاز لمسي", "التفاعل", 0f, 1f),
    c("corner", "انحناء البطاقة", "المظهر", 0f, 44f), c("border", "الحد", "المظهر", 0f, 6f), c("shadow", "الظل", "المظهر", 0f, 30f),
    c("glass", "مادة زجاجية للنص %", "المظهر", 0f, 100f), c("refl", "انعكاس %", "المظهر", 0f, 100f), c("hi", "لمعة المركزية %", "المظهر", 0f, 100f),
    c("bgDepth", "عمق الخلفية %", "المظهر", 0f, 100f),
    c("imgMode", "وضع الصورة", "المظهر", 0f, 2f, 1f, listOf("قص", "احتواء", "ملء")),
    c("imgPos", "موضع الصورة", "المظهر", 0f, 2f, 1f, listOf("أعلى", "وسط", "أسفل")),
    c("textPos", "موضع النص", "المظهر", 0f, 3f, 1f, listOf("أسفل", "أعلى", "تحت البطاقة", "مخفي")),
    c("grad", "تدرج التغطية %", "المظهر", 0f, 100f),
)

val CAROUSEL_PRESETS = listOf(
    "classic3d" to "Classic 3D", "coverflow" to "Cover Flow", "cinematic" to "Cinematic Depth", "floating" to "Floating Cards",
    "stacked" to "Stacked Carousel", "arc" to "Arc Carousel", "cylinder" to "Cylinder Carousel", "gallery" to "Perspective Gallery",
    "liquid" to "Liquid Carousel", "minimal" to "Minimal Carousel", "vertical" to "Vertical Carousel", "custom" to "Custom Carousel",
)

private val carBase = mapOf(
    "c_orient" to 0f, "c_visible" to 5f, "c_side" to 2f, "c_spacing" to 58f, "c_overlap" to 0f, "c_centerPos" to 50f, "c_w" to 230f, "c_h" to 330f, "c_align" to 1f,
    "c_rotX" to 0f, "c_rotY" to 38f, "c_rotZ" to 0f, "c_persp" to 14f, "c_cam" to 14f, "c_zTrans" to 30f, "c_sideDepth" to 30f, "c_elev" to 14f,
    "c_cScale" to 100f, "c_sScale" to 82f, "c_fScale" to 66f, "c_cAlpha" to 100f, "c_sAlpha" to 78f, "c_blur" to 0f, "c_dim" to 25f,
    "c_sens" to 1f, "c_dur" to 420f, "c_stiff" to 380f, "c_damp" to 85f, "c_inertia" to 55f, "c_snap" to 70f, "c_decel" to 50f, "c_drag" to 10f, "c_over" to 1f,
    "c_tap" to 1f, "c_swipe" to 1f, "c_long" to 1f, "c_auto" to 0f, "c_autoSpd" to 5f, "c_pause" to 1f, "c_loop" to 1f, "c_haptic" to 0f,
    "c_corner" to 24f, "c_border" to 0f, "c_shadow" to 12f, "c_glass" to 35f, "c_refl" to 30f, "c_hi" to 30f, "c_bgDepth" to 45f,
    "c_imgMode" to 0f, "c_imgPos" to 1f, "c_textPos" to 0f, "c_grad" to 55f,
)

/** نوع حساب المواضع لكل نمط (يحدد الصيغة الهندسية). */
fun carKind(p: String) = when (p) { "custom" -> "classic3d"; else -> p }

fun carPreset(name: String): Map<String, Float> = carBase + when (name) {
    "coverflow" -> mapOf("c_spacing" to 38f, "c_overlap" to 35f, "c_rotY" to 62f, "c_sScale" to 88f, "c_sAlpha" to 90f, "c_zTrans" to 45f, "c_dim" to 15f, "c_w" to 210f, "c_h" to 300f, "c_refl" to 55f)
    "cinematic" -> mapOf("c_spacing" to 64f, "c_rotY" to 22f, "c_sScale" to 72f, "c_fScale" to 52f, "c_sAlpha" to 62f, "c_blur" to 6f, "c_dim" to 45f, "c_sideDepth" to 70f, "c_zTrans" to 60f, "c_w" to 250f, "c_h" to 360f, "c_elev" to 24f, "c_grad" to 70f, "c_bgDepth" to 70f)
    "floating" -> mapOf("c_spacing" to 78f, "c_rotY" to 10f, "c_rotZ" to 4f, "c_sScale" to 88f, "c_shadow" to 24f, "c_elev" to 22f, "c_dim" to 10f, "c_corner" to 30f, "c_hi" to 45f, "c_sideDepth" to 55f)
    "stacked" -> mapOf("c_spacing" to 14f, "c_rotY" to 0f, "c_rotZ" to 3f, "c_sScale" to 90f, "c_fScale" to 80f, "c_sAlpha" to 80f, "c_dim" to 30f, "c_visible" to 4f, "c_side" to 3f, "c_zTrans" to 40f, "c_loop" to 0f)
    "arc" -> mapOf("c_spacing" to 60f, "c_rotY" to 0f, "c_rotZ" to 14f, "c_sScale" to 86f, "c_sAlpha" to 82f, "c_zTrans" to 50f, "c_sideDepth" to 60f, "c_w" to 200f, "c_h" to 290f, "c_dim" to 20f)
    "cylinder" -> mapOf("c_spacing" to 80f, "c_rotY" to 40f, "c_sScale" to 90f, "c_sAlpha" to 90f, "c_zTrans" to 55f, "c_persp" to 20f, "c_cam" to 20f, "c_dim" to 35f, "c_w" to 200f, "c_h" to 300f)
    "gallery" -> mapOf("c_spacing" to 52f, "c_rotY" to 48f, "c_rotX" to 4f, "c_sScale" to 84f, "c_sAlpha" to 92f, "c_overlap" to 12f, "c_zTrans" to 35f, "c_w" to 190f, "c_h" to 280f, "c_visible" to 7f, "c_side" to 3f, "c_dim" to 18f, "c_align" to 1f)
    "liquid" -> mapOf("c_stiff" to 170f, "c_damp" to 52f, "c_inertia" to 80f, "c_snap" to 45f, "c_spacing" to 56f, "c_rotY" to 30f, "c_sScale" to 84f, "c_corner" to 34f, "c_dur" to 700f, "c_decel" to 30f, "c_hi" to 45f)
    "minimal" -> mapOf("c_rotY" to 0f, "c_rotX" to 0f, "c_spacing" to 52f, "c_sScale" to 88f, "c_fScale" to 80f, "c_sAlpha" to 55f, "c_dim" to 0f, "c_shadow" to 4f, "c_elev" to 4f, "c_corner" to 18f, "c_cam" to 8f, "c_persp" to 8f, "c_zTrans" to 10f, "c_sideDepth" to 10f, "c_refl" to 8f, "c_hi" to 10f, "c_dur" to 300f, "c_stiff" to 520f, "c_damp" to 100f, "c_glass" to 20f, "c_grad" to 40f)
    "vertical" -> mapOf("c_orient" to 1f, "c_rotY" to 0f, "c_rotX" to 34f, "c_spacing" to 40f, "c_w" to 280f, "c_h" to 150f, "c_sScale" to 86f, "c_visible" to 5f, "c_dim" to 25f, "c_sideDepth" to 40f)
    else -> emptyMap()
}

// ===================== مواصفات البطاقات حسب النوع =====================

val CARD_TYPES = listOf("media", "featured", "channel", "small", "large")
val CARD_TYPE_NAMES = mapOf(
    "all" to "كل الأنواع", "media" to "بطاقات الوسائط", "featured" to "البطاقات المميزة", "channel" to "بطاقات القنوات",
    "small" to "بطاقات صغيرة (متابعة المشاهدة)", "large" to "بطاقات كبيرة (الرئيسية)",
)
val CARD_SPECS: List<Spec> = listOf(
    Spec("r", "انحناء الزوايا", "البطاقات", 0f, 44f), Spec("sh", "الظل", "البطاقات", 0f, 3f), Spec("bd", "الحد", "البطاقات", 0f, 4f),
    Spec("op", "الشفافية %", "البطاقات", 30f, 100f), Spec("tp", "موضع النص", "البطاقات", 0f, 2f, 1f, listOf("تحت", "فوق الصورة", "مخفي")),
    Spec("ts", "حجم الخط %", "البطاقات", 70f, 140f), Spec("pd", "المسافة الداخلية", "البطاقات", 0f, 24f),
    Spec("gr", "تدرج التغطية %", "البطاقات", 0f, 100f), Spec("rf", "انعكاس %", "البطاقات", 0f, 100f), Spec("sz", "الحجم %", "البطاقات", 60f, 150f),
)

// ===================== مكتبة الأنماط الجاهزة (لا تُعدَّل مباشرة) =====================

val BUILTIN_PRESETS: Map<String, Map<String, String>> = linkedMapOf(
    "Default Optical" to mapOf(
        "mode" to "optical", "fullOptical" to "0", "glassPreset" to "pure", "theme" to "dark", "accent" to "gold", "ui" to "glass",
        "navStyle" to "aero", "toolbarStyle" to "pill", "homeLayout" to "carousel", "carPreset" to "classic3d", "bgMode" to "optical", "perf" to "auto", "listStyle" to "glass",
    ),
    "Full Optical Glass" to mapOf(
        "mode" to "optical", "fullOptical" to "1", "glassPreset" to "optical", "theme" to "dark", "accent" to "gold", "ui" to "ambient",
        "navStyle" to "aero", "toolbarStyle" to "island", "homeLayout" to "carousel", "carPreset" to "coverflow", "bgMode" to "optical", "perf" to "full", "listStyle" to "glass", "motion" to "spring",
    ),
    "Crystal Experience" to mapOf(
        "mode" to "optical", "fullOptical" to "1", "glassPreset" to "crystal", "theme" to "blue", "accent" to "cyan", "ui" to "glass",
        "navStyle" to "aero", "toolbarStyle" to "pill", "homeLayout" to "carousel", "carPreset" to "gallery", "bgMode" to "optical", "perf" to "advanced", "listStyle" to "glass",
    ),
    "Liquid Experience" to mapOf(
        "mode" to "optical", "fullOptical" to "1", "glassPreset" to "liquid", "theme" to "violet", "accent" to "pink", "ui" to "ambient",
        "navStyle" to "bubble", "toolbarStyle" to "island", "homeLayout" to "carousel", "carPreset" to "liquid", "bgMode" to "optical", "perf" to "advanced", "motion" to "spring", "listStyle" to "glass",
    ),
    "Cinematic Experience" to mapOf(
        "mode" to "optical", "fullOptical" to "0", "glassPreset" to "dark", "theme" to "amoled", "accent" to "gold", "ui" to "cinema",
        "navStyle" to "aero", "toolbarStyle" to "ghost", "homeLayout" to "carousel", "carPreset" to "cinematic", "bgMode" to "blurred", "perf" to "balanced", "listStyle" to "plain",
    ),
    "Minimal Experience" to mapOf(
        "mode" to "optical", "fullOptical" to "0", "glassPreset" to "soft", "theme" to "charcoal", "accent" to "mono", "ui" to "flat",
        "navStyle" to "text", "toolbarStyle" to "ghost", "homeLayout" to "carousel", "carPreset" to "minimal", "bgMode" to "solid", "perf" to "balanced", "motion" to "minimal", "listStyle" to "plain",
    ),
    "Dark Glass Experience" to mapOf(
        "mode" to "optical", "fullOptical" to "1", "glassPreset" to "dark", "theme" to "graphite", "accent" to "amber", "ui" to "glass",
        "navStyle" to "aero", "toolbarStyle" to "pill", "homeLayout" to "carousel", "carPreset" to "floating", "bgMode" to "optical", "perf" to "advanced", "listStyle" to "glass",
    ),
    "Light Glass Experience" to mapOf(
        "mode" to "optical", "fullOptical" to "1", "glassPreset" to "frosted", "theme" to "ash", "accent" to "orange", "ui" to "glass",
        "navStyle" to "aero", "toolbarStyle" to "pill", "homeLayout" to "carousel", "carPreset" to "classic3d", "bgMode" to "gradient", "perf" to "advanced", "listStyle" to "glass",
    ),
)
