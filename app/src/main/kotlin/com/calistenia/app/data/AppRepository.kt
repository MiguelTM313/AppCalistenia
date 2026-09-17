package com.calistenia.app.data

import androidx.room.withTransaction
import com.calistenia.app.data.local.*
import com.calistenia.domain.model.*
import com.calistenia.domain.engine.ScheduleRebalancer
import com.calistenia.domain.engine.SessionTimeOptimizer
import com.calistenia.domain.usecase.ResolveSetupStateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.*
import java.util.UUID

class AppRepository(private val db: AppDatabase) {
    private val dao = db.dao()
    fun profile(): Flow<UserProfileEntity?> = dao.observeProfile()
    fun planSessions(): Flow<List<SessionWithExercises>> = dao.observePlanSessions()
    fun exercises(): Flow<List<ExerciseEntity>> = dao.observeExercises()
    fun history(): Flow<List<WorkoutSessionEntity>> = dao.observeHistory()
    fun inProgress(): Flow<WorkoutSessionEntity?> = dao.observeInProgressWorkout()
    fun player(sessionId: String): Flow<WorkoutPlayerState?> = dao.observeWorkout(sessionId).map { workout ->
        workout ?: return@map null
        val planned = dao.plannedSession(sessionId) ?: return@map null
        WorkoutPlayerState(planned, workout)
    }

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
            row.exercises.sortedBy { it.exerciseSession.orderIndex }.map { exercise ->
                ExercisePerformance(exercise.exerciseSession.exerciseId, exercise.sets.sortedBy { it.setIndex }.filter { it.status == "COMPLETED" }.map { set ->
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

    suspend fun quickWorkoutContext(minutes: Int, readiness: Readiness, now: LocalDateTime): QuickWorkoutContext? {
        val generation = generationContext(now.toLocalDate(), readiness) ?: return null
        val planId = dao.latestWeeklyPlanId()
        val weekly = planId?.let { dao.sessionsForPlan(it).map(::toDomainSession) }.orEmpty()
        val weekStart = weekly.minOfOrNull { it.date }?.with(DayOfWeek.MONDAY)
            ?: now.toLocalDate().with(DayOfWeek.MONDAY)
        val weekEnd = weekStart.plusDays(6)
        val volume = generation.history.sessions
            .filter { !it.completedAt.toLocalDate().isBefore(weekStart) && !it.completedAt.toLocalDate().isAfter(weekEnd) }
            .flatMap { it.exercises }
            .groupBy { performance -> generation.exercises.firstOrNull { it.id == performance.exerciseId }?.movementPattern }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
            .mapValues { (_, values) -> values.sumOf { performance -> performance.sets.count(::performed) } }
        return QuickWorkoutContext(generation, minutes, now, weekly, volume, weekStart, weekEnd)
    }

    suspend fun startSession(sessionId: String, readiness: Readiness, now: Long = System.currentTimeMillis()): String = db.withTransaction {
        dao.inProgressWorkout(sessionId)?.id ?: run {
            val session = dao.plannedSession(sessionId) ?: error("Sessão não encontrada")
            check(session.session.status == SessionStatus.PLANNED.name) { "Somente sessões planejadas podem ser iniciadas" }
            adaptForReadiness(session, readiness)
            val adapted = dao.plannedSession(sessionId) ?: error("Sessão não encontrada após adaptação")
            val id = UUID.randomUUID().toString()
            dao.startWorkout(WorkoutSessionEntity(id, sessionId, now, null, 0, SessionStatus.IN_PROGRESS.name, readiness.energy, readiness.sleep, readiness.soreness, readiness.motivation))
            adapted.exerciseRows.sortedBy { it.planned.priority }.forEachIndexed { index, row ->
                dao.addExerciseSession(ExerciseSessionEntity(workoutId = id, exerciseId = row.exercise.id, plannedSets = row.planned.sets, orderIndex = index))
            }
            dao.markInProgress(sessionId)
            id
        }
    }

    suspend fun saveSet(sessionId: String, input: SetInput, now: Long = System.currentTimeMillis()) = db.withTransaction {
        val session = dao.plannedSession(sessionId) ?: error("Sessão não encontrada")
        val workout = dao.inProgressWorkout(sessionId) ?: error("A sessão precisa estar em andamento")
        val ordered = session.exerciseRows.sortedBy { it.planned.priority }
        val order = ordered.indexOfFirst { it.exercise.id == input.exerciseId }
        require(order >= 0 && input.setIndex in 0 until ordered[order].planned.sets)
        val exerciseSession = dao.exerciseSession(workout.id, order) ?: error("Execução do exercício não encontrada")
        val row = ordered[order]
        val existing = dao.setLog(exerciseSession.id, input.setIndex)
        dao.addSetLog(SetLogEntity(existing?.id ?: 0, exerciseSession.id, input.setIndex, row.planned.targetMax,
            input.value.takeIf { row.exercise.prescriptionType == PrescriptionType.REPETITIONS.name },
            input.value.takeIf { row.exercise.prescriptionType == PrescriptionType.TIME.name }, input.rir,
            input.discomfort.name, input.techniqueGood, "COMPLETED", now))
        val completed = (exerciseSession.let { dao.exerciseSession(workout.id, order) }?.let { id ->
            (0 until row.planned.sets).all { dao.setLog(id.id, it)?.status == "COMPLETED" }
        } == true)
        dao.updateExerciseStatus(exerciseSession.id, if (completed) "COMPLETED" else "PARTIAL", false)
    }

    suspend fun skipExerciseRemainder(sessionId: String, exerciseId: String, now: Long = System.currentTimeMillis()) = db.withTransaction {
        val session = dao.plannedSession(sessionId) ?: error("Sessão não encontrada")
        val workout = dao.inProgressWorkout(sessionId) ?: error("A sessão precisa estar em andamento")
        val ordered = session.exerciseRows.sortedBy { it.planned.priority }
        val order = ordered.indexOfFirst { it.exercise.id == exerciseId }
        require(order >= 0)
        val row = ordered[order]
        val execution = dao.exerciseSession(workout.id, order) ?: error("Execução do exercício não encontrada")
        var performedCount = 0
        repeat(row.planned.sets) { setIndex ->
            val existing = dao.setLog(execution.id, setIndex)
            if (existing?.status == "COMPLETED") performedCount++
            else dao.addSetLog(SetLogEntity(existing?.id ?: 0, execution.id, setIndex, row.planned.targetMax, null, null, null, Discomfort.NONE.name, true, "SKIPPED", now))
        }
        dao.updateExerciseStatus(execution.id, if (performedCount == 0) "FULLY_SKIPPED" else "PARTIAL", performedCount == 0)
    }

    suspend fun completeSession(sessionId: String, now: Long = System.currentTimeMillis()) = db.withTransaction {
        val workout = dao.inProgressWorkout(sessionId) ?: error("A sessão precisa ser iniciada antes da conclusão")
        val minutes = ((now - workout.startedAt).coerceAtLeast(0) / 60_000L).toInt()
        dao.completeWorkout(workout.id, now, minutes)
        dao.completePlannedSession(sessionId)
    }

    private suspend fun adaptForReadiness(row: SessionWithExercises, readiness: Readiness) {
        if (readiness.score >= 2.5) return
        val session = toDomainSession(row)
        val adjusted = SessionTimeOptimizer().optimize(session, session.estimatedMinutes.coerceAtLeast(5), readiness)
        dao.deleteMutableExercises(session.id)
        dao.savePlannedExercises(adjusted.exercises.map {
            PlannedExerciseEntity(sessionId = session.id, exerciseId = it.exercise.id, sets = it.sets,
                targetMin = it.targetMin, targetMax = it.targetMax, restSeconds = it.restSeconds,
                priority = it.priority, rationale = "Readiness baixo: volume reduzido somente nesta sessão. ${it.rationale}")
        })
        dao.updatePlannedMinutes(session.id, adjusted.estimatedMinutes)
    }

    private fun toDomainSession(row: SessionWithExercises) = PlannedSession(row.session.id,
        LocalDate.ofEpochDay(row.session.dateEpochDay), row.session.title,
        row.exerciseRows.sortedBy { it.planned.priority }.map { item -> PlannedExercise(item.exercise.toDomain(), item.planned.sets,
            item.planned.targetMin, item.planned.targetMax, item.planned.restSeconds, item.planned.priority, item.planned.rationale) },
        row.session.estimatedMinutes, SessionStatus.valueOf(row.session.status))

    private fun performed(set: SetPerformance) = set.actualReps != null || set.actualSeconds != null

    private fun instant(value: Long) = LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault())
}

data class SetInput(val exerciseId: String, val setIndex: Int, val value: Int = 0, val rir: Int? = null, val discomfort: Discomfort = Discomfort.NONE, val techniqueGood: Boolean = true)

data class WorkoutPlayerState(val planned: SessionWithExercises, val workout: WorkoutWithExercises) {
    val orderedExercises get() = workout.exercises.sortedBy { it.exerciseSession.orderIndex }
    val nextExerciseIndex: Int get() = orderedExercises.indexOfFirst { exercise ->
        exercise.exerciseSession.completionStatus == "PENDING" ||
            (exercise.exerciseSession.completionStatus == "PARTIAL" && exercise.sets.map { it.setIndex }.toSet().size < exercise.exerciseSession.plannedSets)
    }.let { if (it < 0) orderedExercises.size else it }
    val nextSetIndex: Int get() {
        val exercise = orderedExercises.getOrNull(nextExerciseIndex) ?: return 0
        val completed = exercise.sets.filter { it.status == "COMPLETED" }.map { it.setIndex }.toSet()
        return (0 until exercise.exerciseSession.plannedSets).firstOrNull { index -> exercise.sets.none { it.setIndex == index && it.status == "SKIPPED" } && index !in completed } ?: exercise.exerciseSession.plannedSets
    }
}

fun UserProfileEntity.toDomain() = UserProfile(id, age, sex, heightCm, weightKg, Goal.valueOf(primaryGoal), secondaryGoals.split(',').filter(String::isNotBlank).map(Goal::valueOf).toSet(), ExperienceLevel.valueOf(experience), ExperienceLevel.valueOf(calisthenicsExperience), weeklyFrequency, Availability(availableDays.split(',').filter(String::isNotBlank).map { DayOfWeek.valueOf(it) }.toSet(), minutesPerSession), location, equipment.split(',').filter(String::isNotBlank).map(Equipment::valueOf).toSet(), preferences, limitations)
fun ExerciseEntity.toDomain() = Exercise(id, name, description, instructions, MovementPattern.valueOf(movementPattern), muscleGroups.split(',').filter(String::isNotBlank).toSet(), difficultyLevel, requiredEquipment.split(',').filter(String::isNotBlank).map(Equipment::valueOf).toSet(), minimumSuggestedLevel, progressionExerciseId, regressionExerciseId, PrescriptionType.valueOf(prescriptionType), if (minReps != null && maxReps != null) minReps..maxReps else null, durationSeconds, restSeconds, unilateral, tags.split(',').filter(String::isNotBlank).toSet(), techniqueCues.split('|'), commonMistakes.split('|'), mediaAsset, active)
