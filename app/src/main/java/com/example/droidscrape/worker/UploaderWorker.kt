package com.example.droidscrape.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.droidscrape.WellStoreApp
import com.example.droidscrape.data.remote.ApiClient
import com.example.droidscrape.data.remote.dto.RecordDto
import com.example.droidscrape.data.remote.dto.SampleDto
import com.example.droidscrape.data.remote.dto.UploadRequest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class UploaderWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as WellStoreApp
        val settingsRepository = app.settingsRepository
        val sampleDao = app.database.sampleDao()

        val endpointUrl = settingsRepository.endpointUrl.first()
        if (endpointUrl.isNullOrEmpty()) {
            return Result.failure()
        }

        val pendingSamples = sampleDao.getPending(100)
        if (pendingSamples.isEmpty()) {
            return Result.success()
        }

        val deviceId = settingsRepository.deviceId.first()
        val collectedAt = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)

        val sampleDtos = pendingSamples.map {
            val records = Json.decodeFromString<List<RecordDto>>(it.payloadJson)
            SampleDto(it.intervalStart, it.intervalEnd, records)
        }
        val uploadRequest = UploadRequest(deviceId = deviceId, collectedAt = collectedAt, samples = sampleDtos)

        try {
            val response = ApiClient.uploadService.upload(endpointUrl, uploadRequest)
            if (response.isSuccessful) {
                sampleDao.markSent(pendingSamples.map { it.id }, System.currentTimeMillis())
                settingsRepository.setLastSuccessfulUploadTime(System.currentTimeMillis())
                return Result.success()
            } else {
                pendingSamples.forEach { sampleDao.updateFailure(it.id, response.message()) }
                return Result.retry()
            }
        } catch (e: Exception) {
            pendingSamples.forEach { sampleDao.updateFailure(it.id, e.message ?: "Unknown error") }
            return Result.retry()
        }
    }
}
