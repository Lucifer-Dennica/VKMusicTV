package com.example.videodownloader

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager

class VideoDownloaderApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration =
        Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Форсируем инициализацию WorkManager
        WorkManager.getInstance(this)
    }
}
