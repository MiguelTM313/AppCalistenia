@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.calistenia.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.calistenia.domain.model.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val LocalBusy = compositionLocalOf { false }

@Composable internal fun Page(title: String, subtitle: String? = null, scrollable: Boolean = true,
    onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val viewport = Modifier.widthIn(max = 680.dp).fillMaxSize().imePadding()
            Column((if (scrollable) viewport.verticalScroll(scroll) else viewport).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("CALISTENIA / EM CASA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (onBack != null) TextButton(onClick = onBack) { Text("Voltar") }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineLarge)
                    subtitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge) }
                }
                content()
            }
        }
    }
}

@Composable internal fun PrimaryAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled && !LocalBusy.current,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) { Text(label) }
}

@Composable internal fun SectionCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            content()
        }
    }
}

@Composable internal fun Hero(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface)))
            .padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable internal fun Notice(text: String, error: Boolean = false) {
    Surface(color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable internal fun <T> Choices(items: List<T>, selected: (T) -> Boolean, label: (T) -> String, choose: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.forEach { item -> FilterChip(selected(item), { choose(item) }, label = { Text(label(item)) }, enabled = !LocalBusy.current) }
    }
}

@Composable internal fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable internal fun Counter(label: String, value: Int, range: IntRange, onValue: (Int) -> Unit, increment: Int = 1) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton({ onValue((value - increment).coerceIn(range)) }, Modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp).semantics { contentDescription = "Diminuir $label" }, enabled = value > range.first && !LocalBusy.current) { Text("−") }
            Text("$value", style = MaterialTheme.typography.headlineLarge)
            OutlinedButton({ onValue((value + increment).coerceIn(range)) }, Modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp).semantics { contentDescription = "Aumentar $label" }, enabled = value < range.last && !LocalBusy.current) { Text("+") }
        }
    }
}

@Composable internal fun NavGlyph(route: String) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(22.dp)) {
        val w = size.width; val h = size.height; val stroke = 1.8.dp.toPx()
        when (route) {
            "home" -> {
                drawLine(color, Offset(w * .08f, h * .45f), Offset(w * .5f, h * .1f), stroke)
                drawLine(color, Offset(w * .5f, h * .1f), Offset(w * .92f, h * .45f), stroke)
                drawRect(color, Offset(w * .23f, h * .46f), Size(w * .54f, h * .44f), style = Stroke(stroke))
            }
            "weekly" -> {
                drawRect(color, Offset(w * .12f, h * .2f), Size(w * .76f, h * .7f), style = Stroke(stroke))
                drawLine(color, Offset(w * .12f, h * .42f), Offset(w * .88f, h * .42f), stroke)
                drawLine(color, Offset(w * .33f, 0f), Offset(w * .33f, h * .3f), stroke)
                drawLine(color, Offset(w * .67f, 0f), Offset(w * .67f, h * .3f), stroke)
            }
            "progress" -> listOf(.45f, .7f, 1f).forEachIndexed { i, fraction ->
                drawRect(color, Offset(w * (.12f + i * .3f), h * (1f - fraction)), Size(w * .16f, h * fraction))
            }
            else -> {
                drawCircle(color, w * .3f, Offset(w * .4f, h * .4f), style = Stroke(stroke))
                drawLine(color, Offset(w * .63f, h * .63f), Offset(w * .95f, h * .95f), stroke)
            }
        }
    }
}

internal fun Goal.label() = when (this) {
    Goal.STRENGTH -> "Ganhar força"; Goal.HYPERTROPHY -> "Ganhar massa"; Goal.CONDITIONING -> "Mais fôlego"
    Goal.FAT_LOSS_SUPPORT -> "Apoiar emagrecimento"; Goal.CALISTHENICS_SKILLS -> "Aprender movimentos"
    Goal.MOBILITY -> "Mais mobilidade"; Goal.GENERAL_HEALTH -> "Me movimentar mais"
}
internal fun MovementPattern.label() = when (this) {
    MovementPattern.PUSH -> "Empurrar"; MovementPattern.PULL -> "Puxar"; MovementPattern.LEGS -> "Pernas"
    MovementPattern.CORE -> "Abdômen e tronco"; MovementPattern.MOBILITY -> "Mobilidade"; MovementPattern.CONDITIONING -> "Condicionamento"
}
internal fun Equipment.label() = when (this) {
    Equipment.NONE -> "Sem equipamento"; Equipment.WALL -> "Parede"; Equipment.BENCH -> "Banco firme"
    Equipment.PULL_UP_BAR -> "Barra fixa"; Equipment.PARALLETTES -> "Paralelas"; Equipment.RINGS -> "Argolas"
    Equipment.RESISTANCE_BAND -> "Elástico"; Equipment.SUSPENSION -> "Fita de suspensão"; Equipment.ADDED_WEIGHT -> "Peso extra"
}
internal fun Discomfort.label() = when (this) {
    Discomfort.NONE -> "Sem desconforto"; Discomfort.MILD -> "Desconforto leve"; Discomfort.PAIN -> "Dor"
    Discomfort.SHARP_PAIN -> "Dor aguda"; Discomfort.DIZZINESS -> "Tontura"; Discomfort.UNEXPECTED_BREATHLESSNESS -> "Falta de ar incomum"; Discomfort.OTHER -> "Outro"
}
internal fun dateLabel(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.forLanguageTag("pt-BR")))
internal fun timeLabel(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM • HH:mm", Locale.forLanguageTag("pt-BR")))
internal fun durationLabel(seconds: Long): String = "%02d:%02d".format(seconds.coerceAtLeast(0) / 60, seconds.coerceAtLeast(0) % 60)
internal fun statusLabel(status: String) = when(status) { "COMPLETED" -> "Concluído"; "IN_PROGRESS" -> "Em andamento"; "SKIPPED" -> "Não realizado"; else -> "Planejado" }
