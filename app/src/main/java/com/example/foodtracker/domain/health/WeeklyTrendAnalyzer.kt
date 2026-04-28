package com.example.foodtracker.domain.health

import com.example.foodtracker.data.db.DailyNutritionLog

/**
 * Weekly Trend Analyzer - Detects nutrition patterns over time
 * Provides insights to feed into AI recommendations
 */
object WeeklyTrendAnalyzer {
    
    data class NutritionTrends(
        val averageCalories: Int,
        val averageProtein: Int,
        val averageCarbs: Int,
        val averageFat: Int,
        val averageWater: Int,
        val consistency: ConsistencyLevel,
        val patterns: List<String>,
        val improvements: List<String>
    )
    
    enum class ConsistencyLevel {
        EXCELLENT,      // Logs 6-7 days
        GOOD,           // Logs 4-5 days
        MODERATE,       // Logs 2-3 days
        INCONSISTENT    // Logs 0-1 days
    }
    
    /**
     * Analyze weekly nutrition trends
     */
    fun analyzeTrends(logs: List<DailyNutritionLog>): NutritionTrends {
        if (logs.isEmpty()) {
            return NutritionTrends(
                averageCalories = 0,
                averageProtein = 0,
                averageCarbs = 0,
                averageFat = 0,
                averageWater = 0,
                consistency = ConsistencyLevel.INCONSISTENT,
                patterns = listOf("No data yet. Start tracking to see trends!"),
                improvements = listOf("Log your meals consistently this week")
            )
        }
        
        val avgCal = logs.map { it.caloriesConsumed }.average().toInt()
        val avgProtein = logs.map { it.proteinConsumed }.average().toInt()
        val avgCarbs = logs.map { it.carbsConsumed }.average().toInt()
        val avgFat = logs.map { it.fatConsumed }.average().toInt()
        val avgWater = logs.map { it.waterConsumedMl }.average().toInt()
        
        val consistency = when (logs.size) {
            in 6..7 -> ConsistencyLevel.EXCELLENT
            in 4..5 -> ConsistencyLevel.GOOD
            in 2..3 -> ConsistencyLevel.MODERATE
            else -> ConsistencyLevel.INCONSISTENT
        }
        
        val patterns = detectPatterns(logs)
        val improvements = suggestImprovements(logs)
        
        return NutritionTrends(
            averageCalories = avgCal,
            averageProtein = avgProtein,
            averageCarbs = avgCarbs,
            averageFat = avgFat,
            averageWater = avgWater,
            consistency = consistency,
            patterns = patterns,
            improvements = improvements
        )
    }
    
    /**
     * Detect nutrition patterns
     */
    private fun detectPatterns(logs: List<DailyNutritionLog>): List<String> {
        val patterns = mutableListOf<String>()
        
        // Check calorie consistency
        val calGoals = logs.map { it.caloriesGoal }.distinct()
        val avgCalGoal = calGoals.average().toInt()
        val avgCalConsumed = logs.map { it.caloriesConsumed }.average().toInt()
        val calPercent = if (avgCalGoal > 0) (avgCalConsumed.toFloat() / avgCalGoal * 100).toInt() else 0
        
        when {
            calPercent in 90..110 -> patterns.add("✓ Consistent calorie tracking around ${avgCalGoal} cal/day")
            calPercent < 80 -> patterns.add("⚠ Consistently under calorie goal (${calPercent}% of target)")
            calPercent > 120 -> patterns.add("⚠ Consistently over calorie goal (${calPercent}% of target)")
        }
        
        // Check protein intake
        val avgProteinGoal = logs.map { it.proteinGoal }.average().toInt()
        val avgProteinConsumed = logs.map { it.proteinConsumed }.average().toInt()
        val proteinPercent = if (avgProteinGoal > 0) (avgProteinConsumed.toFloat() / avgProteinGoal * 100).toInt() else 0
        
        when {
            proteinPercent >= 90 -> patterns.add("✓ Meeting protein goals consistently (${avgProteinConsumed}g avg)")
            proteinPercent < 70 -> patterns.add("⚠ Low protein intake pattern (${proteinPercent}% of ${avgProteinGoal}g goal)")
            proteinPercent in 70..89 -> patterns.add("○ Moderate protein intake (${avgProteinConsumed}g avg, could be higher)")
        }
        
        // Check water intake
        val avgWaterGoal = logs.map { it.waterGoalMl }.average().toInt()
        val avgWaterConsumed = logs.map { it.waterConsumedMl }.average().toInt()
        val waterPercent = if (avgWaterGoal > 0) (avgWaterConsumed.toFloat() / avgWaterGoal * 100).toInt() else 0
        
        when {
            waterPercent >= 90 -> patterns.add("✓ Great hydration habits (${avgWaterConsumed}ml avg)")
            waterPercent < 60 -> patterns.add("⚠ Dehydration risk - averaging only ${waterPercent}% of water goal")
            else -> patterns.add("○ Hydration needs attention (${avgWaterConsumed}ml avg)")
        }
        
        // Check meal frequency
        val avgMeals = logs.map { it.mealCount }.average()
        when {
            avgMeals >= 3 -> patterns.add("✓ Regular meal pattern (${String.format("%.1f", avgMeals)} meals/day avg)")
            avgMeals < 2 -> patterns.add("⚠ Skipping meals - only ${String.format("%.1f", avgMeals)} meals/day avg")
        }
        
        // Check for weekend vs weekday patterns (if enough data)
        if (logs.size >= 5) {
            val variability = calculateCalorieVariability(logs)
            if (variability > 30) {
                patterns.add("○ High day-to-day calorie variability - consider more consistency")
            }
        }
        
        return patterns.ifEmpty { listOf("Building your nutrition profile...") }
    }
    
