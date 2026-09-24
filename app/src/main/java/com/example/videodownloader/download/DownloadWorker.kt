package com.example.videodownloader.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.videodownloader.data.repository.DownloadRepository
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val id = inputData.getLong(KEY_ID, 0L)

        val repository = DownloadRepository(applicationContext)
        val current = repository.getById(id)
        if (current != null) {
            repository.update(current.copy(status = "DOWNLOADING", progress = 0))
        }

        showNotification(id, "Скачивание…", 0, ongoing = true)

        return try {
            val resolved = resolveDirectUrl(url)
                ?: throw Exception("Не удалось получить ссылку на видео")

            repository.getById(id)?.let {
                repository.update(it.copy(thumbnailUrl = resolved.thumbnail, progress = 5))
            }

            val tempFile = File(applicationContext.cacheDir, "video_$id.mp4")
            downloadFile(resolved.videoUrl, tempFile, id, repository)

            val fileName = "video_${id}_${System.currentTimeMillis()}.mp4"
            val savedPath = saveToPublicDcim(tempFile, fileName)
            tempFile.delete()

            repository.getById(id)?.let {
                repository.update(it.copy(
                    filePath = savedPath,
                    status = "COMPLETED",
                    progress = 100,
                    error = null
                ))
            }
            showNotification(id, "✅ Видео скачано", 100, ongoing = false)
            cancelNotificationDelayed(id)
            Result.success(workDataOf(KEY_FILE to savedPath))
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Ошибка", e)
            repository.getById(id)?.let {
                repository.update(it.copy(status = "ERROR", error = e.message))
            }
            showNotification(id, "❌ Ошибка: ${e.message}", 0, ongoing = false)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Ошибка")))
        }
    }

    data class Resolved(val videoUrl: String, val thumbnail: String?)

    /**
     * Всё идёт через Cobalt — он умеет TikTok (включая короткие ссылки),
     * YouTube, Instagram, Facebook, Twitter, VK и десятки других сервисов.
     */
    private fun resolveDirectUrl(url: String): Resolved? = resolveCobalt(url)

    /** Универсальный метод через свой Cobalt на Railway. */
    private fun resolveCobalt(url: String): Resolved? {
        val body = """{"url":"$url","videoQuality":"720"}"""
        val response = httpPostJson(COBALT_URL, body) ?: return null
        val obj = JSONObject(response)

        // Проверка на ошибку
        val status = obj.optString("status", "")
        if (status == "error") {
            val errorObj = obj.optJSONObject("error")
            val errorMsg = errorObj?.optString("code") ?: obj.optString("error", "unknown")
            throw Exception("Cobalt: $errorMsg")
        }

        // Новый формат Cobalt: либо "url", либо "picker" с массивом
        val video = obj.optString("url").ifBlank {
            val picker = obj.optJSONArray("picker")
            if (picker != null && picker.length() > 0) {
                picker.getJSONObject(0).optString("url", "")
            } else ""
        }.ifBlank { null } ?: return null

        val thumb = obj.optString("thumbnail").ifBlank { null }
        return Resolved(video, thumb)
    }

    private fun httpPostJson(apiUrl: String, body: String): String? {
        val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val errText = conn.errorStream?.bufferedReader()?.readText().orEmpty()
                throw Exception("HTTP $code: $errText")
            }
            conn.inputStream.bufferedReader().readText()
        } finally { conn.disconnect() }
    }

    private suspend fun downloadFile(
        url: String,
        outFile: File,
        id: Long,
        repository: DownloadRepository
    ) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong
            var done = 0L
            var lastNotified = 0

            conn.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val percent = 5 + ((done * 90) / total).toInt()
                            repository.getById(id)?.let {
                                repository.update(it.copy(progress = percent))
                            }
                            if (percent - lastNotified >= 10) {
                                lastNotified = percent
                                showNotification(id, "Скачивание… $percent%", percent, ongoing = true)
                            }
                        }
                    }
                }
            }
        } finally { conn.disconnect() }
    }

    private fun saveToPublicDcim(tempFile: File, fileName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/VideoDownloader")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = applicationContext.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("Не удалось создать файл в MediaStore")

            resolver.openOutputStream(uri).use { out ->
                if (out == null) throw Exception("Не удалось открыть поток")
                tempFile.inputStream().use { input -> input.copyTo(out, 64 * 1024) }
            }

            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri.toString()
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "VideoDownloader"
            ).apply { mkdirs() }
            val target = File(dir, fileName)
            tempFile.copyTo(target, overwrite = true)
            return target.absolutePath
        }
    }

    private fun showNotification(id: Long, text: String, progress: Int, ongoing: Boolean) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "downloads"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Загрузки", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
        val notif = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("VideoDownloader")
            .setContentText(text)
            .setProgress(100, progress, progress == 0)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .build()
        manager.notify(id.toInt(), notif)
    }

    private fun cancelNotificationDelayed(id: Long) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            manager.cancel(id.toInt())
        }, 3000)
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_ID = "id"
        const val KEY_PROGRESS = "progress"
        const val KEY_FILE = "file"
        const val KEY_ERROR = "error"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        // Твой Cobalt на Railway
        private const val COBALT_URL = "https://cobalt-tools-production-e535.up.railway.app/"
    }
}
