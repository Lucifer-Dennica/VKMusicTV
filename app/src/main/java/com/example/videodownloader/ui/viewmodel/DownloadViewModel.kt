package com.example.videodownloader.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.videodownloader.data.local.DownloadEntity
import com.example.videodownloader.data.repository.DownloadRepository
import com.example.videodownloader.download.DownloadWorker
import com.example.videodownloader.util.UrlParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DownloadViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = DownloadRepository(app); val items = repo.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun enqueue(raw: String) { val parsed = UrlParser.parse(raw) ?: return; viewModelScope.launch { val id = repo.add(parsed.value, "Видео • ${parsed.service}"); val request = OneTimeWorkRequestBuilder<DownloadWorker>().setInputData(workDataOf(DownloadWorker.KEY_URL to parsed.value, DownloadWorker.KEY_ID to id)).build(); WorkManager.getInstance(getApplication()).enqueue(request) } }
    fun delete(item: DownloadEntity) = viewModelScope.launch { repo.delete(item) }
}