    /**
     * Suggest improvements based on trends
     */
    private fun suggestImprovements(logs: List<DailyNutritionLog>): List<String> {
        val improvements = mutableListOf<String>()
        
        val avgProteinGoal = logs.map { it.proteinGoal }.average().toInt()
        val avgProteinConsumed = logs.map { it.proteinConsumed }.average().toInt()
        val proteinPercent = if (avgProteinGoal > 0) (avgProteinConsumed.toFloat() / avgProteinGoal * 100).toInt() else 0
        
        if (proteinPercent < 80) {
            improvements.add("Add 20-30g more protein daily - try eggs at breakfast or Greek yogurt snacks")
        }
        
        val avgWaterGoal = logs.map { it.waterGoalMl }.average().toInt()
        val avgWaterConsumed = logs.map { it.waterConsumedMl }.average().toInt()
        val waterPercent = if (avgWaterGoal > 0) (avgWaterConsumed.toFloat() / avgWaterGoal * 100).toInt() else 0
        
        if (waterPercent < 70) {
            improvements.add("Increase water intake by ${(avgWaterGoal - avgWaterConsumed) / 250} more glasses per day")
        }
        
        val avgMeals = logs.map { it.mealCount }.average()
        if (avgMeals < 3) {
            improvements.add("Aim for 3 balanced meals per day for better energy and metabolism")
        }
        
        // Check consistency
        if (logs.size < 5) {
            improvements.add("Track at least 5 days this week for better insights and progress")
        }
        
        // Macro balance check
        val avgCalories = logs.map { it.caloriesConsumed }.average().toInt()
        val proteinCal = avgProteinConsumed * 4
        val carbsCal = logs.map { it.carbsConsumed }.average().toInt() * 4
        val fatCal = logs.map { it.fatConsumed }.average().toInt() * 9
        val totalMacroCal = proteinCal + carbsCal + fatCal
        
        if (totalMacroCal > 100 && avgCalories > 100) {
            val proteinPercOfCal = (proteinCal.toFloat() / totalMacroCal * 100).toInt()
            val fatPercOfCal = (fatCal.toFloat() / totalMacroCal * 100).toInt()
            
            if (proteinPercOfCal < 15) {
                improvements.add("Increase protein to at least 20% of calories for better satiety")
            }
            
            if (fatPercOfCal < 20) {
                improvements.add("Include more healthy fats (nuts, avocado, olive oil) for hormonal health")
            }
        }
        
        return improvements.ifEmpty { 
            listOf("You're doing great! Keep tracking consistently for personalized insights")
        }
    }
    
    /**
     * Calculate calorie variability (standard deviation as % of mean)
     */
    private fun calculateCalorieVariability(logs: List<DailyNutritionLog>): Int {
        if (logs.size < 2) return 0
        
        val calories = logs.map { it.caloriesConsumed.toDouble() }
        val mean = calories.average()
        val variance = calories.map { (it - mean) * (it - mean) }.average()
        val stdDev = kotlin.math.sqrt(variance)
        
        return if (mean > 0) ((stdDev / mean) * 100).toInt() else 0
    }
    
    /**
     * Get weekly summary text
     */
    fun getWeeklySummaryText(trends: NutritionTrends): String {
        val consistency = when (trends.consistency) {
            ConsistencyLevel.EXCELLENT -> "excellent"
            ConsistencyLevel.GOOD -> "good"
            ConsistencyLevel.MODERATE -> "moderate"
            ConsistencyLevel.INCONSISTENT -> "inconsistent"
        }
        
        return buildString {
            append("Weekly Averages: ${trends.averageCalories} cal, ${trends.averageProtein}g protein, ${trends.averageWater}ml water. ")
            append("Tracking consistency: $consistency. ")
            
            if (trends.patterns.isNotEmpty()) {
                append("Key insights: ${trends.patterns.joinToString("; ")}. ")
            }
        }
    }
}
