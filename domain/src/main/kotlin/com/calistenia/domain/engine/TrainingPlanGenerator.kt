package com.calistenia.domain.engine

import com.calistenia.domain.model.*
import java.time.LocalDate

class TrainingPlanGenerator(private val timeOptimizer: SessionTimeOptimizer = SessionTimeOptimizer()) {
    fun generate(context: GenerationContext): TrainingPlan {
        val days = context.user.availability.days.sortedBy { it.value }
        require(days.isNotEmpty()) { "At least one training day is required" }
        val templates = templatesFor(days.size)
        val weekStart = context.today.minusDays((context.today.dayOfWeek.value - 1).toLong())
        val sessions = days.mapIndexed { index, day ->
            val patterns = templates[index % templates.size]
            val selected = patterns.mapNotNull { pattern -> selectExercise(context, pattern) }
                .distinctBy { it.id }
                .mapIndexed { priority, exercise -> prescription(context, exercise, priority) }
            val date = weekStart.plusDays((day.value - 1).toLong())
            val base = PlannedSession("${weekStart}-$index", date, title(days.size, index), selected, 0)
            timeOptimizer.optimize(base, context.user.availability.defaultMinutes, context.readiness)
        }
        return TrainingPlan(
            id = "plan-$weekStart",
            weekStart = weekStart,
            sessions = sessions,
            explanations = listOf("Plano baseado em níveis funcionais independentes, equipamentos e disponibilidade.")
        )
    }

    private fun selectExercise(context: GenerationContext, pattern: MovementPattern): Exercise? {
        val available = context.user.equipment + Equipment.NONE
        val level = context.functionalProfile.levelFor(pattern)
        val candidates = context.exercises.filter {
            it.active && it.movementPattern == pattern && it.minimumSuggestedLevel <= level + 1 &&
                it.requiredEquipment.all(available::contains)
        }
        val recentIds = context.recentSessions.flatMap { s -> s.exercises.map { it.exerciseId } }.toSet()
        return candidates.sortedWith(
            compareByDescending<Exercise> { it.difficultyLevel <= level }
                .thenBy { if (it.id in recentIds) 0 else 1 }
                .thenByDescending { it.difficultyLevel }
                .thenBy { it.id }
        ).firstOrNull()
    }

    private fun prescription(context: GenerationContext, exercise: Exercise, priority: Int): PlannedExercise {
        val lowReadiness = context.readiness?.score?.let { it < 2.5 } == true
        val range = exercise.suggestedRepRange ?: (exercise.suggestedDurationSeconds ?: 30).let { it..it }
        return PlannedExercise(
            exercise = exercise,
            sets = if (lowReadiness) 2 else 3,
            targetMin = range.first,
            targetMax = range.last,
            restSeconds = exercise.defaultRestSeconds,
            priority = priority,
            rationale = "${exercise.movementPattern.name}: compatível com seu nível ${context.functionalProfile.levelFor(exercise.movementPattern)} e equipamentos disponíveis."
        )
    }

    private fun templatesFor(count: Int): List<List<MovementPattern>> = when (count) {
        1 -> listOf(fullBody())
        2 -> listOf(fullBody(), listOf(MovementPattern.LEGS, MovementPattern.PULL, MovementPattern.PUSH, MovementPattern.CORE, MovementPattern.MOBILITY))
        3 -> List(3) { fullBody().drop(it % 2) + fullBody().take(it % 2) }
        4 -> listOf(
            listOf(MovementPattern.PUSH, MovementPattern.PULL, MovementPattern.CORE, MovementPattern.MOBILITY),
            listOf(MovementPattern.LEGS, MovementPattern.CORE, MovementPattern.MOBILITY),
            listOf(MovementPattern.PULL, MovementPattern.PUSH, MovementPattern.CORE, MovementPattern.MOBILITY),
            listOf(MovementPattern.LEGS, MovementPattern.CONDITIONING, MovementPattern.MOBILITY)
        )
        else -> listOf(
            listOf(MovementPattern.PUSH, MovementPattern.CORE), listOf(MovementPattern.LEGS, MovementPattern.MOBILITY),
            listOf(MovementPattern.PULL, MovementPattern.CORE), fullBody(), listOf(MovementPattern.CONDITIONING, MovementPattern.MOBILITY)
        )
    }

    private fun fullBody() = listOf(MovementPattern.PUSH, MovementPattern.PULL, MovementPattern.LEGS, MovementPattern.CORE, MovementPattern.MOBILITY)
    private fun title(count: Int, index: Int) = when (count) {
        2, 3 -> "Corpo inteiro ${'A' + index}"
        4 -> if (index % 2 == 0) "Parte superior ${index / 2 + 1}" else "Parte inferior ${index / 2 + 1}"
        else -> "Sessão ${index + 1}"
    }
}
