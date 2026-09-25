package com.example.videodownloader.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.videodownloader.data.local.*
import kotlinx.coroutines.flow.Flow

class DownloadRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = AppDatabase.get(appContext).downloads()

    val items: Flow<List<DownloadEntity>> = dao.observeAll()

    suspend fun add(url: String, title: String) =
        dao.insert(DownloadEntity(url = url, title = title))

    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun update(item: DownloadEntity) = dao.update(item)
    suspend fun delete(item: DownloadEntity) = dao.delete(item)

    /**
     * Сканирует папку DCIM/VideoDownloader через MediaStore
     * и добавляет в базу те файлы, которых там ещё нет.
     */
    suspend fun scanFolder() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.RELATIVE_PATH
        )

        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%DCIM/VideoDownloader%")
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            appContext.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "video.mp4"
                    val dateSec = cursor.getLong(dateCol)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        mediaId
                    )
                    val uriStr = uri.toString()

                    // Проверяем, есть ли уже такая запись
                    if (dao.getByPath(uriStr) == null) {
                        dao.insert(
                            DownloadEntity(
                                url = "",
                                title = name,
                                filePath = uriStr,
                                status = "COMPLETED",
                                progress = 100,
                                createdAt = dateSec * 1000
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // игнорируем — просто не добавится ничего лишнего
        }
    }
}
