package com.example.droidscrape

import android.app.Application
import com.example.droidscrape.data.datastore.SettingsRepository
import com.example.droidscrape.data.local.AppDatabase

class WellStoreApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
