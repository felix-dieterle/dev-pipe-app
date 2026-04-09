package com.devpipe.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpipe.app.data.logging.LogManager
import com.devpipe.app.data.model.Job
import com.devpipe.app.data.model.Session
import com.devpipe.app.data.repository.DevPipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionDetailUiState(
    val isLoading: Boolean = false,
    val session: Session? = null,
    val jobs: List<Job> = emptyList(),
    val actionInProgress: Boolean = false,
    val error: String? = null,
    val actionError: String? = null
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val repository: DevPipeRepository,
    private val logManager: LogManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String? = savedStateHandle["sessionId"]

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState

    init {
        if (sessionId != null) {
            load()
        } else {
            _uiState.value = SessionDetailUiState(error = "Session ID missing")
        }
    }

    fun load() {
        val id = sessionId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val sessionResult = repository.getSession(id)
            val jobsResult = repository.getJobs()
            val error = sessionResult.exceptionOrNull()?.message
            if (error != null) {
                logManager.error("SessionDetail", "Load failed for session $id: $error")
            }
            val session = sessionResult.getOrNull()
            val allJobs = jobsResult.getOrNull() ?: emptyList()
            val sessionJobs = session?.let { s ->
                allJobs.filter { it.name.contains(s.name, ignoreCase = true) }
            } ?: emptyList()
            _uiState.value = SessionDetailUiState(
                session = session,
                jobs = sessionJobs,
                error = error
            )
        }
    }

    fun approve() = performAction("approve")

    fun reject(reason: String? = null) = performAction("reject", reason)

    fun mergePr() = performAction("merge")

    private fun performAction(action: String, reason: String? = null) {
        val id = sessionId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true, actionError = null)
            repository.sessionAction(id, action, reason).fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(
                        session = updated,
                        actionInProgress = false
                    )
                },
                onFailure = { e ->
                    logManager.error("SessionDetail", "Action '$action' failed for session $id: ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = false,
                        actionError = e.message
                    )
                }
            )
        }
    }
}
