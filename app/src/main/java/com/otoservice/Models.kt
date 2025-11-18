package com.otoservice

data class AutoReplyLog(
    val appName: String,
    val target: String,
    val timestamp: Long,
    val preview: String
)

object FrequencyDefaults {
    val replySeconds = listOf(5, 10, 30, 60)
    val locationMinutes = listOf(1, 5, 10, 15, 30)
}
