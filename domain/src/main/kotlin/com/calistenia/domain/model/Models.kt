package com.calistenia.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

enum class MovementPattern { PUSH, PULL, LEGS, CORE, MOBILITY, CONDITIONING }
enum class Equipment { NONE, WALL, BENCH, PULL_UP_BAR, PARALLETTES, RINGS, RESISTANCE_BAND, SUSPENSION, ADDED_WEIGHT }
enum class Goal { STRENGTH, HYPERTROPHY, CONDITIONING, FAT_LOSS_SUPPORT, CALISTHENICS_SKILLS, MOBILITY, GENERAL_HEALTH }
enum class ExperienceLevel { NONE, BEGINNER, INTERMEDIATE, ADVANCED }
enum class PrescriptionType { REPETITIONS, TIME, DISTANCE }
enum class SessionStatus { PLANNED, IN_PROGRESS, COMPLETED, SKIPPED }
enum class Discomfort { NONE, MILD, PAIN, SHARP_PAIN, DIZZINESS, UNEXPECTED_BREATHLESSNESS, OTHER }
enum class ProgressionAction { PROGRESS, MAINTAIN, REGRESS, DELOAD, BLOCKED_FOR_SAFETY }

data class Availability(val days: Set<DayOfWeek>, val defaultMinutes: Int)

data class UserProfile(
    val id: Long = 1,
    val age: Int,
    val sex: String? = null,
    val heightCm: Int,
    val weightKg: Double,
    val primaryGoal: Goal,
    val secondaryGoals: Set<Goal> = emptySet(),
    val generalExperience: ExperienceLevel,
    val calisthenicsExperience: ExperienceLevel,
    val currentWeeklyFrequency: Int,
    val availability: Availability,
    val trainingLocation: String,
    val equipment: Set<Equipment>,
    val preferences: String = "",
    val limitations: String = ""
)

data class FunctionalProfile(
    val levels: Map<MovementPattern, Int>,
    val assessedAt: LocalDateTime = LocalDateTime.now()
) {
    fun levelFor(pattern: MovementPattern): Int = levels[pattern]?.coerceIn(0, 9) ?: 0
}

data class Exercise(
    val id: String,
    val name: String,
    val description: String,
    val instructions: String,
    val movementPattern: MovementPattern,
    val muscleGroups: Set<String>,
    val difficultyLevel: Int,
    val requiredEquipment: Set<Equipment> = setOf(Equipment.NONE),
    val minimumSuggestedLevel: Int,
    val progressionExerciseId: String? = null,
    val regressionExerciseId: String? = null,
    val prescriptionType: PrescriptionType,
    val suggestedRepRange: IntRange? = null,
    val suggestedDurationSeconds: Int? = null,
    val defaultRestSeconds: Int,
    val unilateral: Boolean = false,
    val tags: Set<String> = emptySet(),
    val techniqueCues: List<String> = emptyList(),
    val commonMistakes: List<String> = emptyList(),
    val mediaAsset: String? = null,
    val active: Boolean = true
)

data class Readiness(val energy: Int, val sleep: Int, val soreness: Int, val motivation: Int) {
    val score: Double get() = (energy + sleep + (6 - soreness) + motivation) / 4.0
}

data class PlannedExercise(
    val exercise: Exercise,
    val sets: Int,
    val targetMin: Int,
    val targetMax: Int,
    val restSeconds: Int,
    val priority: Int,
    val rationale: String
)

data class PlannedSession(
    val id: String,
    val date: LocalDate,
    val title: String,
    val exercises: List<PlannedExercise>,
    val estimatedMinutes: Int,
    val status: SessionStatus = SessionStatus.PLANNED
)

data class TrainingPlan(
    val id: String,
    val weekStart: LocalDate,
    val sessions: List<PlannedSession>,
    val explanations: List<String>
)

data class SetPerformance(
    val plannedReps: Int?,
    val actualReps: Int?,
    val plannedSeconds: Int?,
    val actualSeconds: Int?,
    val rir: Int?,
    val discomfort: Discomfort = Discomfort.NONE,
    val techniqueGood: Boolean = true
)

data class ExercisePerformance(val exerciseId: String, val sets: List<SetPerformance>, val completedAt: LocalDateTime)
data class WorkoutHistory(val sessions: List<CompletedWorkout> = emptyList())
data class CompletedWorkout(val id: String, val plannedSessionId: String?, val completedAt: LocalDateTime, val durationMinutes: Int, val exercises: List<ExercisePerformance>)

data class GenerationContext(
    val user: UserProfile,
    val functionalProfile: FunctionalProfile,
    val exercises: List<Exercise>,
    val history: WorkoutHistory = WorkoutHistory(),
    val recentSessions: List<CompletedWorkout> = emptyList(),
    val readiness: Readiness? = null,
    val today: LocalDate = LocalDate.now()
)

data class ProgressionDecision(val action: ProgressionAction, val suggestedExerciseId: String?, val reason: String)

data class AssessmentResult(
    val id: Long = 0,
    val date: LocalDateTime,
    val exerciseId: String,
    val pattern: MovementPattern,
    val repetitions: Int? = null,
    val durationSeconds: Int? = null,
    val estimatedLevel: Int
)
