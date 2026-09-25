package com.example.videodownloader.util

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.videodownloader.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    // ⚠️ ЗАМЕНИ на свой GitHub-логин и название репозитория
    private const val GITHUB_REPO = "VKMusicTV/VKMusicTV"
    private const val TAG = "UpdateChecker"

    data class UpdateInfo(
        val newVersion: String,
        val downloadUrl: String,
        val releaseNotes: String
    )

    /** Проверяет последний релиз на GitHub. Возвращает info, если есть обновление. */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val api = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
            val conn = (URL(api).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "VideoDownloader-App")
            }

            if (conn.responseCode !in 200..299) {
                Log.w(TAG, "GitHub API вернул ${conn.responseCode}")
                conn.disconnect()
                return@withContext null
            }

            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val obj = JSONObject(json)
            val tag = obj.optString("tag_name", "").removePrefix("v").trim()
            val releaseNotes = obj.optString("body", "").trim()

            // Ищем APK в assets
            var downloadUrl = ""
            val assets = obj.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            if (tag.isBlank() || downloadUrl.isBlank()) {
                Log.d(TAG, "Нет тега или APK в релизе")
                return@withContext null
            }

            val currentVersion = BuildConfig.VERSION_NAME
            if (isNewer(tag, currentVersion)) {
                Log.d(TAG, "Доступно обновление: $currentVersion → $tag")
                UpdateInfo(tag, downloadUrl, releaseNotes)
            } else {
                Log.d(TAG, "Обновлений нет (current=$currentVersion, latest=$tag)")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки обновлений", e)
            null
        }
    }

    /** Сравнивает "1.4.0" и "1.3.2" — возвращает true, если latest новее. */
    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        val size = maxOf(l.size, c.size)
        for (i in 0 until size) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }

    /** Показывает диалог с предложением обновиться. */
    fun showUpdateDialog(context: Context, info: UpdateInfo) {
        AlertDialog.Builder(context)
            .setTitle("Доступно обновление ${info.newVersion}")
            .setMessage(
                if (info.releaseNotes.isNotBlank())
                    info.releaseNotes.take(500)
                else
                    "Рекомендуется установить новую версию приложения."
            )
            .setPositiveButton("Скачать") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (_: Exception) { }
            }
            .setNegativeButton("Позже", null)
            .show()
    }
}
