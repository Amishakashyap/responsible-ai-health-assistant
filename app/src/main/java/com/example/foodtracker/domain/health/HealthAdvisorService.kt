package com.example.foodtracker.domain.health

import android.content.Context
import android.util.Log
import com.example.foodtracker.data.db.AppDatabase
import com.example.foodtracker.data.db.DailyHealthSummary
import com.example.foodtracker.data.db.DailyNutritionLog
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.domain.ai.AIConfig
import com.example.foodtracker.domain.ai.AIHealthAdvisor
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Health Advisor Service - Orchestrates the AI health assistant flow
 * 
 * Flow:
 * 1. Fetch user profile + daily logs
 * 2. Apply health rules engine
 * 3. Calculate nutrition targets
 * 4. Analyze weekly trends
 * 5. Get suitable recipes
 * 6. Send to AI with structured context
 * 7. Store response in database
 * 8. Return summary for display
 */
class HealthAdvisorService(private val context: Context) {
    
    private val db = AppDatabase.get(context)
    private val userPrefs = UserPreferences(context)
    private val aiConfig = AIConfig(context)
    private val aiAdvisor = AIHealthAdvisor(context)
    private val gson = Gson()
    
    data class DailySummaryResult(
        val success: Boolean,
        val summary: DailyHealthSummary?,
        val error: String? = null
    )
    
    /**
     * Generate daily health summary and recommendations
     */
    suspend fun generateDailySummary(
        userId: Long = userPrefs.userId,
        date: String = getCurrentDate()
    ): DailySummaryResult = withContext(Dispatchers.IO) {
        try {
            Log.d("HealthAdvisorService", "Generating summary for user $userId on $date")
            
            // Step 1: Fetch user profile + daily log
            val dailyLog = db.dailyNutritionLogDao().getByUserAndDate(userId, date)
            
            // Create log if doesn't exist
            val currentLog = dailyLog ?: createInitialDailyLog(userId, date)
            
            // Step 2: Apply health rules
            val healthCheck = HealthRulesEngine.runHealthChecks(userPrefs, currentLog)
            
            // Step 3: Get recent logs for trend analysis
            val recentLogs = db.dailyNutritionLogDao().getRecentLogs(userId, 7)
            val trends = WeeklyTrendAnalyzer.analyzeTrends(recentLogs)
            
            // Step 4: Get suitable recipes
            val recipes = findSuitableRecipes(currentLog)
            
            // Step 5: Generate AI recommendation (or fallback to local)
            val recommendation = if (aiConfig.isEnabled && aiConfig.isConfigured()) {
                aiAdvisor.generateDailySummary(
                    userPrefs = userPrefs,
                    dailyLog = currentLog,
                    recentLogs = recentLogs,
                    healthCheck = healthCheck,
                    availableRecipes = recipes
                )
            } else {
                // Use local rule-based recommendation
                generateLocalSummary(userPrefs, currentLog, healthCheck, trends)
            }
            
            if (recommendation == null) {
                return@withContext DailySummaryResult(
                    success = false,
                    summary = null,
                    error = "Failed to generate recommendation"
                )
            }
            
            // Step 6: Find recommended recipe ID
            val recipeId = findRecipeIdByName(recipes, recommendation.recipeRecommendation)
            
            // Step 7: Create and save summary
            val summary = DailyHealthSummary(
                userId = userId,
                date = date,
                summaryText = recommendation.summary,
                mealSuggestion = recommendation.mealSuggestion,
                recipeId = recipeId,
                habitTip = recommendation.habitTip,
                warnings = gson.toJson(healthCheck.warnings),
                motivationalMessage = recommendation.motivationalMessage,
                generatedAt = System.currentTimeMillis()
            )
            
            val summaryId = db.dailyHealthSummaryDao().insert(summary)
            
            Log.d("HealthAdvisorService", "Summary generated successfully with ID $summaryId")
            
            DailySummaryResult(
                success = true,
                summary = summary.copy(id = summaryId)
            )
            
        } catch (e: Exception) {
            Log.e("HealthAdvisorService", "Error generating daily summary", e)
            DailySummaryResult(
                success = false,
                summary = null,
                error = e.message
            )
        }
    }
    
