package com.example.videodownloader.download

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.videodownloader.data.repository.DownloadRepository
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.flow.first
import java.io.File

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val id = inputData.getLong(KEY_ID, 0L)

        val repository = DownloadRepository(applicationContext)
        val current = repository.items.first().firstOrNull { it.id == id }

        current?.let { repository.update(it.copy(status = "DOWNLOADING", progress = 0)) }

        val dir = File(
            applicationContext.getExternalFilesDir("Movies"),
            "VideoDownloader"
        ).apply { mkdirs() }

        val request = YoutubeDLRequest(url)

        // Куда сохранять
        request.addOption("-o", "${dir.absolutePath}/%(title)s.%(ext)s")

        // Формат: лучшее видео + аудио, склейка через FFmpeg
        request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best")

        // Отключаем плейлисты (качаем только одно видео)
        request.addOption("--no-playlist")

        // Безопасные имена файлов
        request.addOption("--restrict-filenames")

        // TikTok — без водяного знака
        if (url.contains("tiktok.com")) {
            request.addOption("--no-watermark")
        }

        // Cookies.txt (если пользователь загрузил)
        val cookiesFile = File(applicationContext.filesDir, "cookies.txt")
        if (cookiesFile.exists() && cookiesFile.length() > 0) {
            request.addOption("--cookies", cookiesFile.absolutePath)
        }

        return try {
            var lastFile: String? = null

            YoutubeDL.getInstance().execute(request, null) { progress, _, line ->
                try {
                    setProgress(workDataOf(KEY_PROGRESS to progress.toInt()))
                } catch (_: Exception) {}
                if (line.contains("[download] Destination:")) {
                    lastFile = line.substringAfter("Destination:").trim()
                }
            }

            current?.let {
                repository.update(
                    it.copy(
                        filePath = lastFile ?: dir.absolutePath,
                        status = "COMPLETED",
                        progress = 100,
                        error = null
                    )
                )
            }
            Result.success(workDataOf(KEY_FILE to (lastFile ?: dir.absolutePath)))
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Ошибка скачивания", e)
            current?.let {
                repository.update(
                    it.copy(
                        status = "ERROR",
                        error = e.message ?: "Ошибка загрузки"
                    )
                )
            }
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Ошибка загрузки")))
        }
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_ID = "id"
        const val KEY_PROGRESS = "progress"
        const val KEY_FILE = "file"
        const val KEY_ERROR = "error"
    }
}