package com.example.foodtracker.domain.ai

import android.content.Context
import android.util.Log
import com.example.foodtracker.data.db.DailyNutritionLog
import com.example.foodtracker.data.db.Recipe
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.domain.health.HealthRulesEngine
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * AI Health Advisor - Integrates with LLM for personalized health recommendations
 * This layer formats structured data, sends to AI, and parses responses
 */
class AIHealthAdvisor(private val context: Context) {
    
    private val config = AIConfig(context)
    private val client = OkHttpClient()
    private val ollamaClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private var onDeviceEngine: OnDeviceLlmEngine? = null
    
    companion object {
        private const val TAG = "AIHealthAdvisor"
    }
    
    data class DailyRecommendation(
        val summary: String,
        val mealSuggestion: String,
        val habitTip: String,
        val motivationalMessage: String,
        val recipeRecommendation: String = ""
    )
    
    /**
     * Generate daily health summary and recommendations
     */
    suspend fun generateDailySummary(
        userPrefs: UserPreferences,
        dailyLog: DailyNutritionLog?,
        recentLogs: List<DailyNutritionLog>,
        healthCheck: HealthRulesEngine.HealthCheck,
        availableRecipes: List<Recipe>
    ): DailyRecommendation? = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(userPrefs, dailyLog, recentLogs, healthCheck, availableRecipes)
            
