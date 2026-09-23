package com.example.videodownloader.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.videodownloader.data.local.DownloadEntity
import com.example.videodownloader.data.repository.DownloadRepository
import com.example.videodownloader.download.DownloadWorker
import com.example.videodownloader.util.UrlParser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class DownloadViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = DownloadRepository(app)

    val items = repo.items.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun enqueue(raw: String) {
        val parsed = UrlParser.parse(raw) ?: return
        viewModelScope.launch {
            val id = repo.add(parsed.value, "Видео • ${parsed.service}")
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(
                    workDataOf(
                        DownloadWorker.KEY_URL to parsed.value,
                        DownloadWorker.KEY_ID to id
                    )
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    10,
                    TimeUnit.SECONDS
                )
                .build()
            WorkManager.getInstance(getApplication()).enqueue(request)
        }
    }

    fun delete(item: DownloadEntity) = viewModelScope.launch {
        repo.delete(item)
    }
}
