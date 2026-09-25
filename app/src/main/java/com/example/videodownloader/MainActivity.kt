package com.example.videodownloader

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.videodownloader.ui.screens.*
import com.example.videodownloader.ui.theme.VideoDownloaderTheme
import com.example.videodownloader.ui.viewmodel.DownloadViewModel

class MainActivity : ComponentActivity() {

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val shared = if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        } else ""

        setContent {
            VideoDownloaderTheme {
                VideoDownloaderRoot(shared, { folderPicker.launch(null) })
            }
        }
    }
}

@Composable
private fun VideoDownloaderRoot(
    sharedLink: String,
    onChooseFolder: () -> Unit,
    vm: DownloadViewModel = viewModel()
) {
    var tab by remember { mutableIntStateOf(0) }
    val active by vm.activeItems.collectAsState()
    val completed by vm.completedItems.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf("Главная", "Загрузки", "Настройки").forEachIndexed { i, label ->
                    NavigationBarItem(
                        selected = i == tab,
                        onClick = { tab = i },
                        icon = {},
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> HomeScreen(sharedLink, active, vm::enqueue, vm::delete)
                1 -> DownloadsScreen(completed, vm::delete, vm::rescanFolder)
                else -> SettingsScreen(onChooseFolder)
            }
        }
    }
}
