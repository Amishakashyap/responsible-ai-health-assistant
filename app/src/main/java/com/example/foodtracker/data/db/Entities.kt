package com.example.foodtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "name_normalized") val nameNormalized: String,
    @ColumnInfo(name = "calories_kcal_per_100g") val calories: Double?,
    @ColumnInfo(name = "protein_g_per_100g") val proteinG: Double?,
    @ColumnInfo(name = "fat_g_per_100g") val fatG: Double?,
    @ColumnInfo(name = "carbs_g_per_100g") val carbsG: Double?,
    @ColumnInfo(name = "fiber_g_per_100g") val fiberG: Double?,
    @ColumnInfo(name = "sugar_g_per_100g") val sugarG: Double?,
    @ColumnInfo(name = "sodium_mg_per_100g") val sodiumMg: Double?,
    @ColumnInfo(name = "calcium_mg_per_100g") val calciumMg: Double?,
    @ColumnInfo(name = "iron_mg_per_100g") val ironMg: Double?,
    @ColumnInfo(name = "vitamin_c_mg_per_100g") val vitaminCMg: Double?,
    @ColumnInfo(name = "vitamin_b11_mg_per_100g") val vitaminB11Mg: Double?,
)

@Entity(tableName = "entry")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // yyyy-MM-dd
    @ColumnInfo(name = "meal_type") val mealType: String, // breakfast|lunch|dinner|snack
    @ColumnInfo(name = "food_id") val foodId: Long,
    @ColumnInfo(name = "quantity_g") val quantityG: Double,
    @ColumnInfo(name = "user_id") val userId: Long = 0, // 0 = guest (not saved), >0 = registered user
)

@Entity(tableName = "user")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val password: String = "", // Not used for login — email+name auth
    val name: String,
    val city: String = "",
    val gender: String = "",
    val age: Int = 0,
    @ColumnInfo(name = "blood_group") val bloodGroup: String = "",
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val userId: Long,
    @ColumnInfo(name = "medical_history") val medicalHistory: String = "",
    val goal: String, // weightloss|gain|muscle
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
    @ColumnInfo(name = "height_cm") val heightCm: Double,
    @ColumnInfo(name = "target_weight_kg") val targetWeightKg: Double = 0.0,
    @ColumnInfo(name = "exercise_freq") val exerciseFreq: String, // none|twice_week|regular|proper_workout
    @ColumnInfo(name = "diet_type") val dietType: String = "none", // vegetarian|vegan|keto|paleo|mediterranean|none
    @ColumnInfo(name = "allergies") val allergies: String = "", // comma-separated
    @ColumnInfo(name = "food_preferences") val foodPreferences: String = "" // comma-separated
)

/**
 * Daily nutrition tracking log
 */
@Entity(tableName = "daily_nutrition_log")
data class DailyNutritionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    val date: String, // yyyy-MM-dd
    @ColumnInfo(name = "calories_consumed") val caloriesConsumed: Int = 0,
    @ColumnInfo(name = "protein_consumed") val proteinConsumed: Int = 0,
    @ColumnInfo(name = "carbs_consumed") val carbsConsumed: Int = 0,
    @ColumnInfo(name = "fat_consumed") val fatConsumed: Int = 0,
    @ColumnInfo(name = "water_consumed_ml") val waterConsumedMl: Int = 0,
    @ColumnInfo(name = "calories_goal") val caloriesGoal: Int,
    @ColumnInfo(name = "protein_goal") val proteinGoal: Int,
    @ColumnInfo(name = "carbs_goal") val carbsGoal: Int,
    @ColumnInfo(name = "fat_goal") val fatGoal: Int,
    @ColumnInfo(name = "water_goal_ml") val waterGoalMl: Int,
    @ColumnInfo(name = "meal_count") val mealCount: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Verified recipe with known nutrition data
 */
@Entity(tableName = "recipe")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    @ColumnInfo(name = "calories_per_serving") val caloriesPerServing: Int,
    @ColumnInfo(name = "protein_per_serving") val proteinPerServing: Int,
    @ColumnInfo(name = "carbs_per_serving") val carbsPerServing: Int,
    @ColumnInfo(name = "fat_per_serving") val fatPerServing: Int,
    @ColumnInfo(name = "servings") val servings: Int = 1,
    @ColumnInfo(name = "prep_time_minutes") val prepTimeMinutes: Int,
    @ColumnInfo(name = "cook_time_minutes") val cookTimeMinutes: Int,
    @ColumnInfo(name = "diet_type") val dietType: String, // vegetarian|vegan|keto|paleo|mediterranean|omnivore
    @ColumnInfo(name = "meal_type") val mealType: String, // breakfast|lunch|dinner|snack
    val ingredients: String, // JSON array or newline-separated
    val instructions: String, // Newline-separated steps
    val tags: String = "" // comma-separated: high-protein, low-carb, quick, etc.
)

/**
 * AI-generated daily health summary
 */
@Entity(tableName = "daily_health_summary")
data class DailyHealthSummary(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    val date: String, // yyyy-MM-dd
    @ColumnInfo(name = "summary_text") val summaryText: String, // AI-generated daily summary
    @ColumnInfo(name = "meal_suggestion") val mealSuggestion: String, // Next meal suggestion
    @ColumnInfo(name = "recipe_id") val recipeId: Long? = null, // Recommended recipe
    @ColumnInfo(name = "habit_tip") val habitTip: String, // Small habit recommendation
    @ColumnInfo(name = "warnings") val warnings: String = "", // JSON array of health warnings
    @ColumnInfo(name = "motivational_message") val motivationalMessage: String = "",
    @ColumnInfo(name = "generated_at") val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Weight entry for tracking user weight over time
 */
@Entity(tableName = "weight_entry")
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    val date: String, // yyyy-MM-dd
    @ColumnInfo(name = "weight_kg") val weightKg: Float,
    @ColumnInfo(name = "recorded_at") val recordedAt: Long = System.currentTimeMillis()
)
