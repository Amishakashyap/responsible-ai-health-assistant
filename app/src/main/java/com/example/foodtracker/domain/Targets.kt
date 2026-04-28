package com.example.foodtracker.domain

data class Targets(
    val calories: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int,
    val fiberG: Int,
    val sodiumMg: Int,
)

enum class ExerciseFreq(val activityFactor: Double) {
    none(1.2),
    twice_week(1.375),
    regular(1.55),
    proper_workout(1.725);
}

fun computeTargets(
    gender: String,
    age: Int,
    heightCm: Double,
    weightKg: Double,
    exerciseFreq: String,
    goal: String,
    bodyFatPct: Double? = null,
): Targets {
    val bmr = if (bodyFatPct != null) {
        val lbm = weightKg * (1 - bodyFatPct / 100)
        370 + 21.6 * lbm
    } else {
        if (gender.lowercase() == "male") {
            10 * weightKg + 6.25 * heightCm - 5 * age + 5
        } else {
            10 * weightKg + 6.25 * heightCm - 5 * age - 161
        }
    }
    val af = try {
        ExerciseFreq.valueOf(exerciseFreq).activityFactor
    } catch (e: Exception) {
        1.2
    }
    var cals = bmr * af
    when (goal.lowercase()) {
        "weightloss" -> cals *= 0.85
        "gain" -> cals *= 1.10
        "muscle" -> cals *= 1.15
    }
    val minCals = if (gender.lowercase() == "male") 1500.0 else 1200.0
    cals = maxOf(cals, minCals)

    val proteinPerKg = if (goal.lowercase() == "muscle") 2.2 else if (goal.lowercase() == "weightloss") 2.0 else 1.8
    val proteinG = maxOf(1.2 * weightKg, proteinPerKg * weightKg)
    var fatG = maxOf(0.6 * weightKg, (cals * 0.30) / 9.0)
    val proteinKcal = proteinG * 4
    val fatKcal = fatG * 9
    val carbsKcal = (cals - proteinKcal - fatKcal).coerceAtLeast(0.0)
    val carbsG = carbsKcal / 4.0

    val fiberG = ((cals / 1000.0) * 14).toInt()
    val sodiumMg = 2300

    return Targets(
        calories = cals.toInt(),
        proteinG = proteinG.toInt(),
        fatG = fatG.toInt(),
        carbsG = carbsG.toInt(),
        fiberG = fiberG,
        sodiumMg = sodiumMg,
    )
}
