package com.devpipe.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpipe.app.data.logging.LogManager
import com.devpipe.app.data.model.CreateSessionRequest
import com.devpipe.app.data.repository.DevPipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateSessionUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CreateSessionViewModel @Inject constructor(
    private val repository: DevPipeRepository,
    private val logManager: LogManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateSessionUiState())
    val uiState: StateFlow<CreateSessionUiState> = _uiState

    fun createSession(name: String, description: String, repoOwner: String, repoName: String) {
        viewModelScope.launch {
            _uiState.value = CreateSessionUiState(isLoading = true)
            repository.createSession(
                CreateSessionRequest(
                    name = name,
                    description = description,
                    repoOwner = repoOwner,
                    repoName = repoName
                )
            ).fold(
                onSuccess = { _uiState.value = CreateSessionUiState(success = true) },
                onFailure = { e ->
                    logManager.error("CreateSession", "Create session failed: ${e.message}")
                    _uiState.value = CreateSessionUiState(error = e.message)
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = CreateSessionUiState()
    }
}
