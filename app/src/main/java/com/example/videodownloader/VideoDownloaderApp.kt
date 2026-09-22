package com.example.videodownloader

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.yausername.youtubedl_android.YoutubeDL

class VideoDownloaderApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration =
        Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
            Log.d("VideoDownloader", "YoutubeDL инициализирован")
        } catch (e: Exception) {
            Log.e("VideoDownloader", "Ошибка инициализации YoutubeDL", e)
        }
    }
}