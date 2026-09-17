package com.calistenia.domain.engine

import com.calistenia.domain.model.*
import com.calistenia.domain.usecase.GenerateQuickWorkoutUseCase
import com.calistenia.domain.usecase.ResolveSetupStateUseCase
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class AdaptiveCycleTest {
    private val push = exercise("push", MovementPattern.PUSH, 3, next = "decline", previous = "knee")
    private val decline = exercise("decline", MovementPattern.PUSH, 4, equipment = setOf(Equipment.NONE), previous = "push")

    private fun withHistory(vararg performances: ExercisePerformance, equipment: Set<Equipment> = setOf(Equipment.NONE)): GenerationContext {
        val base = context(days = 1, equipment = equipment, level = 4)
        val workout = CompletedWorkout("w", "s", LocalDateTime.now(), 20, performances.toList())
        return base.copy(exercises = listOf(push, decline) + base.exercises.filter { it.movementPattern != MovementPattern.PUSH }, history = WorkoutHistory(listOf(workout)), recentSessions = listOf(workout))
    }

    @Test fun `generator progresses after two successful exposures`() {
        val plan = TrainingPlanGenerator().generate(withHistory(performance("push", 12), performance("push", 12, daysAgo = 2)))
        val prescribed = plan.sessions.single().exercises.first { it.exercise.movementPattern == MovementPattern.PUSH }
        assertEquals("decline", prescribed.exercise.id)
        assertTrue(prescribed.rationale.contains("Topo da faixa"))
    }

    @Test fun `one good and one bad exposure maintains`() {
        val decision = ProgressionEngine().evaluate(push, listOf(performance("push", 12), performance("push", 8)))
        assertEquals(ProgressionAction.MAINTAIN, decision.action)
    }

    @Test fun `repeated poor technique never progresses`() {
        val decision = ProgressionEngine().evaluate(push, listOf(performance("push", 12, good = false), performance("push", 12, good = false)))
        assertEquals(ProgressionAction.REGRESS, decision.action)
    }

    @Test fun `unsafe symptom blocks otherwise successful progression`() {
        val decision = ProgressionEngine().evaluate(push, listOf(performance("push", 12, discomfort = Discomfort.DIZZINESS), performance("push", 12)))
        assertEquals(ProgressionAction.BLOCKED_FOR_SAFETY, decision.action)
    }

    @Test fun `unavailable progression equipment preserves current exercise`() {
        val equippedNext = decline.copy(requiredEquipment = setOf(Equipment.BENCH))
        val ctx = withHistory(performance("push", 12), performance("push", 12, daysAgo = 2)).copy(exercises = listOf(push, equippedNext))
        assertEquals("push", TrainingPlanGenerator().generate(ctx).sessions.single().exercises.single().exercise.id)
    }

    @Test fun `progression above functional level is rejected with explanation`() {
        val unsafeJump = decline.copy(difficultyLevel = 9, minimumSuggestedLevel = 9)
        val ctx = withHistory(performance("push", 12), performance("push", 12, daysAgo = 2))
            .copy(exercises = listOf(push, unsafeJump), functionalProfile = FunctionalProfile(MovementPattern.entries.associateWith { 3 }))
        val prescribed = TrainingPlanGenerator().generate(ctx).sessions.single().exercises.single()
        assertEquals("push", prescribed.exercise.id)
        assertTrue(prescribed.rationale.contains("rejeitada"))
        assertTrue(prescribed.rationale.contains("nível"))
    }

    @Test fun `quick workout stays in budget and prioritizes uncovered pattern`() {
        val ctx = context(days = 1, minutes = 30)
        val quick = GenerateQuickWorkoutUseCase()(QuickWorkoutContext(ctx, 15, LocalDateTime.of(2026, 9, 17, 12, 0), emptyList(), mapOf(MovementPattern.PUSH to 20)))
        assertTrue(quick.estimatedMinutes <= 16)
        assertNotEquals(MovementPattern.PUSH, quick.exercises.first().exercise.movementPattern)
    }

    @Test fun `weekly planned but uncovered pull outranks covered push`() {
        val ctx = context(days = 1, minutes = 30, equipment = setOf(Equipment.NONE), level = 2)
        val planned = TrainingPlanGenerator().generate(ctx).sessions.single()
        val quick = GenerateQuickWorkoutUseCase()(QuickWorkoutContext(ctx, 15,
            LocalDateTime.of(2026, 9, 17, 12, 0), listOf(planned), mapOf(MovementPattern.PUSH to 6)))
        assertEquals(MovementPattern.PULL, quick.exercises.first().exercise.movementPattern)
    }

    @Test fun `quick workout budgets remain reasonable`() {
        val ctx = context(days = 1, minutes = 30)
        listOf(10, 15, 20, 30).forEach { minutes ->
            val quick = GenerateQuickWorkoutUseCase()(QuickWorkoutContext(ctx, minutes, LocalDateTime.of(2026, 9, 17, 12, 0), emptyList()))
            assertTrue("budget $minutes", quick.estimatedMinutes <= minutes + 1)
            assertTrue(quick.exercises.isNotEmpty())
        }
    }

    @Test fun `setup state resumes interrupted assessment and missing plan`() {
        val resolver = ResolveSetupStateUseCase()
        assertEquals(SetupState.ASSESSMENT_PENDING, resolver(true, true, false, false))
        assertEquals(SetupState.PLAN_PENDING, resolver(true, true, true, false))
        assertEquals(SetupState.READY, resolver(true, true, true, true))
    }
}
