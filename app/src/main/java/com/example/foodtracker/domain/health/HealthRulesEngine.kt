package com.example.foodtracker.domain.health

import com.example.foodtracker.data.db.DailyNutritionLog
import com.example.foodtracker.data.user.UserPreferences

/**
 * Health Rules Engine - Safety rails for health advice
 * Applies rule-based checks before AI reasoning
 */
object HealthRulesEngine {
    
    data class HealthCheck(
        val warnings: List<String> = emptyList(),
        val suggestions: List<String> = emptyList(),
        val riskLevel: RiskLevel = RiskLevel.SAFE
    )
    
    enum class RiskLevel {
        SAFE,           // All good
        CAUTION,        // Minor concerns
        WARNING,        // Moderate concerns  
        CRITICAL        // Serious health risk - medical advice needed
    }
    
    /**
     * Run all health checks on user profile and daily log
     */
    fun runHealthChecks(
        userPrefs: UserPreferences,
        dailyLog: DailyNutritionLog?
    ): HealthCheck {
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var maxRisk = RiskLevel.SAFE
        
        // Check BMI
        val bmiCheck = checkBMI(userPrefs.bmi)
        warnings.addAll(bmiCheck.warnings)
        suggestions.addAll(bmiCheck.suggestions)
        maxRisk = maxOf(maxRisk, bmiCheck.riskLevel)
        
        // Check daily calories if log exists
        dailyLog?.let { log ->
            val calorieCheck = checkCalorieIntake(log.caloriesConsumed, log.caloriesGoal, userPrefs.bmi)
            warnings.addAll(calorieCheck.warnings)
            suggestions.addAll(calorieCheck.suggestions)
            maxRisk = maxOf(maxRisk, calorieCheck.riskLevel)
            
            val proteinCheck = checkProteinIntake(log.proteinConsumed, log.proteinGoal, userPrefs.weight)
            warnings.addAll(proteinCheck.warnings)
            suggestions.addAll(proteinCheck.suggestions)
            maxRisk = maxOf(maxRisk, proteinCheck.riskLevel)
            
            val waterCheck = checkWaterIntake(log.waterConsumedMl, log.waterGoalMl)
            warnings.addAll(waterCheck.warnings)
            suggestions.addAll(waterCheck.suggestions)
            maxRisk = maxOf(maxRisk, waterCheck.riskLevel)
            
            val macroCheck = checkMacroBalance(
                log.proteinConsumed,
                log.carbsConsumed,
                log.fatConsumed,
                log.caloriesGoal
            )
            warnings.addAll(macroCheck.warnings)
            suggestions.addAll(macroCheck.suggestions)
            maxRisk = maxOf(maxRisk, macroCheck.riskLevel)
            
            val mealCheck = checkMealFrequency(log.mealCount)
            warnings.addAll(mealCheck.warnings)
            suggestions.addAll(mealCheck.suggestions)
            maxRisk = maxOf(maxRisk, mealCheck.riskLevel)
        }
        
        return HealthCheck(warnings, suggestions, maxRisk)
    }
    
    /**
     * Check BMI for health concerns
     */
    private fun checkBMI(bmi: Float): HealthCheck {
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var risk = RiskLevel.SAFE
        
        when {
            bmi < 16.0f -> {
                warnings.add("⚠️ Your BMI (${String.format("%.1f", bmi)}) indicates severe underweight. Please consult a healthcare provider.")
                suggestions.add("Consider eating calorie-dense, nutritious foods and consulting a nutritionist")
                risk = RiskLevel.CRITICAL
            }
            bmi < 18.5f -> {
                warnings.add("Your BMI (${String.format("%.1f", bmi)}) indicates underweight. Monitor your nutrition carefully.")
                suggestions.add("Focus on nutrient-dense foods with adequate protein and healthy fats")
                risk = RiskLevel.WARNING
            }
            bmi >= 30.0f -> {
                warnings.add("⚠️ Your BMI (${String.format("%.1f", bmi)}) indicates obesity. Consider consulting a healthcare provider for a personalized plan.")
                suggestions.add("Focus on sustainable calorie deficit with regular exercise")
                risk = RiskLevel.CRITICAL
            }
            bmi >= 25.0f -> {
                warnings.add("Your BMI (${String.format("%.1f", bmi)}) indicates overweight. Small dietary changes can help.")
                suggestions.add("Consider moderate calorie reduction and increased physical activity")
                risk = RiskLevel.CAUTION
            }
            else -> {
                suggestions.add("Your BMI (${String.format("%.1f", bmi)}) is in the healthy range. Keep up the good work!")
            }
        }
        
        return HealthCheck(warnings, suggestions, risk)
    }
    
