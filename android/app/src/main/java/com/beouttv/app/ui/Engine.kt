package com.beouttv.app.ui

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.beouttv.app.data.HistoryEntry
import com.beouttv.app.data.Prefs
import com.beouttv.app.data.Repo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

/**
 * محرك التشغيل الوحيد في التطبيق: ExoPlayer + حالة Compose + إعادة اتصال + حفظ التقدم.
 *
 * يعيش خارج التركيب (LiveVM للبث المباشر، و remember للمسار VOD) فلا ينقطع التشغيل
 * بتبديل الأقسام أو الدخول والخروج من ملء الشاشة، ولا يُعاد إنشاؤه عند تغيير الاتجاه.
 * كل الحالات المعروضة (isPlaying, buffering, ...) هي Compose State تُقرأ مباشرة من الواجهة.
 */
@Stable
class PlaybackEngine(context: Context, private val scope: CoroutineScope) {

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setHandleAudioBecomingNoisy(true)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 40_000, 1_500, 3_000)
                .build(),
        )
        .build().apply {
            setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true,
            )
        }

    // ---- الحالة المعروضة ----
    var entries by mutableStateOf<List<PlayEntry>>(emptyList()); private set
    var index by mutableIntStateOf(0); private set
    var isPlaying by mutableStateOf(false); private set
    var buffering by mutableStateOf(false); private set
    var tracks by mutableStateOf(Tracks.EMPTY); private set
    var cueText by mutableStateOf(""); private set
    var videoRatio by mutableFloatStateOf(16f / 9f); private set
    var reconnecting by mutableStateOf(false); private set
    var fatal by mutableStateOf(false); private set
    var live by mutableStateOf(false); private set
    var volume by mutableFloatStateOf(1f); private set
    var speed by mutableFloatStateOf(1f); private set

    /** أقصى عدد محاولات إعادة اتصال (من إعدادات الخادم). */
    var maxRetry: Int = 6

    /** true: التحميل يجهّز الوسيط دون أن يبدأ التشغيل (المشغل المباشر وهو مخفي). */
    var holdStart: Boolean = false

    val entry: PlayEntry? get() = entries.getOrNull(index)
    val hasMedia: Boolean get() = player.currentMediaItem != null

    private var attempts = 0
    private var loadJob: Job? = null
    private var retryJob: Job? = null
    private var released = false

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(p: Boolean) { isPlaying = p }
        override fun onPlaybackStateChanged(state: Int) {
            buffering = state == Player.STATE_BUFFERING
            if (state == Player.STATE_READY) { attempts = 0; reconnecting = false; fatal = false }
            if (state == Player.STATE_ENDED) onEnded()
        }
        override fun onTracksChanged(t: Tracks) { tracks = t }
        override fun onCues(cueGroup: CueGroup) {
            cueText = cueGroup.cues.joinToString("\n") { it.text?.toString().orEmpty() }.trim()
        }
        override fun onVideoSizeChanged(v: VideoSize) {
            if (v.width > 0 && v.height > 0) videoRatio = (v.width * v.pixelWidthHeightRatio) / v.height
        }
        override fun onPlayerError(e: PlaybackException) {
            onError(e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW)
        }
    }

    init {
        player.addListener(listener)
        // الجودة القصوى من الإعدادات تُطبَّق فور تغييرها
        scope.launch {
            Prefs.settings.map { it.maxHeight }.distinctUntilChanged().collect { h ->
                val cap = if (h > 0) h else Int.MAX_VALUE
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setMaxVideoSize(Int.MAX_VALUE, cap).build()
            }
        }
        // حفظ موضع المشاهدة كل 10 ثوانٍ أثناء التشغيل (للمحتوى المسجّل فقط)
        scope.launch {
            while (true) {
                delay(10_000)
                if (!released && isPlaying) saveProgress()
            }
        }
    }

    // ---- التشغيل ----

    /** يبدأ قائمة تشغيل من العنصر at (قناة واحدة، أو حلقات متتالية). */
    fun start(list: List<PlayEntry>, at: Int = 0) {
        if (released || list.isEmpty()) return
        retryJob?.cancel()
        entries = list
        index = at.coerceIn(0, list.lastIndex)
        attempts = 0
        reconnecting = false
        load(-1L)
    }

    private fun load(keepPos: Long) {
        val e = entry ?: return
        loadJob?.cancel()
        fatal = false
        buffering = true
        loadJob = scope.launch {
            try {
                val info = Repo.play(e.kind, e.key)
                live = info.live || e.item.isChannel
                applySpeed()
                val s = Prefs.settingsNow()
                val saved = when {
                    keepPos >= 0 -> keepPos
                    !live && s.resume -> Prefs.historyEntry(e.key)?.let { h ->
                        if (h.positionMs > 5_000 && h.durationMs > 0 && h.positionMs < h.durationMs * 95 / 100) h.positionMs else 0L
                    } ?: 0L
                    else -> 0L
                }
                player.setMediaItem(MediaItem.fromUri(info.url), saved)
                player.prepare()
                player.playWhenReady = !holdStart
                if (e.item.isChannel) Prefs.setLastChannel(e.item.id)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                onError(false)
            }
        }
    }

    private fun onError(behindLive: Boolean) {
        attempts++
        if (attempts > maxRetry) { fatal = true; reconnecting = false; return }
        reconnecting = true
        retryJob?.cancel()
        retryJob = scope.launch {
            delay((1_000L * attempts).coerceAtMost(8_000L))
            if (attempts >= 2 || player.currentMediaItem == null) {
                // طلب رابط جديد: البث المباشر يبدأ من الحافة الحية، والمسجّل يستأنف من موضعه
                load(if (live) -1L else player.currentPosition)
            } else {
                if (behindLive) player.seekToDefaultPosition()
                player.prepare()
            }
        }
    }

    /** إعادة المحاولة اليدوية بعد فشل نهائي. */
    fun retry() {
        attempts = 0
        fatal = false
        reconnecting = false
        load(if (live || entry == null) -1L else player.currentPosition)
    }

    private fun onEnded() {
        val e = entry ?: return
        if (e.item.isChannel) return
        saveProgress()
        if (index < entries.lastIndex) { index++; attempts = 0; load(-1L) }
    }

    /** الانتقال في قائمة التشغيل (حلقات VOD). */
    fun go(delta: Int) {
        val n = index + delta
        if (n !in entries.indices) return
        saveProgress()
        index = n
        attempts = 0
        load(-1L)
    }

    /** تشغيل؛ في البث المباشر يقفز إلى الحافة الحية أولاً (بعد إيقاف مؤقت لا يُعرض ما فات). */
    fun play() {
        if (player.playbackState == Player.STATE_IDLE && player.currentMediaItem != null) player.prepare()
        if (live) player.seekToDefaultPosition()
        player.play()
    }

    fun pause() = player.pause()

    fun toggle() { if (player.isPlaying) pause() else play() }

    fun seekTo(ms: Long) = player.seekTo(ms)

    fun seekBy(ms: Long) {
        val d = player.duration
        player.seekTo((player.currentPosition + ms).coerceIn(0, if (d > 0) d else Long.MAX_VALUE))
    }

    @JvmName("adjustVolume")
    fun setVolume(v: Float) { volume = v.coerceIn(0f, 1f); player.volume = volume }

    @JvmName("adjustSpeed")
    fun setSpeed(s: Float) { speed = s; applySpeed() }

    /** تسريع مؤقت (ضغط مطوّل) ثم العودة للسرعة المختارة. */
    fun boost(on: Boolean) { player.setPlaybackSpeed(if (on) 2f else if (live) 1f else speed) }

    private fun applySpeed() { player.setPlaybackSpeed(if (live) 1f else speed) }

    // ---- الحفظ والتحرير ----

    fun saveProgress() {
        if (released) return
        val e = entry ?: return
        if (live || e.item.isChannel) return
        val d = player.duration
        if (d <= 0) return
        val h = HistoryEntry(e.key, e.item, e.episodeId, e.label, player.currentPosition, d, System.currentTimeMillis())
        CoroutineScope(Dispatchers.IO).launch { Repo.saveProgress(h) }
    }

    fun release() {
        if (released) return
        saveProgress()
        released = true
        loadJob?.cancel()
        retryJob?.cancel()
        player.removeListener(listener)
        player.release()
    }
}

