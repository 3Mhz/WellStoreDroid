package com.example.droidscrape.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.droidscrape.WellStoreApp
import com.example.droidscrape.data.remote.ApiClient
import com.example.droidscrape.worker.CollectorWorker
import com.example.droidscrape.worker.UploaderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as WellStoreApp
    private val settingsRepository = app.settingsRepository
    private val sampleDao = app.database.sampleDao()
    private val workManager = WorkManager.getInstance(application)

    companion object {
        private const val COLLECTOR_WORKER_NAME = "collector_worker"
        private const val UPLOADER_WORKER_NAME = "uploader_worker"
    }

    val endpointUrl = settingsRepository.endpointUrl.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    suspend fun setEndpointUrl(url: String) {
        settingsRepository.setEndpointUrl(url)
    }

    val collectionEnabled = settingsRepository.collectionEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun toggleCollection(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCollectionEnabled(enabled)
            if (enabled) {
                startWorkers()
            } else {
                stopWorkers()
            }
        }
    }

    private fun startWorkers() {
        val collectorWorkRequest = PeriodicWorkRequestBuilder<CollectorWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            COLLECTOR_WORKER_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            collectorWorkRequest
        )

        val uploaderWorkRequest = PeriodicWorkRequestBuilder<UploaderWorker>(60, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            UPLOADER_WORKER_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            uploaderWorkRequest
        )
    }

    private fun stopWorkers() {
        workManager.cancelUniqueWork(COLLECTOR_WORKER_NAME)
        workManager.cancelUniqueWork(UPLOADER_WORKER_NAME)
    }

    private val _testConnectionStatus = MutableStateFlow<String?>(null)
    val testConnectionStatus = _testConnectionStatus.asStateFlow()

    fun testConnection() {
        viewModelScope.launch {
            val url = endpointUrl.value
            if (url.isNullOrEmpty()) {
                _testConnectionStatus.value = "Endpoint URL is not set."
                return@launch
            }
            _testConnectionStatus.value = "Testing..."
            try {
                val response = ApiClient.uploadService.test(url)
                if (response.isSuccessful) {
                    _testConnectionStatus.value = "Connection successful!"
                } else {
                    _testConnectionStatus.value = "Connection failed: ${response.code()}"
                }
            } catch (e: Exception) {
                _testConnectionStatus.value = "Connection failed: ${e.message}"
            }
        }
    }

    val lastCollectionTime = settingsRepository.lastCollectionTime.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val lastSuccessfulUploadTime = settingsRepository.lastSuccessfulUploadTime.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val queueSize = sampleDao.getPendingCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val lastError = sampleDao.getLastError().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
}
