package com.calistenia.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM user_profile WHERE id = 1") fun observeProfile(): Flow<UserProfileEntity?>
    @Query("SELECT * FROM user_profile WHERE id = 1") suspend fun profile(): UserProfileEntity?
    @Upsert suspend fun saveProfile(profile: UserProfileEntity)
    @Query("SELECT * FROM functional_levels") suspend fun levels(): List<FunctionalLevelEntity>
    @Upsert suspend fun saveLevels(levels: List<FunctionalLevelEntity>)
    @Insert suspend fun saveAssessments(results: List<AssessmentEntity>)
    @Query("SELECT * FROM exercises WHERE active = 1 ORDER BY movementPattern, difficultyLevel") fun observeExercises(): Flow<List<ExerciseEntity>>
    @Query("SELECT * FROM exercises WHERE active = 1") suspend fun exercises(): List<ExerciseEntity>
    @Query("SELECT COUNT(*) FROM exercises") suspend fun exerciseCount(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExercises(exercises: List<ExerciseEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePlan(plan: TrainingPlanEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSessions(sessions: List<PlannedSessionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePlannedExercises(exercises: List<PlannedExerciseEntity>)
    @Transaction @Query("SELECT * FROM planned_sessions ORDER BY dateEpochDay") fun observePlanSessions(): Flow<List<SessionWithExercises>>
    @Transaction @Query("SELECT * FROM planned_sessions WHERE id = :id") suspend fun plannedSession(id: String): SessionWithExercises?
    @Insert suspend fun startWorkout(workout: WorkoutSessionEntity)
    @Insert suspend fun addExerciseSession(session: ExerciseSessionEntity): Long
    @Insert suspend fun addSetLog(log: SetLogEntity)
    @Query("UPDATE workout_sessions SET completedAt = :completedAt, durationMinutes = :minutes, status = 'COMPLETED' WHERE id = :id") suspend fun completeWorkout(id: String, completedAt: Long, minutes: Int)
    @Query("UPDATE planned_sessions SET status = 'COMPLETED' WHERE id = :id") suspend fun completePlannedSession(id: String)
    @Query("SELECT * FROM workout_sessions WHERE status = 'COMPLETED' ORDER BY completedAt DESC") fun observeHistory(): Flow<List<WorkoutSessionEntity>>
}
