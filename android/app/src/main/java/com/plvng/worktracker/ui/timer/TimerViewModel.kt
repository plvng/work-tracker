package com.plvng.worktracker.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.plvng.worktracker.data.SettingsRepository
import com.plvng.worktracker.data.WorkRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TimerUiState(
    val taskName: String = SettingsRepository.DEFAULT_TASK_NAME,
    val isRecording: Boolean = false,
    val elapsedMs: Long = 0L,
    val todayDurationMs: Long = 0L,
    val todayAmountRub: Int = 0,
    val hourlyRateRub: Int = SettingsRepository.DEFAULT_HOURLY_RATE,
    val showTaskSheet: Boolean = false,
    val taskNameSuggestions: List<String> = emptyList(),
)

class TimerViewModel(
    private val repository: WorkRepository,
) : ViewModel() {
    private val tick = MutableStateFlow(System.currentTimeMillis())
    private val todayStats = MutableStateFlow(WorkRepository.TodayStats(0, 0))
    private val showTaskSheet = MutableStateFlow(false)

    val uiState: StateFlow<TimerUiState> = combine(
        repository.timerState,
        tick,
        todayStats,
        repository.taskNames,
        showTaskSheet,
    ) { snapshot, now, today, suggestions, sheetOpen ->
        val active = snapshot.activeSession
        TimerUiState(
            taskName = snapshot.currentTaskName,
            isRecording = active != null,
            elapsedMs = active?.durationMs(now) ?: 0L,
            todayDurationMs = today.durationMs,
            todayAmountRub = today.amountRub,
            hourlyRateRub = snapshot.hourlyRateRub,
            showTaskSheet = sheetOpen,
            taskNameSuggestions = suggestions,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerUiState())

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                tick.value = System.currentTimeMillis()
                todayStats.value = repository.getTodayStats()
            }
        }
        viewModelScope.launch {
            todayStats.value = repository.getTodayStats()
        }
    }

    fun toggleRecording() {
        viewModelScope.launch {
            if (repository.getActiveSession() != null) {
                repository.stopSession()
            } else {
                repository.startSession(uiState.value.taskName)
            }
            todayStats.value = repository.getTodayStats()
        }
    }

    fun openTaskSheet() {
        showTaskSheet.value = true
    }

    fun dismissTaskSheet() {
        showTaskSheet.value = false
    }

    fun saveTaskName(newName: String) {
        viewModelScope.launch {
            val trimmed = newName.trim().ifEmpty { SettingsRepository.DEFAULT_TASK_NAME }
            val active = repository.getActiveSession()
            showTaskSheet.value = false

            if (active != null && trimmed != active.taskName) {
                repository.switchTaskWhileRecording(trimmed)
            } else {
                repository.updateLastTaskName(trimmed)
            }
            todayStats.value = repository.getTodayStats()
        }
    }

    class Factory(private val repository: WorkRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TimerViewModel(repository) as T
        }
    }
}
