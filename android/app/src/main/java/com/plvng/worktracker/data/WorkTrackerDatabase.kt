package com.plvng.worktracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WorkSession::class, TaskNote::class],
    version = 2,
    exportSchema = false,
)
abstract class WorkTrackerDatabase : RoomDatabase() {
    abstract fun workSessionDao(): WorkSessionDao
    abstract fun taskNoteDao(): TaskNoteDao

    companion object {
        @Volatile
        private var instance: WorkTrackerDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_notes (
                        taskName TEXT NOT NULL PRIMARY KEY,
                        comment TEXT
                    )
                    """.trimIndent(),
                )
            }
        }

        fun get(context: Context): WorkTrackerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorkTrackerDatabase::class.java,
                    "work_tracker.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
