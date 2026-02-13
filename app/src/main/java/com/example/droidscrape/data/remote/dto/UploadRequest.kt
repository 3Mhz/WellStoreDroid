package com.example.droidscrape.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UploadRequest(
    val schemaVersion: Int = 1,
    val deviceId: String,
    val collectedAt: String,
    val samples: List<SampleDto>
)

@Serializable
data class SampleDto(
    val intervalStart: String,
    val intervalEnd: String,
    val records: List<RecordDto>
)

@Serializable
data class RecordDto(
    val packageName: String,
    val usageMillisIncrement: Long,
    val appLabel: String?
)
