package com.devpipe.app.data.logging

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogManager @Inject constructor() {

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val counter = AtomicLong(0)
    private val maxEntries = 500

    fun log(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(
            id = counter.incrementAndGet(),
            level = level,
            tag = tag,
            message = message
        )
        // Prepend so the list is newest-first; pre-size avoids extra allocations
        _logs.update { current ->
            buildList(minOf(current.size + 1, maxEntries)) {
                add(entry)
                addAll(current.take(maxEntries - 1))
            }
        }
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }
    }

    fun debug(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun info(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun warn(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun error(tag: String, message: String) = log(LogLevel.ERROR, tag, message)

    fun clear() {
        _logs.value = emptyList()
    }

    fun exportText(): String = _logs.value
        .reversed()
        .joinToString("\n") { it.toExportString() }
}
