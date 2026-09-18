package com.calistenia.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.calistenia.app.data.SetInput
import com.calistenia.app.data.WorkoutPlayerState
import com.calistenia.domain.model.Discomfort
import kotlinx.coroutines.delay

internal fun remainingRestSeconds(player: WorkoutPlayerState, now: Long): Long {
    val latest = player.orderedExercises.flatMap { it.sets }.maxByOrNull { it.recordedAt } ?: return 0
    if (latest.status != "COMPLETED" || latest.recordedAt <= 0) return 0
    val execution = player.orderedExercises.firstOrNull { it.exerciseSession.id == latest.exerciseSessionId } ?: return 0
    val prescription = player.planned.exerciseRows.sortedBy { it.planned.priority }.getOrNull(execution.exerciseSession.orderIndex) ?: return 0
    return ((latest.recordedAt + prescription.planned.restSeconds * 1000L - now + 999) / 1000).coerceIn(0, prescription.planned.restSeconds.toLong())
}

@Composable internal fun WorkoutPlayerScreen(player: WorkoutPlayerState, saveSet: (SetInput) -> Unit, skip: (String) -> Unit, onBack: () -> Unit, finish: () -> Unit) {
    val rows = player.planned.exerciseRows.sortedBy { it.planned.priority }
    val exerciseIndex = player.nextExerciseIndex
    val setIndex = player.nextSetIndex
    val row = rows.getOrNull(exerciseIndex)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(player.workout.workout.id) { while (true) { now = System.currentTimeMillis(); delay(1_000) } }
    val view = LocalView.current
    DisposableEffect(view) { val previous = view.keepScreenOn; view.keepScreenOn = true; onDispose { view.keepScreenOn = previous } }
    var confirmEnd by rememberSaveable { mutableStateOf(false) }
    var confirmSkip by rememberSaveable { mutableStateOf(false) }
    val allSets = player.orderedExercises.flatMap { it.sets }
    val completed = allSets.count { it.status == "COMPLETED" }
    val total = rows.sumOf { it.planned.sets }.coerceAtLeast(1)
    val latestTimestamp = allSets.maxOfOrNull { it.recordedAt } ?: 0L
    var dismissedRest by rememberSaveable { mutableLongStateOf(0L) }
    val rest = if (dismissedRest == latestTimestamp) 0L else remainingRestSeconds(player, now)
    val elapsed = ((now - player.workout.workout.startedAt) / 1000).coerceAtLeast(0)

    Page(if (row == null) "Você chegou ao fim." else row.exercise.name,
        "${player.planned.session.title} • ${durationLabel(elapsed)}", onBack = onBack) {
        LinearProgressIndicator(progress = { allSets.size.toFloat() / total }, modifier = Modifier.fillMaxWidth())
        Text("$completed séries realizadas • ${allSets.count { it.status == "SKIPPED" }} não realizadas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (row == null) {
            Hero {
                Text("TUDO REGISTRADO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                Text("$completed séries salvas", style = MaterialTheme.typography.headlineMedium)
                Text("Finalize para incluir esta sessão no seu histórico.")
                PrimaryAction("Finalizar treino", onClick = finish)
            }
        } else {
            key(row.exercise.id, setIndex) {
                val timed = row.exercise.prescriptionType == "TIME"
                var value by rememberSaveable { mutableIntStateOf(row.planned.targetMin.coerceAtLeast(1)) }
                var rir by rememberSaveable { mutableIntStateOf(2) }
                var technique by rememberSaveable { mutableStateOf(true) }
                var discomfortName by rememberSaveable { mutableStateOf(Discomfort.NONE.name) }
                var instructions by rememberSaveable { mutableStateOf(false) }
                var feedback by rememberSaveable { mutableStateOf(false) }
                var effortStartedAt by rememberSaveable { mutableLongStateOf(0L) }
                val discomfort = Discomfort.valueOf(discomfortName)
                Hero {
                    Text("EXERCÍCIO ${exerciseIndex + 1} DE ${rows.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Série ${setIndex + 1} de ${row.planned.sets}", style = MaterialTheme.typography.headlineMedium)
                    Text("Alvo: ${row.planned.targetMin}–${row.planned.targetMax} ${if (timed) "segundos" else "repetições"}", style = MaterialTheme.typography.titleMedium)
                    TextButton({ instructions = !instructions }) { Text(if (instructions) "Ocultar instruções" else "Como fazer este exercício") }
                    if (instructions) ExerciseInstructions(row.exercise)
                }
                if (rest > 0) SectionCard("Hora de recuperar") {
                    Text(durationLabel(rest), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.secondary)
                    Text("A próxima série fica disponível ao final do descanso.")
                    TextButton({ dismissedRest = latestTimestamp }) { Text("Encerrar descanso") }
                }
                SectionCard {
                    if (timed) {
                        if (effortStartedAt > 0) Text(durationLabel((now - effortStartedAt) / 1000), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.secondary)
                        OutlinedButton({
                            if (effortStartedAt == 0L) effortStartedAt = System.currentTimeMillis()
                            else {
                                value = ((System.currentTimeMillis() - effortStartedAt) / 1000).toInt().coerceIn(1, maxOf(300, row.planned.targetMax * 2))
                                effortStartedAt = 0
                            }
                        }, Modifier.fillMaxWidth(), enabled = rest == 0L && !LocalBusy.current) {
                            Text(if (effortStartedAt == 0L) "Iniciar cronômetro" else "Parar e usar tempo")
                        }
                    }
                    Counter(if (timed) "Segundos realizados" else "Repetições realizadas", value,
                        1..maxOf(if (timed) 300 else 100, row.planned.targetMax * 2), { value = it }, if (timed) 5 else 1)
                    HorizontalDivider()
                    Text("Quanto ainda conseguiria fazer?", style = MaterialTheme.typography.titleMedium)
                    Text("RIR: repetições que sobrariam com boa técnica. 0 = nenhuma.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Choices((0..5).toList(), { it == rir }, { if (it == 5) "5+" else "$it" }) { rir = it }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Técnica adequada", Modifier.weight(1f).padding(top = 12.dp))
                        Switch(technique, { technique = it }, enabled = !LocalBusy.current)
                    }
                    OutlinedButton({ feedback = !feedback }, Modifier.fillMaxWidth()) { Text(discomfort.label()) }
                    if (feedback) Choices(Discomfort.entries.toList(), { it == discomfort }, { it.label() }) { discomfortName = it.name }
                    if (discomfort in setOf(Discomfort.PAIN, Discomfort.SHARP_PAIN, Discomfort.DIZZINESS, Discomfort.UNEXPECTED_BREATHLESSNESS)) {
                        Notice("Interrompa a atividade e procure orientação adequada. Você pode registrar a série que já realizou e encerrar a sessão.", error = true)
                    }
                    PrimaryAction("Salvar série", rest == 0L && effortStartedAt == 0L) { saveSet(SetInput(row.exercise.id, setIndex, value, rir, discomfort, technique)) }
                    Text("Cada série confirmada fica salva no aparelho.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val saved = player.orderedExercises.getOrNull(exerciseIndex)?.sets.orEmpty().filter { it.status == "COMPLETED" }
                if (saved.isNotEmpty()) SectionCard("Já realizado neste exercício") {
                    saved.sortedBy { it.setIndex }.forEach { set -> Text("Série ${set.setIndex + 1} · ${set.actualReps?.let { "$it reps" } ?: "${set.actualSeconds} s"} · RIR ${set.rir ?: "—"}", style = MaterialTheme.typography.bodyMedium) }
                }
                TextButton({ confirmSkip = true }, enabled = !LocalBusy.current) { Text("Pular restante do exercício") }
                TextButton({ confirmEnd = true }, enabled = !LocalBusy.current) { Text("Encerrar treino agora") }
            }
        }
        Text("Para pausar, volte ao início. Suas séries confirmadas continuam salvas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (confirmSkip && row != null) AlertDialog(onDismissRequest = { confirmSkip = false }, title = { Text("Pular o restante?") },
        text = { Text("As séries já realizadas serão preservadas. Apenas as séries restantes deste exercício serão marcadas como não realizadas.") },
        confirmButton = { TextButton({ confirmSkip = false; skip(row.exercise.id) }) { Text("Pular restante") } },
        dismissButton = { TextButton({ confirmSkip = false }) { Text("Continuar exercício") } })
    if (confirmEnd) AlertDialog(onDismissRequest = { confirmEnd = false }, title = { Text("Encerrar esta sessão?") },
        text = { Text("As $completed séries confirmadas serão mantidas. O que falta será marcado como não realizado. Valores ainda não confirmados não serão salvos.") },
        confirmButton = { TextButton({ confirmEnd = false; finish() }) { Text("Encerrar e salvar") } },
        dismissButton = { TextButton({ confirmEnd = false }) { Text("Voltar ao treino") } })
}
