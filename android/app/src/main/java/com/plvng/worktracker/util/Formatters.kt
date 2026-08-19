package com.plvng.worktracker.util

import java.util.Locale

object DurationFormatter {
    fun formatClock(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    fun formatHuman(durationMs: Long): String {
        val totalMinutes = durationMs / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}ч ${minutes}м"
            hours > 0 -> "${hours}ч"
            minutes > 0 -> "${minutes}м"
            else -> "< 1м"
        }
    }

    fun formatHoursDecimal(durationMs: Long): String {
        val hours = durationMs / 3_600_000.0
        return String.format(Locale.US, "%.2f", hours)
    }

    fun formatTime(timestampMs: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestampMs }
        return String.format(
            Locale.getDefault(),
            "%02d:%02d",
            cal.get(java.util.Calendar.HOUR_OF_DAY),
            cal.get(java.util.Calendar.MINUTE),
        )
    }

    fun formatDate(timestampMs: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestampMs }
        return String.format(
            Locale.getDefault(),
            "%04d-%02d-%02d",
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH),
        )
    }
}

object MoneyFormatter {
    fun formatRub(amount: Int): String {
        return String.format(Locale("ru", "RU"), "%,d ₽", amount).replace(',', ' ')
    }
}

object HapticHelper {
    fun performConfirm(view: android.view.View) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
    }

    fun performTick(view: android.view.View) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }
}
