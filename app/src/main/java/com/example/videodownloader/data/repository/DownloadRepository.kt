package com.example.videodownloader.data.repository

import android.content.Context
import com.example.videodownloader.data.local.*
import kotlinx.coroutines.flow.Flow

class DownloadRepository(context: Context) {

    private val dao = AppDatabase.get(context).downloads()

    val items: Flow<List<DownloadEntity>> = dao.observeAll()

    suspend fun add(url: String, title: String) =
        dao.insert(DownloadEntity(url = url, title = title))

    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun update(item: DownloadEntity) = dao.update(item)
    suspend fun delete(item: DownloadEntity) = dao.delete(item)
}
