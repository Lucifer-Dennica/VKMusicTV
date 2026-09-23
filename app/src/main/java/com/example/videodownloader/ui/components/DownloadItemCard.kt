package com.example.videodownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.videodownloader.data.local.DownloadEntity

@Composable
fun DownloadItemCard(item: DownloadEntity, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text(item.status, color = MaterialTheme.colorScheme.secondary)

            if (item.status == "DOWNLOADING") {
                LinearProgressIndicator(
                    { item.progress / 100f },
                    Modifier.fillMaxWidth()
                )
            }

            // Показываем текст ошибки, если есть
            item.error?.let { err ->
                Spacer(Modifier.height(4.dp))
                Text(
                    err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDelete) { Text("Удалить") }
            }
        }
    }
}
