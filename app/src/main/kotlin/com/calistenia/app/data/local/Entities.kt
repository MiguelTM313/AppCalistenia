package com.calistenia.app.data.local

import androidx.room.*

@Entity(tableName = "app_setup")
data class AppSetupEntity(@PrimaryKey val id: Int = 1, val safetyAccepted: Boolean)

@Entity(tableName = "user_profile")
data class UserProfileEntity(@PrimaryKey val id: Long = 1, val age: Int, val sex: String?, val heightCm: Int, val weightKg: Double, val primaryGoal: String, val secondaryGoals: String, val experience: String, val calisthenicsExperience: String, val weeklyFrequency: Int, val availableDays: String, val minutesPerSession: Int, val location: String, val equipment: String, val preferences: String, val limitations: String, val onboardingComplete: Boolean)

@Entity(tableName = "functional_levels")
data class FunctionalLevelEntity(@PrimaryKey val pattern: String, val level: Int, val assessedAt: Long)

@Entity(tableName = "assessment_results")
data class AssessmentEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val assessedAt: Long, val exerciseId: String, val pattern: String, val repetitions: Int?, val durationSeconds: Int?, val estimatedLevel: Int)

@Entity(tableName = "exercises")
data class ExerciseEntity(@PrimaryKey val id: String, val name: String, val description: String, val instructions: String, val movementPattern: String, val muscleGroups: String, val difficultyLevel: Int, val requiredEquipment: String, val minimumSuggestedLevel: Int, val progressionExerciseId: String?, val regressionExerciseId: String?, val prescriptionType: String, val minReps: Int?, val maxReps: Int?, val durationSeconds: Int?, val restSeconds: Int, val unilateral: Boolean, val tags: String, val techniqueCues: String, val commonMistakes: String, val mediaAsset: String?, val active: Boolean)

@Entity(tableName = "training_plans")
data class TrainingPlanEntity(@PrimaryKey val id: String, val weekStartEpochDay: Long, val createdAt: Long)

@Entity(tableName = "planned_sessions", foreignKeys = [ForeignKey(entity = TrainingPlanEntity::class, parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.CASCADE)], indices = [Index("planId")])
data class PlannedSessionEntity(@PrimaryKey val id: String, val planId: String, val dateEpochDay: Long, val title: String, val estimatedMinutes: Int, val status: String)

@Entity(tableName = "planned_exercises", foreignKeys = [ForeignKey(entity = PlannedSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)], indices = [Index("sessionId"), Index("exerciseId")])
data class PlannedExerciseEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val sessionId: String, val exerciseId: String, val sets: Int, val targetMin: Int, val targetMax: Int, val restSeconds: Int, val priority: Int, val rationale: String)

@Entity(tableName = "workout_sessions", indices = [Index("plannedSessionId")])
data class WorkoutSessionEntity(@PrimaryKey val id: String, val plannedSessionId: String?, val startedAt: Long, val completedAt: Long?, val durationMinutes: Int, val status: String, val readinessEnergy: Int? = null, val readinessSleep: Int? = null, val readinessSoreness: Int? = null, val readinessMotivation: Int? = null)

@Entity(tableName = "exercise_sessions", foreignKeys = [ForeignKey(entity = WorkoutSessionEntity::class, parentColumns = ["id"], childColumns = ["workoutId"], onDelete = ForeignKey.CASCADE)], indices = [Index("workoutId"), Index("exerciseId")])
data class ExerciseSessionEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val workoutId: String, val exerciseId: String, val plannedSets: Int, val orderIndex: Int, val skipped: Boolean = false)

@Entity(tableName = "set_logs", foreignKeys = [ForeignKey(entity = ExerciseSessionEntity::class, parentColumns = ["id"], childColumns = ["exerciseSessionId"], onDelete = ForeignKey.CASCADE)], indices = [Index("exerciseSessionId")])
data class SetLogEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val exerciseSessionId: Long, val setIndex: Int, val plannedValue: Int, val actualReps: Int?, val actualSeconds: Int?, val rir: Int?, val discomfort: String, val techniqueGood: Boolean)

@Entity(tableName = "measurements")
data class MeasurementEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val recordedAt: Long, val weightKg: Double?, val notes: String)

data class PlannedExerciseRow(@Embedded val planned: PlannedExerciseEntity, @Relation(parentColumn = "exerciseId", entityColumn = "id") val exercise: ExerciseEntity)
data class SessionWithExercises(@Embedded val session: PlannedSessionEntity, @Relation(entity = PlannedExerciseEntity::class, parentColumn = "id", entityColumn = "sessionId") val exerciseRows: List<PlannedExerciseRow>)
data class ExerciseSessionWithSets(@Embedded val exerciseSession: ExerciseSessionEntity, @Relation(parentColumn = "id", entityColumn = "exerciseSessionId") val sets: List<SetLogEntity>)
data class WorkoutWithExercises(@Embedded val workout: WorkoutSessionEntity, @Relation(entity = ExerciseSessionEntity::class, parentColumn = "id", entityColumn = "workoutId") val exercises: List<ExerciseSessionWithSets>)
