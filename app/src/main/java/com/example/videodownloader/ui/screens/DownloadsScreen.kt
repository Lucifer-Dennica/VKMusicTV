package com.example.videodownloader.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.storage.StorageManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.videodownloader.data.local.DownloadEntity
import com.example.videodownloader.ui.components.DownloadItemCard
import java.io.File

@Composable
fun DownloadsScreen(
    items: List<DownloadEntity>,
    onDelete: (DownloadEntity) -> Unit,
    onRescan: () -> Unit
) {
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Мои видео", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onRescan) {
                Text("🔄 Обновить")
            }
        }

        Spacer(Modifier.height(8.dp))

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📭", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Здесь появятся скачанные видео",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { it.id }) { item ->
                    DownloadItemCard(
                        item = item,
                        onDelete = { onDelete(item) },
                        onOpen = { openVideo(context, it) },
                        onOpenFolder = { openFolder(context, it) }
                    )
                }
            }
        }
    }
}

private fun openVideo(context: Context, item: DownloadEntity) {
    val path = item.filePath ?: run {
        Toast.makeText(context, "Файл не найден", Toast.LENGTH_SHORT).show()
        return
    }

    val uri: Uri = if (path.startsWith("content://")) {
        Uri.parse(path)
    } else {
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(context, "Файл не найден", Toast.LENGTH_SHORT).show()
            return
        }
        Uri.fromFile(file)
    }

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/mp4")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Не удалось открыть: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Открывает папку с видео через системный DocumentsUI.
 * Использует StorageManager.createOpenDocumentTreeIntent() — это работает
 * на Android 5+ и открывается в любом стандартном файловом менеджере.
 */
private fun openFolder(context: Context, item: DownloadEntity) {
    val service = detectService(item)
    val fullPath = "DCIM/VideoDownloader/$service"

    // Способ 1: через StorageManager — самый надёжный
    try {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val volume = storageManager.primaryStorageVolume
        val intent = volume.createOpenDocumentTreeIntent()

        val initialUri = Uri.parse(
            "content://com.android.externalstorage.documents/root/primary%3A" +
                    Uri.encode(fullPath)
        )
        intent.putExtra("android.provider.extra.INITIAL_URI", initialUri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return
    } catch (_: Exception) { }

    // Способ 2: открыть корень VideoDownloader
    try {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val volume = storageManager.primaryStorageVolume
        val intent = volume.createOpenDocumentTreeIntent()

        val rootUri = Uri.parse(
            "content://com.android.externalstorage.documents/root/primary%3A" +
                    Uri.encode("DCIM/VideoDownloader")
        )
        intent.putExtra("android.provider.extra.INITIAL_URI", rootUri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return
    } catch (_: Exception) { }

    // Способ 3: просто открыть выбор папок
    try {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val volume = storageManager.primaryStorageVolume
        val intent = volume.createOpenDocumentTreeIntent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Toast.makeText(
            context,
            "Найдите папку: $fullPath",
            Toast.LENGTH_LONG
        ).show()
        return
    } catch (_: Exception) { }

    // Если ничего не сработало — показываем путь
    Toast.makeText(
        context,
        "Путь: $fullPath",
        Toast.LENGTH_LONG
    ).show()
}

private fun detectService(item: DownloadEntity): String {
    val url = item.url.lowercase()
    val title = item.title.lowercase()
    val path = (item.filePath ?: "").lowercase()
    return when {
        url.contains("tiktok") || title.contains("tiktok") || path.contains("tiktok") -> "TikTok"
        url.contains("youtube") || url.contains("youtu.be") ||
                title.contains("youtube") || path.contains("youtube") -> "YouTube"
        url.contains("instagram") || title.contains("instagram") ||
                path.contains("instagram") -> "Instagram"
        url.contains("facebook") || url.contains("fb.watch") ||
                title.contains("facebook") || path.contains("facebook") -> "Facebook"
        url.contains("vk.com") || title.contains("vk") || path.contains("vk") -> "VK"
        url.contains("twitter") || url.contains("x.com") ||
                title.contains("twitter") || path.contains("twitter") -> "Twitter"
        url.contains("reddit") || path.contains("reddit") -> "Reddit"
        url.contains("pinterest") || url.contains("pin.it") ||
                path.contains("pinterest") -> "Pinterest"
        url.contains("snapchat") || path.contains("snapchat") -> "Snapchat"
        url.contains("vimeo") || path.contains("vimeo") -> "Vimeo"
        url.contains("dailymotion") || path.contains("dailymotion") -> "Dailymotion"
        url.contains("twitch") || path.contains("twitch") -> "Twitch"
        url.contains("rutube") || path.contains("rutube") -> "Rutube"
        url.contains("soundcloud") || path.contains("soundcloud") -> "SoundCloud"
        else -> "Другое"
    }
}
