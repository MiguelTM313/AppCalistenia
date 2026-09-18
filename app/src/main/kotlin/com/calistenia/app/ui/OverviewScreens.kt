package com.calistenia.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calistenia.app.data.local.*
import com.calistenia.domain.model.*
import java.time.*

@Composable internal fun HomeScreen(state: AppUiState, workout: (String, Boolean) -> Unit, quick: (Int) -> Unit, regenerate: () -> Unit) {
    val today = LocalDate.now()
    val active = state.sessions.firstOrNull { it.session.status == "IN_PROGRESS" }
    val next = active ?: state.sessions.filter { it.session.status == "PLANNED" && it.session.dateEpochDay >= today.toEpochDay() }.minByOrNull { it.session.dateEpochDay }
    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val thisWeek = state.history.filter {
        val date = Instant.ofEpochMilli(it.completedAt ?: it.startedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        date >= weekStart && date < weekStart.plusDays(7)
    }
    Page("Um treino de cada vez.", dateLabel(today.toEpochDay())) {
        Hero {
            Text(if (active != null) "CONTINUE DE ONDE PAROU" else "SEU PRÓXIMO TREINO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(next?.session?.title ?: "Seu ritmo, sua semana", style = MaterialTheme.typography.headlineMedium)
            if (next != null) {
                Text("${next.exerciseRows.size} exercícios • cerca de ${next.session.estimatedMinutes} min")
                Text(dateLabel(next.session.dateEpochDay), color = MaterialTheme.colorScheme.onSurfaceVariant)
                PrimaryAction(if (active != null) "Retomar treino" else "Preparar treino") { workout(next.session.id, active != null) }
            } else {
                Text("Não há sessões pendentes. Você pode atualizar o plano da semana ou escolher um treino rápido.")
                PrimaryAction("Atualizar plano da semana", onClick = regenerate)
            }
        }
        SectionCard("Sua semana até aqui") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Metric("${thisWeek.size}", "treinos concluídos", Modifier.weight(1f))
                Metric("${thisWeek.sumOf { it.durationMinutes }}", "minutos registrados", Modifier.weight(1f))
            }
        }
        SectionCard("Pouco tempo? Também conta.") {
            Text(if (active == null) "Escolha uma sessão rápida para o tempo que você tem." else "Retome ou finalize seu treino atual antes de começar outro.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            var selected by rememberSaveable { mutableIntStateOf(15) }
            Choices(listOf(10, 15, 20, 30), { it == selected }, { "$it min" }) { selected = it }
            PrimaryAction("Treino rápido de $selected min", active == null) { quick(selected) }
        }
        state.profile?.let { profile ->
            Text("${Goal.valueOf(profile.primaryGoal).label()} • ${profile.weeklyFrequency} dias por semana\nTudo salvo neste aparelho.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable internal fun WeeklyPlanScreen(sessions: List<SessionWithExercises>, workout: (String, Boolean) -> Unit, regenerate: () -> Unit) {
    val today = LocalDate.now()
    val start = today.minusDays((today.dayOfWeek.value - 1).toLong()).toEpochDay()
    val active = sessions.firstOrNull { it.session.status == "IN_PROGRESS" }
    val visible = sessions.filter { it.session.dateEpochDay in start until start + 7 || it.session.status == "IN_PROGRESS" }.sortedBy { it.session.dateEpochDay }
    Page("Seu plano", "Abra uma sessão para ver os exercícios.", scrollable = false) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (visible.isEmpty()) item {
                SectionCard {
                    Text("Ainda não há um plano para esta semana.")
                    PrimaryAction("Gerar semana atual", onClick = regenerate)
                }
            }
            items(visible, key = { it.session.id }) { item ->
                var expanded by rememberSaveable(item.session.id) { mutableStateOf(false) }
                Card(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(dateLabel(item.session.dateEpochDay), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        Text(item.session.title, style = MaterialTheme.typography.titleLarge)
                        Text("${statusLabel(item.session.status)} • ${item.session.estimatedMinutes} min", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (expanded) "Ocultar exercícios −" else "Ver ${item.exerciseRows.size} exercícios +", style = MaterialTheme.typography.labelMedium)
                        if (expanded) {
                            HorizontalDivider()
                            item.exerciseRows.sortedBy { it.planned.priority }.forEach { row ->
                                Text("${row.exercise.name}\n${row.planned.sets} × ${row.planned.targetMin}–${row.planned.targetMax} ${if (row.exercise.prescriptionType == "TIME") "s" else "reps"}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (item.session.status in setOf("PLANNED", "IN_PROGRESS")) {
                                val canStart = active == null || active.session.id == item.session.id
                                PrimaryAction(if (item.session.status == "IN_PROGRESS") "Retomar treino" else "Preparar treino", canStart) { workout(item.session.id, item.session.status == "IN_PROGRESS") }
                                if (!canStart) Text("Finalize o treino em andamento antes de iniciar outro.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable internal fun ProgressScreen(history: List<WorkoutSessionEntity>, sessions: List<SessionWithExercises>, details: List<WorkoutWithExercises> = emptyList(), exercises: List<ExerciseEntity> = emptyList()) {
    Page("Seu histórico", "Cada sessão registrada conta.", scrollable = false) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Hero {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Metric("${history.size}", "treinos", Modifier.weight(1f))
                    Metric("${history.sumOf { it.durationMinutes }}", "minutos", Modifier.weight(1f))
                }
            } }
            if (history.isEmpty()) item { Notice("Seu primeiro treino aparecerá aqui ao finalizar. As séries são salvas durante a sessão.") }
            items(history, key = { it.id }) { workout ->
                var expanded by rememberSaveable(workout.id) { mutableStateOf(false) }
                Card(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(sessions.firstOrNull { it.session.id == workout.plannedSessionId }?.session?.title ?: "Treino concluído", style = MaterialTheme.typography.titleMedium)
                        Text("${timeLabel(workout.completedAt ?: workout.startedAt)} • ${workout.durationMinutes} min", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (expanded) "Ocultar séries −" else "Ver séries salvas +", color = MaterialTheme.colorScheme.primary)
                        if (expanded) {
                            val recorded = details.firstOrNull { it.workout.id == workout.id }
                            if (recorded == null) Text("Carregando os registros…")
                            recorded?.exercises?.sortedBy { it.exerciseSession.orderIndex }?.forEach { entry ->
                                HorizontalDivider()
                                Text(exercises.firstOrNull { it.id == entry.exerciseSession.exerciseId }?.name ?: "Exercício", style = MaterialTheme.typography.titleMedium)
                                entry.sets.sortedBy { it.setIndex }.forEach { set ->
                                    if (set.status == "SKIPPED") Text("Série ${set.setIndex + 1} · não realizada", style = MaterialTheme.typography.bodySmall)
                                    else {
                                        Text("Série ${set.setIndex + 1} · ${set.actualReps?.let { "$it reps" } ?: "${set.actualSeconds} s"} · RIR ${set.rir ?: "—"}", style = MaterialTheme.typography.bodyMedium)
                                        if (!set.techniqueGood) Text("Técnica a melhorar", style = MaterialTheme.typography.bodySmall)
                                        if (set.discomfort != Discomfort.NONE.name) Text(Discomfort.valueOf(set.discomfort).label(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable internal fun ExerciseLibraryScreen(exercises: List<ExerciseEntity>) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("ALL") }
    val visible = exercises.filter { (filter == "ALL" || it.movementPattern == filter) && it.name.contains(search.trim(), ignoreCase = true) }
    Page("Explore os movimentos", scrollable = false) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Buscar exercício") }, singleLine = true) }
            item { Choices(listOf("ALL") + MovementPattern.entries.map { it.name }, { it == filter }, { if (it == "ALL") "Todos" else MovementPattern.valueOf(it).label() }) { filter = it } }
            if (visible.isEmpty()) item { Text("Nenhum exercício encontrado. Tente outro nome ou grupo.") }
            items(visible, key = { it.id }) { exercise ->
                var expanded by rememberSaveable(exercise.id) { mutableStateOf(false) }
                Card(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(MovementPattern.valueOf(exercise.movementPattern).label(), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                        Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                        Text("Nível ${exercise.difficultyLevel} • ${exercise.requiredEquipment.split(',').filter { it.isNotBlank() }.joinToString { Equipment.valueOf(it).label() }}", style = MaterialTheme.typography.bodySmall)
                        Text(if (expanded) "Fechar detalhes −" else "Como fazer +", color = MaterialTheme.colorScheme.primary)
                        if (expanded) ExerciseInstructions(exercise)
                    }
                }
            }
        }
    }
}

@Composable internal fun ExerciseInstructions(exercise: ExerciseEntity) {
    Text(exercise.instructions)
    exercise.techniqueCues.split('|').filter { it.isNotBlank() }.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
    if (exercise.commonMistakes.isNotBlank()) Text("Evite: ${exercise.commonMistakes.replace('|', ';')}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable internal fun SummaryScreen(state: AppUiState, home: () -> Unit) {
    Page("Treino registrado.", "Mais um passo na sua rotina.") {
        Hero {
            Text("SESSÃO CONCLUÍDA", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            Text("Bom descanso.", style = MaterialTheme.typography.headlineMedium)
            Text("As séries que você confirmou estão salvas. Consulte os detalhes na aba Histórico.")
            state.history.firstOrNull()?.let { Text("${it.durationMinutes} minutos registrados", style = MaterialTheme.typography.titleLarge) }
        }
        PrimaryAction("Voltar ao início", onClick = home)
    }
}
