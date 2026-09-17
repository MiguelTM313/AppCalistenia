package com.calistenia.domain.engine

import com.calistenia.domain.model.*

class ProgressionEngine(private val successfulExposuresRequired: Int = 2) {
    fun evaluate(exercise: Exercise, recent: List<ExercisePerformance>): ProgressionDecision {
        val relevant = recent.filter { it.exerciseId == exercise.id }.sortedByDescending { it.completedAt }
            .take(successfulExposuresRequired)
        if (relevant.any { performance -> performance.sets.any { it.discomfort in unsafeSymptoms } }) {
            return ProgressionDecision(ProgressionAction.BLOCKED_FOR_SAFETY, null, "Sintoma de segurança registrado; não progredir e buscar orientação adequada.")
        }
        if (relevant.size < successfulExposuresRequired) return maintain("São necessárias $successfulExposuresRequired exposições consistentes.")
        val max = exercise.suggestedRepRange?.last ?: exercise.suggestedDurationSeconds ?: return maintain("Sem faixa configurada.")
        val min = exercise.suggestedRepRange?.first ?: exercise.suggestedDurationSeconds ?: max
        val successful = relevant.all { p -> p.sets.isNotEmpty() && p.sets.all { value(it) >= max && (it.rir ?: 2) >= 1 && it.techniqueGood } }
        if (successful && exercise.progressionExerciseId != null) return ProgressionDecision(ProgressionAction.PROGRESS, exercise.progressionExerciseId, "Topo da faixa atingido com técnica e margem em $successfulExposuresRequired exposições.")
        val insufficient = relevant.all { p -> p.sets.isEmpty() || p.sets.any { value(it) < min || !it.techniqueGood } }
        if (insufficient) return ProgressionDecision(if (exercise.regressionExerciseId != null) ProgressionAction.REGRESS else ProgressionAction.DELOAD, exercise.regressionExerciseId, "Desempenho repetidamente abaixo da faixa; reduzir dificuldade ou volume.")
        return maintain("Desempenho dentro da faixa; consolidar antes de progredir.")
    }

    private fun value(set: SetPerformance) = set.actualReps ?: set.actualSeconds ?: 0
    private fun maintain(reason: String) = ProgressionDecision(ProgressionAction.MAINTAIN, null, reason)
    private val unsafeSymptoms = setOf(Discomfort.SHARP_PAIN, Discomfort.DIZZINESS, Discomfort.UNEXPECTED_BREATHLESSNESS)
}
