package com.devpipe.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpipe.app.data.model.ComponentStatus
import com.devpipe.app.data.model.StatusResponse
import com.devpipe.app.data.repository.DevPipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = false,
    val components: List<ComponentStatus> = emptyList(),
    val sessionCounts: Map<String, Int> = emptyMap(),
    val jobCounts: Map<String, Int> = emptyMap(),
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DevPipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val health = repository.getHealth()
            val status = repository.getStatus()
            _uiState.value = DashboardUiState(
                isLoading = false,
                components = health.getOrNull()?.components ?: emptyList(),
                sessionCounts = status.getOrNull()?.sessionCounts ?: emptyMap(),
                jobCounts = status.getOrNull()?.jobCounts ?: emptyMap(),
                error = health.exceptionOrNull()?.message ?: status.exceptionOrNull()?.message
            )
        }
    }
}
