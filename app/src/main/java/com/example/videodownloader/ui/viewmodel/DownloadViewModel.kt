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
                10, java.util.concurrent.TimeUnit.SECONDS
            )
            .build()
        WorkManager.getInstance(getApplication()).enqueue(request)
    }
}