            when (config.aiProvider) {
                AIProvider.ON_DEVICE -> callOnDeviceSummary(prompt, userPrefs, dailyLog, healthCheck, availableRecipes)
                AIProvider.OPENAI -> callOpenAI(prompt)
                AIProvider.GEMINI -> callGemini(prompt)
                AIProvider.OLLAMA -> callOllama(prompt)
                AIProvider.LOCAL -> generateLocalRecommendation(userPrefs, dailyLog, healthCheck, availableRecipes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "AI summary generation failed", e)
            generateLocalRecommendation(userPrefs, dailyLog, healthCheck, availableRecipes)
        }
    }
    
    /**
     * Ask AI a specific question (for chat interface).
     * Throws [IllegalStateException] when no API key is configured so the caller
     * can fall back to its own local response generator.
     */
    suspend fun askQuestion(prompt: String): String = withContext(Dispatchers.IO) {
        when (config.aiProvider) {
            AIProvider.ON_DEVICE -> callOnDeviceChat(prompt)
            AIProvider.OPENAI -> callOpenAIChat(prompt)
            AIProvider.GEMINI -> callGeminiChat(prompt)
            AIProvider.OLLAMA -> callOllamaChat(prompt)
            AIProvider.LOCAL  -> throw IllegalStateException("no_api_key")
        }
    }

    /**
     * Ask a question using an explicitly chosen provider.
     * This avoids race conditions where UI provider toggles change mid-request.
     */
    suspend fun askQuestion(prompt: String, provider: AIProvider): String = withContext(Dispatchers.IO) {
        when (provider) {
            AIProvider.ON_DEVICE -> callOnDeviceChat(prompt)
            AIProvider.OPENAI -> callOpenAIChat(prompt)
            AIProvider.GEMINI -> callGeminiChat(prompt)
            AIProvider.OLLAMA -> callOllamaChat(prompt)
            AIProvider.LOCAL  -> throw IllegalStateException("no_api_key")
        }
    }

    /** Initialize the on-device LLM engine (call once from chat screen). */
    fun initOnDevice(): Boolean {
        if (onDeviceEngine != null) return true
        val path = config.onDeviceModelPath
        if (path.isEmpty()) return false
        onDeviceEngine = OnDeviceLlmEngine.create(context, path)
        return onDeviceEngine != null
    }

    /** Release on-device LLM resources. */
    fun closeOnDevice() {
        onDeviceEngine?.close()
        onDeviceEngine = null
    }

    private suspend fun callOnDeviceChat(prompt: String): String {
        if (onDeviceEngine == null) initOnDevice()
        val engine = onDeviceEngine ?: throw IllegalStateException("on_device_unavailable")
        val result = engine.generate(prompt)
        if (result.isBlank()) {
            // Recreate engine in case it entered a bad state (native error)
            Log.w(TAG, "Empty response – recreating engine")
            onDeviceEngine = engine.recreate()
            throw IllegalStateException("on_device_empty_response")
        }
        return result
    }

    private suspend fun callOnDeviceSummary(
        prompt: String,
        userPrefs: UserPreferences,
        dailyLog: DailyNutritionLog?,
        healthCheck: HealthRulesEngine.HealthCheck,
        availableRecipes: List<Recipe>
    ): DailyRecommendation {
        val raw = callOnDeviceChat(prompt)
        return parseAIResponse(raw) ?: generateLocalRecommendation(userPrefs, dailyLog, healthCheck, availableRecipes)
    }
    
    /**
     * Build structured prompt for AI
     */
    private fun buildPrompt(
        userPrefs: UserPreferences,
        dailyLog: DailyNutritionLog?,
        recentLogs: List<DailyNutritionLog>,
        healthCheck: HealthRulesEngine.HealthCheck,
        availableRecipes: List<Recipe>
    ): String {
        val prompt = StringBuilder()
        
        prompt.append("You are a supportive, knowledgeable health advisor. Be warm and encouraging.\n\n")
        
        // User Profile
        prompt.append("USER PROFILE:\n")
        prompt.append("- Name: ${userPrefs.nickname}\n")
        prompt.append("- Age: ${userPrefs.age}, Gender: ${userPrefs.gender}\n")
        prompt.append("- Weight: ${userPrefs.weight}kg, Height: ${userPrefs.height}cm\n")
        prompt.append("- BMI: ${String.format("%.1f", userPrefs.bmi)} (${getBMICategory(userPrefs.bmi)})\n")
        prompt.append("- Activity Level: ${userPrefs.activityLevel}\n")
        prompt.append("- Goal: ${userPrefs.fitnessGoal}\n")
        prompt.append("- Diet Type: ${userPrefs.dietType}\n\n")
        
        // Today's Progress
        prompt.append("TODAY'S NUTRITION:\n")
        if (dailyLog != null) {
            prompt.append("- Calories: ${dailyLog.caloriesConsumed}/${dailyLog.caloriesGoal} cal (${getPercentage(dailyLog.caloriesConsumed, dailyLog.caloriesGoal)}%)\n")
            prompt.append("- Protein: ${dailyLog.proteinConsumed}/${dailyLog.proteinGoal}g (${getPercentage(dailyLog.proteinConsumed, dailyLog.proteinGoal)}%)\n")
            prompt.append("- Carbs: ${dailyLog.carbsConsumed}/${dailyLog.carbsGoal}g\n")
            prompt.append("- Fat: ${dailyLog.fatConsumed}/${dailyLog.fatGoal}g\n")
            prompt.append("- Water: ${dailyLog.waterConsumedMl}/${dailyLog.waterGoalMl}ml (${getPercentage(dailyLog.waterConsumedMl, dailyLog.waterGoalMl)}%)\n")
            prompt.append("- Meals logged: ${dailyLog.mealCount}\n\n")
        } else {
            prompt.append("- No meals logged yet today\n")
            prompt.append("- Daily goals: ${userPrefs.calorieGoal} cal, ${userPrefs.proteinGoal}g protein, ${userPrefs.waterGoal}ml water\n\n")
        }
        
        // Weekly Trend (if available)
        if (recentLogs.isNotEmpty()) {
            prompt.append("WEEKLY TREND (last ${recentLogs.size} days):\n")
            val avgCalories = recentLogs.map { it.caloriesConsumed }.average().toInt()
            val avgProtein = recentLogs.map { it.proteinConsumed }.average().toInt()
            val avgWater = recentLogs.map { it.waterConsumedMl }.average().toInt()
            prompt.append("- Average calories: $avgCalories cal/day\n")
            prompt.append("- Average protein: ${avgProtein}g/day\n")
            prompt.append("- Average water: ${avgWater}ml/day\n\n")
        }
        
        // Health Warnings (from rules engine)
        if (healthCheck.warnings.isNotEmpty()) {
            prompt.append("HEALTH WARNINGS:\n")
            healthCheck.warnings.forEach { prompt.append("- $it\n") }
            prompt.append("\n")
        }
        
        // Suggestions from rules
        if (healthCheck.suggestions.isNotEmpty()) {
            prompt.append("AUTOMATED SUGGESTIONS:\n")
            healthCheck.suggestions.forEach { prompt.append("- $it\n") }
            prompt.append("\n")
        }
        
        // Available recipes
        if (availableRecipes.isNotEmpty()) {
            prompt.append("AVAILABLE RECIPES (choose ONE that best fits their needs):\n")
            availableRecipes.take(5).forEach { recipe ->
                prompt.append("- ${recipe.name}: ${recipe.caloriesPerServing} cal, ${recipe.proteinPerServing}g protein (${recipe.dietType}, ${recipe.mealType})\n")
            }
            prompt.append("\n")
        }
        
        prompt.append("""
TASK:
Generate a response with these EXACT sections (use markdown headers):

## Daily Summary
Write a 2-3 sentence warm, personalized summary of their day's nutrition. Address them as ${userPrefs.nickname}. Be encouraging.

## Next Meal Suggestion
Suggest what type of meal they should have next based on their remaining goals. Be specific about nutrients to focus on.

## Recipe Recommendation
If recipes are available, recommend ONE by name from the list above. Explain why it fits their current needs. If none are suitable, suggest a general meal type.

## Habit Tip
Give ONE small, actionable habit tip (not related to today's meals). Examples: meal prep, mindful eating, hydration timing, sleep, stress management.

## Motivation
End with a short (1 sentence) motivational message.

Remember:
- Be warm, supportive, and personal
- Use ${userPrefs.nickname}'s name
- Don't lecture - guide gently
- Focus on what they CAN do, not what they shouldn't
- Keep it conversational and human
""")
        
        return prompt.toString()
    }
    
    /**
     * Call OpenAI API
     */
    private suspend fun callOpenAI(prompt: String): DailyRecommendation? {
        if (config.openAIKey.isEmpty()) {
            Log.w("AIHealthAdvisor", "OpenAI key not configured")
            return null
        }
        
        val requestBody = OpenAIRequest(
            model = "gpt-4",
            messages = listOf(
                Message(role = "system", content = "You are a supportive health advisor who gives personalized, evidence-based nutrition advice."),
                Message(role = "user", content = prompt)
            ),
            temperature = 0.7,
            maxTokens = 800
        )
        
        val json = gson.toJson(requestBody)
        val body = json.toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer ${config.openAIKey}")
            .post(body)
            .build()
        
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val openAIResponse = gson.fromJson(responseBody, OpenAIResponse::class.java)
                val content = openAIResponse.choices.firstOrNull()?.message?.content ?: return null
                parseAIResponse(content)
            } else {
                Log.e("AIHealthAdvisor", "OpenAI API error: ${response.code}")
                null
            }
        } catch (e: IOException) {
            Log.e("AIHealthAdvisor", "Network error calling OpenAI", e)
            null
        }
    }
    
    /**
     * Call Google Gemini API
     */
    private suspend fun callGemini(prompt: String): DailyRecommendation? {
        if (config.geminiKey.isEmpty()) {
            Log.w("AIHealthAdvisor", "Gemini key not configured")
            return null
        }
        
        val requestBody = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            generationConfig = GeminiConfig(
                temperature = 0.7,
                maxOutputTokens = 800
            )
        )
        
        val json = gson.toJson(requestBody)
        val body = json.toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=${config.geminiKey}")
            .post(body)
            .build()
        
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
                val content = geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return null
                parseAIResponse(content)
            } else {
                Log.e("AIHealthAdvisor", "Gemini API error: ${response.code}")
                null
            }
        } catch (e: IOException) {
            Log.e("AIHealthAdvisor", "Network error calling Gemini", e)
            null
        }
    }
    
    /**
     * Parse AI response into structured recommendation
     */
    private fun parseAIResponse(content: String): DailyRecommendation {
        var summary = ""
        var mealSuggestion = ""
        var recipeRecommendation = ""
        var habitTip = ""
        var motivation = ""
        
        // Simple markdown parsing
        val sections = content.split("##").map { it.trim() }
        
        for (section in sections) {
            when {
                section.startsWith("Daily Summary", ignoreCase = true) -> {
                    summary = section.substringAfter("Daily Summary", "").trim()
                }
                section.startsWith("Next Meal", ignoreCase = true) -> {
                    mealSuggestion = section.substringAfter("Next Meal", "").trim()
                        .substringAfter("Suggestion", "").trim()
                }
                section.startsWith("Recipe", ignoreCase = true) -> {
                    recipeRecommendation = section.substringAfter("Recipe", "").trim()
                        .substringAfter("Recommendation", "").trim()
                }
                section.startsWith("Habit", ignoreCase = true) -> {
                    habitTip = section.substringAfter("Habit", "").trim()
                        .substringAfter("Tip", "").trim()
                }
                section.startsWith("Motivation", ignoreCase = true) -> {
                    motivation = section.substringAfter("Motivation", "").trim()
                }
            }
        }
        
        // Fallback: use entire content if parsing failed
        if (summary.isEmpty() && mealSuggestion.isEmpty()) {
            summary = content.take(200)
            mealSuggestion = "Stay consistent with your nutrition goals today!"
            habitTip = "Track your meals consistently for better insights."
            motivation = "You've got this! 💪"
        }
        
        return DailyRecommendation(
            summary = summary,
            mealSuggestion = mealSuggestion,
            habitTip = habitTip,
            motivationalMessage = motivation,
            recipeRecommendation = recipeRecommendation
        )
    }
    
    /**
     * Generate local (rule-based) recommendation when AI is unavailable
     */
    private fun generateLocalRecommendation(
        userPrefs: UserPreferences,
        dailyLog: DailyNutritionLog?,
        healthCheck: HealthRulesEngine.HealthCheck,
        availableRecipes: List<Recipe> = emptyList()
    ): DailyRecommendation {
        val name = userPrefs.nickname
        
        val summary = if (dailyLog != null) {
            val calPercent = getPercentage(dailyLog.caloriesConsumed, dailyLog.caloriesGoal)
            when {
                calPercent < 50 -> "Good morning, $name! You're off to a great start. Let's fuel your day with nutritious meals to reach your ${dailyLog.caloriesGoal} calorie goal."
                calPercent < 90 -> "Hi $name! You've consumed ${dailyLog.caloriesConsumed} of your ${dailyLog.caloriesGoal} calorie goal. Keep up the balanced eating!"
                else -> "Excellent progress today, $name! You've hit ${calPercent}% of your calorie goal with ${dailyLog.proteinConsumed}g protein. Finish strong!"
            }
        } else {
            "Good morning, $name! Ready to start tracking your nutrition today? Your daily goal is ${userPrefs.calorieGoal} calories with ${userPrefs.proteinGoal}g protein."
        }
        
        val mealSuggestion = if (dailyLog != null) {
            val proteinPercent = getPercentage(dailyLog.proteinConsumed, dailyLog.proteinGoal)
            when {
                proteinPercent < 70 -> "Focus on protein for your next meal. Try grilled chicken, fish, eggs, or legumes to reach your ${dailyLog.proteinGoal}g protein goal."
                dailyLog.waterConsumedMl < dailyLog.waterGoalMl / 2 -> "Hydration time! Drink 2-3 glasses of water, then have a balanced meal with lean protein and vegetables."
                else -> "Have a balanced meal with lean protein, complex carbs, and healthy fats. Listen to your hunger cues."
            }
        } else {
            "Start with a protein-rich breakfast to fuel your day. Eggs, Greek yogurt, or a protein smoothie are great options."
        }
        
        val habitTip = listOf(
            "Prep healthy snacks on Sunday for the whole week.",
            "Drink a glass of water before each meal to aid digestion.",
            "Take 3 deep breaths before eating to practice mindful eating.",
            "Aim for 7-9 hours of quality sleep for better recovery.",
            "Add one extra serving of vegetables to your meals today."
        ).random()
        
        val motivation = when (healthCheck.riskLevel) {
            HealthRulesEngine.RiskLevel.SAFE -> "You're doing great, $name! Keep up the excellent work! 🌟"
            HealthRulesEngine.RiskLevel.CAUTION -> "Small steps lead to big changes, $name. You've got this! 💪"
            HealthRulesEngine.RiskLevel.WARNING -> "Progress over perfection, $name. Each healthy choice counts! 🎯"
            HealthRulesEngine.RiskLevel.CRITICAL -> "Your health matters, $name. Consider consulting a healthcare professional for personalized guidance. 🏥"
        }
        
        return DailyRecommendation(
            summary = summary,
            mealSuggestion = mealSuggestion,
            habitTip = habitTip,
            motivationalMessage = motivation
        )
    }
    
    private fun getBMICategory(bmi: Float): String {
        return when {
            bmi < 18.5f -> "Underweight"
            bmi < 25f -> "Normal"
            bmi < 30f -> "Overweight"
            else -> "Obese"
        }
    }
    
    private fun getPercentage(consumed: Int, goal: Int): Int {
        return if (goal > 0) ((consumed.toFloat() / goal) * 100).toInt() else 0
    }
    
    // OpenAI API models
    private data class OpenAIRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double,
        @SerializedName("max_tokens") val maxTokens: Int
    )
    
    private data class Message(
        val role: String,
        val content: String
    )
    
    private data class OpenAIResponse(
        val choices: List<Choice>
    )
    
    private data class Choice(
        val message: Message
    )
    
    // Gemini API models
    private data class GeminiRequest(
        val contents: List<GeminiContent>,
        val generationConfig: GeminiConfig
    )
    
    private data class GeminiContent(
        val parts: List<GeminiPart>
    )
    
    private data class GeminiPart(
        val text: String
    )
    
    private data class GeminiConfig(
        val temperature: Double,
        val maxOutputTokens: Int
    )
    
    private data class GeminiResponse(
        val candidates: List<GeminiCandidate>
    )
    
    private data class GeminiCandidate(
        val content: GeminiContent
    )
    
    /**
     * Call OpenAI for chat responses (simpler, direct text)
     */
    private suspend fun callOpenAIChat(prompt: String): String {
        val apiKey = config.openAIKey
        if (apiKey.isEmpty()) throw Exception("OpenAI API key not configured")
        
        val requestBody = gson.toJson(mapOf(
            "model" to "gpt-4",
            "messages" to listOf(
                mapOf("role" to "system", "content" to """You are an expert personal trainer and nutritionist.
You personalise every response using the user's weight, height, BMI, age, gender, activity level, and diet type provided in the user message.
For food advice: always give specific foods, portion sizes, and macros.
For exercise advice: always give specific workout types, duration, frequency, and intensity suited to the user's BMI category.
For water: calculate from body weight (35 ml/kg). For steps: adapt to activity level.
Be warm, concise (under 200 words unless a full plan is requested), and data-driven.
Use bullet points or numbered lists for plans. Use the user's name once."""),
                mapOf("role" to "user", "content" to prompt)
            ),
            "max_tokens" to 500,
            "temperature" to 0.7
        ))
        
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("OpenAI API failed: ${response.code}")
        
        val jsonResponse = gson.fromJson(response.body?.string(), Map::class.java)
        val choices = jsonResponse["choices"] as? List<*> ?: throw Exception("No response from OpenAI")
        val firstChoice = choices[0] as? Map<*, *> ?: throw Exception("Invalid response format")
        val message = firstChoice["message"] as? Map<*, *> ?: throw Exception("No message in response")
        
        return message["content"] as? String ?: throw Exception("No content in message")
    }
    
    /**
     * Call Gemini for chat responses
     */
    private suspend fun callGeminiChat(prompt: String): String {
        val apiKey = config.geminiKey
        if (apiKey.isEmpty()) throw Exception("Gemini API key not configured")
        
        val requestBody = gson.toJson(GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart("You are an expert personal trainer and nutritionist. " +
                    "Personalise every response using the user's weight, height, BMI, activity level and diet type. " +
                    "Give specific food plans, exercise routines, water targets (35 ml/kg), and step goals. " +
                    "Be concise, warm, and data-driven. Use bullet lists for plans.\n\n" + prompt))),
            ),
            generationConfig = GeminiConfig(
                temperature = 0.7,
                maxOutputTokens = 300
            )
        ))
        
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Gemini API failed: ${response.code}")
        
        val geminiResponse = gson.fromJson(response.body?.string(), GeminiResponse::class.java)
        return geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("No response from Gemini")
    }
    
    /**
     * Call Ollama local LLM for chat responses
     */
    private suspend fun callOllamaChat(prompt: String): String {
        val baseUrl = config.ollamaUrl.trimEnd('/')
        if (baseUrl.isEmpty()) throw Exception("Ollama URL not configured")
        
        val requestBody = gson.toJson(mapOf(
            "model" to config.ollamaModel,
            "messages" to listOf(
                mapOf("role" to "system", "content" to """You are an expert personal trainer and nutritionist inside a health tracking app.
You MUST personalise every response using the user's actual data provided below (weight, height, BMI, age, gender, activity level, diet type, today's nutrition, weekly trends).
ALWAYS reference the user's ACTUAL numbers — never give generic advice when you have specific data.
If the user has allergies listed, NEVER suggest foods containing those allergens.
For food advice: give specific foods, portion sizes, and estimated macros.
For exercise: give specific workout types, duration, frequency, and intensity suited to their BMI.
For water: calculate from body weight (35 ml/kg). For steps: adapt to activity level.
Be warm, concise (under 250 words), and data-driven.
Use bullet points or numbered lists for plans. Use the user's name once.
If the user asks about anything UNRELATED to health, fitness, nutrition, exercise, or this app, politely decline and redirect them back to health topics.
NEVER give a generic answer — always cite the user's actual numbers."""),
                mapOf("role" to "user", "content" to prompt)
            ),
            "stream" to false,
            "options" to mapOf("temperature" to 0.3)
        ))
        
        val request = Request.Builder()
            .url("$baseUrl/api/chat")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = ollamaClient.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Ollama API failed: ${response.code}")
        
        val jsonResponse = gson.fromJson(response.body?.string(), Map::class.java)
        val message = jsonResponse["message"] as? Map<*, *> ?: throw Exception("No message in Ollama response")
        return message["content"] as? String ?: throw Exception("No content in Ollama response")
    }
    
    /**
     * Call Ollama for structured daily recommendation
     */
    private suspend fun callOllama(prompt: String): DailyRecommendation? {
        return try {
            val content = callOllamaChat(prompt)
            parseAIResponse(content)
        } catch (e: Exception) {
            Log.e(TAG, "Ollama call failed", e)
            null
        }
    }
}
