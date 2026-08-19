package com.plvng.worktracker

import android.app.Application
import com.plvng.worktracker.data.SettingsRepository
import com.plvng.worktracker.data.WorkRepository
import com.plvng.worktracker.data.WorkTrackerDatabase

class WorkTrackerApp : Application() {
    lateinit var repository: WorkRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = WorkTrackerDatabase.get(this)
        val settings = SettingsRepository(this)
        repository = WorkRepository(db.workSessionDao(), db.taskNoteDao(), settings)
    }
}
