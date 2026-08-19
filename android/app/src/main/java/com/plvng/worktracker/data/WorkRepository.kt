package com.plvng.worktracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.Calendar

class WorkRepository(
    private val dao: WorkSessionDao,
    private val taskNoteDao: TaskNoteDao,
    private val settings: SettingsRepository,
) {
    val activeSession: Flow<WorkSession?> = dao.observeActive()
    val allSessions: Flow<List<WorkSession>> = dao.observeAll()
    val taskNames: Flow<List<String>> = dao.observeTaskNames()
    val taskNotes: Flow<List<TaskNote>> = taskNoteDao.observeAll()
    val hourlyRateRub: Flow<Int> = settings.hourlyRateRub
    val lastTaskName: Flow<String> = settings.lastTaskName

    val timerState: Flow<TimerSnapshot> = combine(
        activeSession,
        lastTaskName,
        hourlyRateRub,
    ) { active, taskName, rate ->
        TimerSnapshot(
            activeSession = active,
            currentTaskName = active?.taskName ?: taskName,
            hourlyRateRub = rate,
        )
    }

    suspend fun getActiveSession(): WorkSession? = dao.getActive()

    suspend fun startSession(taskName: String): WorkSession {
        val trimmed = taskName.trim().ifEmpty { SettingsRepository.DEFAULT_TASK_NAME }
        check(dao.getActive() == null) { "Session already active" }
        settings.setLastTaskName(trimmed)
        val session = WorkSession(taskName = trimmed, startedAt = System.currentTimeMillis())
        val id = dao.insert(session)
        return session.copy(id = id)
    }

    suspend fun stopSession(): WorkSession? {
        val active = dao.getActive() ?: return null
        val stopped = active.copy(endedAt = System.currentTimeMillis())
        dao.update(stopped)
        return stopped
    }

    data class SwitchResult(
        val closedSession: WorkSession,
        val newSession: WorkSession,
    )

    suspend fun switchTaskWhileRecording(newTaskName: String): SwitchResult {
        val active = dao.getActive() ?: error("No active session")
        val trimmed = newTaskName.trim().ifEmpty { SettingsRepository.DEFAULT_TASK_NAME }
        if (trimmed == active.taskName) {
            settings.setLastTaskName(trimmed)
            return SwitchResult(active, active)
        }

        val closed = active.copy(endedAt = System.currentTimeMillis())
        dao.update(closed)

        settings.setLastTaskName(trimmed)
        val newSession = WorkSession(taskName = trimmed, startedAt = System.currentTimeMillis())
        val id = dao.insert(newSession)
        return SwitchResult(closed, newSession.copy(id = id))
    }

    suspend fun updateLastTaskName(taskName: String) {
        val trimmed = taskName.trim().ifEmpty { SettingsRepository.DEFAULT_TASK_NAME }
        settings.setLastTaskName(trimmed)
    }

    suspend fun updateTaskComment(taskName: String, comment: String?) {
        val trimmedComment = comment?.trim()?.takeIf { it.isNotEmpty() }
        taskNoteDao.upsert(TaskNote(taskName = taskName, comment = trimmedComment))
    }

    suspend fun setHourlyRateRub(value: Int) {
        settings.setHourlyRateRub(value)
    }

    suspend fun getTodayStats(now: Long = System.currentTimeMillis()): TodayStats {
        val (start, end) = dayBounds(now)
        val sessions = dao.getSessionsOverlappingDay(start, end)
        val durationMs = sessions.sumOf { session ->
            overlapDurationMs(session.startedAt, session.endedAt, start, end, now)
        }
        val rate = settings.hourlyRateRub.first()
        return TodayStats(durationMs = durationMs, amountRub = amountRub(durationMs, rate))
    }

    private fun overlapDurationMs(
        startedAt: Long,
        endedAt: Long?,
        dayStart: Long,
        dayEnd: Long,
        now: Long,
    ): Long {
        val sessionEnd = endedAt ?: now
        val overlapStart = maxOf(startedAt, dayStart)
        val overlapEnd = minOf(sessionEnd, dayEnd)
        return (overlapEnd - overlapStart).coerceAtLeast(0)
    }

    suspend fun buildSummaries(hourlyRate: Int, sessions: List<WorkSession>? = null): List<TaskSummary> {
        val all = sessions ?: dao.getAllSessions()
        val notes = taskNoteDao.observeAll().first().associate { it.taskName to it.comment }
        val now = System.currentTimeMillis()
        return all
            .groupBy { it.taskName }
            .map { (name, group) ->
                TaskSummary(
                    taskName = name,
                    totalDurationMs = group.sumOf { it.durationMs(now) },
                    taskComment = notes[name],
                )
            }
            .sortedByDescending { it.totalDurationMs }
    }

    suspend fun buildDetails(hourlyRate: Int, sessions: List<WorkSession>? = null): List<SessionDetail> {
        val all = sessions ?: dao.getAllSessions()
        val now = System.currentTimeMillis()
        return all
            .sortedByDescending { it.startedAt }
            .map { session ->
                val duration = session.durationMs(now)
                SessionDetail(
                    session = session,
                    durationMs = duration,
                    amountRub = amountRub(duration, hourlyRate),
                )
            }
    }

    suspend fun clearAll() {
        dao.deleteAll()
        taskNoteDao.deleteAll()
    }

    fun amountRub(durationMs: Long, hourlyRate: Int): Int {
        return ((durationMs / 3_600_000.0) * hourlyRate).toInt()
    }

    private fun dayBounds(now: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return start to cal.timeInMillis
    }

    data class TimerSnapshot(
        val activeSession: WorkSession?,
        val currentTaskName: String,
        val hourlyRateRub: Int,
    )

    data class TodayStats(
        val durationMs: Long,
        val amountRub: Int,
    )
}
