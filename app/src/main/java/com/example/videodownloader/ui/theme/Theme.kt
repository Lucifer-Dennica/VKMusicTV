package com.example.videodownloader.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(primary = Color(0xFF78A7FF), secondary = Color(0xFF8BD6C2), background = Color(0xFF0B1020), surface = Color(0xFF151D30))
@Composable fun VideoDownloaderTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = Dark, content = content) }
