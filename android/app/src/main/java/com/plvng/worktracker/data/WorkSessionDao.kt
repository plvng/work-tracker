package com.plvng.worktracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkSessionDao {
    @Query("SELECT * FROM work_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WorkSession>>

    @Query("SELECT * FROM work_sessions WHERE endedAt IS NULL LIMIT 1")
    fun observeActive(): Flow<WorkSession?>

    @Query("SELECT * FROM work_sessions WHERE endedAt IS NULL LIMIT 1")
    suspend fun getActive(): WorkSession?

    @Query("SELECT DISTINCT taskName FROM work_sessions ORDER BY taskName COLLATE NOCASE ASC")
    fun observeTaskNames(): Flow<List<String>>

    @Query(
        """
        SELECT * FROM work_sessions
        WHERE startedAt < :dayEnd AND (endedAt IS NULL OR endedAt > :dayStart)
        ORDER BY startedAt DESC
        """,
    )
    suspend fun getSessionsOverlappingDay(dayStart: Long, dayEnd: Long): List<WorkSession>

    @Query("SELECT * FROM work_sessions ORDER BY startedAt DESC")
    suspend fun getAllSessions(): List<WorkSession>

    @Insert
    suspend fun insert(session: WorkSession): Long

    @Update
    suspend fun update(session: WorkSession)

    @Query("DELETE FROM work_sessions")
    suspend fun deleteAll()
}
