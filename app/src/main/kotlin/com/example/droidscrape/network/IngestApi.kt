package com.example.droidscrape.network

import kotlinx.serialization.Serializable

// --- Request Payloads ---

@Serializable
data class IngestPayload(
    val schemaVersion: Int,
    val deviceId: String,
    val collectedAt: String,
    val samples: List<Sample>
)

@Serializable
data class Sample(
    val sampleId: String,
    val intervalStart: String,
    val intervalEnd: String,
    val records: List<Record>
)

@Serializable
data class Record(
    val packageName: String,
    val appLabel: String,
    val usageMillisIncrement: Long
)

// --- Response Payload ---

@Serializable
data class IngestResponse(
    val ok: Boolean,
    val duplicates: Int,
    val insertedSamples: Int,
    val insertedRecords: Int
)
