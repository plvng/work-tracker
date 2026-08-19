package com.plvng.worktracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_sessions")
data class WorkSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskName: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val comment: String? = null,
) {
    val isActive: Boolean get() = endedAt == null

    fun durationMs(now: Long = System.currentTimeMillis()): Long {
        val end = endedAt ?: now
        return (end - startedAt).coerceAtLeast(0)
    }
}