// ---------------------------------------------------------------------------------------------
// أدوات مشتركة للمشغل
// ---------------------------------------------------------------------------------------------

internal class Opt(val label: String, val selected: Boolean, val pick: () -> Unit)

internal fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val ss = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, ss) else "%02d:%02d".format(m, ss)
}

/**
 * يحسب مقاس مربع الفيديو داخل الحاوية حسب وضع العرض. ratio = عرض/ارتفاع الفيديو (من VideoSize).
 *  fit     كامل الفيديو ظاهر بنسبته (أشرطة سوداء عند الحاجة) — الافتراضي
 *  fill    يملأ الشاشة بنسبته (قص الأطراف الزائدة)
 *  zoom    يملأ الشاشة ثم تكبير إضافي 15% (لإزالة الأشرطة المدمجة في الفيديو)
 *  stretch تمديد إلى كامل الحاوية (تشويه النسبة)
 *  auto    يملأ إن كانت نسبة الفيديو قريبة من الشاشة (فرق < 6%) وإلا ملاءمة، فلا قص ملحوظ ولا أشرطة كبيرة
 */
internal fun videoBox(mode: String, cw: Float, ch: Float, ratio: Float): Pair<Float, Float> {
    if (cw <= 0f || ch <= 0f) return 0f to 0f
    val cr = cw / ch
    fun fit() = if (cr > ratio) (ch * ratio) to ch else cw to (cw / ratio)
    fun cover() = if (cr > ratio) cw to (cw / ratio) else (ch * ratio) to ch
    return when (mode) {
        "fill" -> cover()
        "zoom" -> cover().let { it.first * 1.15f to it.second * 1.15f }
        "stretch" -> cw to ch
        "auto" -> if (abs(cr - ratio) / ratio < 0.06f) cover() else fit()
        else -> fit()
    }
}

