package com.calistenia.domain.engine

import com.calistenia.domain.model.*
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SessionTimeOptimizerTest {
    @Test fun `ten minute session is rebuilt around priorities`() {
        val exercises = listOf(
            PlannedExercise(exercise("push", MovementPattern.PUSH, 1), 3, 8, 12, 60, 0, "priority"),
            PlannedExercise(exercise("legs", MovementPattern.LEGS, 1), 3, 8, 12, 60, 1, "priority"),
            PlannedExercise(exercise("core", MovementPattern.CORE, 1), 3, 8, 12, 60, 2, "priority")
        )
        val result = SessionTimeOptimizer().optimize(PlannedSession("id", LocalDate.now(), "test", exercises, 0), 10)
        assertTrue(result.estimatedMinutes <= 11)
        assertTrue(result.exercises.first().exercise.id == "push")
        assertTrue(result.exercises.isNotEmpty())
    }
}
