package com.calistenia.app.data

import androidx.room.withTransaction
import com.calistenia.app.data.local.*
import com.calistenia.domain.model.*
import com.calistenia.domain.engine.ScheduleRebalancer
import com.calistenia.domain.usecase.ResolveSetupStateUseCase
import kotlinx.coroutines.flow.Flow
import java.time.*
import java.util.UUID

class AppRepository(private val db: AppDatabase) {
    private val dao = db.dao()
    fun profile(): Flow<UserProfileEntity?> = dao.observeProfile()
    fun planSessions(): Flow<List<SessionWithExercises>> = dao.observePlanSessions()
    fun exercises(): Flow<List<ExerciseEntity>> = dao.observeExercises()
    fun history(): Flow<List<WorkoutSessionEntity>> = dao.observeHistory()
    fun inProgress(): Flow<WorkoutSessionEntity?> = dao.observeInProgressWorkout()

    suspend fun seed() { if (dao.exerciseCount() == 0) dao.insertExercises(ExerciseSeed.exercises) }
    suspend fun saveProfile(profile: UserProfileEntity) = dao.saveProfile(profile)
    suspend fun acceptSafety() = dao.saveAppSetup(AppSetupEntity(safetyAccepted = true))

    suspend fun saveAssessment(levels: Map<MovementPattern, Int>) {
        val now = System.currentTimeMillis()
        dao.saveLevels(levels.map { FunctionalLevelEntity(it.key.name, it.value, now) })
        dao.saveAssessments(levels.map { AssessmentEntity(assessedAt = now, exerciseId = "initial_${it.key.name.lowercase()}", pattern = it.key.name, repetitions = null, durationSeconds = null, estimatedLevel = it.value) })
    }

    suspend fun workoutHistory(): WorkoutHistory = WorkoutHistory(dao.completedWorkouts().mapNotNull { row ->
        val completedAt = row.workout.completedAt ?: return@mapNotNull null
        CompletedWorkout(row.workout.id, row.workout.plannedSessionId, instant(completedAt), row.workout.durationMinutes,
            row.exercises.sortedBy { it.exerciseSession.orderIndex }.filterNot { it.exerciseSession.skipped }.map { exercise ->
                ExercisePerformance(exercise.exerciseSession.exerciseId, exercise.sets.sortedBy { it.setIndex }.map { set ->
                    SetPerformance(set.plannedValue.takeIf { set.actualReps != null }, set.actualReps, set.plannedValue.takeIf { set.actualSeconds != null }, set.actualSeconds, set.rir, Discomfort.valueOf(set.discomfort), set.techniqueGood)
                }, instant(completedAt))
            })
    })

    suspend fun generationContext(today: LocalDate = LocalDate.now(), readiness: Readiness? = null): GenerationContext? {
        val profile = dao.profile() ?: return null
        val levels = dao.levels().associate { MovementPattern.valueOf(it.pattern) to it.level }
        val history = workoutHistory()
        return GenerationContext(profile.toDomain(), FunctionalProfile(levels), dao.exercises().map { it.toDomain() }, history, history.sessions.take(8), readiness, today)
    }

    suspend fun setupState(): SetupState {
        val hasProfile = dao.profile() != null
        val safetyAccepted = dao.appSetup()?.safetyAccepted == true || hasProfile
        return ResolveSetupStateUseCase()(safetyAccepted, hasProfile, dao.levelCount() > 0, dao.planSessionCount() > 0)
    }

    suspend fun rebalanceSchedule(today: LocalDate = LocalDate.now()) {
        val planId = dao.latestWeeklyPlanId() ?: return
        val rows = dao.sessionsForPlan(planId)
        val sessions = rows.map { row -> PlannedSession(row.session.id, LocalDate.ofEpochDay(row.session.dateEpochDay), row.session.title, row.exerciseRows.sortedBy { it.planned.priority }.map { item -> PlannedExercise(item.exercise.toDomain(), item.planned.sets, item.planned.targetMin, item.planned.targetMax, item.planned.restSeconds, item.planned.priority, item.planned.rationale) }, row.session.estimatedMinutes, SessionStatus.valueOf(row.session.status)) }
        val completed = sessions.filter { it.status == SessionStatus.COMPLETED }.map { it.id }.toSet()
        val result = ScheduleRebalancer().rebalance(TrainingPlan(planId, sessions.minOf { it.date }.with(DayOfWeek.MONDAY), sessions, emptyList()), today, completed)
        dao.markMissedSkipped(today.toEpochDay())
        savePlan(result)
    }