// ---------- خيارات المسارات ----------

internal fun qualityOptions(p: ExoPlayer, tracks: Tracks): List<Opt> {
    val params = p.trackSelectionParameters
    val hasOverride = params.overrides.values.any { it.type == C.TRACK_TYPE_VIDEO }
    val out = ArrayList<Opt>()
    tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }.forEach { g ->
        for (i in 0 until g.length) {
            if (!g.isTrackSupported(i)) continue
            val h = g.getTrackFormat(i).height
            if (h <= 0) continue
            out += Opt("${h}p", hasOverride && g.isTrackSelected(i)) {
                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                    .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i)).build()
            }
        }
    }
    val sorted = out.distinctBy { it.label }.sortedByDescending { it.label.dropLast(1).toIntOrNull() ?: 0 }
    return listOf(Opt(S.auto, !hasOverride) {
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_VIDEO).build()
    }) + sorted
}

private fun trackLabel(f: Format, i: Int): String =
    f.label ?: f.language?.takeIf { it != "und" }?.let { Locale(it).getDisplayLanguage(Locale("ar")) } ?: "#${i + 1}"

internal fun audioOptions(p: ExoPlayer, tracks: Tracks): List<Opt> {
    val out = ArrayList<Opt>()
    tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }.forEach { g ->
        for (i in 0 until g.length) {
            if (!g.isTrackSupported(i)) continue
            out += Opt(trackLabel(g.getTrackFormat(i), out.size), g.isTrackSelected(i)) {
                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                    .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i)).build()
            }
        }
    }
    return out
}

internal fun subtitleOptions(p: ExoPlayer, tracks: Tracks): List<Opt> {
    val disabled = p.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
    val out = ArrayList<Opt>()
    out += Opt(S.off, disabled || tracks.groups.none { it.type == C.TRACK_TYPE_TEXT && it.isSelected }) {
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
    }
    tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }.forEach { g ->
        for (i in 0 until g.length) {
            if (!g.isTrackSupported(i)) continue
            out += Opt(trackLabel(g.getTrackFormat(i), out.size - 1), !disabled && g.isTrackSelected(i)) {
                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i)).build()
            }
        }
    }
    return if (out.size == 1) emptyList() else out
}
