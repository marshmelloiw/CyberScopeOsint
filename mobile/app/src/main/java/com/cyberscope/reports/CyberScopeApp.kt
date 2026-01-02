package com.cyberscope.reports

import android.app.Application

class CyberScopeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: CyberScopeApp
            private set
    }
}
