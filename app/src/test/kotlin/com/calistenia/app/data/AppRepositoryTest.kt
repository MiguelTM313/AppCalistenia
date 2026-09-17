package com.calistenia.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.calistenia.app.data.local.*
import com.calistenia.domain.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.*

@RunWith(RobolectricTestRunner::class)
class AppRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository

    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build()
        repository = AppRepository(db)
        db.dao().insertExercises(listOf(exerciseEntity()))
        repository.saveProfile(profileEntity())
        repository.saveAssessment(MovementPattern.entries.associateWith { 3 })
    }

    @After fun close() = db.close()

    @Test fun `persisted sets reconstruct complete domain history and generation context`() = runBlocking {
        val plan = plan()
        repository.savePlan(plan)
        val start = Instant.parse("2026-09-17T10:00:00Z").toEpochMilli()
        repository.startSession("session", Readiness(4, 3, 2, 5), start)
        repository.saveSet("session", SetInput("push", 0, 10, 2, Discomfort.MILD, false), start + 1_000)
        repository.completeSession("session", start + 12 * 60_000)

        val workout = repository.workoutHistory().sessions.single()
        assertEquals(12, workout.durationMinutes)
        assertEquals(10, workout.exercises.single().sets.single().actualReps)
        assertEquals(Discomfort.MILD, workout.exercises.single().sets.single().discomfort)
        assertFalse(workout.exercises.single().sets.single().techniqueGood)
        assertEquals(workout, repository.generationContext(LocalDate.of(2026, 9, 17))!!.recentSessions.single())
    }

    @Test fun `regeneration never reverts completed session or deletes logs`() = runBlocking {
        repository.savePlan(plan())
        val start = 1_700_000_000_000
        repository.startSession("session", Readiness(3, 3, 3, 3), start)
        repository.saveSet("session", SetInput("push", 0, 9, 2), start + 1_000)
        repository.completeSession("session", start + 60_000)
        repository.savePlan(plan().copy(sessions = listOf(plan().sessions.single().copy(title = "substituído"))))

        assertEquals("COMPLETED", db.dao().plannedSession("session")!!.session.status)
        assertEquals(9, repository.workoutHistory().sessions.single().exercises.single().sets.single().actualReps)
    }

    @Test fun `set is durable idempotent and resume keeps start and next set`() = runBlocking {
        repository.savePlan(plan(sets = 3))
        val start = 1_700_000_000_000
        repository.startSession("session", Readiness(3, 3, 3, 3), start)
        repository.saveSet("session", SetInput("push", 0, 10, 2, Discomfort.MILD, false), start + 1_000)
        val reopened = AppRepository(db)
        reopened.saveSet("session", SetInput("push", 0, 11, 1, Discomfort.SHARP_PAIN, true), start + 2_000)

        val player = reopened.player("session").first { it != null }!!
        assertEquals(start, player.workout.workout.startedAt)
        assertEquals(1, player.nextSetIndex)
        assertEquals(1, player.orderedExercises.single().sets.size)
        assertEquals(11, player.orderedExercises.single().sets.single().actualReps)
        assertEquals("SHARP_PAIN", player.orderedExercises.single().sets.single().discomfort)
        reopened.saveSet("session", SetInput("push", 1, 9, 2), start + 3_000)
        reopened.saveSet("session", SetInput("push", 2, 8, 2), start + 4_000)
        reopened.completeSession("session", start + 60_000)
        assertEquals(listOf(11, 9, 8), reopened.workoutHistory().sessions.single().exercises.single().sets.map { it.actualReps })
    }

    @Test fun `partial skip preserves completed sets symptom and performed volume`() = runBlocking {
        repository.savePlan(plan(sets = 3))
        val start = 1_700_000_000_000
        repository.startSession("session", Readiness(3, 3, 3, 3), start)
        repository.saveSet("session", SetInput("push", 0, 10, 2), start + 1_000)
        repository.saveSet("session", SetInput("push", 1, 8, 1, Discomfort.SHARP_PAIN), start + 2_000)
        repository.skipExerciseRemainder("session", "push", start + 3_000)
        repository.completeSession("session", start + 60_000)

        val history = repository.workoutHistory().sessions.single().exercises.single()
        assertEquals(2, history.sets.size)
        assertEquals(Discomfort.SHARP_PAIN, history.sets.last().discomfort)
        assertEquals(ProgressionAction.BLOCKED_FOR_SAFETY,
            com.calistenia.domain.engine.ProgressionEngine().evaluate(exerciseEntity().toDomain(), listOf(history)).action)
        val stored = db.dao().completedWorkouts().single().exercises.single()
        assertEquals("PARTIAL", stored.exerciseSession.completionStatus)
        assertEquals(1, stored.sets.count { it.status == "SKIPPED" })
    }

    @Test fun `skip before any set is fully skipped`() = runBlocking {
        repository.savePlan(plan(sets = 3))
        repository.startSession("session", Readiness(3, 3, 3, 3), 1_700_000_000_000)
        repository.skipExerciseRemainder("session", "push")
        repository.completeSession("session")
        val stored = db.dao().completedWorkouts().single().exercises.single()
        assertEquals("FULLY_SKIPPED", stored.exerciseSession.completionStatus)
        assertEquals(3, stored.sets.count { it.status == "SKIPPED" })
        assertTrue(repository.workoutHistory().sessions.single().exercises.isEmpty())
    }

    @Test fun `low readiness adapts once while normal and high keep prescription`() = runBlocking {
        repository.savePlan(plan(sets = 4))
        val start = 1_700_000_000_000
        repository.startSession("session", Readiness(1, 1, 5, 1), start)
        val adjusted = db.dao().plannedSession("session")!!.exerciseRows.single().planned
        assertEquals(2, adjusted.sets)
        assertTrue(adjusted.rationale.contains("Readiness baixo"))
        repository.startSession("session", Readiness(1, 1, 5, 1), start + 10_000)
        assertEquals(2, db.dao().plannedSession("session")!!.exerciseRows.single().planned.sets)
        assertEquals(start, db.dao().inProgressWorkout("session")!!.startedAt)
    }

    @Test fun `completed quick workout updates weekly volume without changing weekly plan`() = runBlocking {
        repository.savePlan(plan())
        val quick = plan().copy(id = "quick-plan-q", sessions = listOf(plan().sessions.single().copy(id = "quick-q", title = "Rápido")))
        repository.savePlan(quick)
        val start = Instant.parse("2026-09-17T10:00:00Z").toEpochMilli()
        repository.startSession("quick-q", Readiness(3, 3, 3, 3), start)
        repository.saveSet("quick-q", SetInput("push", 0, 10, 2), start + 1_000)
        repository.completeSession("quick-q", start + 60_000)

        val context = repository.quickWorkoutContext(15, Readiness(3, 3, 3, 3), LocalDateTime.of(2026, 9, 17, 12, 0))!!
        assertEquals(1, context.executedWeeklyVolume[MovementPattern.PUSH])
        assertEquals("Treino", db.dao().plannedSession("session")!!.session.title)
        assertEquals("PLANNED", db.dao().plannedSession("session")!!.session.status)
    }

    @Test fun `quick workout uses current calendar week when latest weekly plan is stale`() = runBlocking {
        val stale = plan().copy(
            id = "plan-stale",
            weekStart = LocalDate.of(2026, 9, 7),
            sessions = listOf(plan().sessions.single().copy(id = "stale-session", date = LocalDate.of(2026, 9, 10)))
        )
        repository.savePlan(stale)
        val quick = plan().copy(id = "quick-plan-current", sessions = listOf(plan().sessions.single().copy(id = "quick-current")))
        repository.savePlan(quick)
        val start = Instant.parse("2026-09-17T10:00:00Z").toEpochMilli()
        repository.startSession("quick-current", Readiness(3, 3, 3, 3), start)
        repository.saveSet("quick-current", SetInput("push", 0, 10, 2), start + 1_000)
        repository.completeSession("quick-current", start + 60_000)

        val context = repository.quickWorkoutContext(15, Readiness(3, 3, 3, 3), LocalDateTime.of(2026, 9, 17, 12, 0))!!
        assertEquals(LocalDate.of(2026, 9, 14), context.weekStart)
        assertEquals(LocalDate.of(2026, 9, 20), context.weekEnd)
        assertTrue(context.weeklySessions.isEmpty())
        assertEquals(1, context.executedWeeklyVolume[MovementPattern.PUSH])
    }

    private fun plan(sets: Int = 1): TrainingPlan {
        val exercise = exerciseEntity().toDomain()
        return TrainingPlan("plan-week", LocalDate.of(2026, 9, 14), listOf(PlannedSession("session", LocalDate.of(2026, 9, 17), "Treino", listOf(PlannedExercise(exercise, sets, 8, 12, 60, 0, "teste")), 15)), emptyList())
    }

    private fun exerciseEntity() = ExerciseEntity("push", "Flexão", "", "Controle", "PUSH", "peito", 3, "NONE", 3, null, null, "REPETITIONS", 8, 12, null, 60, false, "", "", "", null, true)
    private fun profileEntity() = UserProfileEntity(1, 30, null, 175, 75.0, "STRENGTH", "", "BEGINNER", "BEGINNER", 1, "THURSDAY", 20, "Casa", "NONE", "", "", true)
}
