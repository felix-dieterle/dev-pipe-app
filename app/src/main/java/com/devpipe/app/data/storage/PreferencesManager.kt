package com.devpipe.app.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "devpipe_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    val backendUrl: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_BACKEND_URL] ?: DEFAULT_BACKEND_URL
    }

    val theme: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: "system"
    }

    val phpDiscoveryUrl: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_PHP_DISCOVERY_URL] ?: ""
    }

    suspend fun saveBackendUrl(url: String) {
        dataStore.edit { prefs -> prefs[KEY_BACKEND_URL] = url }
    }

    suspend fun saveTheme(theme: String) {
        dataStore.edit { prefs -> prefs[KEY_THEME] = theme }
    }

    suspend fun savePhpDiscoveryUrl(url: String) {
        dataStore.edit { prefs -> prefs[KEY_PHP_DISCOVERY_URL] = url }
    }

    companion object {
        private val KEY_BACKEND_URL = stringPreferencesKey("backend_url")
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_PHP_DISCOVERY_URL = stringPreferencesKey("php_discovery_url")
        const val DEFAULT_BACKEND_URL = "http://localhost:8080/"
    }
}
