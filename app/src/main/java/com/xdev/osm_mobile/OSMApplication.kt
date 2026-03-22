package com.xdev.osm_mobile

import android.app.Application
import com.xdev.osm_mobile.utils.SessionManager

class OSMApplication : Application() {

    companion object {
        lateinit var sessionManager: SessionManager
        lateinit var appContext: Application
    }

    override fun onCreate() {
        super.onCreate()

        appContext = this
        sessionManager = SessionManager.getInstance(this)
    }
}