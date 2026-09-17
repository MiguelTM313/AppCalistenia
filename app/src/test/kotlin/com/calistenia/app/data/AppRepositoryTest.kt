package com.calistenia.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.calistenia.app.data.local.*
import com.calistenia.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.*
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
        repository.completeSession("session", listOf(SetInput("push", 10, 2, Discomfort.MILD, false)), start + 12 * 60_000)

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
        repository.completeSession("session", listOf(SetInput("push", 9, 2)), start + 60_000)
        repository.savePlan(plan().copy(sessions = listOf(plan().sessions.single().copy(title = "substituído"))))

        assertEquals("COMPLETED", db.dao().plannedSession("session")!!.session.status)
        assertEquals(9, repository.workoutHistory().sessions.single().exercises.single().sets.single().actualReps)
    }

    private fun plan(): TrainingPlan {
        val exercise = exerciseEntity().toDomain()
        return TrainingPlan("plan", LocalDate.of(2026, 9, 14), listOf(PlannedSession("session", LocalDate.of(2026, 9, 17), "Treino", listOf(PlannedExercise(exercise, 1, 8, 12, 60, 0, "teste")), 15)), emptyList())
    }

    private fun exerciseEntity() = ExerciseEntity("push", "Flexão", "", "Controle", "PUSH", "peito", 3, "NONE", 3, null, null, "REPETITIONS", 8, 12, null, 60, false, "", "", "", null, true)
    private fun profileEntity() = UserProfileEntity(1, 30, null, 175, 75.0, "STRENGTH", "", "BEGINNER", "BEGINNER", 1, "THURSDAY", 20, "Casa", "NONE", "", "", true)
}