    /**
     * Check daily calorie intake
     */
    private fun checkCalorieIntake(consumed: Int, goal: Int, bmi: Float): HealthCheck {
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var risk = RiskLevel.SAFE
        
        val percentOfGoal = if (goal > 0) (consumed.toFloat() / goal * 100).toInt() else 0
        
        when {
            consumed < 1000 -> {
                warnings.add("⚠️ CRITICAL: Calorie intake (${consumed} cal) is dangerously low. Minimum 1200 calories recommended for health.")
                suggestions.add("Eat at least 3 balanced meals today. Very low calorie intake can harm metabolism and health.")
                risk = RiskLevel.CRITICAL
            }
            consumed < 1200 -> {
                warnings.add("⚠️ Calorie intake (${consumed} cal) is very low. This may not be sustainable or healthy.")
                suggestions.add("Add a nutritious snack or larger portions to reach at least 1200 calories")
                risk = RiskLevel.WARNING
            }
            percentOfGoal < 70 -> {
                warnings.add("You're consuming only ${percentOfGoal}% of your calorie goal (${consumed}/${goal} cal)")
                suggestions.add("Consider adding a balanced meal or snack to meet your energy needs")
                risk = RiskLevel.CAUTION
            }
            percentOfGoal > 130 && bmi >= 25.0f -> {
                warnings.add("Calorie intake (${consumed} cal) is ${percentOfGoal}% of goal. This may slow weight management progress.")
                suggestions.add("Review portion sizes and choose lower-calorie dense foods")
                risk = RiskLevel.CAUTION
            }
            percentOfGoal in 90..110 -> {
                suggestions.add("Excellent! You're at ${percentOfGoal}% of your calorie goal (${consumed}/${goal} cal)")
            }
        }
        
        return HealthCheck(warnings, suggestions, risk)
    }
    
    /**
     * Check protein intake
     */
    private fun checkProteinIntake(consumed: Int, goal: Int, weightKg: Float): HealthCheck {
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var risk = RiskLevel.SAFE
        
        val percentOfGoal = if (goal > 0) (consumed.toFloat() / goal * 100).toInt() else 0
        val proteinPerKg = if (weightKg > 0) consumed / weightKg else 0f
        
        when {
            proteinPerKg < 0.5f -> {
                warnings.add("⚠️ Protein intake (${consumed}g, ${String.format("%.1f", proteinPerKg)}g/kg) is critically low.")
                suggestions.add("Add protein-rich foods: eggs, chicken, fish, legumes, dairy, or protein powder")
                risk = RiskLevel.WARNING
            }
            percentOfGoal < 70 -> {
                warnings.add("Protein intake is low: ${consumed}g (${percentOfGoal}% of ${goal}g goal)")
                suggestions.add("Include protein in each meal: add eggs to breakfast, chicken to lunch, or legumes to dinner")
                risk = RiskLevel.CAUTION
            }
            percentOfGoal > 200 && proteinPerKg > 2.5f -> {
                warnings.add("Protein intake is very high (${consumed}g). Ensure adequate hydration.")
                suggestions.add("High protein is generally safe, but drink plenty of water and balance with other nutrients")
                risk = RiskLevel.CAUTION
            }
            percentOfGoal in 90..110 -> {
                suggestions.add("Great protein intake! ${consumed}g (${percentOfGoal}% of goal)")
            }
        }
        
        return HealthCheck(warnings, suggestions, risk)
    }
    
