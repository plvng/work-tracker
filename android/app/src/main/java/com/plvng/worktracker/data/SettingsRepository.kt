package com.plvng.worktracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "work_tracker_prefs")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val hourlyRateRub = intPreferencesKey("hourly_rate_rub")
        val lastTaskName = stringPreferencesKey("last_task_name")
    }

    val hourlyRateRub: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.hourlyRateRub] ?: DEFAULT_HOURLY_RATE
    }

    val lastTaskName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.lastTaskName] ?: DEFAULT_TASK_NAME
    }

    suspend fun setHourlyRateRub(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.hourlyRateRub] = value.coerceAtLeast(1)
        }
    }

    suspend fun setLastTaskName(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        context.dataStore.edit { prefs ->
            prefs[Keys.lastTaskName] = trimmed
        }
    }

    companion object {
        const val DEFAULT_HOURLY_RATE = 1000
        const val DEFAULT_TASK_NAME = "Новая задача"
    }
}
