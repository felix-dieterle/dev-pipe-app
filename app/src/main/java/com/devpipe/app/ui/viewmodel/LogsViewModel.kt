package com.devpipe.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.devpipe.app.data.logging.LogEntry
import com.devpipe.app.data.logging.LogManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logManager: LogManager
) : ViewModel() {

    val logs: StateFlow<List<LogEntry>> = logManager.logs

    fun clear() = logManager.clear()

    fun exportText(): String = logManager.exportText()
}
