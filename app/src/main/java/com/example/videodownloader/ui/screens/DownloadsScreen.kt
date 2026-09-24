package com.example.videodownloader.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.videodownloader.data.local.DownloadEntity
import com.example.videodownloader.ui.components.DownloadItemCard
import java.io.File

@Composable
fun DownloadsScreen(
    items: List<DownloadEntity>,
    onDelete: (DownloadEntity) -> Unit
) {
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            "Мои видео",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
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
                        onOpen = { openVideo(context, it) }
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
