package com.calistenia.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM app_setup WHERE id=1") suspend fun appSetup(): AppSetupEntity?
    @Upsert suspend fun saveAppSetup(setup: AppSetupEntity)
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
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun savePlan(plan: TrainingPlanEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSessions(sessions: List<PlannedSessionEntity>)
    @Query("UPDATE planned_sessions SET dateEpochDay=:date, title=:title, estimatedMinutes=:minutes WHERE id=:id AND status='PLANNED'") suspend fun updateMutableSession(id: String, date: Long, title: String, minutes: Int)
    @Query("DELETE FROM planned_exercises WHERE sessionId=:sessionId AND EXISTS (SELECT 1 FROM planned_sessions WHERE id=:sessionId AND status='PLANNED')") suspend fun deleteMutableExercises(sessionId: String)
    @Query("SELECT status='PLANNED' FROM planned_sessions WHERE id=:id") suspend fun isMutableSession(id: String): Boolean
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePlannedExercises(exercises: List<PlannedExerciseEntity>)
    @Transaction @Query("SELECT * FROM planned_sessions ORDER BY dateEpochDay") fun observePlanSessions(): Flow<List<SessionWithExercises>>
    @Transaction @Query("SELECT * FROM planned_sessions WHERE planId=:planId ORDER BY dateEpochDay") suspend fun sessionsForPlan(planId: String): List<SessionWithExercises>
    @Query("SELECT id FROM training_plans WHERE id LIKE 'plan-%' ORDER BY createdAt DESC LIMIT 1") suspend fun latestWeeklyPlanId(): String?
    @Query("UPDATE planned_sessions SET status='SKIPPED' WHERE dateEpochDay < :today AND status='PLANNED'") suspend fun markMissedSkipped(today: Long)
    @Transaction @Query("SELECT * FROM planned_sessions WHERE id = :id") suspend fun plannedSession(id: String): SessionWithExercises?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun startWorkout(workout: WorkoutSessionEntity)
    @Query("UPDATE planned_sessions SET status='IN_PROGRESS' WHERE id=:id AND status='PLANNED'") suspend fun markInProgress(id: String)
    @Query("SELECT * FROM workout_sessions WHERE plannedSessionId=:sessionId AND status='IN_PROGRESS' LIMIT 1") suspend fun inProgressWorkout(sessionId: String): WorkoutSessionEntity?
    @Query("SELECT * FROM workout_sessions WHERE status='IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1") fun observeInProgressWorkout(): Flow<WorkoutSessionEntity?>
    @Insert suspend fun addExerciseSession(session: ExerciseSessionEntity): Long
    @Insert suspend fun addSetLog(log: SetLogEntity)
    @Query("UPDATE workout_sessions SET completedAt = :completedAt, durationMinutes = :minutes, status = 'COMPLETED' WHERE id = :id") suspend fun completeWorkout(id: String, completedAt: Long, minutes: Int)
    @Query("UPDATE planned_sessions SET status = 'COMPLETED' WHERE id = :id") suspend fun completePlannedSession(id: String)
    @Query("SELECT * FROM workout_sessions WHERE status = 'COMPLETED' ORDER BY completedAt DESC") fun observeHistory(): Flow<List<WorkoutSessionEntity>>
    @Transaction @Query("SELECT * FROM workout_sessions WHERE status='COMPLETED' ORDER BY completedAt DESC") suspend fun completedWorkouts(): List<WorkoutWithExercises>
    @Query("SELECT COUNT(*) FROM functional_levels") suspend fun levelCount(): Int
    @Query("SELECT COUNT(*) FROM planned_sessions") suspend fun planSessionCount(): Int
}
