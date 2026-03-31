package com.devpipe.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val repository: DevPipeRepository
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
    }

    fun saveBackendUrl(url: String) {
        viewModelScope.launch { preferencesManager.saveBackendUrl(url) }
    }

    fun savePhpDiscoveryUrl(url: String) {
        viewModelScope.launch { preferencesManager.savePhpDiscoveryUrl(url) }
    }

    fun saveTheme(theme: String) {
        viewModelScope.launch { preferencesManager.saveTheme(theme) }
    }

    fun discoverUrl() {
        val token = tokenManager.getToken() ?: run {
            _uiState.value = SettingsUiState(discoveryError = "No API token configured")
            return
        }
        viewModelScope.launch {
            val phpUrl = preferencesManager.phpDiscoveryUrl.first()
            if (phpUrl.isBlank()) {
                _uiState.value = SettingsUiState(discoveryError = "No PHP discovery URL configured")
                return@launch
            }
            _uiState.value = SettingsUiState(discoveryInProgress = true)
            repository.discoverUrl(phpUrl, token).fold(
                onSuccess = { response ->
                    preferencesManager.saveBackendUrl(response.url)
                    _uiState.value = SettingsUiState(discoverySuccess = true)
                },
                onFailure = { e ->
                    _uiState.value = SettingsUiState(discoveryError = e.message)
                }
            )
        }
    }
}
