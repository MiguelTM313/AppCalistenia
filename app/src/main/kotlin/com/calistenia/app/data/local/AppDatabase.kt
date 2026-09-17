package com.calistenia.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AppSetupEntity::class, UserProfileEntity::class, FunctionalLevelEntity::class, AssessmentEntity::class, ExerciseEntity::class, TrainingPlanEntity::class, PlannedSessionEntity::class, PlannedExerciseEntity::class, WorkoutSessionEntity::class, ExerciseSessionEntity::class, SetLogEntity::class, MeasurementEntity::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN readinessEnergy INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN readinessSleep INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN readinessSoreness INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN readinessMotivation INTEGER")
                db.execSQL("CREATE TABLE IF NOT EXISTS app_setup (id INTEGER NOT NULL, safetyAccepted INTEGER NOT NULL, PRIMARY KEY(id))")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise_sessions ADD COLUMN completionStatus TEXT NOT NULL DEFAULT 'PENDING'")
                db.execSQL("UPDATE exercise_sessions SET completionStatus = CASE WHEN skipped = 1 THEN 'FULLY_SKIPPED' ELSE 'COMPLETED' END")
                db.execSQL("ALTER TABLE set_logs ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'")
                db.execSQL("ALTER TABLE set_logs ADD COLUMN recordedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    INSERT INTO exercise_sessions (workoutId, exerciseId, plannedSets, orderIndex, skipped, completionStatus)
                    SELECT workout.id, planned.exerciseId, planned.sets, planned.priority, 0, 'PENDING'
                    FROM workout_sessions AS workout
                    INNER JOIN planned_exercises AS planned ON planned.sessionId = workout.plannedSessionId
                    WHERE workout.status = 'IN_PROGRESS'
                      AND NOT EXISTS (
                          SELECT 1 FROM exercise_sessions AS execution
                          WHERE execution.workoutId = workout.id
                            AND execution.orderIndex = planned.priority
                      )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_exercise_sessions_workoutId_orderIndex ON exercise_sessions(workoutId, orderIndex)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_set_logs_exerciseSessionId_setIndex ON set_logs(exerciseSessionId, setIndex)")
            }
        }
    }
}
