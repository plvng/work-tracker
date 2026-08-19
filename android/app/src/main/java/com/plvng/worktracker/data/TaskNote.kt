package com.plvng.worktracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_notes")
data class TaskNote(
    @PrimaryKey val taskName: String,
    val comment: String? = null,
)
