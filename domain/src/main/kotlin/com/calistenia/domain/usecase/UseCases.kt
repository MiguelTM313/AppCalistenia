package com.calistenia.domain.usecase

import com.calistenia.domain.engine.*
import com.calistenia.domain.model.*
import java.time.LocalDate

class GenerateTrainingPlanUseCase(private val generator: TrainingPlanGenerator) { operator fun invoke(context: GenerationContext) = generator.generate(context) }
class CalculateProgressionUseCase(private val engine: ProgressionEngine) { operator fun invoke(exercise: Exercise, history: List<ExercisePerformance>) = engine.evaluate(exercise, history) }
class RebalanceScheduleUseCase(private val rebalancer: ScheduleRebalancer) { operator fun invoke(plan: TrainingPlan, today: LocalDate, completed: Set<String>) = rebalancer.rebalance(plan, today, completed) }

interface HealthDataGateway {
    suspend fun latestWeightKg(): Double? = null
    suspend fun recentSleepHours(): Double? = null
}
