package com.example.droidscrape.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.droidscrape.data.datastore.SettingsRepository
import com.example.droidscrape.data.local.AppDatabase
import com.example.droidscrape.data.local.model.SampleEntity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class CollectorWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settingsRepository = SettingsRepository(applicationContext)
        val sampleDao = AppDatabase.getDatabase(applicationContext).sampleDao()
        val usageStatsCollector = UsageStatsCollector(applicationContext)

        val lastCollectionTime = settingsRepository.lastCollectionTime.first() ?: (System.currentTimeMillis() - 15 * 60 * 1000)
        val currentTime = System.currentTimeMillis()

        val records = usageStatsCollector.getUsageStats(lastCollectionTime, currentTime)

        if (records.isNotEmpty()) {
            val payloadJson = Json.encodeToString(records)
            val sample = SampleEntity(
                intervalStart = formatTime(lastCollectionTime),
                intervalEnd = formatTime(currentTime),
                payloadJson = payloadJson
            )
            sampleDao.insertSample(sample)
            settingsRepository.setLastCollectionTime(currentTime)

            val uploadWorkRequest = OneTimeWorkRequestBuilder<UploaderWorker>().build()
            WorkManager.getInstance(applicationContext).enqueue(uploadWorkRequest)
        }

        return Result.success()
    }

    private fun formatTime(time: Long): String {
        return Instant.ofEpochMilli(time).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
    }
}
