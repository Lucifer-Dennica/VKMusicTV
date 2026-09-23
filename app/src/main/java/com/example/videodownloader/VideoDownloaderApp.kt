package com.example.videodownloader

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoDownloaderApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration =
        Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Форсируем инициализацию WorkManager сразу при старте,
        // иначе первая задача может застрять в QUEUED
        WorkManager.getInstance(this)

        try {
            YoutubeDL.getInstance().init(this)
            Log.d("VideoDownloader", "YoutubeDL инициализирован")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(this@VideoDownloaderApp)
                    Log.d("VideoDownloader", "yt-dlp обновлён")
                } catch (e: Exception) {
                    Log.e("VideoDownloader", "Не удалось обновить yt-dlp", e)
                }
            }
        } catch (e: Exception) {
            Log.e("VideoDownloader", "Ошибка инициализации YoutubeDL", e)
        }
    }
}
