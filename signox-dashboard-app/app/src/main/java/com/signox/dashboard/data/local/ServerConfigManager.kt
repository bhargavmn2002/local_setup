package com.signox.dashboard.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerConfigManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        // Default server URL - users can change this in the app
        const val DEFAULT_SERVER_URL = "https://www.signoxcms.com"
    }
    
    suspend fun saveServerUrl(url: String) {
        val cleanUrl = url.trim().removeSuffix("/")
        dataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = cleanUrl
        }
    }
    
    fun getServerUrl(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[SERVER_URL_KEY]
        }
    }
    
    suspend fun getServerUrlSync(): String {
        return getServerUrl().first() ?: DEFAULT_SERVER_URL
    }
    
    suspend fun isServerConfigured(): Boolean {
        return !getServerUrl().first().isNullOrEmpty()
    }
    
    suspend fun clearServerUrl() {
        dataStore.edit { preferences ->
            preferences.remove(SERVER_URL_KEY)
        }
    }
}
