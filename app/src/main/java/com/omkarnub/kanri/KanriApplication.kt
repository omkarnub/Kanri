package com.omkarnub.kanri

import android.app.Application
import com.omkarnub.kanri.data.crash.CrashLogger

class KanriApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}
