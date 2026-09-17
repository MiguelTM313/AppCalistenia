package com.calistenia.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calistenia.app.data.*
import com.calistenia.app.data.local.*
import com.calistenia.domain.engine.TrainingPlanGenerator
import com.calistenia.domain.model.*
import com.calistenia.domain.usecase.GenerateQuickWorkoutUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class AppUiState(val loading: Boolean = true, val profile: UserProfileEntity? = null, val sessions: List<SessionWithExercises> = emptyList(), val history: List<WorkoutSessionEntity> = emptyList(), val exercises: List<ExerciseEntity> = emptyList(), val setup: SetupState? = null, val error: String? = null)

class AppViewModel(private val repository: AppRepository) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)
    private val setup = MutableStateFlow<SetupState?>(null)
    private val metadata = combine(setup, error) { setupState, message -> setupState to message }
    val state: StateFlow<AppUiState> = combine(repository.profile(), repository.planSessions(), repository.history(), repository.exercises(), metadata) { profile, sessions, history, exercises, meta ->
        AppUiState(meta.first == null, profile, sessions, history, exercises, meta.first, meta.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init { viewModelScope.launch {
        repository.seed()
        if (repository.setupState() == SetupState.PLAN_PENDING) repository.generationContext()?.let { repository.savePlan(TrainingPlanGenerator().generate(it)) }
        repository.rebalanceSchedule()
        refreshSetup()
    } }

    fun acceptSafety(done: () -> Unit) = launch { repository.acceptSafety(); refreshSetup(); done() }

    fun finishOnboarding(age: Int, height: Int, weight: Double, goal: Goal, days: Int, minutes: Int, equipment: Set<Equipment>, done: () -> Unit) = launch {
        repository.saveProfile(UserProfileEntity(age = age, sex = null, heightCm = height, weightKg = weight, primaryGoal = goal.name, secondaryGoals = "", experience = ExperienceLevel.BEGINNER.name, calisthenicsExperience = ExperienceLevel.BEGINNER.name, weeklyFrequency = days, availableDays = DayOfWeek.entries.take(days).joinToString(",") { it.name }, minutesPerSession = minutes, location = "Casa", equipment = (equipment + Equipment.NONE).joinToString(",") { it.name }, preferences = "", limitations = "", onboardingComplete = true))
        refreshSetup()
        done()
    }

    fun finishAssessment(levels: Map<MovementPattern, Int>, done: () -> Unit) = launch {
        repository.saveAssessment(levels)
        repository.generationContext()?.let { repository.savePlan(TrainingPlanGenerator().generate(it)) }
        refreshSetup()
        done()
    }

    fun regenerate(done: (() -> Unit)? = null) = launch {
        repository.generationContext()?.let { context ->
            repository.savePlan(TrainingPlanGenerator().generate(context))
        }
        done?.invoke()
    }

    fun start(sessionId: String, readiness: Readiness, done: () -> Unit) = launch { repository.startSession(sessionId, readiness); done() }
    fun quick(minutes: Int, readiness: Readiness, done: (String) -> Unit) = launch {
        val context = repository.generationContext(readiness = readiness) ?: return@launch
        val session = GenerateQuickWorkoutUseCase()(QuickWorkoutContext(context, minutes, java.time.LocalDateTime.now(), emptyList()))
        repository.savePlan(TrainingPlan("quick-plan-${session.id}", session.date, listOf(session), listOf("Sessão rápida contextual; plano semanal preservado.")))
        repository.startSession(session.id, readiness)
        done(session.id)
    }
    fun complete(sessionId: String, inputs: List<SetInput>, done: () -> Unit) = launch { repository.completeSession(sessionId, inputs); done() }
    private suspend fun refreshSetup() { setup.value = repository.setupState() }
    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { runCatching { block() }.onFailure { error.value = it.message } }
}
