package com.beouttv.app.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/** عنصر محتوى موحد: movie | series | channel | episode */
@Immutable
@Serializable
data class Item(
    val id: String,
    val kind: String,
    val title: String,
    val poster: String? = null,
    val backdrop: String? = null,
    val logo: String? = null,
    val year: Int? = null,
    val description: String? = null,
    val durationMin: Int? = null,
    val quality: String? = null,
    val category: String? = null,
    val live: Boolean = false,
    val online: Boolean = true,
    val parentId: String? = null,
) {
    val isChannel get() = kind == "channel"
    val isSeries get() = kind == "series"
}

@Immutable
@Serializable
data class Section(
    val id: String,
    /** hero | row | channels | continue | categories | suggest */
    val type: String,
    val title: String = "",
    val items: List<Item> = emptyList(),
)

@Immutable
@Serializable
data class HomeResponse(val sections: List<Section> = emptyList())

@Immutable
@Serializable
data class Category(val id: String, val name: String)

@Immutable
@Serializable
data class Page(val items: List<Item> = emptyList(), val next: String? = null)

@Immutable
@Serializable
data class Episode(
    val id: String,
    val number: Int,
    val title: String = "",
    val durationMin: Int? = null,
    val poster: String? = null,
)

@Immutable
@Serializable
data class Season(
    val id: String,
    val number: Int,
    val title: String = "",
    val episodes: List<Episode> = emptyList(),
)

@Immutable
@Serializable
data class Detail(
    val item: Item,
    val seasons: List<Season> = emptyList(),
    val related: List<Item> = emptyList(),
)

@Immutable
@Serializable
data class PlayInfo(val url: String, val live: Boolean = false)

@Immutable
@Serializable
data class AppConfig(
    val minVersion: Int = 0,
    val maxReconnect: Int = 6,
    val maintenance: String? = null,
)

@Immutable
@Serializable
data class HistoryEntry(
    val key: String,
    val item: Item,
    val episodeId: String? = null,
    val label: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val at: Long = 0,
)

@Immutable
data class Settings(
    val theme: String = "dark",      // light | dark | blue | slate | violet
    val maxHeight: Int = 0,          // 0 = تلقائي
    val resume: Boolean = true,
    val notifications: Boolean = true,
    val keepScreenOn: Boolean = true,
    val uiMode: String = "auto",     // auto | phone | tv
    val videoFit: String = "fit",    // auto | fit | fill | zoom | stretch
    val longPressBoost: Boolean = true,
    val autoPlay: Boolean = true,        // تشغيل آخر قناة تلقائياً عند فتح التطبيق
    val autoFullscreen: Boolean = true,  // ملء الشاشة تلقائياً عند تدوير الهاتف أفقياً
    val navStyle: String = "labels",     // labels | compact | icons
    val toolbarStyle: String = "pill",   // pill | circles | square
    val homeLayout: String = "hero",     // hero | rows
    val chLayout: String = "auto",       // auto | stacked | split
    val accent: String = "theme",        // theme | blue | violet | teal | rose | amber | green
    val radius: String = "m",            // s | m | l  (استدارة الزوايا)
    val glass: String = "m",             // s | m | l  (شفافية الزجاج)
    val iconStyle: String = "duo",       // line | duo | solid
    val fontScale: String = "m",         // s | m | l
    val posterSize: String = "m",        // s | m | l
    val ui: String = "glass",            // cinema | glass | ios | flat | ambient
    val depth: String = "bal",           // min | bal | ref | rich
    val shadow: String = "1",            // 0..3
    val borderW: String = "0",           // 0 | 1 | 2
    val cardDepth: String = "1",         // 0..2
    val clarity: String = "m",           // s | m | l
    val space: String = "m",             // s | m | l  (مسافات داخلية)
    val density: String = "m",           // s | m | l  (كثافة العناصر)
    val listStyle: String = "cards",     // cards | glass | grouped | plain | compact
    val cardStyle: String = "below",     // below | overlay
    val playerLayout: String = "classic",// classic | bar
)
