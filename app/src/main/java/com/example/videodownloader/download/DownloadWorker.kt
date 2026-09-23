package com.example.videodownloader.download

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.videodownloader.data.repository.DownloadRepository
import kotlinx.coroutines.flow.first
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

        val repository = DownloadRepository(applicationContext)
        val current = repository.items.first().firstOrNull { it.id == id }
        current?.let { repository.update(it.copy(status = "DOWNLOADING", progress = 0)) }

        val dir = File(applicationContext.getExternalFilesDir("Movies"), "VideoDownloader")
            .apply { mkdirs() }

        return try {
            val directUrl = resolveDirectUrl(url)
                ?: throw Exception("Не удалось получить ссылку на видео")

            val outFile = File(dir, "video_${id}.mp4")
            downloadFile(directUrl, outFile)

            current?.let {
                repository.update(it.copy(
                    filePath = outFile.absolutePath,
                    status = "COMPLETED",
                    progress = 100,
                    error = null
                ))
            }
            Result.success(workDataOf(KEY_FILE to outFile.absolutePath))
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Ошибка", e)
            current?.let {
                repository.update(it.copy(status = "ERROR", error = e.message))
            }
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Ошибка")))
        }
    }

    private fun resolveDirectUrl(url: String): String? {
        return when {
            url.contains("tiktok.com") -> resolveTikTok(url)
            url.contains("instagram.com") -> resolveInstagram(url)
            else -> resolveCobalt(url)
        }
    }

    /** TikTok через tikwm.com — уже проверено, работает. */
    private fun resolveTikTok(url: String): String? {
        val api = "https://tikwm.com/api/?url=" + URLEncoder.encode(url, "UTF-8")
        val json = httpGetString(api) ?: return null
        val obj = JSONObject(json)
        if (obj.optInt("code", -1) != 0) {
            throw Exception("tikwm: " + obj.optString("msg"))
        }
        return obj.getJSONObject("data").optString("play").ifBlank { null }
    }

    /** Instagram — пробуем через Cobalt (иногда работает для публичных). */
    private fun resolveInstagram(url: String): String? {
        return try {
            resolveCobalt(url)
        } catch (e: Exception) {
            throw Exception(
                "Instagram требует авторизации. " +
                "Откройте «Настройки → Как скачать из Instagram» и следуйте инструкции."
            )
        }
    }

    /** Универсальный метод через Cobalt API. */
    private fun resolveCobalt(url: String): String? {
        val api = "https://api.cobalt.tools/api/json"
        val body = """{"url":"$url","vQuality":"720","isAudioOnly":false}"""
        val response = httpPostJson(api, body) ?: return null
        val obj = JSONObject(response)
        if (obj.optString("status") == "error") {
            throw Exception("Cobalt: " + obj.optString("text", "error"))
        }
        return obj.optString("url").ifBlank { null }
    }

    private fun httpGetString(apiUrl: String): String? {
        val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().readText()
        } finally { conn.disconnect() }
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
            if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().readText()
        } finally { conn.disconnect() }
    }

    private fun downloadFile(url: String, outFile: File) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode}")
            conn.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output, 64 * 1024)
                }
            }
        } finally { conn.disconnect() }
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_ID = "id"
        const val KEY_PROGRESS = "progress"
        const val KEY_FILE = "file"
        const val KEY_ERROR = "error"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
