package com.example.videodownloader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val url: String, val title: String, val filePath: String? = null, val status: String = "QUEUED", val progress: Int = 0, val createdAt: Long = System.currentTimeMillis(), val error: String? = null)
