package com.beouttv.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.toArgb
import com.beouttv.app.data.Prefs
import com.beouttv.app.ui.Root
import com.beouttv.app.ui.colorsFor

class MainActivity : ComponentActivity() {
    private val askNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // السمة المحفوظة تُقرأ بشكل متزامن (SharedPreferences) قبل رسم أول إطار:
        // لون النافذة وأيقونات الشريطين صحيحان من الإطار الأول بدون وميض أبيض.
        val c = colorsFor(Prefs.themeMirror(this))
        val style = if (c.dark) SystemBarStyle.dark(Color.TRANSPARENT)
        else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        window.setBackgroundDrawable(ColorDrawable(c.bgBottom.toArgb()))
        maybeAskNotificationPermission()
        setContent { Root() }
    }

    /** يُطلب الإذن مرة واحدة فقط (Android 13+). */
    private fun maybeAskNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        val sp = getSharedPreferences("core", MODE_PRIVATE)
        if (sp.getBoolean("askedNotif", false)) return
        sp.edit().putBoolean("askedNotif", true).apply()
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
