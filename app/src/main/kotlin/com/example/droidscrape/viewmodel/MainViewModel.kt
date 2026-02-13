package com.example.droidscrape.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.droidscrape.data.datastore.SettingsRepository
import com.example.droidscrape.network.IngestPayload
import com.example.droidscrape.network.Record
import com.example.droidscrape.network.RetrofitClient
import com.example.droidscrape.network.Sample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    private val _testConnectionResult = MutableStateFlow<String?>(null)
    val testConnectionResult: StateFlow<String?> = _testConnectionResult.asStateFlow()

    fun sendTestRequest() {
        viewModelScope.launch {
            _testConnectionResult.value = "Sending..."
            try {
                val apiKey = settingsRepository.apiKey.first()
                var endpointUrl = settingsRepository.endpointUrl.first().trim()
                val deviceId = settingsRepository.ensureDeviceId()
                
                // Ensure the URL starts with a protocol
                if (!endpointUrl.startsWith("http://") && !endpointUrl.startsWith("https://")) {
                    endpointUrl = "http://$endpointUrl"
                }
                
                // Ensure no trailing slash for consistent concatenation
                val normalizedUrl = endpointUrl.removeSuffix("/")
                val fullUrl = "$normalizedUrl/api/ingest"

                val apiService = RetrofitClient.create(apiKey)

                val testPayload = createTestPayload(deviceId)
                val response = apiService.ingest(fullUrl, testPayload)

                if (response.isSuccessful) {
                    _testConnectionResult.value = "Success! Code: ${response.code()}"
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    _testConnectionResult.value = "Error: ${response.code()} - $errorMsg"
                }
            } catch (e: Exception) {
                _testConnectionResult.value = "Exception: ${e.message}"
            }
        }
    }

    private fun createTestPayload(deviceId: String): IngestPayload {
        val now = Instant.now().toString()
        return IngestPayload(
            schemaVersion = 1,
            deviceId = deviceId,
            collectedAt = now,
            samples = listOf(
                Sample(
                    sampleId = UUID.randomUUID().toString(),
                    intervalStart = Instant.now().minusSeconds(900).toString(), // 15 minutes ago
                    intervalEnd = now,
                    records = listOf(
                        Record(
                            packageName = "com.foo",
                            appLabel = "Foo",
                            usageMillisIncrement = 12345
                        )
                    )
                )
            )
        )
    }
}
