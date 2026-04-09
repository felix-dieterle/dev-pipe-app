package com.devpipe.app.data.logging

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val counter = AtomicLong(0)
    private val maxEntries = 500
    private val logFile: File get() = File(context.filesDir, "devpipe_logs.txt")

    // This scope intentionally lives for the application lifetime because LogManager is a Singleton.
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Seed the counter from the last persisted entry ID so that IDs assigned during
        // this session are always greater than those from previous sessions. This prevents
        // ID collisions when we later merge the file entries with any entries that were
        // logged during app startup (before loadFromFile completes).
        try {
            if (logFile.exists()) {
                logFile.useLines { lines ->
                    lines.lastOrNull { it.isNotBlank() }
                        ?.split("|", limit = 2)
                        ?.firstOrNull()
                        ?.toLongOrNull()
                }?.let { lastId -> counter.set(lastId) }
            }
        } catch (e: Exception) {
            Log.e("LogManager", "Failed to pre-read counter from file: ${e.message}")
        }
        ioScope.launch { loadFromFile() }
    }

    private fun loadFromFile() {
        try {
            if (!logFile.exists()) return
            val fileEntries = logFile.readLines()
                .mapNotNull { parseLine(it) }
                .takeLast(maxEntries)
                .reversed()
            if (fileEntries.isEmpty()) return
            val maxId = fileEntries.maxOf { it.id }
            // Ensure counter is at least as large as the highest file entry ID.
            counter.updateAndGet { maxOf(it, maxId) }
            // Merge: keep any entries already in memory (logged during this session,
            // IDs > maxId because counter was seeded above) then append file entries.
            // This avoids overwriting startup log entries with the historical file data.
            _logs.update { current ->
                if (current.isEmpty()) {
                    fileEntries
                } else {
                    buildList {
                        addAll(current)
                        addAll(fileEntries)
                    }.take(maxEntries)
                }
            }
        } catch (e: Exception) {
            Log.e("LogManager", "Failed to load logs from file: ${e.message}")
        }
    }

    private fun parseLine(line: String): LogEntry? = try {
        if (line.isBlank()) null
        else {
            val parts = line.split("|", limit = 5)
            if (parts.size < 5) null
            else LogEntry(
                id = parts[0].toLong(),
                timestamp = parts[1].toLong(),
                level = LogLevel.valueOf(parts[2]),
                tag = parts[3],
                message = parts[4]
            )
        }
    } catch (e: Exception) {
        null
    }

    private fun entryToLine(entry: LogEntry): String =
        "${entry.id}|${entry.timestamp}|${entry.level.name}|${entry.tag}|${entry.message}"

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
        ioScope.launch { appendToFile(entry) }
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }
    }

    private fun appendToFile(entry: LogEntry) {
        try {
            logFile.appendText(entryToLine(entry) + "\n")
            // Trim periodically to avoid unbounded growth; exact trimming is not critical
            if (entry.id % 50 == 0L) {
                val lines = logFile.readLines()
                if (lines.size > maxEntries) {
                    logFile.writeText(lines.takeLast(maxEntries).joinToString("\n") + "\n")
                }
            }
        } catch (e: Exception) {
            Log.e("LogManager", "Failed to write log to file: ${e.message}")
        }
    }

    fun debug(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun info(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun warn(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun error(tag: String, message: String) = log(LogLevel.ERROR, tag, message)

    fun clear() {
        _logs.value = emptyList()
        ioScope.launch {
            try {
                logFile.delete()
            } catch (e: Exception) {
                Log.e("LogManager", "Failed to delete log file: ${e.message}")
            }
        }
    }

    fun exportText(): String = _logs.value
        .reversed()
        .joinToString("\n") { it.toExportString() }
}
