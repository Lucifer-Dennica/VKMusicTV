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
import java.net.URLEncoder

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val id = inputData.getLong(KEY_ID, 0L)

        Log.d(TAG, "=== Начало: url=$url id=$id ===")

        val repository = DownloadRepository(applicationContext)
        val current = repository.getById(id)
        if (current != null) {
            repository.update(current.copy(status = "DOWNLOADING", progress = 0))
        }

        showNotification(id, "Скачивание…", 0, ongoing = true)

        return try {
            val resolved = resolveDirectUrl(url)
                ?: throw Exception("Не удалось получить ссылку на видео")

            Log.d(TAG, "resolved: video=${resolved.videoUrl.take(80)}..., thumb=${resolved.thumbnail?.take(60)}")

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
            Log.d(TAG, "=== Готово: $savedPath ===")
            Result.success(workDataOf(KEY_FILE to savedPath))
        } catch (e: Exception) {
            Log.e(TAG, "=== Ошибка ===", e)
            repository.getById(id)?.let {
                repository.update(it.copy(status = "ERROR", error = e.message))
            }
            showNotification(id, "❌ Ошибка: ${e.message}", 0, ongoing = false)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Ошибка")))
        }
    }

    data class Resolved(val videoUrl: String, val thumbnail: String?)

    private fun resolveDirectUrl(url: String): Resolved? {
        val lower = url.lowercase()
        return if (lower.contains("tiktok.com")) {
            Log.d(TAG, "→ TikTok → tikwm")
            resolveTikTok(url)
        } else {
            Log.d(TAG, "→ Не TikTok → Cobalt")
            resolveCobalt(url)
        }
    }

    // ---------- TikTok через tikwm ----------

    private fun resolveTikTok(url: String): Resolved? {
        val api = "https://tikwm.com/api/?url=" + URLEncoder.encode(url, "UTF-8") + "&hd=1"
        Log.d(TAG, "GET $api")

        val json = httpGetString(api, timeoutMs = 20_000)
            ?: throw Exception("tikwm не ответил")

        Log.d(TAG, "tikwm ответ: ${json.take(200)}...")

        val obj = JSONObject(json)
        val code = obj.optInt("code", -1)
        if (code != 0) {
            throw Exception("tikwm: ${obj.optString("msg", "unknown error")}")
        }

        val data = obj.optJSONObject("data")
            ?: throw Exception("tikwm: нет поля data")

        // Пытаемся взять play, если нет — hdplay
        val video = data.optString("play").ifBlank { null }
            ?: data.optString("hdplay").ifBlank { null }
            ?: throw Exception("tikwm: нет ссылки на видео")

        val cover = data.optString("cover").ifBlank { null }

        Log.d(TAG, "TikTok OK: video=${video.take(60)}..., cover=${cover?.take(60)}")
        return Resolved(video, cover)
    }

    // ---------- Cobalt через перебор инстансов ----------

    private fun resolveCobalt(url: String): Resolved? {
        val instances = listOf(
            "https://cobalt-api.kwiatekmiki.com/",
            "https://co.eepy.today/",
            "https://cobalt-api.ayo.tf/",
            "https://cobalt.255x.ru/",
            "https://api.cobalt.best/",
            COBALT_URL
        )
        val body = """{"url":"$url","videoQuality":"720"}"""
        var lastError = "Нет инстансов"

        for (base in instances) {
            try {
                Log.d(TAG, "Cobalt: пробую $base")
                val response = httpPostJson(base, body, timeoutMs = 15_000) ?: continue
                val obj = JSONObject(response)
                val status = obj.optString("status", "")
                if (status == "error") {
                    val err = obj.optJSONObject("error")
                    lastError = err?.optString("code") ?: "unknown"
                    Log.w(TAG, "Cobalt $base: $lastError")
                    continue
                }
                val video = obj.optString("url").ifBlank {
                    val picker = obj.optJSONArray("picker")
                    if (picker != null && picker.length() > 0) {
                        picker.getJSONObject(0).optString("url", "")
                    } else ""
                }.ifBlank { null } ?: continue

                val thumb = obj.optString("thumbnail").ifBlank { null }
                Log.d(TAG, "Cobalt OK через $base")
                return Resolved(video, thumb)
            } catch (e: Exception) {
                lastError = e.message ?: "unknown"
                Log.w(TAG, "Cobalt $base: $lastError")
            }
        }

        throw Exception("Все Cobalt-инстансы недоступны ($lastError)")
    }

    // ---------- HTTP ----------

    private fun httpGetString(apiUrl: String, timeoutMs: Int = 20_000): String? {
        val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = timeoutMs
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            val code = conn.responseCode
            if (code !in 200..299) throw Exception("HTTP $code")
            conn.inputStream.bufferedReader().readText()
        } finally { conn.disconnect() }
    }

    private fun httpPostJson(apiUrl: String, body: String, timeoutMs: Int = 30_000): String? {
        val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = timeoutMs
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
                throw Exception("HTTP $code: ${errText.take(200)}")
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
        private const val TAG = "DownloadWorker"
        const val KEY_URL = "url"
        const val KEY_ID = "id"
        const val KEY_PROGRESS = "progress"
        const val KEY_FILE = "file"
        const val KEY_ERROR = "error"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private const val COBALT_URL = "https://cobalt-tools-production-e535.up.railway.app/"
    }
}
