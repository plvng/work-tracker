package com.plvng.worktracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskNoteDao {
    @Query("SELECT * FROM task_notes")
    fun observeAll(): Flow<List<TaskNote>>

    @Query("SELECT * FROM task_notes WHERE taskName = :taskName LIMIT 1")
    suspend fun getByTaskName(taskName: String): TaskNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: TaskNote)

    @Query("DELETE FROM task_notes")
    suspend fun deleteAll()
}
