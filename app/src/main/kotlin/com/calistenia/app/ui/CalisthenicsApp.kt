package com.calistenia.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.calistenia.app.CalisthenicsApplication
import com.calistenia.app.data.SetInput
import com.calistenia.app.data.local.*
import com.calistenia.domain.model.*
import kotlinx.coroutines.delay

@Composable fun CalisthenicsApp() {
    val application = LocalContext.current.applicationContext as CalisthenicsApplication
    val vm: AppViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = AppViewModel(application.repository) as T
    })
    val state by vm.state.collectAsState()
    CalisthenicsTheme {
        if (state.loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else AppNavigation(state, vm)
    }
}

@Composable private fun AppNavigation(state: AppUiState, vm: AppViewModel) {
    val nav = rememberNavController()
    val initial = when (state.setup) { SetupState.SAFETY_PENDING, SetupState.PROFILE_PENDING -> "safety"; SetupState.ASSESSMENT_PENDING -> "assessment"; else -> "home" }
    NavHost(nav, initial) {
        composable("safety") { SafetyScreen { vm.acceptSafety { nav.navigate("onboarding") } } }
        composable("onboarding") { OnboardingScreen { age, height, weight, goal, days, minutes, equipment -> vm.finishOnboarding(age, height, weight, goal, days, minutes, equipment) { nav.navigate("assessment") } } }
        composable("assessment") { AssessmentScreen { levels -> vm.finishAssessment(levels) { nav.navigate("result") } } }
        composable("result") { SimpleScreen("Perfil funcional criado", "Seus níveis são independentes por padrão de movimento.", "Ver meu plano") { nav.navigate("home") { popUpTo("safety") { inclusive = true } } } }
        composable("home") { HomeScreen(state, { nav.navigate("weekly") }, { id, resume -> nav.navigate(if (resume) "workout/$id" else "readiness/session-$id") }, { nav.navigate("readiness/quick-$it") }, { nav.navigate("progress") }, { nav.navigate("library") }) }
        composable("weekly") { WeeklyPlanScreen(state.sessions) { id, resume -> nav.navigate(if (resume) "workout/$id" else "readiness/session-$id") } }
        composable("readiness/{target}", arguments = listOf(navArgument("target") { type = NavType.StringType })) { entry ->
            val target = entry.arguments?.getString("target") ?: return@composable
            ReadinessScreen { readiness ->
                if (target.startsWith("quick-")) vm.quick(target.removePrefix("quick-").toInt(), readiness) { nav.navigate("workout/$it") }
                else target.removePrefix("session-").let { id -> vm.start(id, readiness) { nav.navigate("workout/$id") } }
            }
        }
        composable("workout/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            LaunchedEffect(id) { vm.openPlayer(id) }
            state.player?.takeIf { it.planned.session.id == id }?.let { player ->
                WorkoutPlayerScreen(player, { vm.saveSet(id, it) }, { vm.skipExercise(id, it) }) {
                    vm.complete(id) { nav.navigate("summary") }
                }
            } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        composable("summary") { SimpleScreen("Treino concluído", "Séries realizadas foram salvas no aparelho e passam a compor seu histórico.", "Voltar ao início") { nav.navigate("home") { popUpTo("home") { inclusive = true } } } }
        composable("progress") { ProgressScreen(state.history) }
        composable("library") { ExerciseLibraryScreen(state.exercises) }
    }
}

@Composable private fun Page(title: String, content: @Composable ColumnScope.() -> Unit) = Scaffold { padding ->
    Column(Modifier.padding(padding).padding(horizontal = 20.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp), content = { Spacer(Modifier.height(12.dp)); Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); content() })
}

@Composable private fun SafetyScreen(next: () -> Unit) = Page("Antes de começar") {
    Text("Você sente dor no peito, desmaio/tontura importante, falta de ar incomum ou possui orientação profissional para não se exercitar?")
    Card { Text("O aplicativo não diagnostica condições. Diante desses sinais, interrompa e procure avaliação profissional. Nunca treine através da dor.", Modifier.padding(16.dp)) }
    Button(next, Modifier.fillMaxWidth().height(56.dp)) { Text("Não tenho esses sinais") }
    OutlinedButton({}, Modifier.fillMaxWidth()) { Text("Preciso de orientação antes") }
}

@Composable private fun OnboardingScreen(done: (Int, Int, Double, Goal, Int, Int, Set<Equipment>) -> Unit) = Page("Seu ponto de partida") {
    var age by remember { mutableFloatStateOf(30f) }; var height by remember { mutableFloatStateOf(175f) }; var weight by remember { mutableFloatStateOf(75f) }
    var days by remember { mutableFloatStateOf(3f) }; var minutes by remember { mutableFloatStateOf(20f) }; var goal by remember { mutableStateOf(Goal.STRENGTH) }; var bar by remember { mutableStateOf(false) }
    Text("Idade: ${age.toInt()}"); Slider(age, { age = it }, valueRange = 18f..80f)
    Text("Altura: ${height.toInt()} cm"); Slider(height, { height = it }, valueRange = 140f..210f)
    Text("Peso: ${weight.toInt()} kg"); Slider(weight, { weight = it }, valueRange = 40f..160f)
    Text("Objetivo principal"); Goal.entries.take(4).forEach { Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(goal == it, { goal = it }); Text(it.label()) } }
    Text("Dias por semana: ${days.toInt()}"); Slider(days, { days = it }, valueRange = 2f..5f, steps = 2)
    Text("Minutos por sessão: ${minutes.toInt()}"); Slider(minutes, { minutes = it }, valueRange = 10f..45f, steps = 6)
    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(bar, { bar = it }); Text("Tenho barra fixa") }
    Button({ done(age.toInt(), height.toInt(), weight.toDouble(), goal, days.toInt(), minutes.toInt(), if (bar) setOf(Equipment.PULL_UP_BAR) else emptySet()) }, Modifier.fillMaxWidth()) { Text("Continuar para avaliação") }
}

