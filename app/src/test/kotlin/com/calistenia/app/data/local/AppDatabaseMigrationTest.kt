package com.calistenia.app.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {
    @Test fun `v2 workout data and readiness survive migration to v3`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${System.nanoTime()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = createV2(db)
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO workout_sessions VALUES ('workout','session',1000,NULL,0,'IN_PROGRESS',2,3,4,5)")
        db.execSQL("INSERT INTO exercise_sessions VALUES (7,'workout','push',3,0,0)")
        db.execSQL("INSERT INTO set_logs VALUES (9,7,0,12,10,NULL,2,'SHARP_PAIN',1)")

        AppDatabase.MIGRATION_2_3.migrate(db)

        db.query("SELECT readinessEnergy, readinessSleep, readinessSoreness, readinessMotivation FROM workout_sessions").use {
            assertTrue(it.moveToFirst()); assertEquals(2, it.getInt(0)); assertEquals(3, it.getInt(1)); assertEquals(4, it.getInt(2)); assertEquals(5, it.getInt(3))
        }
        db.query("SELECT completionStatus FROM exercise_sessions WHERE id=7").use { assertTrue(it.moveToFirst()); assertEquals("COMPLETED", it.getString(0)) }
        db.query("SELECT status, recordedAt, discomfort FROM set_logs WHERE id=9").use {
            assertTrue(it.moveToFirst()); assertEquals("COMPLETED", it.getString(0)); assertEquals(0L, it.getLong(1)); assertEquals("SHARP_PAIN", it.getString(2))
        }
        db.query("SELECT name FROM sqlite_master WHERE type='index' AND name IN ('index_exercise_sessions_workoutId_orderIndex','index_set_logs_exerciseSessionId_setIndex')").use { assertEquals(2, it.count) }
        helper.close(); context.deleteDatabase(name)
    }

    private fun createV2(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE workout_sessions (id TEXT NOT NULL PRIMARY KEY, plannedSessionId TEXT, startedAt INTEGER NOT NULL, completedAt INTEGER, durationMinutes INTEGER NOT NULL, status TEXT NOT NULL, readinessEnergy INTEGER, readinessSleep INTEGER, readinessSoreness INTEGER, readinessMotivation INTEGER)")
        db.execSQL("CREATE TABLE exercise_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, workoutId TEXT NOT NULL, exerciseId TEXT NOT NULL, plannedSets INTEGER NOT NULL, orderIndex INTEGER NOT NULL, skipped INTEGER NOT NULL, FOREIGN KEY(workoutId) REFERENCES workout_sessions(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX index_exercise_sessions_workoutId ON exercise_sessions(workoutId)")
        db.execSQL("CREATE INDEX index_exercise_sessions_exerciseId ON exercise_sessions(exerciseId)")
        db.execSQL("CREATE TABLE set_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, exerciseSessionId INTEGER NOT NULL, setIndex INTEGER NOT NULL, plannedValue INTEGER NOT NULL, actualReps INTEGER, actualSeconds INTEGER, rir INTEGER, discomfort TEXT NOT NULL, techniqueGood INTEGER NOT NULL, FOREIGN KEY(exerciseSessionId) REFERENCES exercise_sessions(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX index_set_logs_exerciseSessionId ON set_logs(exerciseSessionId)")
    }
}
