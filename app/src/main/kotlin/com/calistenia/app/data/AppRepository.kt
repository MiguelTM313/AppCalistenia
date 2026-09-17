package com.calistenia.app.data

import androidx.room.withTransaction
import com.calistenia.app.data.local.*
import com.calistenia.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.*
import java.util.UUID

class AppRepository(private val db: AppDatabase) {
    private val dao = db.dao()
    fun profile(): Flow<UserProfileEntity?> = dao.observeProfile()
    fun planSessions(): Flow<List<SessionWithExercises>> = dao.observePlanSessions()
    fun exercises(): Flow<List<ExerciseEntity>> = dao.observeExercises()
    fun history(): Flow<List<WorkoutSessionEntity>> = dao.observeHistory()

    suspend fun seed() { if (dao.exerciseCount() == 0) dao.insertExercises(ExerciseSeed.exercises) }
    suspend fun saveProfile(profile: UserProfileEntity) = dao.saveProfile(profile)

    suspend fun saveAssessment(levels: Map<MovementPattern, Int>) {
        val now = System.currentTimeMillis()
        dao.saveLevels(levels.map { FunctionalLevelEntity(it.key.name, it.value, now) })
        dao.saveAssessments(levels.map { AssessmentEntity(assessedAt = now, exerciseId = "initial_${it.key.name.lowercase()}", pattern = it.key.name, repetitions = null, durationSeconds = null, estimatedLevel = it.value) })
    }

    suspend fun generationContext(today: LocalDate = LocalDate.now()): GenerationContext? {
        val profile = dao.profile() ?: return null
        val levels = dao.levels().associate { MovementPattern.valueOf(it.pattern) to it.level }
        return GenerationContext(profile.toDomain(), FunctionalProfile(levels), dao.exercises().map { it.toDomain() }, today = today)
    }

    suspend fun savePlan(plan: TrainingPlan) = db.withTransaction {
        dao.savePlan(TrainingPlanEntity(plan.id, plan.weekStart.toEpochDay(), System.currentTimeMillis()))
        dao.saveSessions(plan.sessions.map { PlannedSessionEntity(it.id, plan.id, it.date.toEpochDay(), it.title, it.estimatedMinutes, it.status.name) })
        plan.sessions.forEach { session -> dao.savePlannedExercises(session.exercises.map { PlannedExerciseEntity(sessionId = session.id, exerciseId = it.exercise.id, sets = it.sets, targetMin = it.targetMin, targetMax = it.targetMax, restSeconds = it.restSeconds, priority = it.priority, rationale = it.rationale) }) }
    }

    suspend fun completeSession(sessionId: String, values: List<SetInput>) = db.withTransaction {
        val session = dao.plannedSession(sessionId) ?: return@withTransaction
        val workoutId = UUID.randomUUID().toString()
        val started = System.currentTimeMillis() - session.session.estimatedMinutes * 60_000L
        dao.startWorkout(WorkoutSessionEntity(workoutId, sessionId, started, null, 0, "IN_PROGRESS"))
        session.exerciseRows.sortedBy { it.planned.priority }.forEachIndexed { index, row ->
            val exerciseSessionId = dao.addExerciseSession(ExerciseSessionEntity(workoutId = workoutId, exerciseId = row.exercise.id, plannedSets = row.planned.sets, orderIndex = index))
            values.filter { it.exerciseId == row.exercise.id }.forEachIndexed { setIndex, input ->
                dao.addSetLog(SetLogEntity(exerciseSessionId = exerciseSessionId, setIndex = setIndex, plannedValue = row.planned.targetMax, actualReps = if (row.exercise.prescriptionType == "REPETITIONS") input.value else null, actualSeconds = if (row.exercise.prescriptionType == "TIME") input.value else null, rir = input.rir, discomfort = input.discomfort.name, techniqueGood = input.techniqueGood))
            }
        }
        dao.completeWorkout(workoutId, System.currentTimeMillis(), session.session.estimatedMinutes)
        dao.completePlannedSession(sessionId)
    }
}

data class SetInput(val exerciseId: String, val value: Int, val rir: Int?, val discomfort: Discomfort = Discomfort.NONE, val techniqueGood: Boolean = true)

fun UserProfileEntity.toDomain() = UserProfile(id, age, sex, heightCm, weightKg, Goal.valueOf(primaryGoal), secondaryGoals.split(',').filter(String::isNotBlank).map(Goal::valueOf).toSet(), ExperienceLevel.valueOf(experience), ExperienceLevel.valueOf(calisthenicsExperience), weeklyFrequency, Availability(availableDays.split(',').filter(String::isNotBlank).map { DayOfWeek.valueOf(it) }.toSet(), minutesPerSession), location, equipment.split(',').filter(String::isNotBlank).map(Equipment::valueOf).toSet(), preferences, limitations)
fun ExerciseEntity.toDomain() = Exercise(id, name, description, instructions, MovementPattern.valueOf(movementPattern), muscleGroups.split(',').toSet(), difficultyLevel, requiredEquipment.split(',').map(Equipment::valueOf).toSet(), minimumSuggestedLevel, progressionExerciseId, regressionExerciseId, PrescriptionType.valueOf(prescriptionType), if (minReps != null && maxReps != null) minReps..maxReps else null, durationSeconds, restSeconds, unilateral, tags.split(',').toSet(), techniqueCues.split('|'), commonMistakes.split('|'), mediaAsset, active)
