package com.beouttv.app.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Icon
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.beouttv.app.MainActivity
import com.beouttv.app.data.Prefs
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** إشعارات تذكير محلية بالكامل: رسائل مخزنة داخل التطبيق، جدولة WorkManager، بدون شبكة. */
object Reminders {
    private const val CHANNEL = "reminders"
    private const val WORK = "daily_reminder"

    /** رسائل قصيرة بلا رموز تعبيرية. التدوير بالتسلسل يمنع التكرار المتتالي. */
    val messages = listOf(
        "هناك محتوى جديد بانتظارك",
        "عد لمتابعة مشاهدتك",
        "اكتشف ما أضيف حديثاً",
        "قناتك المفضلة بانتظارك",
        "وقت مناسب لمشاهدة شيء جديد",
        "تابع من حيث توقفت",
    )

    fun ensureChannel(c: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = c.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL) == null)
                nm.createNotificationChannel(NotificationChannel(CHANNEL, "تذكيرات المشاهدة", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    fun schedule(c: Context) {
        // مرة يومياً مع نافذة مرونة 8 ساعات لتخفيف الاستيقاظ
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS, 8, TimeUnit.HOURS)
            .setInitialDelay(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun apply(c: Context, on: Boolean) {
        if (on) schedule(c) else WorkManager.getInstance(c).cancelUniqueWork(WORK)
    }

    internal fun smallIcon(): Icon {
        val b = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        val cv = Canvas(b)
        cv.drawPath(Path().apply { moveTo(30f, 18f); lineTo(78f, 48f); lineTo(30f, 78f); close() }, p)
        return Icon.createWithBitmap(b)
    }
}

class ReminderWorker(private val c: Context, params: WorkerParameters) : CoroutineWorker(c, params) {
    override suspend fun doWork(): Result {
        if (!Prefs.settingsNow().notifications) return Result.success()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour !in 10..22) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 &&
            c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        Reminders.ensureChannel(c)
        val text = Reminders.messages[Prefs.nextMessageIndex(Reminders.messages.size)]
        val pi = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(c, "reminders") else @Suppress("DEPRECATION") Notification.Builder(c)
        val n = b.setSmallIcon(Reminders.smallIcon()).setContentTitle("BEOUT TV").setContentText(text)
            .setContentIntent(pi).setAutoCancel(true).build()
        c.getSystemService(NotificationManager::class.java).notify(1001, n)
        return Result.success()
    }
}
