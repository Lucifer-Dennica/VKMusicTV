package com.example.videodownloader.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.videodownloader.data.local.DownloadEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadItemCard(
    item: DownloadEntity,
    onDelete: () -> Unit,
    onOpen: (DownloadEntity) -> Unit
) {
    val context = LocalContext.current

    // Размер файла
    val fileSize = remember(item.filePath) {
        val path = item.filePath ?: return@remember null
        try {
            if (path.startsWith("content://")) {
                null
            } else {
                val f = File(path)
                if (f.exists()) Formatter.formatShortFileSize(context, f.length()) else null
            }
        } catch (e: Exception) { null }
    }

    // Дата скачивания
    val dateText = remember(item.createdAt) {
        SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault()).format(Date(item.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { if (item.status == "COMPLETED") onOpen(item) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Обложка во всю ширину 16:9
            if (item.thumbnailUrl != null) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎬", style = MaterialTheme.typography.displaySmall)
                }
            }

            Column(Modifier.padding(12.dp)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                // Строка с датой и размером
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    fileSize?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Статус и кнопки
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when (item.status) {
                        "QUEUED" -> "⏳ В очереди"
                        "DOWNLOADING" -> "⬇️ ${item.progress}%"
                        "COMPLETED" -> "✅ Готово"
                        "ERROR" -> "❌ Ошибка"
                        else -> item.status
                    }
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (item.status) {
                            "COMPLETED" -> MaterialTheme.colorScheme.primary
                            "ERROR" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    )

                    Row {
                        if (item.status == "COMPLETED") {
                            TextButton(onClick = { onOpen(item) }) {
                                Text("Открыть")
                            }
                        }
                        IconButton(onClick = onDelete) {
                            Text("🗑", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                // Прогресс
                if (item.status == "DOWNLOADING") {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { item.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Ошибка
                item.error?.let { err ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
