package com.plvng.worktracker

import com.plvng.worktracker.data.TaskSummary
import com.plvng.worktracker.data.WorkSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class WorkRepositoryLogicTest {
    @Test
    fun amountRub_calculatesFromDurationAndRate() {
        val repo = FakeRepo()
        assertEquals(1000, repo.amountRub(3_600_000, 1000))
        assertEquals(500, repo.amountRub(1_800_000, 1000))
    }

    @Test
    fun summaries_groupByTaskName() {
        val now = 1_000_000L
        val sessions = listOf(
            WorkSession(1, "API", 0, 3_600_000),
            WorkSession(2, "UI", 0, 1_800_000),
            WorkSession(3, "API", 4_000_000, 7_200_000),
        )
        val summaries = sessions
            .groupBy { it.taskName }
            .map { (name, group) ->
                TaskSummary(
                    taskName = name,
                    totalDurationMs = group.sumOf { it.durationMs(now) },
                    taskComment = null,
                )
            }
            .sortedByDescending { it.totalDurationMs }

        assertEquals("API", summaries[0].taskName)
        assertEquals(6_800_000, summaries[0].totalDurationMs)
        assertEquals("UI", summaries[1].taskName)
    }

    @Test
    fun overlapDuration_countsOnlyTodayPortion() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 19, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dayEnd = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, -1)
        cal.add(Calendar.HOUR_OF_DAY, -2)
        val startedYesterday = cal.timeInMillis

        val now = dayStart + 2 * 3_600_000
        val overlap = overlapDurationMs(startedYesterday, null, dayStart, dayEnd, now)
        assertEquals(2 * 3_600_000L, overlap)
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

    private class FakeRepo {
        fun amountRub(durationMs: Long, hourlyRate: Int): Int {
            return ((durationMs / 3_600_000.0) * hourlyRate).toInt()
        }
    }
}
