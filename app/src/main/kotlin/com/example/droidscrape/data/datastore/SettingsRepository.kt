package com.example.droidscrape.data.datastore

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val ENDPOINT_URL = stringPreferencesKey("endpoint_url")
        val API_KEY = stringPreferencesKey("api_key")
        val USERNAME = stringPreferencesKey("username")
        val COLLECTION_ENABLED = stringPreferencesKey("collection_enabled")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val LAST_COLLECTION_TIME = longPreferencesKey("last_collection_time")
        val LAST_SUCCESSFUL_UPLOAD_TIME = longPreferencesKey("last_successful_upload_time")
    }

    val endpointUrl: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ENDPOINT_URL] ?: "http://10.0.2.2:3000"
        }

    val apiKey: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.API_KEY] ?: "VGQCbC1l1FlCHGAEXoB2knShV64Y2-K2MJvMPS1wenc"
        }
        
    val username: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USERNAME] ?: "User"
        }

    suspend fun saveSettings(url: String, apiKey: String, username: String) {
        context.dataStore.edit {
            it[PreferencesKeys.ENDPOINT_URL] = url
            it[PreferencesKeys.API_KEY] = apiKey
            it[PreferencesKeys.USERNAME] = username
            
            // Re-generate device ID whenever name/username changes to keep it in sync
            it[PreferencesKeys.DEVICE_ID] = formatDeviceId(username)
        }
    }

    val collectionEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.COLLECTION_ENABLED]?.toBoolean() ?: false
        }

    suspend fun setCollectionEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[PreferencesKeys.COLLECTION_ENABLED] = enabled.toString()
        }
    }

    val deviceId: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DEVICE_ID] ?: ""
        }
        
    suspend fun ensureDeviceId(): String {
        var currentId = ""
        context.dataStore.edit { preferences ->
            val existing = preferences[PreferencesKeys.DEVICE_ID]
            val currentUsername = preferences[PreferencesKeys.USERNAME] ?: "User"
            
            // Update if empty, UUID, or doesn't start with the current username
            val needsUpdate = existing.isNullOrBlank() || 
                             isUuid(existing) || 
                             !existing.startsWith(currentUsername)
            
            if (needsUpdate) {
                val readableName = formatDeviceId(currentUsername)
                preferences[PreferencesKeys.DEVICE_ID] = readableName
                currentId = readableName
            } else {
                currentId = existing!!
            }
        }
        return currentId
    }

    private fun formatDeviceId(username: String): String {
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".capitalizeWords()
        return "$username ($deviceName)"
    }

    private fun isUuid(value: String): Boolean {
        return try {
            UUID.fromString(value)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    val lastCollectionTime: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAST_COLLECTION_TIME]
        }

    suspend fun setLastCollectionTime(time: Long) {
        context.dataStore.edit {
            it[PreferencesKeys.LAST_COLLECTION_TIME] = time
        }
    }

    val lastSuccessfulUploadTime: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAST_SUCCESSFUL_UPLOAD_TIME]
        }

    suspend fun setLastSuccessfulUploadTime(time: Long) {
        context.dataStore.edit {
            it[PreferencesKeys.LAST_SUCCESSFUL_UPLOAD_TIME] = time
        }
    }
}
