package com.devpipe.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpipe.app.data.logging.LogManager
import com.devpipe.app.data.repository.DevPipeRepository
import com.devpipe.app.data.storage.PreferencesManager
import com.devpipe.app.data.storage.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    val discoveryInProgress: Boolean = false,
    val discoveryError: String? = null,
    val discoverySuccess: Boolean = false,
    val discoveredUrl: String? = null,
    val discoveredUrlUpdated: String? = null,
    val discoveredIp: String? = null,
    val discoveredIpUpdated: String? = null,
    val discoveredLanIp: String? = null,
    val discoveryServerStatus: String? = null,
    val apiTokenFetchedFromDiscovery: Boolean = false
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

    val phpDiscoveryToken: StateFlow<String> = preferencesManager.phpDiscoveryToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val theme: StateFlow<String> = preferencesManager.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "system")

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun getToken(): String = tokenManager.getToken() ?: ""

    fun saveToken(token: String) {
        tokenManager.saveToken(token)
        logManager.info("Settings", "API token updated")
    }

    fun savePhpDiscoveryToken(token: String) {
        viewModelScope.launch {
            preferencesManager.savePhpDiscoveryToken(token)
            logManager.info("Settings", "PHP discovery token updated")
            // Auto-trigger discovery if a discovery URL is already configured
            val phpUrl = phpDiscoveryUrl.value
            if (phpUrl.isNotBlank() && token.isNotBlank()) {
                logManager.info("Settings", "Discovery token changed – re-running auto-discovery")
                discoverUrl()
            }
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
            val token = preferencesManager.phpDiscoveryToken.first()
            if (url.isNotBlank() && token.isNotBlank()) {
                logManager.info("Settings", "Discovery URL changed – re-running auto-discovery")
                discoverUrl()
            }
        }
    }

    fun saveTheme(theme: String) {
        viewModelScope.launch { preferencesManager.saveTheme(theme) }
    }

    companion object {
        /** Default port used when constructing a fallback URL from a bare IP address. */
        private const val FALLBACK_PORT = 8080
    }

    fun saveAndDiscover(url: String, token: String) {
        viewModelScope.launch {
            preferencesManager.savePhpDiscoveryUrl(url)
            preferencesManager.savePhpDiscoveryToken(token)
            logManager.info("Settings", "PHP discovery settings saved – starting discovery")
            discoverUrl()
        }
    }

    fun discoverUrl() {
        viewModelScope.launch {
            val phpUrl = preferencesManager.phpDiscoveryUrl.first()
            val discoveryToken = preferencesManager.phpDiscoveryToken.first()

            if (phpUrl.isBlank()) {
                val msg = "Auto-discovery failed: no PHP discovery URL configured"
                logManager.error("AutoDiscovery", msg)
                _uiState.value = SettingsUiState(discoveryError = "No PHP discovery URL configured")
                return@launch
            }
            if (discoveryToken.isBlank()) {
                val msg = "Auto-discovery failed: no PHP discovery token configured"
                logManager.error("AutoDiscovery", msg)
                _uiState.value = SettingsUiState(discoveryError = "No PHP discovery token configured")
                return@launch
            }

            logManager.info("AutoDiscovery", "Starting discovery via $phpUrl")
            _uiState.value = SettingsUiState(discoveryInProgress = true)

            // Fetch URL, IP, LAN IP, server status, and API token in parallel
            val urlDeferred = async { repository.discoverUrl(phpUrl, discoveryToken) }
            val ipDeferred = async { repository.discoverIp(phpUrl, discoveryToken) }
            val lanIpDeferred = async { repository.discoverLanIp(phpUrl, discoveryToken) }
            val statusDeferred = async { repository.getDiscoveryStatus(phpUrl, discoveryToken) }
            val apiTokenDeferred = async { repository.fetchApiToken(phpUrl, discoveryToken) }

            val urlResult = urlDeferred.await()
            val ipResult = ipDeferred.await()
            val lanIpResult = lanIpDeferred.await()
            val statusResult = statusDeferred.await()
            val apiTokenResult = apiTokenDeferred.await()

            // Auto-save the Dev-Pipe API token if successfully retrieved
            val apiTokenFetched = apiTokenResult.fold(
                onSuccess = { response ->
                    tokenManager.saveToken(response.token)
                    logManager.info("AutoDiscovery", "API token fetched and saved from discovery endpoint")
                    true
                },
                onFailure = { e ->
                    logManager.warn("AutoDiscovery", "API token fetch failed: ${e.message}")
                    false
                }
            )

            val lanIpResponse = lanIpResult.onFailure { e ->
                logManager.warn("AutoDiscovery", "LAN IP fetch failed: ${e.message}")
            }.getOrNull()

            // Persist the LAN IP whenever we successfully retrieve it so the
            // app can use it as a last-resort fallback in subsequent sessions.
            lanIpResponse?.ip?.let { preferencesManager.saveLanIp(it) }

            urlResult.fold(
                onSuccess = { response ->
                    val ipResponse = ipResult.onFailure { e ->
                        logManager.warn("AutoDiscovery", "IP fetch failed: ${e.message}")
                    }.getOrNull()
                    val serverStatus = statusResult.onFailure { e ->
                        logManager.warn("AutoDiscovery", "Status fetch failed: ${e.message}")
                    }.getOrNull()

                    // If the server reports itself as offline at the discovered URL,
                    // and we have a LAN IP, fall back to it (handles hairpin-NAT / stale
                    // dynamic-DNS scenarios when the device is on the same LAN).
                    val lanIp = lanIpResponse?.ip
                    val effectiveUrl = if (serverStatus?.isOnline == false && !lanIp.isNullOrEmpty()) {
                        val lanFallback = "http://$lanIp:$FALLBACK_PORT/"
                        logManager.info(
                            "AutoDiscovery",
                            "Server offline at ${response.url}; falling back to LAN IP: $lanFallback"
                        )
                        preferencesManager.saveBackendUrl(lanFallback)
                        lanFallback
                    } else {
                        preferencesManager.saveBackendUrl(response.url)
                        logManager.info(
                            "AutoDiscovery",
                            "Discovery succeeded – backend URL: ${response.url}, updated: ${response.updated}"
                        )
                        response.url
                    }

                    _uiState.value = SettingsUiState(
                        discoverySuccess = true,
                        discoveredUrl = effectiveUrl,
                        discoveredUrlUpdated = formatTimestamp(response.updated),
                        discoveredIp = ipResponse?.ip,
                        discoveredIpUpdated = formatTimestamp(ipResponse?.updated),
                        discoveredLanIp = lanIpResponse?.ip,
                        discoveryServerStatus = serverStatus?.status,
                        apiTokenFetchedFromDiscovery = apiTokenFetched
                    )
                },
                onFailure = { e ->
                    // URL discovery failed – try LAN IP as a last-resort fallback.
                    val lanIp = lanIpResponse?.ip
                    if (!lanIp.isNullOrEmpty()) {
                        val lanFallback = "http://$lanIp:$FALLBACK_PORT/"
                        preferencesManager.saveBackendUrl(lanFallback)
                        logManager.info(
                            "AutoDiscovery",
                            "URL discovery failed; using LAN IP fallback: $lanFallback"
                        )
                        _uiState.value = SettingsUiState(
                            discoverySuccess = true,
                            discoveredUrl = lanFallback,
                            discoveredLanIp = lanIp,
                            discoveryServerStatus = null,
                            apiTokenFetchedFromDiscovery = apiTokenFetched
                        )
                    } else {
                        val msg = e.message ?: "Unknown error"
                        logManager.error("AutoDiscovery", "Discovery failed: $msg")
                        _uiState.value = SettingsUiState(discoveryError = msg)
                    }
                }
            )
        }
    }

    private fun formatTimestamp(updated: String?): String? {
        if (updated == null) return null
        val displayFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
        // Try ISO 8601 format (e.g. "2026-04-08T18:52:31+00:00")
        for (pattern in listOf("yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss'Z'")) {
            try {
                val date = SimpleDateFormat(pattern, Locale.US).parse(updated)
                if (date != null) return displayFormat.format(date)
            } catch (_: java.text.ParseException) {}
        }
        // Try Unix timestamp in seconds
        try {
            val epochSeconds = updated.toLong()
            return displayFormat.format(Date(epochSeconds * 1000))
        } catch (_: NumberFormatException) {}
        return updated
    }
}
