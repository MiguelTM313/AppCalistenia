package com.calistenia.domain.engine

import com.calistenia.domain.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

fun exercise(id: String, pattern: MovementPattern, level: Int, equipment: Set<Equipment> = setOf(Equipment.NONE), next: String? = null, previous: String? = null) = Exercise(
    id, id, "description", "instructions", pattern, setOf("test"), level, equipment, level, next, previous,
    PrescriptionType.REPETITIONS, 8..12, null, 60
)

fun context(days: Int = 3, minutes: Int = 20, equipment: Set<Equipment> = setOf(Equipment.NONE), level: Int = 2): GenerationContext {
    val availableDays = DayOfWeek.entries.take(days).toSet()
    return GenerationContext(
        user = UserProfile(1, 30, null, 175, 75.0, Goal.STRENGTH, emptySet(), ExperienceLevel.BEGINNER, ExperienceLevel.BEGINNER, 2, Availability(availableDays, minutes), "casa", equipment),
        functionalProfile = FunctionalProfile(MovementPattern.entries.associateWith { level }),
        exercises = MovementPattern.entries.flatMap { pattern -> (1..5).map { exercise("${pattern.name}-$it", pattern, it, if (pattern == MovementPattern.PULL && it >= 3) setOf(Equipment.PULL_UP_BAR) else setOf(Equipment.NONE)) } },
        today = LocalDate.of(2026, 9, 14)
    )
}

fun performance(id: String, reps: Int, rir: Int = 2, discomfort: Discomfort = Discomfort.NONE, good: Boolean = true, daysAgo: Long = 0) = ExercisePerformance(
    id, List(3) { SetPerformance(12, reps, null, null, rir, discomfort, good) }, LocalDateTime.now().minusDays(daysAgo)
)
