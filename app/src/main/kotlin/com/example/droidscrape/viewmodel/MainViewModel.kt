package com.example.droidscrape.viewmodel

import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.droidscrape.data.datastore.SettingsRepository
import com.example.droidscrape.network.IngestPayload
import com.example.droidscrape.network.Record
import com.example.droidscrape.network.RetrofitClient
import com.example.droidscrape.network.Sample
import com.example.droidscrape.worker.UsageStatsCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val collector = UsageStatsCollector(application)

    private val _testConnectionResult = MutableStateFlow<String?>(null)
    val testConnectionResult: StateFlow<String?> = _testConnectionResult.asStateFlow()

    fun sendTestRequest() {
        viewModelScope.launch {
            _testConnectionResult.value = "Checking permissions and data..."
            
            if (!hasUsageStatsPermission()) {
                _testConnectionResult.value = "Error: Usage Access permission not granted. Please enable it in Android Settings."
                return@launch
            }

            _testConnectionResult.value = "Sending..."
            try {
                val apiKey = settingsRepository.apiKey.first()
                var endpointUrl = settingsRepository.endpointUrl.first().trim()
                val deviceId = settingsRepository.ensureDeviceId()
                
                if (!endpointUrl.startsWith("http://") && !endpointUrl.startsWith("https://")) {
                    endpointUrl = "http://$endpointUrl"
                }
                
                val normalizedUrl = endpointUrl.removeSuffix("/")
                val fullUrl = "$normalizedUrl/api/ingest"

                // Collect real data for the last 15 minutes
                val endTime = System.currentTimeMillis()
                val startTime = endTime - (15 * 60 * 1000)
                val realRecords = collector.getUsageStats(startTime, endTime)
                    .filter { it.usageMillisIncrement > 0 } // Only send apps that were actually used

                if (realRecords.isEmpty()) {
                    _testConnectionResult.value = "Success! (But no app usage found in last 15m)"
                    // We still send the empty payload to test connectivity
                }

                val apiService = RetrofitClient.create(apiKey)
                val testPayload = createTestPayload(deviceId, realRecords, startTime, endTime)
                val response = apiService.ingest(fullUrl, testPayload)

                if (response.isSuccessful) {
                    _testConnectionResult.value = "Success! Code: ${response.code()} (${realRecords.size} records sent)"
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    _testConnectionResult.value = "Error: ${response.code()} - $errorMsg"
                }
            } catch (e: Exception) {
                _testConnectionResult.value = "Exception: ${e.message}"
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getApplication<Application>().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.noteOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            getApplication<Application>().packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun createTestPayload(
        deviceId: String, 
        records: List<Record>,
        startTimeMillis: Long,
        endTimeMillis: Long
    ): IngestPayload {
        val collectedAt = Instant.now().toString()
        val intervalStart = Instant.ofEpochMilli(startTimeMillis).toString()
        val intervalEnd = Instant.ofEpochMilli(endTimeMillis).toString()
        
        return IngestPayload(
            schemaVersion = 1,
            deviceId = deviceId,
            collectedAt = collectedAt,
            samples = listOf(
                Sample(
                    sampleId = UUID.randomUUID().toString(),
                    intervalStart = intervalStart,
                    intervalEnd = intervalEnd,
                    records = records
                )
            )
        )
    }
}
