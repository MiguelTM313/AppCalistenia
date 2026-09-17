package com.calistenia.domain.engine

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import com.calistenia.domain.model.SessionStatus

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

    @Test fun `missed past sessions become skipped while completed remains completed`() {
        val original = TrainingPlanGenerator().generate(context(days = 4, minutes = 30))
        val completedId = original.sessions.first().id
        val plan = original.copy(sessions = original.sessions.map { if (it.id == completedId) it.copy(status = SessionStatus.COMPLETED) else it })
        val result = ScheduleRebalancer().rebalance(plan, LocalDate.of(2026, 9, 17), setOf(completedId))
        assertEquals(SessionStatus.COMPLETED, result.sessions.first { it.id == completedId }.status)
        assertTrue(result.sessions.filter { it.date < LocalDate.of(2026, 9, 17) && it.id != completedId }.all { it.status == SessionStatus.SKIPPED })
        assertEquals(plan.sessions.map { it.id }.toSet(), result.sessions.map { it.id }.toSet())
    }

    @Test fun `two missed sessions preserve every id and reorder only future sessions`() {
        val plan = TrainingPlanGenerator().generate(context(days = 5, minutes = 20))
        val today = LocalDate.of(2026, 9, 16)
        val result = ScheduleRebalancer().rebalance(plan, today, emptySet())
        assertEquals(plan.sessions.map { it.id }.toSet(), result.sessions.map { it.id }.toSet())
        assertTrue(result.sessions.filter { it.date < today }.all { it.status == SessionStatus.SKIPPED })
        assertTrue(result.sessions.filter { it.date >= today }.all { it.status == SessionStatus.PLANNED })
    }

    @Test fun `end of week does not create sessions outside original dates`() {
        val plan = TrainingPlanGenerator().generate(context(days = 3, minutes = 20))
        val originalDates = plan.sessions.map { it.date }.toSet()
        val result = ScheduleRebalancer().rebalance(plan, LocalDate.of(2026, 9, 20), emptySet())
        assertEquals(originalDates, result.sessions.map { it.date }.toSet())
        assertTrue(result.sessions.all { it.status == SessionStatus.SKIPPED })
    }
}
