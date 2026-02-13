package com.example.droidscrape.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.droidscrape.data.local.model.SampleDao
import com.example.droidscrape.data.local.model.SampleEntity

@Database(entities = [SampleEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sampleDao(): SampleDao
}