@Composable private fun AssessmentScreen(done: (Map<MovementPattern, Int>) -> Unit) = Page("Avaliação funcional") {
    Text("Informe sua capacidade atual. Faça apenas testes confortáveis; pare diante de dor, tontura ou falta de ar fora do esperado.")
    val values = remember { mutableStateMapOf<MovementPattern, Float>().apply { MovementPattern.entries.forEach { put(it, 1f) } } }
    MovementPattern.entries.filter { it != MovementPattern.CONDITIONING }.forEach { pattern ->
        Text("${pattern.label()}: nível estimado ${values[pattern]!!.toInt()}"); Slider(values[pattern]!!, { values[pattern] = it }, valueRange = 0f..6f, steps = 5)
    }
    Button({ done(MovementPattern.entries.associateWith { values[it]?.toInt() ?: 0 }) }, Modifier.fillMaxWidth()) { Text("Gerar programação") }
}

@Composable private fun ReadinessScreen(done: (Readiness) -> Unit) = Page("Como você está agora?") {
    Text("Prontidão é apenas uma heurística de treino, não uma avaliação médica.")
    var energy by remember { mutableIntStateOf(3) }; var sleep by remember { mutableIntStateOf(3) }
    var soreness by remember { mutableIntStateOf(3) }; var motivation by remember { mutableIntStateOf(3) }
    fun label(name: String, value: Int) = "$name: $value/5"
    Text(label("Energia", energy)); Slider(energy.toFloat(), { energy = it.toInt() }, valueRange = 1f..5f, steps = 3)
    Text(label("Sono percebido", sleep)); Slider(sleep.toFloat(), { sleep = it.toInt() }, valueRange = 1f..5f, steps = 3)
    Text(label("Dor muscular", soreness)); Slider(soreness.toFloat(), { soreness = it.toInt() }, valueRange = 1f..5f, steps = 3)
    Text(label("Motivação", motivation)); Slider(motivation.toFloat(), { motivation = it.toInt() }, valueRange = 1f..5f, steps = 3)
    Button({ done(Readiness(energy, sleep, soreness, motivation)) }, Modifier.fillMaxWidth()) { Text("Iniciar treino") }
}

@Composable private fun HomeScreen(state: AppUiState, weekly: () -> Unit, workout: (String, Boolean) -> Unit, quick: (Int) -> Unit, progress: () -> Unit, library: () -> Unit) = Page("Hoje") {
    val next = state.sessions.firstOrNull { it.session.status == "IN_PROGRESS" } ?: state.sessions.firstOrNull { it.session.status == "PLANNED" }
    val resuming = next?.session?.status == "IN_PROGRESS"
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(next?.session?.title ?: "Plano em dia", style = MaterialTheme.typography.titleLarge); Text(next?.let { "${it.exerciseRows.size} exercícios • ~${it.session.estimatedMinutes} min" } ?: "Conclua uma avaliação para criar o treino."); if (next != null) Button({ workout(next.session.id, resuming) }, Modifier.fillMaxWidth().height(56.dp)) { Text(if (resuming) "RETOMAR TREINO" else "TREINAR AGORA") } } }
    Text("Tenho alguns minutos agora", fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(10, 15, 20, 30).forEach { OutlinedButton({ quick(it) }, contentPadding = PaddingValues(10.dp)) { Text("$it min") } } }
    Button(weekly, Modifier.fillMaxWidth()) { Text("Programação semanal") }; OutlinedButton(progress, Modifier.fillMaxWidth()) { Text("Progresso e histórico") }; OutlinedButton(library, Modifier.fillMaxWidth()) { Text("Biblioteca de exercícios") }
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

@Composable private fun WeeklyPlanScreen(sessions: List<SessionWithExercises>, workout: (String, Boolean) -> Unit) = Page("Programação semanal") {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(sessions, key = { it.session.id }) { item -> Card(onClick = { if (item.session.status in setOf("PLANNED", "IN_PROGRESS")) workout(item.session.id, item.session.status == "IN_PROGRESS") }, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(item.session.title, fontWeight = FontWeight.Bold); Text("${item.exerciseRows.size} exercícios • ${item.session.estimatedMinutes} min • ${item.session.status}") } } } }
}

