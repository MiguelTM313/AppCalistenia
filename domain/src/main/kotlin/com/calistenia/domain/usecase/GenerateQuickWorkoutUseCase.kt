package com.calistenia.domain.usecase

import com.calistenia.domain.engine.SessionTimeOptimizer
import com.calistenia.domain.engine.TrainingPlanGenerator
import com.calistenia.domain.model.*

class GenerateQuickWorkoutUseCase(
    private val generator: TrainingPlanGenerator = TrainingPlanGenerator(),
    private val optimizer: SessionTimeOptimizer = SessionTimeOptimizer()
) {
    operator fun invoke(input: QuickWorkoutContext): PlannedSession {
        require(input.availableMinutes in 5..90)
        val covered = MovementPattern.entries.associateWith { pattern ->
            input.executedWeeklyVolume[pattern] ?: input.generation.history.sessions
                .filter { !it.completedAt.toLocalDate().isBefore(input.weekStart) && !it.completedAt.toLocalDate().isAfter(input.weekEnd) }
                .flatMap { it.exercises }.filter { performance ->
                    input.generation.exercises.firstOrNull { it.id == performance.exerciseId }?.movementPattern == pattern
                }.sumOf { it.sets.size }
        }
        val plannedPatterns = input.weeklySessions
            .filter { it.status != SessionStatus.SKIPPED }
            .flatMap { it.exercises }
            .map { it.exercise.movementPattern }
            .toSet()
        val uncovered = plannedPatterns.filterTo(mutableSetOf()) { (covered[it] ?: 0) == 0 }
        val base = generator.generate(input.generation.copy(
            user = input.generation.user.copy(availability = Availability(setOf(input.now.dayOfWeek), input.availableMinutes)),
            today = input.now.toLocalDate()
        )).sessions.single()
        val exercises = base.exercises.sortedWith(
            compareBy<PlannedExercise> { if (it.exercise.movementPattern in uncovered) 0 else 1 }
                .thenBy { covered[it.exercise.movementPattern] ?: 0 }
                .thenBy { it.priority }
        ).mapIndexed { index, exercise -> exercise.copy(priority = index,
            rationale = "Treino rápido usa a semana ${input.weekStart}–${input.weekEnd}: ${exercise.exercise.movementPattern.name.lowercase()} tem ${covered[exercise.exercise.movementPattern] ?: 0} séries realizadas${if (exercise.exercise.movementPattern in uncovered) " e estava planejado sem cobertura" else ""}. ${exercise.rationale}") }
        return optimizer.optimize(base.copy(id = "quick-${input.now}", title = "Treino rápido ${input.availableMinutes} min", exercises = exercises), input.availableMinutes, input.generation.readiness)
    }
}
