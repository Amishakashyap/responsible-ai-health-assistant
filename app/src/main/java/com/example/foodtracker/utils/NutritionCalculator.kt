package com.example.foodtracker.utils

import kotlin.math.roundToInt

/**
 * Nutrition Calculator - Calculate daily calorie and macronutrient needs
 * Uses Mifflin-St Jeor Equation for BMR and Harris-Benedict for TDEE
 */
object NutritionCalculator {
    
    /**
     * Calculate Basal Metabolic Rate (BMR) using Mifflin-St Jeor Equation
     * @param weightKg Weight in kilograms
     * @param heightCm Height in centimeters
     * @param age Age in years
     * @param gender "Male" or "Female"
     */
    fun calculateBMR(weightKg: Float, heightCm: Float, age: Int, gender: String): Int {
        val bmr = when (gender.lowercase()) {
            "male" -> (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5
            "female" -> (10 * weightKg) + (6.25 * heightCm) - (5 * age) - 161
            else -> (10 * weightKg) + (6.25 * heightCm) - (5 * age) - 78 // Average
        }
        return bmr.roundToInt()
    }
    
    /**
     * Calculate Total Daily Energy Expenditure (TDEE)
     * @param bmr Basal Metabolic Rate
     * @param activityLevel Activity level string
     */
    fun calculateTDEE(bmr: Int, activityLevel: String): Int {
        val activityMultiplier = when (activityLevel.lowercase()) {
            "sedentary" -> 1.2f
            "light" -> 1.375f
            "moderate" -> 1.55f
            "active" -> 1.725f
            "very active" -> 1.9f
            else -> 1.55f // Default to moderate
        }
        return (bmr * activityMultiplier).roundToInt()
    }
    
    /**
     * Calculate daily calorie goal based on fitness goal
     * @param tdee Total Daily Energy Expenditure
     * @param goal "lose weight", "maintain", "gain muscle"
     */
    fun calculateCalorieGoal(tdee: Int, goal: String = "maintain"): Int {
        return when (goal.lowercase()) {
            "lose weight" -> tdee - 500 // 500 cal deficit for ~0.5kg/week loss
            "gain muscle" -> tdee + 300 // 300 cal surplus for muscle gain
            else -> tdee // Maintenance
        }
    }
    
    /**
     * Calculate daily protein requirement (grams)
     * @param weightKg Weight in kilograms
     * @param activityLevel Activity level
     * @param goal Fitness goal
     */
    fun calculateProteinGoal(weightKg: Float, activityLevel: String, goal: String = "maintain"): Int {
        val proteinPerKg = when {
            goal.lowercase() == "gain muscle" -> 2.0f // 2g per kg for muscle gain
            activityLevel.lowercase() in listOf("active", "very active") -> 1.6f
            else -> 1.2f // Standard recommendation
        }
        return (weightKg * proteinPerKg).roundToInt()
    }
    
    /**
     * Calculate daily carbohydrate requirement (grams)
     * @param calorieGoal Total daily calories
     * @param proteinGrams Protein in grams
     * @param carbPercentage Percentage of calories from carbs (default 40%)
     */
    fun calculateCarbsGoal(calorieGoal: Int, proteinGrams: Int, carbPercentage: Float = 0.4f): Int {
        val carbCalories = calorieGoal * carbPercentage
        return (carbCalories / 4).roundToInt() // 4 calories per gram of carbs
    }
    
    /**
     * Calculate daily fat requirement (grams)
     * @param calorieGoal Total daily calories
     * @param proteinGrams Protein in grams
     * @param carbsGrams Carbs in grams
     */
    fun calculateFatGoal(calorieGoal: Int, proteinGrams: Int, carbsGrams: Int): Int {
        val proteinCalories = proteinGrams * 4
        val carbCalories = carbsGrams * 4
        val fatCalories = calorieGoal - proteinCalories - carbCalories
        return (fatCalories / 9.0).roundToInt() // 9 calories per gram of fat
    }
    
    /**
     * Calculate daily water intake recommendation (ml)
     * @param weightKg Weight in kilograms
     */
    fun calculateWaterGoal(weightKg: Float): Int {
        return (weightKg * 35).roundToInt() // 35ml per kg body weight
    }
    
    /**
     * Get all nutrition goals as a data class
     */
    fun calculateAllNutritionGoals(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        gender: String,
        activityLevel: String,
        goal: String = "maintain"
    ): NutritionGoals {
        val bmr = calculateBMR(weightKg, heightCm, age, gender)
        val tdee = calculateTDEE(bmr, activityLevel)
        val calorieGoal = calculateCalorieGoal(tdee, goal)
        val proteinGoal = calculateProteinGoal(weightKg, activityLevel, goal)
        val carbsGoal = calculateCarbsGoal(calorieGoal, proteinGoal)
        val fatGoal = calculateFatGoal(calorieGoal, proteinGoal, carbsGoal)
        val waterGoal = calculateWaterGoal(weightKg)
        
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
}

data class NutritionGoals(
    val bmr: Int,
    val tdee: Int,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val water: Int
)
