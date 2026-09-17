package com.calistenia.app.data

import com.calistenia.app.data.local.ExerciseEntity

object ExerciseSeed {
    private data class Seed(val id: String, val name: String, val pattern: String, val level: Int, val equipment: String = "NONE", val type: String = "REPETITIONS", val next: String? = null, val previous: String? = null)

    private val seeds = listOf(
        Seed("wall_push_up", "Flexão na parede", "PUSH", 1, next = "incline_push_up"), Seed("incline_push_up", "Flexão inclinada", "PUSH", 2, "BENCH", next = "knee_push_up", previous = "wall_push_up"),
        Seed("knee_push_up", "Flexão com joelhos", "PUSH", 2, next = "push_up", previous = "incline_push_up"), Seed("push_up", "Flexão", "PUSH", 3, next = "decline_push_up", previous = "knee_push_up"),
        Seed("decline_push_up", "Flexão declinada", "PUSH", 5, "BENCH", next = "diamond_push_up", previous = "push_up"), Seed("diamond_push_up", "Flexão diamante", "PUSH", 6, previous = "decline_push_up"),
        Seed("band_row", "Remada com elástico", "PULL", 1, "RESISTANCE_BAND", next = "australian_row"), Seed("ring_row", "Remada nas argolas", "PULL", 2, "RINGS", next = "pull_up", previous = "band_row"),
        Seed("australian_row", "Remada australiana", "PULL", 2, "SUSPENSION", next = "pull_up", previous = "band_row"), Seed("dead_hang", "Suspensão na barra", "PULL", 2, "PULL_UP_BAR", "TIME", "scapular_pull_up"),
        Seed("scapular_pull_up", "Elevação escapular", "PULL", 3, "PULL_UP_BAR", next = "pull_up", previous = "dead_hang"), Seed("pull_up", "Barra fixa", "PULL", 5, "PULL_UP_BAR", next = "chest_to_bar", previous = "scapular_pull_up"),
        Seed("chest_to_bar", "Barra ao peito", "PULL", 7, "PULL_UP_BAR", previous = "pull_up"),
        Seed("chair_squat", "Agachamento no banco", "LEGS", 1, "BENCH", next = "bodyweight_squat"), Seed("bodyweight_squat", "Agachamento livre", "LEGS", 2, next = "split_squat", previous = "chair_squat"),
        Seed("split_squat", "Agachamento dividido", "LEGS", 3, next = "reverse_lunge", previous = "bodyweight_squat"), Seed("reverse_lunge", "Avanço reverso", "LEGS", 4, next = "bulgarian_split_squat", previous = "split_squat"),
        Seed("bulgarian_split_squat", "Agachamento búlgaro", "LEGS", 5, "BENCH", next = "assisted_pistol", previous = "reverse_lunge"), Seed("assisted_pistol", "Pistol assistido", "LEGS", 6, "BENCH", next = "pistol_squat", previous = "bulgarian_split_squat"),
        Seed("pistol_squat", "Pistol squat", "LEGS", 8, previous = "assisted_pistol"), Seed("calf_raise", "Elevação de panturrilha", "LEGS", 1),
        Seed("dead_bug", "Dead bug", "CORE", 1, next = "forearm_plank"), Seed("forearm_plank", "Prancha", "CORE", 2, type = "TIME", next = "hollow_hold", previous = "dead_bug"),
        Seed("side_plank", "Prancha lateral", "CORE", 3, type = "TIME", previous = "forearm_plank"), Seed("hollow_hold", "Hollow hold", "CORE", 4, type = "TIME", next = "hanging_knee_raise", previous = "forearm_plank"),
        Seed("bird_dog", "Bird dog", "CORE", 1), Seed("hanging_knee_raise", "Elevação de joelhos suspenso", "CORE", 5, "PULL_UP_BAR", previous = "hollow_hold"),
        Seed("shoulder_mobility", "Mobilidade de ombros", "MOBILITY", 1, type = "TIME"), Seed("ankle_dorsiflexion", "Dorsiflexão de tornozelo", "MOBILITY", 1, type = "TIME"),
        Seed("hip_90_90", "Mobilidade de quadril 90/90", "MOBILITY", 1, type = "TIME"), Seed("thoracic_rotation", "Rotação torácica", "MOBILITY", 1, type = "TIME"),
        Seed("wrist_prep", "Preparação de punhos", "MOBILITY", 1, type = "TIME"), Seed("deep_squat_hold", "Agachamento profundo sustentado", "MOBILITY", 2, type = "TIME"),
        Seed("march_in_place", "Marcha estacionária", "CONDITIONING", 1, type = "TIME"), Seed("low_impact_jacks", "Polichinelo sem salto", "CONDITIONING", 2, type = "TIME"),
        Seed("mountain_climber", "Escalador", "CONDITIONING", 4, type = "TIME")
    )

    val exercises: List<ExerciseEntity> = seeds.map { s ->
        val timed = s.type == "TIME"
        ExerciseEntity(s.id, s.name, "Variação de ${s.pattern.lowercase()} para treinamento progressivo.", "Execute de forma controlada e interrompa diante de dor aguda ou tontura.", s.pattern, s.pattern.lowercase(), s.level, s.equipment, s.level, s.next, s.previous, s.type, if (timed) null else 6, if (timed) null else 12, if (timed) 30 else null, 60, false, "casa,calistenia", "Mantenha controle|Respire naturalmente", "Perder alinhamento|Acelerar sem controle", null, true)
    }
}
