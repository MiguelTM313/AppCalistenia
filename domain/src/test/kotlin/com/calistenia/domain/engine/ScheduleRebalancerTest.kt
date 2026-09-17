package com.calistenia.domain.engine

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ScheduleRebalancerTest {
    @Test fun `missed sessions are rebalanced without deleting past sessions`() {
        val original = TrainingPlanGenerator().generate(context(days = 4, minutes = 30))
        val today = LocalDate.of(2026, 9, 16)
        val completed = setOf(original.sessions.first().id)
        val result = ScheduleRebalancer().rebalance(original, today, completed)
        assertEquals(original.sessions.size, result.sessions.size)
        assertTrue(result.sessions.any { it.id == original.sessions.first().id })
        assertEquals(original.sessions.map { it.id }.toSet(), result.sessions.map { it.id }.toSet())
    }
}
