package com.example.videodownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.videodownloader.data.local.DownloadEntity

@Composable
fun DownloadItemCard(
    item: DownloadEntity,
    onDelete: () -> Unit,
    onOpen: (DownloadEntity) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

            if (item.thumbnailUrl != null) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))

                val statusText = when (item.status) {
                    "QUEUED" -> "⏳ В очереди"
                    "DOWNLOADING" -> "⬇️ Скачивание ${item.progress}%"
                    "COMPLETED" -> "✅ Готово"
                    "ERROR" -> "❌ Ошибка"
                    else -> item.status
                }
                Text(
                    statusText,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall
                )

                if (item.status == "DOWNLOADING") {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { item.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item.error?.let { err ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (item.status == "COMPLETED") {
                        TextButton(onClick = { onOpen(item) }) { Text("Открыть") }
                    }
                    TextButton(onClick = onDelete) { Text("Удалить") }
                }
            }
        }
    }
}
