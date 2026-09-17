package com.calistenia.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UserProfileEntity::class, FunctionalLevelEntity::class, AssessmentEntity::class, ExerciseEntity::class, TrainingPlanEntity::class, PlannedSessionEntity::class, PlannedExerciseEntity::class, WorkoutSessionEntity::class, ExerciseSessionEntity::class, SetLogEntity::class, MeasurementEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() { abstract fun dao(): AppDao }
