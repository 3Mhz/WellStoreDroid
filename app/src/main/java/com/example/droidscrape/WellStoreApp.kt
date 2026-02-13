package com.example.droidscrape

import android.app.Application
import androidx.room.Room
import com.example.droidscrape.data.datastore.SettingsRepository
import com.example.droidscrape.data.local.AppDatabase

class WellStoreApp : Application() {
    lateinit var database: AppDatabase
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "well-store-db"
        ).build()

        settingsRepository = SettingsRepository(this)
    }
}
