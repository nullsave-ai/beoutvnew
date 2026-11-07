package com.beouttv.app.ui

import com.beouttv.app.data.HistoryEntry

/** ما يمكن متابعته: أفلام وحلقات بدأت ولم تنتهِ (لا قنوات). تستخدمها صفحة الأفلام. */
internal fun inProgress(h: List<HistoryEntry>): List<HistoryEntry> =
    h.filter { it.item.kind != "channel" && it.durationMs > 0 && it.positionMs in 1 until (it.durationMs * 95 / 100) }
        .distinctBy { it.item.id }
