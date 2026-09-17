package com.calistenia.domain.engine

import com.calistenia.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class TrainingPlanGeneratorTest {
    private val generator = TrainingPlanGenerator()

    @Test fun `beginner without equipment receives three balanced sessions`() {
        val plan = generator.generate(context())
        assertEquals(3, plan.sessions.size)
        assertTrue(plan.sessions.all { it.exercises.any { e -> e.exercise.movementPattern == MovementPattern.PUSH } })
        assertTrue(plan.sessions.flattenExercises().all { it.exercise.requiredEquipment.all(setOf(Equipment.NONE)::contains) })
    }

    @Test fun `intermediate with bar receives pull exercises within level`() {
        val ctx = context(days = 4, minutes = 30, equipment = setOf(Equipment.PULL_UP_BAR), level = 5)
        val plan = generator.generate(ctx)
        assertEquals(4, plan.sessions.size)
        assertTrue(plan.sessions.flattenExercises().filter { it.exercise.movementPattern == MovementPattern.PULL }.any { Equipment.PULL_UP_BAR in it.exercise.requiredEquipment })
        assertTrue(plan.sessions.flattenExercises().all { it.exercise.minimumSuggestedLevel <= 6 })
    }

    @Test fun `missing equipment is never recommended`() {
        val plan = generator.generate(context(equipment = setOf(Equipment.NONE), level = 5))
        assertFalse(plan.sessions.flattenExercises().any { Equipment.PULL_UP_BAR in it.exercise.requiredEquipment })
    }

    @Test fun `unevaluated user only receives entry level exercises`() {
        val plan = generator.generate(context(level = 0))
        assertTrue(plan.sessions.flattenExercises().all { it.exercise.minimumSuggestedLevel <= 1 })
    }

    private fun List<PlannedSession>.flattenExercises() = flatMap { it.exercises }
}
