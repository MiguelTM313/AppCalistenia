package com.calistenia.app.data

data class ExerciseVideo(val youtubeId: String, val author: String, val variation: String? = null) {
    init { require(youtubeId.matches(Regex("[A-Za-z0-9_-]{11}"))) }
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$youtubeId"
}

/** Bundled by stable exercise ID so existing Room catalogues also receive new videos. */
object ExerciseVideos {
    val all: Map<String, ExerciseVideo> = mapOf(
        "wall_push_up" to ExerciseVideo("QpMTk21EmaM", "HASfit"),
        "incline_push_up" to ExerciseVideo("nptMG5hV90c", "Calixpert"),
        "knee_push_up" to ExerciseVideo("ybdJn-OwSww", "HASfit"),
        "push_up" to ExerciseVideo("chj5koXqjcw", "HASfit"),
        "decline_push_up" to ExerciseVideo("qhEfTHPO3aI", "HASfit"),
        "diamond_push_up" to ExerciseVideo("UiIMMoKnkWc", "HASfit"),
        "band_row" to ExerciseVideo("LSkyinhmA8k", "Get Healthy U · Chris Freytag", "Remada sentada com elástico"),
        "ring_row" to ExerciseVideo("sEAOZc77wk8", "CrossFit"),
        "australian_row" to ExerciseVideo("vXmX6u_raTs", "Wright Training", "Remada em fitas de suspensão"),
        "dead_hang" to ExerciseVideo("vG159HkLrhY", "Calixpert"),
        "scapular_pull_up" to ExerciseVideo("W7bcEoXlmOg", "Calixpert"),
        "pull_up" to ExerciseVideo("HRV5YKKaeVw", "CrossFit"),
        "chest_to_bar" to ExerciseVideo("PmdNNN8nLGI", "CrossFit", "Barra ao peito sem balanço"),
        "chair_squat" to ExerciseVideo("b7I5_cCeYY8", "Rehab Hero", "Agachamento tocando o banco"),
        "bodyweight_squat" to ExerciseVideo("eAFSpUExcwc", "Calixpert"),
        "split_squat" to ExerciseVideo("tE1QpUMzo2w", "Rehab Hero"),
        "reverse_lunge" to ExerciseVideo("hwdGTe09_18", "Calixpert"),
        "bulgarian_split_squat" to ExerciseVideo("pfRlldgfGRQ", "Calixpert"),
        "assisted_pistol" to ExerciseVideo("LrA-UKXoQ4o", "Rehab Hero", "Pistol com apoio no banco"),
        "pistol_squat" to ExerciseVideo("rJY-zB9h6YM", "HASfit"),
        "calf_raise" to ExerciseVideo("Bir4geFDeJw", "HASfit"),
        "dead_bug" to ExerciseVideo("UKOwvzv1zuw", "Calixpert"),
        "forearm_plank" to ExerciseVideo("MypRN5Q754o", "HASfit"),
        "side_plank" to ExerciseVideo("E5koNMd2Ic0", "Rehab Hero"),
        "hollow_hold" to ExerciseVideo("TuLnKCIf5xI", "Calixpert"),
        "bird_dog" to ExerciseVideo("pBOXOVDDiUM", "Calixpert"),
        "hanging_knee_raise" to ExerciseVideo("PpmXveEoNOI", "HASfit"),
        "shoulder_mobility" to ExerciseVideo("AVFrTWQKHJA", "Rehab Hero", "Deslizamento dos braços na parede"),
        "ankle_dorsiflexion" to ExerciseVideo("dr1FYumuGC4", "Rehab Hero"),
        "hip_90_90" to ExerciseVideo("F5owFeKgpoc", "Rehab Hero"),
        "thoracic_rotation" to ExerciseVideo("hMQqwTkGwvs", "Rehab Hero"),
        "wrist_prep" to ExerciseVideo("YdZ57sqKqqk", "Rehab Hero", "Círculos de punhos em quatro apoios"),
        "deep_squat_hold" to ExerciseVideo("IHApHfNA2Ag", "Hinge Health"),
        "march_in_place" to ExerciseVideo("WmXNILMVHbY", "Diabetes.co.uk"),
        "low_impact_jacks" to ExerciseVideo("Bqy1xIXX2nc", "HASfit"),
        "mountain_climber" to ExerciseVideo("eJllA-pZlb8", "Calixpert")
    )
}
