package com.example.videodownloader.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun SettingsScreen(onChooseFolder: () -> Unit) {
    val context = LocalContext.current

    val cookiesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val cookiesFile = File(context.filesDir, "cookies.txt")
                inputStream?.use { input ->
                    cookiesFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(context, "Cookies загружены", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)

        Text("Качество")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("Лучшее", "1080p", "720p").forEachIndexed { i, label ->
                SegmentedButton(
                    selected = i == 0,
                    onClick = {},
                    shape = SegmentedButtonDefaults.itemShape(i, 3)
                ) { Text(label) }
            }
        }

        Button(onClick = onChooseFolder) {
            Text("Выбрать папку сохранения")
        }

        HorizontalDivider()

        Text("Авторизация", style = MaterialTheme.typography.titleMedium)
        Text(
            "Загрузите cookies.txt, чтобы скачивать видео из своих аккаунтов " +
            "(приватные видео, подписки, возрастные ограничения).",
            style = MaterialTheme.typography.bodySmall
        )
        Button(onClick = { cookiesPicker.launch("text/plain") }) {
            Text("Загрузить cookies.txt")
        }

        HorizontalDivider()

        Text("Правила использования", style = MaterialTheme.typography.titleMedium)
        Text(
            "Загружайте только те материалы, на которые у вас есть права. " +
            "Приложение не обходит DRM и платные подписки.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}