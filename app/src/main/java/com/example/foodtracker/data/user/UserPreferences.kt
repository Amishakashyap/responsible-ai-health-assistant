package com.example.foodtracker.data.user

import android.content.Context
import android.content.SharedPreferences
import com.example.foodtracker.utils.BMICalculator
import com.example.foodtracker.utils.NutritionCalculator
import com.example.foodtracker.utils.NutritionGoals

/**
 * UserPreferences - Store and retrieve user profile and personalized data
 */
class UserPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "user_preferences"
        
        // User ID Key
        private const val KEY_USER_ID = "user_id"
        
        // User Profile Keys
        private const val KEY_NICKNAME = "user_nickname"
        private const val KEY_WEIGHT = "user_weight"
        private const val KEY_HEIGHT = "user_height"
        private const val KEY_AGE = "user_age"
        private const val KEY_GENDER = "user_gender"
        private const val KEY_ACTIVITY_LEVEL = "user_activity_level"
        private const val KEY_FITNESS_GOAL = "user_fitness_goal"
        
        // Diet Preferences
        private const val KEY_DIET_TYPE = "diet_type"
        private const val KEY_ALLERGIES = "allergies"
        private const val KEY_FOOD_PREFERENCES = "food_preferences"
        
        // Calculated Values Keys
        private const val KEY_BMI = "calculated_bmi"
        private const val KEY_CALORIE_GOAL = "daily_calorie_goal"
        private const val KEY_PROTEIN_GOAL = "daily_protein_goal"
        private const val KEY_CARBS_GOAL = "daily_carbs_goal"
        private const val KEY_FAT_GOAL = "daily_fat_goal"
        private const val KEY_WATER_GOAL = "daily_water_goal"
        private const val KEY_BMR = "bmr"
        private const val KEY_TDEE = "tdee"
        
        // Setup Status
        private const val KEY_PROFILE_COMPLETE = "profile_complete"
    }
    
    // User ID (0 = guest, >0 = registered user)
    var userId: Long
        get() = prefs.getLong(KEY_USER_ID, 0L)
        set(value) = prefs.edit().putLong(KEY_USER_ID, value).apply()
    
    // Nickname
    var nickname: String
        get() = prefs.getString(KEY_NICKNAME, "User") ?: "User"
        set(value) = prefs.edit().putString(KEY_NICKNAME, value).apply()
    
    // Weight (kg)
    var weight: Float
        get() = prefs.getFloat(KEY_WEIGHT, 0f)
        set(value) = prefs.edit().putFloat(KEY_WEIGHT, value).apply()
    
    // Height (cm)
    var height: Float
        get() = prefs.getFloat(KEY_HEIGHT, 0f)
        set(value) = prefs.edit().putFloat(KEY_HEIGHT, value).apply()
    
    // Age
    var age: Int
        get() = prefs.getInt(KEY_AGE, 0)
        set(value) = prefs.edit().putInt(KEY_AGE, value).apply()
    
    // Gender
    var gender: String
        get() = prefs.getString(KEY_GENDER, "Other") ?: "Other"
        set(value) = prefs.edit().putString(KEY_GENDER, value).apply()
    
    // Activity Level
    var activityLevel: String
        get() = prefs.getString(KEY_ACTIVITY_LEVEL, "Moderate") ?: "Moderate"
        set(value) = prefs.edit().putString(KEY_ACTIVITY_LEVEL, value).apply()
    
    // Fitness Goal
    var fitnessGoal: String
        get() = prefs.getString(KEY_FITNESS_GOAL, "maintain") ?: "maintain"
        set(value) = prefs.edit().putString(KEY_FITNESS_GOAL, value).apply()
    
    // Diet Type
    var dietType: String
        get() = prefs.getString(KEY_DIET_TYPE, "none") ?: "none"
        set(value) = prefs.edit().putString(KEY_DIET_TYPE, value).apply()
    
    // Allergies (comma-separated)
    var allergies: String
        get() = prefs.getString(KEY_ALLERGIES, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ALLERGIES, value).apply()
    
    // Food Preferences (comma-separated)
    var foodPreferences: String
        get() = prefs.getString(KEY_FOOD_PREFERENCES, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FOOD_PREFERENCES, value).apply()
    
    // Profile Complete
    var isProfileComplete: Boolean
        get() = prefs.getBoolean(KEY_PROFILE_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_PROFILE_COMPLETE, value).apply()
    
    // BMI (calculated)
    var bmi: Float
        get() = prefs.getFloat(KEY_BMI, 0f)
        private set(value) = prefs.edit().putFloat(KEY_BMI, value).apply()
    
    // Daily Goals
    var calorieGoal: Int
        get() = prefs.getInt(KEY_CALORIE_GOAL, 2000)
        private set(value) = prefs.edit().putInt(KEY_CALORIE_GOAL, value).apply()
    
    var proteinGoal: Int
        get() = prefs.getInt(KEY_PROTEIN_GOAL, 75)
        private set(value) = prefs.edit().putInt(KEY_PROTEIN_GOAL, value).apply()
    
    var carbsGoal: Int
        get() = prefs.getInt(KEY_CARBS_GOAL, 250)
        private set(value) = prefs.edit().putInt(KEY_CARBS_GOAL, value).apply()
    
    var fatGoal: Int
        get() = prefs.getInt(KEY_FAT_GOAL, 65)
        private set(value) = prefs.edit().putInt(KEY_FAT_GOAL, value).apply()
    
    var waterGoal: Int
        get() = prefs.getInt(KEY_WATER_GOAL, 2000)
        private set(value) = prefs.edit().putInt(KEY_WATER_GOAL, value).apply()
    
    var bmr: Int
        get() = prefs.getInt(KEY_BMR, 1500)
        private set(value) = prefs.edit().putInt(KEY_BMR, value).apply()
    
    var tdee: Int
        get() = prefs.getInt(KEY_TDEE, 2000)
        private set(value) = prefs.edit().putInt(KEY_TDEE, value).apply()
    
    /**
     * Calculate and save all personalized goals
     */
    fun calculateAndSaveGoals() {
        if (weight <= 0 || height <= 0 || age <= 0) return
        
        // Calculate BMI
        val calculatedBMI = BMICalculator.calculateBMI(weight, height)
        bmi = calculatedBMI
        
        // Calculate nutrition goals
        val goals = NutritionCalculator.calculateAllNutritionGoals(
            weightKg = weight,
            heightCm = height,
            age = age,
            gender = gender,
            activityLevel = activityLevel,
            goal = fitnessGoal
        )
        
        // Save goals
        bmr = goals.bmr
        tdee = goals.tdee
        calorieGoal = goals.calories
        proteinGoal = goals.protein
        carbsGoal = goals.carbs
        fatGoal = goals.fat
        waterGoal = goals.water
    }
    
    /**
     * Get user profile data
     */
    fun getUserProfile(): UserProfile {
        return UserProfile(
            nickname = nickname,
            weight = weight,
            height = height,
            age = age,
            gender = gender,
            activityLevel = activityLevel,
            fitnessGoal = fitnessGoal,
            bmi = bmi
        )
    }
    
    /**
     * Get nutrition goals
     */
    fun getNutritionGoals(): NutritionGoals {
        return NutritionGoals(
            bmr = bmr,
            tdee = tdee,
            calories = calorieGoal,
            protein = proteinGoal,
            carbs = carbsGoal,
            fat = fatGoal,
            water = waterGoal
        )
    }
    
    /**
     * Clear all user data
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Clear guest user data (for temporary guest sessions)
     */
    fun clearGuestData() {
        if (userId == 0L) {
            prefs.edit().clear().apply()
        }
    }
    
    /**
     * Check if current user is a guest
     */
    fun isGuest(): Boolean {
        return userId == 0L
    }
}

data class UserProfile(
    val nickname: String,
    val weight: Float,
    val height: Float,
    val age: Int,
    val gender: String,
    val activityLevel: String,
    val fitnessGoal: String,
    val bmi: Float
)
