package com.calistenia.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.Density
import com.calistenia.app.data.ExerciseSeed
import com.calistenia.app.data.SetInput
import com.calistenia.app.data.WorkoutPlayerState
import com.calistenia.app.data.local.*
import com.calistenia.domain.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h740dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PersonalWorkoutUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun `profile validates input restores draft and completes all three steps on small screen`() {
        var selectedDays: Set<DayOfWeek>? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent { CalisthenicsTheme { OnboardingScreen { _, _, _, _, days, _, _ -> selectedDays = days } } }
        compose.onNodeWithText("Continuar").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Idade (anos)").performScrollTo().performTextInput("28")
        compose.onNodeWithText("Altura (cm)").performScrollTo().performTextInput("171")
        compose.onNodeWithText("Peso (kg)").performScrollTo().performTextInput("98,5")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("98,5").assertExists()
        compose.onNodeWithText("Continuar").performScrollTo().performClick()
        compose.onNodeWithText("Treino que cabe na rotina").assertExists()
        compose.onNodeWithText("Continuar").performScrollTo().performClick()
        compose.onNodeWithText("Salvar e avaliar meu nível").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), selectedDays)
    }

    @Test fun `assessment final action stays reachable with large text`() {
        var submitted = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                CalisthenicsTheme { AssessmentScreen(ExerciseSeed.exercises) { submitted = true } }
            }
        }
        compose.onNodeWithText("Criar meu plano").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(submitted)
    }

    @Test fun `safety guidance responds without accepting screening`() {
        var accepted = false
        compose.setContent { CalisthenicsTheme { SafetyScreen { accepted = true } } }
        screenshot("01-welcome")
        compose.onNodeWithText("Preciso de orientação antes").performScrollTo().performClick()
        compose.onNodeWithText("Cuide de você primeiro").assertIsDisplayed()
        assertFalse(accepted)
        compose.onNodeWithText("Entendi").performClick()
        compose.onNodeWithText("Não tenho esses sinais").performScrollTo().performClick()
        assertTrue(accepted)
    }

    @Test fun `player defaults to prescribed seconds and submits actual input`() {
        var saved: SetInput? = null
        val player = fixture(timed = true)
        compose.setContent { CalisthenicsTheme { WorkoutPlayerScreen(player, { saved = it }, {}, {}, {}) } }
        screenshot("03-workout")
        compose.onNodeWithContentDescription("Aumentar Segundos realizados").performScrollTo().performClick()
        compose.onNodeWithText("Salvar série").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(35, saved!!.value)
        assertEquals(0, saved!!.setIndex)
    }

    @Test fun `library filters and opens details without nested scrolling crash`() {
        compose.setContent { CalisthenicsTheme { ExerciseLibraryScreen(ExerciseSeed.exercises) } }
        compose.onNodeWithText("Buscar exercício").performTextInput("Prancha lateral")
        compose.onNode(hasText("Prancha lateral") and !hasSetTextAction()).assertExists()
        compose.onNodeWithText("Como fazer +").performClick()
        compose.onNodeWithText("Fechar detalhes −").assertExists()
        screenshot("04-library")
    }

    @Test fun `home highlights saved workout and prevents competing quick workout`() {
        var resumed = false
        val player = fixture()
        compose.setContent { CalisthenicsTheme { HomeScreen(AppUiState(loading = false, sessions = listOf(player.planned)), { _, resume -> resumed = resume }, {}, {}) } }
        screenshot("02-home")
        compose.onNodeWithText("Retomar treino").performScrollTo().performClick()
        assertTrue(resumed)
        compose.onNodeWithText("Treino rápido de 15 min").performScrollTo().assertIsNotEnabled()
    }

    @Test fun `rest is derived from persisted time and does not restart on resume`() {
        val player = fixture().let { it.copy(workout = it.workout.copy(exercises = listOf(it.workout.exercises.single().copy(
            sets = listOf(SetLogEntity(1, 1, 0, 12, 8, null, 2, "NONE", true, "COMPLETED", 10_000L))
        )))) }
        assertEquals(45L, remainingRestSeconds(player, 25_000L))
        assertEquals(0L, remainingRestSeconds(player, 90_000L))
        assertEquals(60L, remainingRestSeconds(player, 5_000L))
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val folder = File("build/ui-screenshots").apply { mkdirs() }
        compose.runOnIdle {
            // PixelCopy does not complete under Robolectric. Draw the real window with its native renderer.
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    private fun fixture(timed: Boolean = false): WorkoutPlayerState {
        val exercise = ExerciseSeed.exercises.first { it.id == if (timed) "forearm_plank" else "wall_push_up" }
        val planned = PlannedExerciseEntity(1, "session", exercise.id, 3, if (timed) 30 else 6, if (timed) 30 else 12, 60, 0, "Compatível com seu nível")
        val session = SessionWithExercises(PlannedSessionEntity("session", "plan-week", LocalDate.now().toEpochDay(), "Corpo inteiro A", 20, "IN_PROGRESS"), listOf(PlannedExerciseRow(planned, exercise)))
        val workout = WorkoutWithExercises(WorkoutSessionEntity("workout", "session", System.currentTimeMillis(), null, 0, "IN_PROGRESS"), listOf(ExerciseSessionWithSets(ExerciseSessionEntity(1, "workout", exercise.id, 3, 0), emptyList())))
        return WorkoutPlayerState(session, workout)
    }
}
