package com.plvng.worktracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.plvng.worktracker.data.SettingsRepository
import com.plvng.worktracker.data.WorkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val hourlyRateRub: Int = SettingsRepository.DEFAULT_HOURLY_RATE,
    val rateInput: String = SettingsRepository.DEFAULT_HOURLY_RATE.toString(),
    val message: String? = null,
)

class SettingsViewModel(
    private val repository: WorkRepository,
) : ViewModel() {
    private val rateInput = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.hourlyRateRub,
        rateInput,
        message,
    ) { rate, input, msg ->
        SettingsUiState(
            hourlyRateRub = rate,
            rateInput = input ?: rate.toString(),
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onRateInputChange(value: String) {
        rateInput.value = value.filter { it.isDigit() }
    }

    fun saveRate() {
        viewModelScope.launch {
            val parsed = (rateInput.value ?: uiState.value.rateInput).toIntOrNull()
            if (parsed == null || parsed < 1) {
                message.value = "Введите ставку от 1 ₽/ч"
                return@launch
            }
            repository.setHourlyRateRub(parsed)
            rateInput.value = null
            message.value = "Сохранено"
        }
    }

    fun clearAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.clearAll()
            message.value = "Все данные удалены"
            onDone()
        }
    }

    fun clearMessage() {
        message.value = null
    }

    class Factory(private val repository: WorkRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
