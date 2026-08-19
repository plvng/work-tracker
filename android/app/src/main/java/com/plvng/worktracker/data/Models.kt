package com.plvng.worktracker.data

data class TaskSummary(
    val taskName: String,
    val totalDurationMs: Long,
    val taskComment: String? = null,
)

data class SessionDetail(
    val session: WorkSession,
    val durationMs: Long,
    val amountRub: Int,
)
