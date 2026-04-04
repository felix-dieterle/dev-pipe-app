package com.devpipe.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpipe.app.data.logging.LogManager
import com.devpipe.app.data.repository.DevPipeRepository
import com.devpipe.app.data.storage.PreferencesManager
import com.devpipe.app.data.storage.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val discoveryInProgress: Boolean = false,
    val discoveryError: String? = null,
    val discoverySuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val tokenManager: TokenManager,
    private val repository: DevPipeRepository,
    private val logManager: LogManager
) : ViewModel() {

    val backendUrl: StateFlow<String> = preferencesManager.backendUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PreferencesManager.DEFAULT_BACKEND_URL)

    val phpDiscoveryUrl: StateFlow<String> = preferencesManager.phpDiscoveryUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val theme: StateFlow<String> = preferencesManager.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "system")

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun getToken(): String = tokenManager.getToken() ?: ""

    fun saveToken(token: String) {
        tokenManager.saveToken(token)
        logManager.info("Settings", "API token updated")
        // Auto-trigger discovery if a PHP discovery URL is already configured
        val phpUrl = phpDiscoveryUrl.value
        if (phpUrl.isNotBlank() && token.isNotBlank()) {
            logManager.info("Settings", "Token changed – re-running auto-discovery")
            discoverUrl()
        }
    }

    fun saveBackendUrl(url: String) {
        viewModelScope.launch {
            preferencesManager.saveBackendUrl(url)
            logManager.info("Settings", "Backend URL set to: $url")
        }
    }

    fun savePhpDiscoveryUrl(url: String) {
        viewModelScope.launch {
            preferencesManager.savePhpDiscoveryUrl(url)
            logManager.info("Settings", "PHP discovery URL set to: $url")
            // Auto-trigger discovery if a token is already configured
            val token = tokenManager.getToken()
            if (url.isNotBlank() && !token.isNullOrBlank()) {
                logManager.info("Settings", "Discovery URL changed – re-running auto-discovery")
                discoverUrl()
            }
        }
    }

    fun saveTheme(theme: String) {
        viewModelScope.launch { preferencesManager.saveTheme(theme) }
    }

    fun discoverUrl() {
        val token = tokenManager.getToken() ?: run {
            val msg = "Auto-discovery failed: no API token configured"
            logManager.error("AutoDiscovery", msg)
            _uiState.value = SettingsUiState(discoveryError = "No API token configured")
            return
        }
        viewModelScope.launch {
            val phpUrl = preferencesManager.phpDiscoveryUrl.first()
            if (phpUrl.isBlank()) {
                val msg = "Auto-discovery failed: no PHP discovery URL configured"
                logManager.error("AutoDiscovery", msg)
                _uiState.value = SettingsUiState(discoveryError = "No PHP discovery URL configured")
                return@launch
            }
            logManager.info("AutoDiscovery", "Starting discovery via $phpUrl")
            _uiState.value = SettingsUiState(discoveryInProgress = true)
            repository.discoverUrl(phpUrl, token).fold(
                onSuccess = { response ->
                    preferencesManager.saveBackendUrl(response.url)
                    logManager.info("AutoDiscovery", "Discovery succeeded – backend URL: ${response.url}")
                    _uiState.value = SettingsUiState(discoverySuccess = true)
                },
                onFailure = { e ->
                    val msg = e.message ?: "Unknown error"
                    logManager.error("AutoDiscovery", "Discovery failed: $msg")
                    _uiState.value = SettingsUiState(discoveryError = msg)
                }
            )
        }
    }
}
