package com.example.videodownloader.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkManager
import java.io.File

@Composable
fun SettingsScreen(onChooseFolder: () -> Unit) {
    val context = LocalContext.current
    var showInstagramHelp by remember { mutableStateOf(false) }

    val cookiesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val cookiesFile = File(context.filesDir, "cookies.txt")
                inputStream?.use { input -> cookiesFile.outputStream().use { it.write(input.readBytes()) } }
                Toast.makeText(context, "Cookies загружены", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showInstagramHelp) {
        InstagramHelpScreen(onBack = { showInstagramHelp = false })
        return
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)

        Text("Качество")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("Лучшее", "1080p", "720p").forEachIndexed { i, label ->
                SegmentedButton(
                    selected = i == 0, onClick = {},
                    shape = SegmentedButtonDefaults.itemShape(i, 3)
                ) { Text(label) }
            }
        }

        Button(onClick = onChooseFolder) { Text("Выбрать папку сохранения") }

        HorizontalDivider()

        Text("Авторизация", style = MaterialTheme.typography.titleMedium)
        Text(
            "Загрузите cookies.txt для скачивания видео с ограничениями.",
            style = MaterialTheme.typography.bodySmall
        )
        Button(onClick = { cookiesPicker.launch("text/plain") }) {
            Text("Загрузить cookies.txt")
        }

        HorizontalDivider()

        Text("Справка", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { showInstagramHelp = true }) {
            Text("Как скачать видео из Instagram")
        }

        HorizontalDivider()

        Text("Очередь", style = MaterialTheme.typography.titleMedium)
        Button(onClick = {
            WorkManager.getInstance(context).cancelAllWork()
            Toast.makeText(context, "Очередь очищена", Toast.LENGTH_SHORT).show()
        }) { Text("Очистить очередь загрузок") }

        HorizontalDivider()
        Text("Правила использования", style = MaterialTheme.typography.titleMedium)
        Text(
            "Загружайте только те материалы, на которые у вас есть права.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun InstagramHelpScreen(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Как скачать видео из Instagram",
            style = MaterialTheme.typography.headlineMedium)

        Text(
            "Instagram — самая защищённая платформа. " +
            "Он блокирует автоматические запросы и требует авторизации. " +
            "Вот почему это работает не так просто, как TikTok или YouTube.",
            style = MaterialTheme.typography.bodyMedium
        )

        Text("Что делать:", style = MaterialTheme.typography.titleMedium)
        Text(
            "1. Откройте Instagram в браузере (Chrome, Firefox).\n" +
            "2. Войдите в свой аккаунт.\n" +
            "3. Установите расширение «Get cookies.txt LOCALLY».\n" +
            "4. Откройте расширение и нажмите Export.\n" +
            "5. Сохраните файл cookies.txt на телефон.\n" +
            "6. В приложении: Настройки → Загрузить cookies.txt.\n" +
            "7. Теперь можно качать публичные видео Instagram.",
            style = MaterialTheme.typography.bodyMedium
        )

        Text("Важно:", style = MaterialTheme.typography.titleMedium)
        Text(
            "• Платформа полностью безопасна — мы не передаём данные на сервер.\n" +
            "• Cookies хранятся только на вашем устройстве.\n" +
            "• Если Instagram перестанет работать — обновите cookies.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Понятно, назад")
        }
    }
}
