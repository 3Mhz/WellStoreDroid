package com.example.droidscrape.data.local.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleDao {
    @Insert
    suspend fun insertSample(sample: SampleEntity)

    @Query("SELECT * FROM samples WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPending(limit: Int): List<SampleEntity>

    @Query("UPDATE samples SET status = 'SENT', sentAt = :sentAt WHERE id IN (:ids)")
    suspend fun markSent(ids: List<Long>, sentAt: Long)

    @Query("UPDATE samples SET lastError = :error, attemptCount = attemptCount + 1 WHERE id = :id")
    suspend fun updateFailure(id: Long, error: String)

    @Query("SELECT COUNT(*) FROM samples WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT lastError FROM samples WHERE lastError IS NOT NULL ORDER BY createdAt DESC LIMIT 1")
    fun getLastError(): Flow<String?>
}
