package com.calistenia.domain.engine

import com.calistenia.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressionEngineTest {
    private val engine = ProgressionEngine(2)
    private val push = exercise("push", MovementPattern.PUSH, 3, next = "decline", previous = "knee")

    @Test fun `two successful exposures progress exercise`() {
        assertEquals(ProgressionAction.PROGRESS, engine.evaluate(push, listOf(performance("push", 12), performance("push", 12, daysAgo = 2))).action)
    }

    @Test fun `one exceptional session does not progress`() {
        assertEquals(ProgressionAction.MAINTAIN, engine.evaluate(push, listOf(performance("push", 12))).action)
    }

    @Test fun `repeated underperformance regresses`() {
        assertEquals(ProgressionAction.REGRESS, engine.evaluate(push, listOf(performance("push", 5), performance("push", 6, daysAgo = 2))).action)
    }

    @Test fun `sharp pain blocks progression`() {
        assertEquals(ProgressionAction.BLOCKED_FOR_SAFETY, engine.evaluate(push, listOf(performance("push", 12, discomfort = Discomfort.SHARP_PAIN), performance("push", 12))).action)
    }
}