    suspend fun savePlan(plan: TrainingPlan) = db.withTransaction {
        dao.savePlan(TrainingPlanEntity(plan.id, plan.weekStart.toEpochDay(), System.currentTimeMillis()))
        plan.sessions.forEach { session ->
            val entity = PlannedSessionEntity(session.id, plan.id, session.date.toEpochDay(), session.title, session.estimatedMinutes, session.status.name)
            dao.insertSessions(listOf(entity))
            dao.updateMutableSession(entity.id, entity.dateEpochDay, entity.title, entity.estimatedMinutes)
            if (dao.isMutableSession(entity.id)) {
                dao.deleteMutableExercises(entity.id)
                dao.savePlannedExercises(session.exercises.map { PlannedExerciseEntity(sessionId = session.id, exerciseId = it.exercise.id, sets = it.sets, targetMin = it.targetMin, targetMax = it.targetMax, restSeconds = it.restSeconds, priority = it.priority, rationale = it.rationale) })
            }
        }
    }

    suspend fun startSession(sessionId: String, readiness: Readiness, now: Long = System.currentTimeMillis()): String = db.withTransaction {
        dao.inProgressWorkout(sessionId)?.id ?: UUID.randomUUID().toString().also { id ->
            dao.startWorkout(WorkoutSessionEntity(id, sessionId, now, null, 0, SessionStatus.IN_PROGRESS.name, readiness.energy, readiness.sleep, readiness.soreness, readiness.motivation))
            dao.markInProgress(sessionId)
        }
    }

    suspend fun completeSession(sessionId: String, values: List<SetInput>, now: Long = System.currentTimeMillis()) = db.withTransaction {
        val session = dao.plannedSession(sessionId) ?: return@withTransaction
        val workout = dao.inProgressWorkout(sessionId) ?: error("A sessão precisa ser iniciada antes da conclusão")
        session.exerciseRows.sortedBy { it.planned.priority }.forEachIndexed { index, row ->
            val inputs = values.filter { it.exerciseId == row.exercise.id }
            val exerciseSessionId = dao.addExerciseSession(ExerciseSessionEntity(workoutId = workout.id, exerciseId = row.exercise.id, plannedSets = row.planned.sets, orderIndex = index, skipped = inputs.any { it.skipped }))
            inputs.filterNot { it.skipped }.forEachIndexed { setIndex, input ->
                dao.addSetLog(SetLogEntity(exerciseSessionId = exerciseSessionId, setIndex = setIndex, plannedValue = row.planned.targetMax, actualReps = input.value.takeIf { row.exercise.prescriptionType == "REPETITIONS" }, actualSeconds = input.value.takeIf { row.exercise.prescriptionType == "TIME" }, rir = input.rir, discomfort = input.discomfort.name, techniqueGood = input.techniqueGood))
            }
        }
        val minutes = ((now - workout.startedAt).coerceAtLeast(0) / 60_000L).toInt()
        dao.completeWorkout(workout.id, now, minutes)
        dao.completePlannedSession(sessionId)
    }

    private fun instant(value: Long) = LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault())
}

data class SetInput(val exerciseId: String, val value: Int = 0, val rir: Int? = null, val discomfort: Discomfort = Discomfort.NONE, val techniqueGood: Boolean = true, val skipped: Boolean = false)

fun UserProfileEntity.toDomain() = UserProfile(id, age, sex, heightCm, weightKg, Goal.valueOf(primaryGoal), secondaryGoals.split(',').filter(String::isNotBlank).map(Goal::valueOf).toSet(), ExperienceLevel.valueOf(experience), ExperienceLevel.valueOf(calisthenicsExperience), weeklyFrequency, Availability(availableDays.split(',').filter(String::isNotBlank).map { DayOfWeek.valueOf(it) }.toSet(), minutesPerSession), location, equipment.split(',').filter(String::isNotBlank).map(Equipment::valueOf).toSet(), preferences, limitations)
fun ExerciseEntity.toDomain() = Exercise(id, name, description, instructions, MovementPattern.valueOf(movementPattern), muscleGroups.split(',').filter(String::isNotBlank).toSet(), difficultyLevel, requiredEquipment.split(',').filter(String::isNotBlank).map(Equipment::valueOf).toSet(), minimumSuggestedLevel, progressionExerciseId, regressionExerciseId, PrescriptionType.valueOf(prescriptionType), if (minReps != null && maxReps != null) minReps..maxReps else null, durationSeconds, restSeconds, unilateral, tags.split(',').filter(String::isNotBlank).toSet(), techniqueCues.split('|'), commonMistakes.split('|'), mediaAsset, active)
