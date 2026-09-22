package com.example.videodownloader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.videodownloader.ui.components.LinkInputField
import com.example.videodownloader.util.UrlParser

@Composable
fun HomeScreen(
    initialLink: String,
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
            AssistChip(
                onClick = {},
                label = { Text("Источник: ${parsed.service}") }
            )
        }

        Button(
            onClick = { onDownload(link) },
            enabled = parsed != null && link.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Скачать")
        }

        Text(
            "Скачивайте только те материалы, на которые у вас есть права. " +
            "Приложение не обходит DRM и платные подписки.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}