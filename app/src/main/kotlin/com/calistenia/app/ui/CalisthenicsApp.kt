package com.calistenia.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.calistenia.app.CalisthenicsApplication
import com.calistenia.domain.model.SetupState

@Composable fun CalisthenicsApp() {
    val application = LocalContext.current.applicationContext as CalisthenicsApplication
    val vm: AppViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = AppViewModel(application.repository) as T
    })
    val state by vm.state.collectAsState()
    CalisthenicsTheme {
        if (state.loading) {
            Page("Preparando seu espaço", "Tudo o que você precisa para treinar em casa.") {
                if (state.error == null) CircularProgressIndicator()
                else {
                    Notice(state.error!!, error = true)
                    PrimaryAction("Tentar novamente", onClick = vm::retryInitialization)
                }
            }
        } else AppNavigation(state, vm)
    }
}

@Composable private fun AppNavigation(state: AppUiState, vm: AppViewModel) {
    val nav = rememberNavController()
    // Setup writes must not replace the graph's start destination mid-navigation.
    val initial = remember { when (state.setup) {
        SetupState.SAFETY_PENDING -> "safety"
        SetupState.PROFILE_PENDING -> "onboarding"
        SetupState.ASSESSMENT_PENDING -> "assessment"
        else -> "home"
    } }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it, withDismissAction = true); vm.clearError() }
    }
    fun home() { nav.navigate("home") { popUpTo(nav.graph.id) { inclusive = false }; launchSingleTop = true } }
    fun openSession(id: String, resume: Boolean) { nav.navigate(if (resume) "workout/$id" else "readiness/session-$id") }
    CompositionLocalProvider(LocalBusy provides state.busy) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
            if (route in listOf("home", "weekly", "progress", "library")) NavigationBar {
                listOf("home" to "Hoje", "weekly" to "Plano", "progress" to "Histórico", "library" to "Exercícios").forEach { (target, label) ->
                    NavigationBarItem(selected = route == target, onClick = {
                        nav.navigate(target) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true }
                    }, icon = { NavGlyph(target) }, label = { Text(label) })
                }
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                NavHost(nav, initial) {
                    composable("safety") { SafetyScreen { vm.acceptSafety { nav.navigate("onboarding") { popUpTo("safety") { inclusive = true } } } } }
                    composable("onboarding") { OnboardingScreen { age, height, weight, goal, days, minutes, equipment ->
                        vm.finishOnboarding(age, height, weight, goal, days, minutes, equipment) {
                            nav.navigate("assessment") { popUpTo("onboarding") { inclusive = true } }
                        }
                    } }
                    composable("assessment") { AssessmentScreen(state.exercises) { levels -> vm.finishAssessment(levels) { home() } } }
                    composable("home") { HomeScreen(state, ::openSession, { nav.navigate("readiness/quick-$it") }, { vm.regenerate() }) }
                    composable("weekly") { WeeklyPlanScreen(state.sessions, ::openSession, { vm.regenerate() }) }
                    composable("readiness/{target}", arguments = listOf(navArgument("target") { type = NavType.StringType })) { backStack ->
                        val target = backStack.arguments?.getString("target") ?: return@composable
                        ReadinessScreen(onBack = { nav.popBackStack() }) { readiness ->
                            if (target.startsWith("quick-")) vm.quick(target.removePrefix("quick-").toInt(), readiness) {
                                nav.navigate("workout/$it") { popUpTo("readiness/{target}") { inclusive = true } }
                            } else {
                                val id = target.removePrefix("session-")
                                vm.start(id, readiness) { nav.navigate("workout/$id") { popUpTo("readiness/{target}") { inclusive = true } } }
                            }
                        }
                    }
                    composable("workout/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStack ->
                        val id = backStack.arguments?.getString("id") ?: return@composable
                        LaunchedEffect(id) { vm.openPlayer(id) }
                        val player = state.player?.takeIf { it.planned.session.id == id }
                        if (player != null) WorkoutPlayerScreen(player,
                            saveSet = { vm.saveSet(id, it) }, skip = { vm.skipExercise(id, it) }, onBack = ::home,
                            finish = { vm.complete(id) { nav.navigate("summary") { popUpTo("workout/{id}") { inclusive = true } } } })
                        else Page("Abrindo treino", onBack = ::home) {
                            Text("Seus registros salvos serão carregados aqui.")
                            if (state.sessions.any { it.session.id == id && it.session.status == "IN_PROGRESS" }) CircularProgressIndicator()
                            else PrimaryAction("Voltar ao início", onClick = ::home)
                        }
                    }
                    composable("summary") { SummaryScreen(state, ::home) }
                    composable("progress") { ProgressScreen(state.history, state.sessions, state.historyDetails, state.exercises) }
                    composable("library") { ExerciseLibraryScreen(state.exercises) }
                }
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}
