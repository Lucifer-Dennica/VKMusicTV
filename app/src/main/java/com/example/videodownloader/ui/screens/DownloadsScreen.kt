package com.example.videodownloader.ui.screens

import android.content.Intent
import android.net.Uri
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
                        onOpenFolder = { openFolder(context) }
                    )
                }
            }
        }
    }
}

private fun openVideo(context: android.content.Context, item: DownloadEntity) {
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
 * Открывает папку DCIM/VideoDownloader напрямую через ExternalStorageProvider.
 * Это работает на всех Android 5+, независимо от MediaStore-URI.
 */
private fun openFolder(context: android.content.Context) {
    // URI папки через встроенный ExternalStorageProvider
    val folderUri = Uri.parse(
        "content://com.android.externalstorage.documents/document/primary%3ADCIM%2FVideoDownloader"
    )

    // Вариант 1: открыть как директорию в файловом менеджере
    val intent1 = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(folderUri, "vnd.android.document/directory")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent1)
        return
    } catch (_: Exception) { }

    // Вариант 2: открыть через ACTION_OPEN_DOCUMENT_TREE (выбор папки)
    val intent2 = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent2)
        Toast.makeText(
            context,
            "Выберите папку DCIM → VideoDownloader",
            Toast.LENGTH_LONG
        ).show()
        return
    } catch (_: Exception) { }

    Toast.makeText(
        context,
        "Не удалось открыть папку. Откройте вручную: DCIM/VideoDownloader",
        Toast.LENGTH_LONG
    ).show()
}
