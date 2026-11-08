package com.beouttv.app.data

/**
 * محتوى تجريبي متكامل للمعاينة والتجربة المباشرة للتطبيق الجديد (v1.4.0)
 * يدعم الصفحة الرئيسية (Hero Pager)، القنوات المباشرة، الأفلام، المسلسلات، والمواسم.
 */
object DemoData {

    val demoChannels = listOf(
        Item(
            id = "ch1",
            kind = "channel",
            title = "قناة الأخبار 1 HD",
            description = "بث إخباري مباشر على مدار 24 ساعة لأحدث الأخبار العالمية والتغطيات الحية.",
            category = "news",
            live = true,
            online = true,
            logo = "https://picsum.photos/seed/ch1/300/300",
            poster = "https://picsum.photos/seed/ch1/300/300",
            backdrop = "https://picsum.photos/seed/ch1b/1280/720",
        ),
        Item(
            id = "ch2",
            kind = "channel",
            title = "قناة الأخبار 2 HD",
            description = "متابعة الأحداث الدولية والتقارير الاستقصائية الميدانية.",
            category = "news",
            live = true,
            online = true,
            logo = "https://picsum.photos/seed/ch2/300/300",
            poster = "https://picsum.photos/seed/ch2/300/300",
            backdrop = "https://picsum.photos/seed/ch2b/1280/720",
        ),
        Item(
            id = "ch3",
            kind = "channel",
            title = "رياضة 1 HD",
            description = "بث رياضي مباشر للمباريات والبطولات الكبرى واستوديوهات التحليل.",
            category = "sports",
            live = true,
            online = true,
            logo = "https://picsum.photos/seed/ch3/300/300",
            poster = "https://picsum.photos/seed/ch3/300/300",
            backdrop = "https://picsum.photos/seed/ch3b/1280/720",
        ),
        Item(
            id = "ch4",
            kind = "channel",
            title = "رياضة 2 HD",
            description = "تغطية شاملة لعالم كرة القدم وسباقات السرعة والرياضات العالمية.",
            category = "sports",
            live = true,
            online = true,
            logo = "https://picsum.photos/seed/ch4/300/300",
            poster = "https://picsum.photos/seed/ch4/300/300",
            backdrop = "https://picsum.photos/seed/ch4b/1280/720",
        ),
        Item(
            id = "ch5",
            kind = "channel",
            title = "أطفال Kids TV",
            description = "مسلسلات ورسوم متحركة ترفيهية وتعليمية للأطفال والناشئة.",
            category = "kids",
            live = true,
            online = true,
            logo = "https://picsum.photos/seed/ch5/300/300",
            poster = "https://picsum.photos/seed/ch5/300/300",
            backdrop = "https://picsum.photos/seed/ch5b/1280/720",
        ),
        Item(
            id = "ch6",
            kind = "channel",
            title = "موسيقى Music Plus",
            description = "أحدث الكليبات الموسيقية والحفلات الغنائية المنوعة.",
            category = "music",
            live = true,
            online = true,
            logo = "https://picsum.photos/seed/ch6/300/300",
            poster = "https://picsum.photos/seed/ch6/300/300",
            backdrop = "https://picsum.photos/seed/ch6b/1280/720",
        ),
        Item(
            id = "ch7",
            kind = "channel",
            title = "وثائقية Doc World",
            description = "أفلام وسلاسل وثائقية عن أسرار الطبيعة والعلوم والتاريخ البشري.",
            category = "news",
            live = true,
            online = true,
            logo = "https://picsum.photos/seed/ch7/300/300",
            poster = "https://picsum.photos/seed/ch7/300/300",
            backdrop = "https://picsum.photos/seed/ch7b/1280/720",
        ),
        Item(
            id = "ch8",
            kind = "channel",
            title = "منوعات Live Max",
            description = "بث برامج ترفيهية ومسابقات ومنوعات ثقافية متنوعة.",
            category = "music",
            live = true,
            online = true,
            logo = "https://picsum.photos/seed/ch8/300/300",
            poster = "https://picsum.photos/seed/ch8/300/300",
            backdrop = "https://picsum.photos/seed/ch8b/1280/720",
        ),
    )

    val demoMovies = listOf(
        Item(
            id = "mv1",
            kind = "movie",
            title = "المهمة المستحيلة: السقوط الأخير",
            description = "فيلم أكشن وتشويق وإثارة مليء بالمطاردات الخطرة في مدن العالم للمعاينة.",
            category = "action",
            year = 2024,
            durationMin = 132,
            quality = "4K HDR",
            live = false,
            online = true,
            poster = "https://picsum.photos/seed/mv1/600/900",
            backdrop = "https://picsum.photos/seed/mv1b/1280/720",
        ),
        Item(
            id = "mv2",
            kind = "movie",
            title = "مطاردة الظل في الشفق",
            description = "فيلم تشويق وغموض وألغاز تجري أحداثه في مدينة يكتنفها الغموض والأسرار.",
            category = "action",
            year = 2024,
            durationMin = 114,
            quality = "FHD",
            live = false,
            online = true,
            poster = "https://picsum.photos/seed/mv2/600/900",
            backdrop = "https://picsum.photos/seed/mv2b/1280/720",
        ),
        Item(
            id = "mv3",
            kind = "movie",
            title = "رحلة إلى أعماق الأفق",
            description = "مغامرة استكشافية ملحمية في بيئات ساحرة تختبر شجاعة المستكشفين.",
            category = "action",
            year = 2023,
            durationMin = 126,
            quality = "FHD",
            live = false,
            online = true,
            poster = "https://picsum.photos/seed/mv3/600/900",
            backdrop = "https://picsum.photos/seed/mv3b/1280/720",
        ),
    )

    val demoSeries = Item(
        id = "sr1",
        kind = "series",
        title = "صراع العروش والفرسان",
        description = "مسلسل درامي ملحمي للاختبار والتجربة مع دعم كامل للمواسم والحلقات وقوائم التشغيل.",
        category = "drama",
        year = 2024,
        quality = "4K UHD",
        live = false,
        online = true,
        poster = "https://picsum.photos/seed/sr1/600/900",
        backdrop = "https://picsum.photos/seed/sr1b/1280/720",
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
            id = "sec_hero",
            type = "hero",
            title = "أبرز الأعمال",
            items = demoMovies + listOf(demoSeries),
        ),
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
            title = "مسلسلات حصرية",
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
                            Episode("sr1e1", 1, "الحلقة 1 - بداية الصراع", 48, "https://picsum.photos/seed/ep1/600/350"),
                            Episode("sr1e2", 2, "الحلقة 2 - عاصفة المعركة", 52, "https://picsum.photos/seed/ep2/600/350"),
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
