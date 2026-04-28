package com.example.foodtracker.data.diet

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PlannedMeal(
    val mealType: String,
    val time: String,
    val items: String,
    val calories: Int = 0,
    val protein: Int = 0
)

/**
 * Manages the user's current diet plan using SharedPreferences (JSON).
 * Plans can come from AI or from a default template.
 */
class DietPlanManager(context: Context) {
    private val prefs = context.getSharedPreferences("diet_plan_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PLAN = "current_diet_plan"
        private const val KEY_PLAN_TIMESTAMP = "plan_timestamp"
        private const val KEY_PLAN_SOURCE = "plan_source"
    }

    fun savePlan(meals: List<PlannedMeal>, source: String = "manual") {
        val arr = JSONArray()
        meals.forEach { meal ->
            arr.put(JSONObject().apply {
                put("mealType", meal.mealType)
                put("time", meal.time)
                put("items", meal.items)
                put("calories", meal.calories)
                put("protein", meal.protein)
            })
        }
        prefs.edit()
            .putString(KEY_PLAN, arr.toString())
            .putLong(KEY_PLAN_TIMESTAMP, System.currentTimeMillis())
            .putString(KEY_PLAN_SOURCE, source)
            .apply()
    }

    fun loadPlan(): List<PlannedMeal> {
        val json = prefs.getString(KEY_PLAN, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                PlannedMeal(
                    mealType = o.optString("mealType", ""),
                    time = o.optString("time", ""),
                    items = o.optString("items", ""),
                    calories = o.optInt("calories", 0),
                    protein = o.optInt("protein", 0)
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun hasPlan(): Boolean = prefs.contains(KEY_PLAN)

    fun getPlanSource(): String = prefs.getString(KEY_PLAN_SOURCE, "default") ?: "default"

    fun clearPlan() = prefs.edit().remove(KEY_PLAN).apply()

    /**
     * Parse AI response text into PlannedMeal list.
     * Looks for meal-type headers (Breakfast:, Lunch:, Dinner:, etc.)
     */
    fun parseFromAIResponse(response: String): List<PlannedMeal> {
        val meals = mutableListOf<PlannedMeal>()
        val lines = response.lines()

        val mealTimeMap = mapOf(
            "breakfast" to "7:00 AM",
            "morning snack" to "10:00 AM",
            "mid-morning" to "10:00 AM",
            "lunch" to "12:30 PM",
            "afternoon snack" to "4:00 PM",
            "evening snack" to "4:00 PM",
            "snack" to "4:00 PM",
            "dinner" to "7:00 PM",
            "supper" to "7:00 PM"
        )

        var currentType = ""
        val currentItems = StringBuilder()

        fun flushMeal() {
            if (currentType.isNotEmpty() && currentItems.isNotEmpty()) {
                val time = mealTimeMap.entries.firstOrNull { (k, _) ->
                    currentType.lowercase().contains(k)
                }?.value ?: ""
                meals.add(PlannedMeal(mealType = currentType, time = time, items = currentItems.toString().trim()))
                currentItems.clear()
            }
        }

        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach

            val normalized = trimmed.lowercase().replace("**", "").replace("#", "")
            val isMealHeader = mealTimeMap.keys.any { key ->
                normalized.startsWith(key) || normalized.contains("$key:")
            }

            if (isMealHeader) {
                flushMeal()
                currentType = trimmed.replace("**", "").replace(":", "").replace("#", "").trim()
            } else if (currentType.isNotEmpty()) {
                val clean = trimmed.trimStart('•', '-', '*', ' ')
                if (clean.isNotEmpty()) {
                    if (currentItems.isNotEmpty()) currentItems.append("\n")
                    currentItems.append(clean)
                }
            }
        }
        flushMeal()

        return meals
    }

    /**
     * Returns true if the given text looks like a meal plan
     * (contains at least 2 meal-type headers).
     */
    fun looksLikeMealPlan(text: String): Boolean {
        val lower = text.lowercase()
        val keywords = listOf("breakfast", "lunch", "dinner", "snack", "supper")
        return keywords.count { lower.contains(it) } >= 2
    }

    /**
     * Default plan based on user's fitness goal and calorie target.
     */
    fun buildDefaultPlan(fitnessGoal: String, calorieGoal: Int): List<PlannedMeal> {
        return when {
            fitnessGoal.contains("muscle") || fitnessGoal.contains("gain") -> listOf(
                PlannedMeal("Breakfast", "7:00 AM", "Oatmeal with milk + 3 eggs scrambled + banana", calorieGoal / 5, calorieGoal / 25),
                PlannedMeal("Morning Snack", "10:00 AM", "Greek yogurt + almonds", calorieGoal / 8, calorieGoal / 40),
                PlannedMeal("Lunch", "1:00 PM", "Grilled chicken breast + brown rice + vegetables", calorieGoal / 4, calorieGoal / 15),
                PlannedMeal("Afternoon Snack", "4:00 PM", "Protein shake + fruit", calorieGoal / 8, calorieGoal / 30),
                PlannedMeal("Dinner", "7:00 PM", "Salmon or lean beef + sweet potato + salad", calorieGoal / 4, calorieGoal / 20)
            )
            fitnessGoal.contains("loss") || fitnessGoal.contains("weight") -> listOf(
                PlannedMeal("Breakfast", "7:00 AM", "2 eggs + whole grain toast + vegetables", calorieGoal / 5, calorieGoal / 25),
                PlannedMeal("Morning Snack", "10:00 AM", "Apple + handful of nuts", calorieGoal / 10, calorieGoal / 50),
                PlannedMeal("Lunch", "1:00 PM", "Grilled chicken + salad + quinoa", calorieGoal / 4, calorieGoal / 15),
                PlannedMeal("Afternoon Snack", "4:00 PM", "Cucumber + hummus", calorieGoal / 10, calorieGoal / 60),
                PlannedMeal("Dinner", "7:00 PM", "Fish + steamed vegetables + lentil soup", calorieGoal / 4, calorieGoal / 20)
            )
            else -> listOf(
                PlannedMeal("Breakfast", "7:00 AM", "Oatmeal with fruits + green tea", calorieGoal / 5, calorieGoal / 30),
                PlannedMeal("Morning Snack", "10:00 AM", "Greek yogurt with berries", calorieGoal / 8, calorieGoal / 40),
                PlannedMeal("Lunch", "1:00 PM", "Grilled protein + whole grains + vegetables", calorieGoal / 4, calorieGoal / 20),
                PlannedMeal("Afternoon Snack", "4:00 PM", "Fruit + nuts", calorieGoal / 10, calorieGoal / 50),
                PlannedMeal("Dinner", "7:00 PM", "Lean protein + vegetables + whole grains", calorieGoal / 4, calorieGoal / 20)
            )
        }
    }
}
