package com.example.droidscrape.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.droidscrape.data.datastore.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConfigViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)

    val endpointUrl = settingsRepository.endpointUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "http://10.0.2.2:3000")

    val apiKey = settingsRepository.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "VGQCbC1l1FlCHGAEXoB2knShV64Y2-K2MJvMPS1wenc")

    fun saveSettings(url: String, key: String) {
        viewModelScope.launch {
            settingsRepository.saveSettings(url, key)
        }
    }
    
    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.saveSettings("http://10.0.2.2:3000", "VGQCbC1l1FlCHGAEXoB2knShV64Y2-K2MJvMPS1wenc")
        }
    }
}
