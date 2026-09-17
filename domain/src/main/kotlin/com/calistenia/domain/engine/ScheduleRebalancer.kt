package com.calistenia.domain.engine

import com.calistenia.domain.model.*
import java.time.LocalDate

class ScheduleRebalancer {
    fun rebalance(plan: TrainingPlan, today: LocalDate, completedSessionIds: Set<String>): TrainingPlan {
        val past = plan.sessions.filter { it.date < today || it.id in completedSessionIds }
        val remaining = plan.sessions.filter { it.date >= today && it.id !in completedSessionIds }.toMutableList()
        val missed = plan.sessions.filter { it.date < today && it.id !in completedSessionIds }
        if (missed.isNotEmpty() && remaining.isNotEmpty()) {
            val missingPatterns = missed.flatMap { it.exercises }.groupingBy { it.exercise.movementPattern }.eachCount()
            remaining.sortByDescending { session -> session.exercises.sumOf { missingPatterns[it.exercise.movementPattern] ?: 0 } }
            val futureDates = plan.sessions.filter { it.date >= today }.map { it.date }.sorted()
            remaining.replaceAll { session -> session.copy(date = futureDates[remaining.indexOf(session)]) }
        }
        return plan.copy(sessions = (past + remaining).distinctBy { it.id }.sortedBy { it.date }, explanations = plan.explanations + "Agenda reequilibrada sem alterar o histórico concluído.")
    }
}