@Composable private fun WorkoutPlayerScreen(player: com.calistenia.app.data.WorkoutPlayerState, saveSet: (SetInput) -> Unit, skip: (String) -> Unit, finish: () -> Unit) = Page(player.planned.session.title) {
    val exerciseIndex = player.nextExerciseIndex; val setIndex = player.nextSetIndex
    var value by remember(exerciseIndex, setIndex) { mutableIntStateOf(8) }; var rir by remember(exerciseIndex, setIndex) { mutableIntStateOf(2) }; var techniqueGood by remember(exerciseIndex, setIndex) { mutableStateOf(true) }; var discomfort by remember(exerciseIndex, setIndex) { mutableStateOf(Discomfort.NONE) }; var discomfortMenu by remember { mutableStateOf(false) }; var rest by remember { mutableIntStateOf(0) }
    LaunchedEffect(rest) { if (rest > 0) { delay(1_000); rest-- } }
    val rows = player.planned.exerciseRows.sortedBy { it.planned.priority }
    val row = rows.getOrNull(exerciseIndex)
    if (row == null) { Button(finish, Modifier.fillMaxWidth().height(56.dp)) { Text("Finalizar treino") }; return@Page }
    LinearProgressIndicator({ exerciseIndex.toFloat() / rows.size }, Modifier.fillMaxWidth()); Text("Exercício ${exerciseIndex + 1} de ${rows.size}")
    val completedSets = player.orderedExercises.getOrNull(exerciseIndex)?.sets.orEmpty().count { it.status == "COMPLETED" }
    if (completedSets > 0) Text("$completedSets série(s) já salva(s) neste exercício")
    Text(row.exercise.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(row.exercise.instructions); Text("Série ${setIndex + 1} de ${row.planned.sets} • alvo ${row.planned.targetMin}–${row.planned.targetMax}")
    val timed = row.exercise.prescriptionType == PrescriptionType.TIME.name
    val maximum = if (timed) maxOf(180, row.planned.targetMax * 2) else maxOf(50, row.planned.targetMax * 2)
    if (setIndex == 0) value = value.coerceIn(1, maximum)
    Text(if (timed) "Segundos realizados: $value" else "Repetições realizadas: $value"); Slider(value.toFloat(), { value = it.toInt() }, valueRange = 1f..maximum.toFloat())
    Text("RIR: $rir — repetições que ainda conseguiria fazer"); Slider(rir.toFloat(), { rir = it.toInt() }, valueRange = 0f..5f, steps = 4)
    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(techniqueGood, { techniqueGood = it }); Text("Técnica adequada") }
    Box { OutlinedButton({ discomfortMenu = true }) { Text("Desconforto: ${discomfort.name.lowercase()}") }; DropdownMenu(discomfortMenu, { discomfortMenu = false }) { Discomfort.entries.forEach { item -> DropdownMenuItem({ Text(item.name.lowercase()) }, { discomfort = item; discomfortMenu = false }) } } }
    if (rest > 0) Text("Descanso: ${rest}s")
    Button({ saveSet(SetInput(row.exercise.id, setIndex, value, rir, discomfort, techniqueGood)); rest = row.planned.restSeconds }, enabled = rest == 0, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Concluir e salvar série") }
    TextButton({ skip(row.exercise.id); rest = 0 }) { Text("Pular restante do exercício") }; Text(row.planned.rationale, style = MaterialTheme.typography.bodySmall)
}

@Composable private fun ProgressScreen(history: List<WorkoutSessionEntity>) = Page("Seu progresso") { Text("${history.size} sessões concluídas", style = MaterialTheme.typography.headlineSmall); Text("${history.sumOf { it.durationMinutes }} minutos treinados"); history.take(10).forEach { Text("• ${it.durationMinutes} min — registro preservado") } }
@Composable private fun ExerciseLibraryScreen(exercises: List<ExerciseEntity>) = Page("Biblioteca") { LazyColumn { items(exercises, key = { it.id }) { Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Column(Modifier.padding(12.dp)) { Text(it.name, fontWeight = FontWeight.Bold); Text("${MovementPattern.valueOf(it.movementPattern).label()} • nível ${it.difficultyLevel}") } } } } }
@Composable private fun SimpleScreen(title: String, message: String, action: String, next: () -> Unit) = Page(title) { Text(message); Button(next, Modifier.fillMaxWidth()) { Text(action) } }

private fun Goal.label() = when (this) { Goal.STRENGTH -> "Ganhar força"; Goal.HYPERTROPHY -> "Hipertrofia"; Goal.CONDITIONING -> "Condicionamento"; Goal.FAT_LOSS_SUPPORT -> "Redução de gordura (apoio)"; Goal.CALISTHENICS_SKILLS -> "Dominar movimentos"; Goal.MOBILITY -> "Mobilidade"; Goal.GENERAL_HEALTH -> "Saúde geral" }
private fun MovementPattern.label() = when (this) { MovementPattern.PUSH -> "Empurrar"; MovementPattern.PULL -> "Puxar"; MovementPattern.LEGS -> "Pernas"; MovementPattern.CORE -> "Centro"; MovementPattern.MOBILITY -> "Mobilidade"; MovementPattern.CONDITIONING -> "Condicionamento" }