    /**
     * Check water intake
     */
    private fun checkWaterIntake(consumed: Int, goal: Int): HealthCheck {
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var risk = RiskLevel.SAFE
        
        val percentOfGoal = if (goal > 0) (consumed.toFloat() / goal * 100).toInt() else 0
        
        when {
            consumed < 1000 -> {
                warnings.add("⚠️ Water intake (${consumed}ml) is very low. Dehydration risk!")
                suggestions.add("Drink at least 1-2 glasses of water right now. Set hourly reminders.")
                risk = RiskLevel.WARNING
            }
            percentOfGoal < 50 -> {
                warnings.add("Water intake is low: ${consumed}ml (${percentOfGoal}% of ${goal}ml goal)")
                suggestions.add("Carry a water bottle and sip throughout the day. Aim for 8 glasses.")
                risk = RiskLevel.CAUTION
            }
            percentOfGoal in 90..110 -> {
                suggestions.add("Perfect hydration! ${consumed}ml (${percentOfGoal}% of goal)")
            }
        }
        
        return HealthCheck(warnings, suggestions, risk)
    }
    
    /**
     * Check macronutrient balance
     */
    private fun checkMacroBalance(
        protein: Int,
        carbs: Int,
        fat: Int,
        calorieGoal: Int
    ): HealthCheck {
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var risk = RiskLevel.SAFE
        
        val totalCalFromMacros = (protein * 4) + (carbs * 4) + (fat * 9)
        
        if (totalCalFromMacros < 100) {
            // Not enough data yet
            return HealthCheck(warnings, suggestions, risk)
        }
        
        val proteinPercent = (protein * 4).toFloat() / totalCalFromMacros * 100
        val carbsPercent = (carbs * 4).toFloat() / totalCalFromMacros * 100
        val fatPercent = (fat * 9).toFloat() / totalCalFromMacros * 100
        
        when {
            fatPercent < 15 -> {
                warnings.add("Fat intake is very low (${fatPercent.toInt()}% of calories). Fats are essential for health.")
                suggestions.add("Include healthy fats: nuts, avocado, olive oil, fatty fish")
                risk = RiskLevel.CAUTION
            }
            fatPercent > 40 -> {
                suggestions.add("Fat intake is high (${fatPercent.toInt()}% of calories). Consider balance with other macros.")
            }
            carbsPercent < 20 && calorieGoal > 1500 -> {
                suggestions.add("Low carb diet (${carbsPercent.toInt()}%). Ensure adequate fiber and energy.")
            }
        }
        
        return HealthCheck(warnings, suggestions, risk)
    }
    
    /**
     * Check meal frequency
     */
    private fun checkMealFrequency(mealCount: Int): HealthCheck {
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var risk = RiskLevel.SAFE
        
        when {
            mealCount < 2 -> {
                warnings.add("Only ${mealCount} meal logged today. Regular meals help maintain energy and metabolism.")
                suggestions.add("Aim for at least 3 meals per day for consistent nutrition")
                risk = RiskLevel.CAUTION
            }
            mealCount >= 3 -> {
                suggestions.add("Good meal frequency! ${mealCount} meals logged today")
            }
        }
        
        return HealthCheck(warnings, suggestions, risk)
    }
    
    /**
     * Get personalized macro recommendations based on goal
     */
    fun getMacroRecommendations(goal: String): String {
        return when (goal.lowercase()) {
            "lose weight", "weight loss", "fat loss" -> 
                "Focus: High protein (30-35%), Moderate carbs (30-40%), Moderate fat (25-30%). Protein preserves muscle during deficit."
            "gain muscle", "muscle gain", "bulk" -> 
                "Focus: High protein (25-30%), High carbs (40-50%), Moderate fat (20-25%). Carbs fuel workouts and recovery."
            "maintain", "maintenance" -> 
                "Balanced: Protein (20-30%), Carbs (40-50%), Fat (25-30%). Maintain current habits with variety."
            else -> 
                "Balanced macros: Protein 20-30%, Carbs 40-50%, Fat 25-30% of daily calories"
        }
    }
}
