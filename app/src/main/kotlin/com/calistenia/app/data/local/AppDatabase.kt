package com.calistenia.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AppSetupEntity::class, UserProfileEntity::class, FunctionalLevelEntity::class, AssessmentEntity::class, ExerciseEntity::class, TrainingPlanEntity::class, PlannedSessionEntity::class, PlannedExerciseEntity::class, WorkoutSessionEntity::class, ExerciseSessionEntity::class, SetLogEntity::class, MeasurementEntity::class], version = 2, exportSchema = true)
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
    }
}
