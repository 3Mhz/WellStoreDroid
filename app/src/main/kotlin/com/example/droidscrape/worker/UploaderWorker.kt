package com.example.droidscrape.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.droidscrape.data.datastore.SettingsRepository
import com.example.droidscrape.data.local.AppDatabase
import com.example.droidscrape.network.IngestPayload
import com.example.droidscrape.network.Record
import com.example.droidscrape.network.RetrofitClient
import com.example.droidscrape.network.Sample
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Instant

class UploaderWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settingsRepository = SettingsRepository(applicationContext)
        val sampleDao = AppDatabase.getDatabase(applicationContext).sampleDao()

        val host = settingsRepository.endpointUrl.first()
        val apiKey = settingsRepository.apiKey.first()

        if (host.isBlank() || apiKey.isBlank()) {
            return Result.failure() // Configuration is missing, do not retry.
        }

        val apiService = RetrofitClient.create(apiKey)

        val pendingSamples = sampleDao.getPending(50)
        if (pendingSamples.isEmpty()) {
            return Result.success()
        }

        val deviceId = settingsRepository.deviceId.first()

        val samples = pendingSamples.map { entity ->
            val records = Json.decodeFromString<List<Record>>(entity.payloadJson)
            Sample(
                sampleId = entity.id.toString(),
                intervalStart = entity.intervalStart,
                intervalEnd = entity.intervalEnd,
                records = records
            )
        }

        val payload = IngestPayload(
            schemaVersion = 1,
            deviceId = deviceId,
            collectedAt = Instant.now().toString(),
            samples = samples
        )

        return try {
            val response = apiService.ingest(host + "/api/ingest", payload)
            if (response.isSuccessful) {
                val sentIds = pendingSamples.map { it.id }
                sampleDao.markSent(sentIds, System.currentTimeMillis())
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
