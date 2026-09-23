package com.example.videodownloader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.videodownloader.data.local.DownloadEntity
import com.example.videodownloader.ui.components.LinkInputField
import com.example.videodownloader.util.UrlParser

@Composable
fun HomeScreen(
    initialLink: String,
    activeItems: List<DownloadEntity>,
    onDownload: (String) -> Unit
) {
    var link by remember(initialLink) { mutableStateOf(initialLink) }
    val parsed = UrlParser.parse(link)

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("VideoDownloader", style = MaterialTheme.typography.headlineLarge)
        Text("Вставьте ссылку на видео из TikTok, YouTube, Instagram или прямую ссылку на файл.")

        LinkInputField(link) { link = it }

        if (parsed != null) {
            AssistChip(onClick = {}, label = { Text("Источник: ${parsed.service}") })
        }

        Button(
            onClick = { onDownload(link); link = "" },
            enabled = parsed != null && link.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Скачать") }

        Text(
            "Скачивайте только те материалы, на которые у вас есть права.",
            style = MaterialTheme.typography.bodySmall
        )

        if (activeItems.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Активные загрузки", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(activeItems, key = { it.id }) { item ->
                    ActiveDownloadCard(item)
                }
            }
        }
    }
}

@Composable
private fun ActiveDownloadCard(item: DownloadEntity) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))

            when (item.status) {
                "QUEUED" -> Text("⏳ В очереди", style = MaterialTheme.typography.bodySmall)
                "DOWNLOADING" -> {
                    Text("⬇️ ${item.progress}%", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { item.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                "ERROR" -> Text(
                    "❌ ${item.error ?: "Ошибка"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
