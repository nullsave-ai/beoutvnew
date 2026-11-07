package com.beouttv.app.data

/**
 * محتوى تجريبي ومعاينة مدمجة: يُستخدم لاختبار وتجربة التطبيق مباشرة
 * بدون الحاجة لاتصال بالخادم، ويتيح معاينة كافة الشاشات والمشغل والمواسم.
 */
object DemoData {

    val demoChannels = listOf(
        Item(
            id = "ch1",
            kind = "channel",
            title = "قناة الأخبار 1",
            description = "بث إخباري مباشر على مدار 24 ساعة",
            category = "news",
            live = true,
            online = true,
        ),
        Item(
            id = "ch2",
            kind = "channel",
            title = "قناة الأخبار 2",
            description = "متابعة الأحداث الدولية والتغطيات الحية",
            category = "news",
            live = true,
            online = true,
        ),
        Item(
            id = "ch3",
            kind = "channel",
            title = "رياضة 1 HD",
            description = "بث رياضي مباشر للمباريات والأحداث الكبرى",
            category = "sports",
            live = true,
            online = true,
        ),
        Item(
            id = "ch4",
            kind = "channel",
            title = "رياضة 2 HD",
            description = "تغطية البطولات والتحليلات الرياضية الحية",
            category = "sports",
            live = true,
            online = true,
        ),
        Item(
            id = "ch5",
            kind = "channel",
            title = "أطفال Kids",
            description = "برامج ورسوم متحركة للأطفال",
            category = "kids",
            live = true,
            online = true,
        ),
        Item(
            id = "ch6",
            kind = "channel",
            title = "موسيقى Music",
            description = "بث كليبات وموسيقى متنوعة",
            category = "music",
            live = true,
            online = true,
        ),
        Item(
            id = "ch7",
            kind = "channel",
            title = "وثائقية Doc",
            description = "أفلام وسلاسل وثائقية واستكشاف الطبيعة",
            category = "news",
            live = true,
            online = true,
        ),
        Item(
            id = "ch8",
            kind = "channel",
            title = "منوعات Live",
            description = "بث ترفيهي ومنوعات عامة",
            category = "music",
            live = true,
            online = true,
        ),
    )

    val demoMovies = listOf(
        Item(
            id = "mv1",
            kind = "movie",
            title = "المهمة المستحيلة (تجريبي)",
            description = "فيلم أكشن وحركة مليء بالإثارة والتشويق للمعاينة والتجربة.",
            category = "action",
            year = 2024,
            durationMin = 118,
            quality = "FHD",
            live = false,
            online = true,
        ),
        Item(
            id = "mv2",
            kind = "movie",
            title = "مطاردة الظل (تجريبي)",
            description = "فيلم تشويق وإثارة في شوارع المدينة الكبرى.",
            category = "action",
            year = 2023,
            durationMin = 104,
            quality = "4K",
            live = false,
            online = true,
        ),
        Item(
            id = "mv3",
            kind = "movie",
            title = "رحلة إلى الأفق (تجريبي)",
            description = "مغامرة استكشافية شيقة في أعماق الطبيعة.",
            category = "action",
            year = 2024,
            durationMin = 125,
            quality = "HD",
            live = false,
            online = true,
        ),
    )

    val demoSeries = Item(
        id = "sr1",
        kind = "series",
        title = "صراع العروش (تجريبي)",
        description = "مسلسل درامي ملحمي للاختبار والتجربة مع دعم المواسم والحلقات.",
        category = "drama",
        year = 2024,
        quality = "FHD",
        live = false,
        online = true,
    )

    private val streamMap = mapOf(
        "ch1" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        "ch2" to "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
        "ch3" to "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
        "ch4" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        "ch5" to "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
        "ch6" to "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
        "ch7" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        "ch8" to "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
        "mv1" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        "mv2" to "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
        "mv3" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        "sr1e1" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        "sr1e2" to "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
    )

    val demoSections = listOf(
        Section(
            id = "sec_channels",
            type = "channels",
            title = "القنوات المباشرة",
            items = demoChannels,
        ),
        Section(
            id = "sec_suggest",
            type = "suggest",
            title = "مقترح لك",
            items = demoMovies + listOf(demoSeries),
        ),
        Section(
            id = "sec_movies",
            type = "row",
            title = "أفلام مميزة",
            items = demoMovies,
        ),
        Section(
            id = "sec_series",
            type = "row",
            title = "مسلسلات",
            items = listOf(demoSeries),
        ),
    )

    fun home(): HomeResponse = HomeResponse(demoSections)

    fun config(): AppConfig = AppConfig(minVersion = 1, maxReconnect = 6, maintenance = null)

    fun channelCategories(): List<Category> = listOf(
        Category("news", "أخبار"),
        Category("sports", "رياضة"),
        Category("kids", "أطفال"),
        Category("music", "موسيقى"),
    )

    fun catalogCategories(kind: String): List<Category> =
        if (kind == "movie") listOf(Category("action", "أكشن"))
        else listOf(Category("drama", "دراما"))

    fun channels(category: String?, cursor: String?): Page {
        val list = if (category.isNullOrBlank()) demoChannels
        else demoChannels.filter { it.category == category }
        return Page(list, null)
    }

    fun catalog(kind: String, category: String?, cursor: String?, sort: String?): Page {
        return if (kind == "movie") {
            val list = if (category.isNullOrBlank()) demoMovies
            else demoMovies.filter { it.category == category }
            Page(list, null)
        } else {
            val list = listOf(demoSeries)
            Page(list, null)
        }
    }

    fun detail(id: String): Detail {
        if (id == "sr1" || id == "sr1e1" || id == "sr1e2") {
            return Detail(
                item = demoSeries,
                seasons = listOf(
                    Season(
                        id = "sr1s1",
                        number = 1,
                        title = "الموسم الأول",
                        episodes = listOf(
                            Episode("sr1e1", 1, "الحلقة 1 - البداية", 45),
                            Episode("sr1e2", 2, "الحلقة 2 - التحدي", 48),
                        ),
                    ),
                ),
                related = demoMovies,
            )
        }
        val mv = demoMovies.find { it.id == id }
        if (mv != null) {
            return Detail(
                item = mv,
                seasons = emptyList(),
                related = demoMovies.filter { it.id != mv.id },
            )
        }
        val ch = demoChannels.find { it.id == id } ?: demoChannels.first()
        return Detail(
            item = ch,
            seasons = emptyList(),
            related = demoChannels.filter { it.id != ch.id },
        )
    }

    fun search(query: String, type: String?, cursor: String?): Page {
        val q = query.trim()
        val all = when (type) {
            "channel" -> demoChannels
            "movie" -> demoMovies
            "series" -> listOf(demoSeries)
            else -> demoChannels + demoMovies + listOf(demoSeries)
        }
        val filtered = if (q.isBlank()) all else all.filter { it.title.contains(q, ignoreCase = true) }
        return Page(filtered, null)
    }

    fun suggest(query: String): List<String> {
        val q = query.trim()
        val all = (demoChannels + demoMovies + listOf(demoSeries)).map { it.title }
        return if (q.isBlank()) all else all.filter { it.contains(q, ignoreCase = true) }
    }

    fun play(kind: String, id: String): PlayInfo {
        val url = streamMap[id] ?: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        val live = kind == "channel"
        return PlayInfo(url = url, live = live)
    }
}
