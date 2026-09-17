package com.calistenia.domain.engine

import com.calistenia.domain.model.*
import kotlin.math.ceil

class SessionTimeOptimizer {
    fun optimize(session: PlannedSession, budgetMinutes: Int, readiness: Readiness? = null): PlannedSession {
        require(budgetMinutes >= 5)
        val fixedSeconds = if (budgetMinutes <= 15) 120 else 300
        val transitionSeconds = 30
        val lowReadiness = readiness?.score?.let { it < 2.5 } == true
        val candidates = session.exercises.sortedBy { it.priority }.map { if (lowReadiness) it.copy(sets = minOf(2, it.sets)) else it }.toMutableList()
        fun duration(items: List<PlannedExercise>) = fixedSeconds + items.sumOf { exerciseSeconds(it) + transitionSeconds }
        while (candidates.size > 1 && duration(candidates) > budgetMinutes * 60) candidates.removeLast()
        while (duration(candidates) > budgetMinutes * 60 && candidates.any { it.sets > 1 }) {
            val i = candidates.indexOfLast { it.sets > 1 }
            candidates[i] = candidates[i].copy(sets = candidates[i].sets - 1)
        }
        return session.copy(exercises = candidates, estimatedMinutes = ceil(duration(candidates) / 60.0).toInt())
    }

    private fun exerciseSeconds(item: PlannedExercise): Int {
        val effort = when (item.exercise.prescriptionType) {
            PrescriptionType.REPETITIONS -> item.targetMax * 4
            PrescriptionType.TIME -> item.targetMax
            PrescriptionType.DISTANCE -> 45
        }
        return item.sets * effort + (item.sets - 1).coerceAtLeast(0) * item.restSeconds
    }
}
