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
                val endpointUrl = settingsRepository.endpointUrl.first()
                val apiService = RetrofitClient.create(apiKey)

                val testPayload = createTestPayload()
                val response = apiService.ingest(endpointUrl + "/api/ingest", testPayload)

                if (response.isSuccessful) {
                    _testConnectionResult.value = "Success! Code: ${response.code()}"
                } else {
                    _testConnectionResult.value = "Error: ${response.code()} - ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _testConnectionResult.value = "Exception: ${e.message}"
            }
        }
    }

    private fun createTestPayload(): IngestPayload {
        val now = Instant.now().toString()
        return IngestPayload(
            schemaVersion = 1,
            deviceId = "b3c7f0ab-95a6-4f2a-9eec-a1f6fd4a9c55",
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
