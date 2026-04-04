package com.devpipe.app.data.logging

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class LogEntry(
    val id: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String
) {
    fun formattedTimestamp(): String =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

    fun toExportString(): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            .format(Date(timestamp))
        return "[${level.name}] $dateStr $tag: $message"
    }
}
