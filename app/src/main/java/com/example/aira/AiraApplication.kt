package com.example.aira

import android.app.Application
import com.example.aira.data.preferences.PreferencesManager
import com.example.aira.utils.NotificationHelper

class AiraApplication : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesManager = PreferencesManager(this)
        NotificationHelper.createNotificationChannel(this)
    }

    companion object {
        lateinit var instance: AiraApplication
            private set
    }
}