    /**
     * Update daily nutrition log
     */
    suspend fun updateDailyNutritionLog(
        userId: Long = userPrefs.userId,
        date: String = getCurrentDate(),
        caloriesConsumed: Int,
        proteinConsumed: Int,
        carbsConsumed: Int,
        fatConsumed: Int,
        waterConsumedMl: Int,
        mealCount: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val log = DailyNutritionLog(
                userId = userId,
                date = date,
                caloriesConsumed = caloriesConsumed,
                proteinConsumed = proteinConsumed,
                carbsConsumed = carbsConsumed,
                fatConsumed = fatConsumed,
                waterConsumedMl = waterConsumedMl,
                caloriesGoal = userPrefs.calorieGoal,
                proteinGoal = userPrefs.proteinGoal,
                carbsGoal = userPrefs.carbsGoal,
                fatGoal = userPrefs.fatGoal,
                waterGoalMl = userPrefs.waterGoal,
                mealCount = mealCount,
                updatedAt = System.currentTimeMillis()
            )
            
            db.dailyNutritionLogDao().upsert(log)
            true
        } catch (e: Exception) {
            Log.e("HealthAdvisorService", "Error updating nutrition log", e)
            false
        }
    }
    
    /**
     * Get today's summary (from database)
     */
    suspend fun getTodaysSummary(
        userId: Long = userPrefs.userId,
        date: String = getCurrentDate()
    ): DailyHealthSummary? = withContext(Dispatchers.IO) {
        try {
            db.dailyHealthSummaryDao().getByUserAndDate(userId, date)
        } catch (e: Exception) {
            Log.e("HealthAdvisorService", "Error fetching today's summary", e)
            null
        }
    }
    
    /**
     * Get today's nutrition log
     */
    suspend fun getTodaysLog(
        userId: Long = userPrefs.userId,
        date: String = getCurrentDate()
    ): DailyNutritionLog? = withContext(Dispatchers.IO) {
        try {
            db.dailyNutritionLogDao().getByUserAndDate(userId, date)
        } catch (e: Exception) {
            Log.e("HealthAdvisorService", "Error fetching today's log", e)
            null
        }
    }
    
    /**
     * Create initial daily log with zero values
     */
    private suspend fun createInitialDailyLog(userId: Long, date: String): DailyNutritionLog {
        val log = DailyNutritionLog(
            userId = userId,
            date = date,
            caloriesGoal = userPrefs.calorieGoal,
            proteinGoal = userPrefs.proteinGoal,
            carbsGoal = userPrefs.carbsGoal,
            fatGoal = userPrefs.fatGoal,
            waterGoalMl = userPrefs.waterGoal
        )
        db.dailyNutritionLogDao().upsert(log)
        return log
    }
    
    /**
     * Find suitable recipes based on current needs
     */
    private suspend fun findSuitableRecipes(dailyLog: DailyNutritionLog): List<com.example.foodtracker.data.db.Recipe> {
        try {
            val remainingCalories = dailyLog.caloriesGoal - dailyLog.caloriesConsumed
            val needsProtein = dailyLog.proteinConsumed < dailyLog.proteinGoal * 0.7
            
            // Determine next meal type based on meal count
            val mealType = when (dailyLog.mealCount) {
                0 -> "breakfast"
                1 -> "lunch"
                2 -> "dinner"
                else -> "snack"
            }
            
            val dietType = userPrefs.dietType.ifEmpty { "omnivore" }
            
            // Find recipes that fit
            val minCal = maxOf(100, remainingCalories / 3)
            val maxCal = minOf(800, remainingCalories + 200)
            
            return db.recipeDao().findSuitableRecipes(
                mealType = mealType,
                dietType = dietType,
                minCalories = minCal,
                maxCalories = maxCal,
                limit = 5
            )
        } catch (e: Exception) {
            Log.e("HealthAdvisorService", "Error finding recipes", e)
            return emptyList()
        }
    }
    
    /**
     * Find recipe ID by name from list
     */
    private fun findRecipeIdByName(
        recipes: List<com.example.foodtracker.data.db.Recipe>,
        recipeName: String
    ): Long? {
        if (recipeName.isEmpty()) return null
        
        return recipes.find { 
            recipeName.contains(it.name, ignoreCase = true) || 
            it.name.contains(recipeName, ignoreCase = true)
        }?.id
    }
    
    /**
     * Generate local summary when AI is not available
     */
    private fun generateLocalSummary(
        userPrefs: UserPreferences,
        dailyLog: DailyNutritionLog,
        healthCheck: HealthRulesEngine.HealthCheck,
        trends: WeeklyTrendAnalyzer.NutritionTrends
    ): AIHealthAdvisor.DailyRecommendation {
        val name = userPrefs.nickname
        
        // Build summary from trends and current day
        val summary = buildString {
            append("Hello $name! ")
            
            if (dailyLog.caloriesConsumed > 0) {
                val calPercent = (dailyLog.caloriesConsumed.toFloat() / dailyLog.caloriesGoal * 100).toInt()
                append("You've consumed ${dailyLog.caloriesConsumed} of your ${dailyLog.caloriesGoal} calorie goal ($calPercent%). ")
                
                if (calPercent >= 90) {
                    append("Great work staying on track! ")
                }
            } else {
                append("Let's start your day strong with good nutrition. ")
            }
            
            // Add trend insight
            if (trends.patterns.isNotEmpty()) {
                append(trends.patterns.first())
            }
        }
        
        val mealSuggestion = buildString {
            val proteinNeeded = dailyLog.proteinGoal - dailyLog.proteinConsumed
            val calNeeded = dailyLog.caloriesGoal - dailyLog.caloriesConsumed
            
            if (proteinNeeded > 30) {
                append("Focus on protein: aim for ${proteinNeeded}g more today. Try chicken, fish, eggs, or plant-based proteins. ")
            }
            
            if (dailyLog.waterConsumedMl < dailyLog.waterGoalMl / 2) {
                append("Hydrate! You need ${(dailyLog.waterGoalMl - dailyLog.waterConsumedMl) / 250} more glasses of water. ")
            }
            
            if (calNeeded > 500) {
                append("You have $calNeeded calories remaining for balanced meals.")
            } else if (calNeeded < 100) {
                append("You're near your calorie goal. Choose light, nutrient-dense options if hungry.")
            }
        }
        
        val habitTip = if (trends.improvements.isNotEmpty()) {
            trends.improvements.first()
        } else {
            "Track your meals consistently for better insights and progress."
        }
        
        val motivation = when (healthCheck.riskLevel) {
            HealthRulesEngine.RiskLevel.SAFE -> "You're doing amazing, $name! Keep it up! 🌟"
            HealthRulesEngine.RiskLevel.CAUTION -> "Small consistent steps lead to big results, $name! 💪"
            HealthRulesEngine.RiskLevel.WARNING -> "Every healthy choice matters, $name. You've got this! 🎯"
            HealthRulesEngine.RiskLevel.CRITICAL -> "Your health is important, $name. Please consult a healthcare provider. 🏥"
        }
        
        return AIHealthAdvisor.DailyRecommendation(
            summary = summary,
            mealSuggestion = mealSuggestion,
            habitTip = habitTip,
            motivationalMessage = motivation
        )
    }
    
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
}
