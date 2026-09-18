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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import java.time.DayOfWeek

data class AppUiState(val loading: Boolean = true, val profile: UserProfileEntity? = null, val sessions: List<SessionWithExercises> = emptyList(), val history: List<WorkoutSessionEntity> = emptyList(), val exercises: List<ExerciseEntity> = emptyList(), val player: WorkoutPlayerState? = null, val setup: SetupState? = null, val error: String? = null, val busy: Boolean = false, val historyDetails: List<WorkoutWithExercises> = emptyList())
private data class UiMetadata(val setup: SetupState?, val error: String?, val player: WorkoutPlayerState?, val busy: Boolean)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppViewModel(private val repository: AppRepository) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)
    private val setup = MutableStateFlow<SetupState?>(null)
    private val playerSessionId = MutableStateFlow<String?>(null)
    private val busy = MutableStateFlow(false)
    private var actionJob: Job? = null
    private val player = playerSessionId.flatMapLatest { id -> id?.let(repository::player) ?: flowOf(null) }
    private val metadata = combine(setup, error, player, busy) { setupState, message, playerState, working -> UiMetadata(setupState, message, playerState, working) }
    private val history = combine(repository.history(), repository.historyDetails()) { summary, details -> summary to details }
    val state: StateFlow<AppUiState> = combine(repository.profile(), repository.planSessions(), history, repository.exercises(), metadata) { profile, sessions, history, exercises, meta ->
        AppUiState(meta.setup == null, profile, sessions, history.first, exercises, meta.player, meta.setup, meta.error, meta.busy, history.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init { retryInitialization() }
    fun retryInitialization() { launch {
        repository.seed()
        if (repository.setupState() == SetupState.PLAN_PENDING) repository.generationContext()?.let { repository.savePlan(TrainingPlanGenerator().generate(it)) }
        repository.rebalanceSchedule()
        refreshSetup()
    } }
    fun clearError() { error.value = null }

    fun acceptSafety(done: () -> Unit) = launch { repository.acceptSafety(); refreshSetup(); done() }

    fun finishOnboarding(age: Int, height: Int, weight: Double, goal: Goal, days: Set<DayOfWeek>, minutes: Int, equipment: Set<Equipment>, done: () -> Unit) = launch {
        require(age in 18..100 && height in 100..230 && weight.isFinite() && weight in 30.0..300.0) { "Confira idade, altura e peso." }
        require(days.size in 1..5 && minutes in 10..45) { "Escolha de 1 a 5 dias e de 10 a 45 minutos." }
        repository.saveProfile(UserProfileEntity(age = age, sex = null, heightCm = height, weightKg = weight, primaryGoal = goal.name, secondaryGoals = "", experience = ExperienceLevel.BEGINNER.name, calisthenicsExperience = ExperienceLevel.BEGINNER.name, weeklyFrequency = days.size, availableDays = days.sortedBy { it.value }.joinToString(",") { it.name }, minutesPerSession = minutes, location = "Casa", equipment = (equipment + Equipment.NONE).joinToString(",") { it.name }, preferences = "", limitations = "", onboardingComplete = true))
        refreshSetup()
        done()
    }

    fun finishAssessment(levels: Map<MovementPattern, Int>, done: () -> Unit) = launch {
        repository.saveAssessment(levels)
        repository.generationContext()?.let { repository.savePlan(TrainingPlanGenerator().generate(it)) }
        repository.rebalanceSchedule()
        refreshSetup()
        done()
    }

    fun regenerate(done: (() -> Unit)? = null) = launch {
        repository.generationContext()?.let { context ->
            repository.savePlan(TrainingPlanGenerator().generate(context))
            repository.rebalanceSchedule()
        }
        done?.invoke()
    }

    fun openPlayer(sessionId: String) { playerSessionId.value = sessionId }
    fun start(sessionId: String, readiness: Readiness, done: () -> Unit) = launch { repository.startSession(sessionId, readiness); playerSessionId.value = sessionId; done() }
    fun quick(minutes: Int, readiness: Readiness, done: (String) -> Unit) = launch {
        check(repository.inProgress().first() == null) { "Retome ou finalize o treino em andamento antes de iniciar outro." }
        val context = repository.quickWorkoutContext(minutes, readiness, java.time.LocalDateTime.now()) ?: error("Conclua seu cadastro antes de iniciar um treino.")
        val session = GenerateQuickWorkoutUseCase()(context)
        repository.savePlan(TrainingPlan("quick-plan-${session.id}", session.date, listOf(session), listOf("Sessão rápida contextual; plano semanal preservado.")))
        repository.startSession(session.id, readiness)
        playerSessionId.value = session.id
        done(session.id)
    }
    fun saveSet(sessionId: String, input: SetInput) = launch { repository.saveSet(sessionId, input) }
    fun skipExercise(sessionId: String, exerciseId: String) = launch { repository.skipExerciseRemainder(sessionId, exerciseId) }
    fun complete(sessionId: String, done: () -> Unit) = launch { repository.finishSession(sessionId); done(); playerSessionId.value = null }
    private suspend fun refreshSetup() { setup.value = repository.setupState() }
    private fun launch(block: suspend () -> Unit) {
        if (actionJob?.isActive == true) return
        busy.value = true
        error.value = null
        actionJob = viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error.value = failure.message ?: "Não foi possível concluir. Tente novamente." }
            finally { busy.value = false }
        }
    }
}
