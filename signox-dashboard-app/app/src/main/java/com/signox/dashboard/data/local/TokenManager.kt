package com.signox.dashboard.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("access_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
    }
    
    suspend fun saveToken(token: String) {
        dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }
    
    fun getToken(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }
    }
    
    suspend fun saveUserInfo(userId: String, email: String, role: String) {
        dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[USER_EMAIL_KEY] = email
            preferences[USER_ROLE_KEY] = role
        }
    }
    
    fun getUserRole(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[USER_ROLE_KEY]
        }
    }
    
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            // Only clear authentication-related keys, not server configuration
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USER_EMAIL_KEY)
            preferences.remove(USER_ROLE_KEY)
        }
    }
    
    suspend fun isLoggedIn(): Boolean {
        var token: String? = null
        dataStore.data.map { preferences ->
            token = preferences[TOKEN_KEY]
        }
        return !token.isNullOrEmpty()
    }
}
