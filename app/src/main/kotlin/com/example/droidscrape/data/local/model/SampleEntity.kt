package com.example.droidscrape.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "samples")
data class SampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val intervalStart: String,
    val intervalEnd: String,
    val payloadJson: String,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis(),
    var sentAt: Long? = null,
    var attemptCount: Int = 0,
    var lastError: String? = null
)
