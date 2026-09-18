package com.calistenia.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.calistenia.app.data.local.ExerciseEntity
import com.calistenia.domain.model.*
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable internal fun SafetyScreen(next: () -> Unit) {
    var guidance by rememberSaveable { mutableStateOf(false) }
    Page("Seu corpo.\nSeu ritmo.", "Um espaço simples para criar constância e treinar em casa.") {
        Hero {
            Text("COMECE COM CUIDADO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text("Como você está hoje?", style = MaterialTheme.typography.headlineSmall)
            Text("Você sente dor no peito, desmaio ou tontura importante, falta de ar incomum, ou recebeu orientação profissional para não se exercitar?")
        }
        Notice("Não treine através da dor. O aplicativo não faz diagnósticos nem substitui avaliação profissional.")
        PrimaryAction("Não tenho esses sinais", onClick = next)
        OutlinedButton({ guidance = true }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Preciso de orientação antes") }
        Text("Sem conta. Sem anúncios. Seus registros ficam neste aparelho.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
    if (guidance) AlertDialog(onDismissRequest = { guidance = false }, title = { Text("Cuide de você primeiro") },
        text = { Text("Interrompa a atividade e procure avaliação profissional antes de iniciar o treino se apresentar esses sinais. Você pode fechar o app e voltar depois.") },
        confirmButton = { TextButton({ guidance = false }) { Text("Entendi") } })
}

@Composable internal fun OnboardingScreen(done: (Int, Int, Double, Goal, Set<DayOfWeek>, Int, Set<Equipment>) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var age by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var goal by rememberSaveable { mutableStateOf(Goal.STRENGTH.name) }
    var daysMask by rememberSaveable { mutableIntStateOf(21) } // Monday, Wednesday, Friday.
    var minutes by rememberSaveable { mutableIntStateOf(20) }
    var equipmentMask by rememberSaveable { mutableIntStateOf(0) }
    val days = DayOfWeek.entries.filter { daysMask and (1 shl it.ordinal) != 0 }.toSet()
    val equipment = Equipment.entries.filter { it != Equipment.NONE && equipmentMask and (1 shl it.ordinal) != 0 }.toSet()
    val ageValue = age.toIntOrNull()
    val heightValue = height.toIntOrNull()
    val weightValue = weight.replace(',', '.').toDoubleOrNull()
    val valid = ageValue != null && ageValue in 18..100 && heightValue != null && heightValue in 100..230 && weightValue != null && weightValue.isFinite() && weightValue in 30.0..300.0
    key(step) {
        Page(listOf("Seu ponto de partida", "Treino que cabe na rotina", "Seu espaço de treino")[step],
            "Etapa ${step + 1} de 3", onBack = if (step > 0) ({ step-- }) else null) {
            LinearProgressIndicator(progress = { (step + 1) / 3f }, modifier = Modifier.fillMaxWidth())
            when (step) {
                0 -> {
                    SectionCard("Sobre você") {
                        ProfileField("Idade (anos)", age, age.isNotBlank() && (ageValue == null || ageValue !in 18..100), "De 18 a 100 anos", { age = it })
                        ProfileField("Altura (cm)", height, height.isNotBlank() && (heightValue == null || heightValue !in 100..230), "De 100 a 230 cm", { height = it })
                        ProfileField("Peso (kg)", weight, weight.isNotBlank() && (weightValue == null || !weightValue.isFinite() || weightValue !in 30.0..300.0), "De 30 a 300 kg; use vírgula ou ponto", { weight = it }, decimal = true)
                    }
                    Text("Este app foi pensado para adultos. Preencha os três campos para continuar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryAction("Continuar", valid) { step++ }
                }
                1 -> {
                    SectionCard("O que você busca?") { Choices(Goal.entries.toList(), { it.name == goal }, { it.label() }) { goal = it.name } }
                    SectionCard("Quais dias funcionam para você?") {
                        Choices(DayOfWeek.entries.toList(), { it in days }, { it.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("pt-BR")) }) { day ->
                            if (day in days || days.size < 5) daysMask = daysMask xor (1 shl day.ordinal)
                        }
                        Text("${days.size} dia(s) por semana. Escolha de 1 a 5.", style = MaterialTheme.typography.bodySmall)
                    }
                    SectionCard("Tempo por sessão") { Choices(listOf(10, 15, 20, 30, 45), { it == minutes }, { "$it min" }) { minutes = it } }
                    PrimaryAction("Continuar", days.isNotEmpty()) { step++ }
                }
                else -> {
                    SectionCard("O que você tem disponível?") {
                        Text("Selecione apenas equipamentos que você pode usar com segurança. Nenhum selecionado = treino sem equipamento.")
                        Choices(Equipment.entries.filter { it != Equipment.NONE }, { it in equipment }, { it.label() }) {
                            equipmentMask = equipmentMask xor (1 shl it.ordinal)
                        }
                    }
                    Hero {
                        Text("SEU PLANO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        Text("${days.size} dias • $minutes min", style = MaterialTheme.typography.headlineMedium)
                        Text(Goal.valueOf(goal).label())
                        Text(if (equipment.isEmpty()) "Sem equipamento" else equipment.joinToString { it.label() }, style = MaterialTheme.typography.bodyMedium)
                    }
                    PrimaryAction("Salvar e avaliar meu nível", valid && days.isNotEmpty()) {
                        done(ageValue!!, heightValue!!, weightValue!!, Goal.valueOf(goal), days, minutes, equipment)
                    }
                }
            }
        }
    }
}

@Composable private fun ProfileField(label: String, value: String, invalid: Boolean, hint: String, update: (String) -> Unit, decimal: Boolean = false) {
    OutlinedTextField(value, { if (it.length <= 6) update(it) }, Modifier.fillMaxWidth(), label = { Text(label) },
        isError = invalid, supportingText = if (invalid) ({ Text(hint) }) else null, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number))
}

@Composable internal fun AssessmentScreen(exercises: List<ExerciseEntity>, done: (Map<MovementPattern, Int>) -> Unit) {
    // Each choice is restored across activity recreation. This is a self estimate, not a diagnostic test.
    var push by rememberSaveable { mutableIntStateOf(1) }
    var pull by rememberSaveable { mutableIntStateOf(1) }
    var legs by rememberSaveable { mutableIntStateOf(1) }
    var core by rememberSaveable { mutableIntStateOf(1) }
    var mobility by rememberSaveable { mutableIntStateOf(1) }
    val levels = mapOf(MovementPattern.PUSH to push, MovementPattern.PULL to pull, MovementPattern.LEGS to legs, MovementPattern.CORE to core, MovementPattern.MOBILITY to mobility, MovementPattern.CONDITIONING to 1)
    Page("Comece de onde está", "Uma estimativa por movimento. Na dúvida, escolha um nível menor.") {
        Notice("Não precisa testar seu limite agora. Considere apenas movimentos que você já realiza com controle e sem dor.")
        MovementPattern.entries.filter { it != MovementPattern.CONDITIONING }.forEach { pattern ->
            SectionCard(pattern.label()) {
                val reference = exercises.filter { it.movementPattern == pattern.name && it.difficultyLevel <= levels.getValue(pattern) }.maxByOrNull { it.difficultyLevel }
                Text(reference?.let { "Referência do catálogo: ${it.name}" } ?: "Ainda estou começando", style = MaterialTheme.typography.bodyMedium)
                Choices(listOf(0, 1, 2, 3), { it == levels[pattern] }, { if (it == 0) "Começando" else "Nível $it" }) { value ->
                    when(pattern) { MovementPattern.PUSH -> push = value; MovementPattern.PULL -> pull = value; MovementPattern.LEGS -> legs = value; MovementPattern.CORE -> core = value; MovementPattern.MOBILITY -> mobility = value; else -> Unit }
                }
            }
        }
        PrimaryAction("Criar meu plano") { done(levels) }
    }
}

@Composable internal fun ReadinessScreen(onBack: () -> Unit, done: (Readiness) -> Unit) {
    var energy by rememberSaveable { mutableIntStateOf(3) }
    var sleep by rememberSaveable { mutableIntStateOf(3) }
    var soreness by rememberSaveable { mutableIntStateOf(1) }
    var motivation by rememberSaveable { mutableIntStateOf(3) }
    Page("Como você está?", "Um check-in rápido antes de começar.", onBack = onBack) {
        Rating("Energia", "1 · baixa    5 · alta", energy) { energy = it }
        Rating("Sono", "1 · ruim    5 · bom", sleep) { sleep = it }
        Rating("Dor muscular", "1 · nenhuma    5 · muita", soreness) { soreness = it }
        Rating("Motivação", "1 · baixa    5 · alta", motivation) { motivation = it }
        val readiness = Readiness(energy, sleep, soreness, motivation)
        Notice(if (readiness.score < 2.5) "Hoje vamos reduzir o volume desta sessão. Respeite seus limites." else "O plano será mantido. Você pode pausar quando precisar.")
        Text("Este check-in não é uma avaliação médica.", style = MaterialTheme.typography.bodySmall)
        PrimaryAction("Começar treino") { done(readiness) }
    }
}

@Composable private fun Rating(title: String, hint: String, value: Int, update: (Int) -> Unit) {
    SectionCard(title) {
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Choices((1..5).toList(), { it == value }, { "$it" }, update)
    }
}
