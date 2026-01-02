package com.cyberscope.reports.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {
    
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
    }
    
    val accessToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ACCESS_TOKEN_KEY]
    }
    
    val refreshToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[REFRESH_TOKEN_KEY]
    }
    
    val userEmail: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_EMAIL_KEY]
    }
    
    suspend fun saveTokens(
        accessToken: String?,
        refreshToken: String?,
        email: String?,
        name: String?,
        role: String?
    ) {
        context.dataStore.edit { prefs ->
            accessToken?.let { prefs[ACCESS_TOKEN_KEY] = it } ?: prefs.remove(ACCESS_TOKEN_KEY)
            refreshToken?.let { prefs[REFRESH_TOKEN_KEY] = it } ?: prefs.remove(REFRESH_TOKEN_KEY)
            email?.let { prefs[USER_EMAIL_KEY] = it } ?: prefs.remove(USER_EMAIL_KEY)
            name?.let { prefs[USER_NAME_KEY] = it } ?: prefs.remove(USER_NAME_KEY)
            role?.let { prefs[USER_ROLE_KEY] = it } ?: prefs.remove(USER_ROLE_KEY)
        }
    }
    
    suspend fun clearTokens() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
            prefs.remove(USER_EMAIL_KEY)
            prefs.remove(USER_NAME_KEY)
            prefs.remove(USER_ROLE_KEY)
        }
    }
    
}

