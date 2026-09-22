package com.example.videodownloader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.videodownloader.data.local.DownloadEntity
import com.example.videodownloader.ui.components.DownloadItemCard

@Composable fun DownloadsScreen(items: List<DownloadEntity>, onDelete: (DownloadEntity) -> Unit) { Column(Modifier.fillMaxSize().padding(20.dp)) { Text("Загрузки", style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(12.dp)); if (items.isEmpty()) Text("Здесь появятся скачанные видео.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(items, key = { it.id }) { item -> DownloadItemCard(item, { onDelete(item) }) } } } }
