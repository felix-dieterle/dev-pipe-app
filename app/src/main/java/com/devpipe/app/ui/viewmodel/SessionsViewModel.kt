package com.devpipe.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpipe.app.data.model.Session
import com.devpipe.app.data.repository.DevPipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionsUiState(
    val isLoading: Boolean = false,
    val sessions: List<Session> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val repository: DevPipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState: StateFlow<SessionsUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getSessions().fold(
                onSuccess = { sessions ->
                    _uiState.value = SessionsUiState(sessions = sessions)
                },
                onFailure = { e ->
                    _uiState.value = SessionsUiState(error = e.message)
                }
            )
        }
    }
}
