package com.example.videodownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.videodownloader.data.local.DownloadEntity

@Composable fun DownloadItemCard(item: DownloadEntity, onDelete: () -> Unit) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(item.title, style = MaterialTheme.typography.titleMedium); Text(item.status, color = MaterialTheme.colorScheme.secondary); if (item.status == "DOWNLOADING") LinearProgressIndicator({ item.progress / 100f }, Modifier.fillMaxWidth()); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onDelete) { Text("Удалить") } } } } }
