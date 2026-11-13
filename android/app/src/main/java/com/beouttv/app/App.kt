package com.beouttv.app

import android.app.Application
import com.beouttv.app.core.Net
import com.beouttv.app.data.Api
import com.beouttv.app.data.Img
import com.beouttv.app.data.Prefs
import com.beouttv.app.notify.Reminders
import java.util.UUID

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Img.init(this)
        Api.init(cacheDir)
        Net.init(this)
        val sp = getSharedPreferences("core", MODE_PRIVATE)
        var id = sp.getString("device", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            sp.edit().putString("device", id).apply()
        }
        Api.deviceId = id
        Reminders.ensureChannel(this)
        Reminders.schedule(this)
    }
}
