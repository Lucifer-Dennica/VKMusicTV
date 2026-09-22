package com.example.videodownloader.util

import android.net.Uri

data class ParsedUrl(val value: String, val host: String, val service: String, val isDirectMedia: Boolean)

object UrlParser {
    fun parse(raw: String): ParsedUrl? {
        val value = raw.trim(); val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
        val host = uri.host?.lowercase() ?: return null
        if (uri.scheme !in listOf("http", "https")) return null
        val service = when { "youtube" in host || host == "youtu.be" -> "YouTube"; "tiktok" in host -> "TikTok"; "instagram" in host -> "Instagram"; else -> host }
        val direct = uri.path?.lowercase()?.let { it.endsWith(".mp4") || it.endsWith(".webm") || it.endsWith(".mov") } == true
        return ParsedUrl(value, host, service, direct)
    }
}
