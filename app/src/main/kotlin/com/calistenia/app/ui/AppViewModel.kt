package com.calistenia.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calistenia.app.data.*
import com.calistenia.app.data.local.*
import com.calistenia.domain.engine.TrainingPlanGenerator
import com.calistenia.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class AppUiState(val loading: Boolean = true, val profile: UserProfileEntity? = null, val sessions: List<SessionWithExercises> = emptyList(), val history: List<WorkoutSessionEntity> = emptyList(), val exercises: List<ExerciseEntity> = emptyList(), val error: String? = null)

class AppViewModel(private val repository: AppRepository) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)
    val state: StateFlow<AppUiState> = combine(repository.profile(), repository.planSessions(), repository.history(), repository.exercises(), error) { profile, sessions, history, exercises, message ->
        AppUiState(false, profile, sessions, history, exercises, message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init { viewModelScope.launch { repository.seed() } }

    fun finishOnboarding(age: Int, height: Int, weight: Double, goal: Goal, days: Int, minutes: Int, equipment: Set<Equipment>, done: () -> Unit) = launch {
        repository.saveProfile(UserProfileEntity(age = age, sex = null, heightCm = height, weightKg = weight, primaryGoal = goal.name, secondaryGoals = "", experience = ExperienceLevel.BEGINNER.name, calisthenicsExperience = ExperienceLevel.BEGINNER.name, weeklyFrequency = days, availableDays = DayOfWeek.entries.take(days).joinToString(",") { it.name }, minutesPerSession = minutes, location = "Casa", equipment = (equipment + Equipment.NONE).joinToString(",") { it.name }, preferences = "", limitations = "", onboardingComplete = true))
        done()
    }

    fun finishAssessment(levels: Map<MovementPattern, Int>, done: () -> Unit) = launch {
        repository.saveAssessment(levels)
        repository.generationContext()?.let { repository.savePlan(TrainingPlanGenerator().generate(it)) }
        done()
    }

    fun regenerate(minutes: Int? = null, done: (() -> Unit)? = null) = launch {
        repository.generationContext()?.let { context ->
            val adjusted = if (minutes != null) context.copy(user = context.user.copy(availability = context.user.availability.copy(defaultMinutes = minutes))) else context
            repository.savePlan(TrainingPlanGenerator().generate(adjusted))
        }
        done?.invoke()
    }

    fun complete(sessionId: String, inputs: List<SetInput>, done: () -> Unit) = launch { repository.completeSession(sessionId, inputs); done() }
    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { runCatching { block() }.onFailure { error.value = it.message } }
}
