package com.example.videodownloader.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable fun LinkInputField(value: String, onValueChange: (String) -> Unit) { OutlinedTextField(value, onValueChange, modifier = Modifier.fillMaxWidth(), label = { androidx.compose.material3.Text("Ссылка на видео") }, placeholder = { androidx.compose.material3.Text("Вставьте публичную ссылку или прямой URL .mp4") }, singleLine = false) }
