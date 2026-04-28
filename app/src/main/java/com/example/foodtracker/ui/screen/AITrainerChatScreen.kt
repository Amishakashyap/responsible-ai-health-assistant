package com.example.foodtracker.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.data.chat.ChatHistoryManager
import com.example.foodtracker.data.db.AppDatabase
import com.example.foodtracker.data.diet.DietPlanManager
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.domain.ai.AIConfig
import com.example.foodtracker.domain.ai.AIHealthAdvisor
import com.example.foodtracker.domain.ai.AIProvider
import com.example.foodtracker.domain.health.HealthRulesEngine
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import com.example.foodtracker.ui.theme.AppPrimary
import com.example.foodtracker.ui.theme.AppTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import kotlin.math.roundToInt

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// ── Helper: determine current meal type from time of day ─────────────────────
private fun currentMealType(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..10  -> "breakfast"
        in 11..14 -> "lunch"
        in 15..17 -> "snack"
        else      -> "dinner"
    }
}



@Composable
fun AITrainerChatScreen(
    scaffoldPadding: PaddingValues = PaddingValues(),
    onOpenSettings: () -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    val db         = remember { AppDatabase.get(context) }
    val userPrefs  = remember { UserPreferences(context) }
    val aiAdvisor  = remember { AIHealthAdvisor(context) }
    val config     = remember { AIConfig(context) }
    val dietPlanManager = remember { DietPlanManager(context) }
    var currentProvider by remember { mutableStateOf(config.aiProvider) }
    var detectedPlanText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val chatHistoryManager = remember { ChatHistoryManager(context) }
    var showClearDialog by remember { mutableStateOf(false) }

    // Initialize on-device LLM if model is downloaded; clean up on exit
    DisposableEffect(aiAdvisor) {
        aiAdvisor.initOnDevice()
        onDispose { aiAdvisor.closeOnDevice() }
    }

    val welcomeMsg = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 5..11  -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else      -> "Hey"
        }
        val name = userPrefs.nickname.ifBlank { "there" }
        buildString {
            append("$greeting, $name! 👋 I'm your AI Health Trainer.\n\n")
            if (userPrefs.weight > 0f && userPrefs.height > 0f) {
                val bmi = userPrefs.bmi
                val bmiLabel = when {
                    bmi < 18.5f -> "Underweight"
                    bmi < 25f   -> "Healthy"
                    bmi < 30f   -> "Overweight"
                    else        -> "Obese"
                }
                append("Your profile: ${userPrefs.weight}kg · ${userPrefs.height.roundToInt()}cm · BMI ${String.format("%.1f", bmi)} ($bmiLabel)\n")
                if (userPrefs.fitnessGoal.isNotBlank()) append("Goal: ${userPrefs.fitnessGoal}\n")
                append("\n")
            } else {
                append("Tell me your height & weight to get personalised advice!\ne.g. \"I'm 170cm and 70kg\"\n\n")
            }
            append("Type anything you'd like to ask — meals, workouts, progress, BMI, water goals, recipes, or just chat! 💬")
        }
    }

    var messages by remember {
        val saved = chatHistoryManager.loadMessages()
        mutableStateOf(if (saved.isNotEmpty()) saved else listOf(ChatMessage(text = welcomeMsg, isUser = false)))
    }
    var inputText  by remember { mutableStateOf("") }
    var isLoading  by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    val listState  = rememberLazyListState()

    // Back button navigates to home instead of closing app
    BackHandler { onNavigateToHome() }

    // Elapsed time counter while loading
    LaunchedEffect(isLoading) {
        elapsedSeconds = 0
        while (isLoading) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    // Auto-scroll to latest message and persist history
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
        if (messages.size > 1) chatHistoryManager.saveMessages(messages)
    }

    // Shared send logic
    fun sendMessage(text: String) {
        if (text.isBlank() || isLoading) return
        scope.launch {
            val providerAtSend = currentProvider
            messages = messages + ChatMessage(text = text, isUser = true)
            inputText = ""
            isLoading = true
            try {
                val response = getAIResponse(
                    userInput      = text,
                    context        = context,
                    db             = db,
                    userPrefs      = userPrefs,
                    aiAdvisor      = aiAdvisor,
                    selectedProvider = providerAtSend,
                    recentMessages = messages.takeLast(8)
                )
                // Clean literal escape sequences the model may output
                val cleaned = response
                    .replace("\\n", "\n")
                    .replace("\\t", "  ")
                    .replace(Regex("\\n{3,}"), "\n\n")
                    .trim()
                messages = messages + ChatMessage(text = cleaned, isUser = false)
                // Detect if the AI response contains a meal plan
                if (dietPlanManager.looksLikeMealPlan(cleaned)) {
                    detectedPlanText = cleaned
                }
            } catch (e: Exception) {
                messages = messages + ChatMessage(
                    text = "Sorry, I had trouble with that. Try again!",
                    isUser = false
                )
            } finally {
                isLoading = false
            }
        }
    }

    // ── UI ─────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = scaffoldPadding.calculateBottomPadding())
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .imePadding()
    ) {
        // ── Top bar ────────────────────────────────────────────────────────
        Surface(shadowElevation = 4.dp, color = AppSurface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AppPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AI", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI Health Trainer", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = if (isLoading) "typing…" else "online",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear chat", tint = Color.White)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "AI Settings",
                        tint = Color.White
                    )
                }
            }
        }

        // ── Messages ───────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading) {
                item {
                    TypingIndicator(elapsedSeconds = elapsedSeconds)
                }
            }
            items(messages.reversed(), key = { it.id }) { message ->
                ChatBubble(message = message)
            }
        }

        // ── Bottom input bar ───────────────────────────────────────────────
        Surface(
            color = AppSurface,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(0.dp)) {
                // ── AI source selector ────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI: ", fontSize = 11.sp, color = AppTextSecondary)
                    val onDeviceEnabled = config.isOnDeviceAvailable
                    val ollamaEnabled   = config.isOllamaAvailable
                    // On-Device chip
                    Surface(
                        color = if (currentProvider == AIProvider.ON_DEVICE) AppPrimary else AppSurface,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = onDeviceEnabled && !isLoading && inputText.isBlank()) {
                                config.preferredProvider = "ON_DEVICE"
                                currentProvider = AIProvider.ON_DEVICE
                                scope.launch { aiAdvisor.initOnDevice() }
                            }
                    ) {
                        Text(
                            "📱 On-Device",
                            fontSize = 11.sp,
                            color = if (currentProvider == AIProvider.ON_DEVICE) Color.Black
                                    else if (onDeviceEnabled) Color.White else Color(0xFF666666),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    // PC / Ollama chip
                    Surface(
                        color = if (currentProvider == AIProvider.OLLAMA) AppPrimary else AppSurface,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = ollamaEnabled && !isLoading && inputText.isBlank()) {
                                config.preferredProvider = "OLLAMA"
                                currentProvider = AIProvider.OLLAMA
                            }
                    ) {
                        Text(
                            "💻 PC/Ollama",
                            fontSize = 11.sp,
                            color = if (currentProvider == AIProvider.OLLAMA) Color.Black
                                    else if (ollamaEnabled) Color.White else Color(0xFF666666),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                // Text field + send button
                if (detectedPlanText.isNotEmpty()) {
                    Button(
                        onClick = {
                            val meals = dietPlanManager.parseFromAIResponse(detectedPlanText)
                            if (meals.isNotEmpty()) {
                                dietPlanManager.savePlan(meals, source = "ai")
                            }
                            detectedPlanText = ""
                            scope.launch {
                                snackbarHostState.showSnackbar("Diet plan saved! ✨ View it in the Diet Plan screen.")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("✨ Apply to Diet Plan")
                    }
                }
                // Text field + send button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask me anything…", fontSize = 14.sp) },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Color(0xFF1565C0),
                            unfocusedBorderColor = Color(0xFFCCCCCC)
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                    Spacer(Modifier.width(8.dp))
                    val canSend = inputText.isNotBlank() && !isLoading
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .align(Alignment.Bottom)
                            .clip(CircleShape)
                            .background(if (canSend) AppPrimary else Color(0xFF444444))
                            .clickable { if (canSend) sendMessage(inputText) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send",
                            tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
    SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    } // end Box

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear chat history?") },
            text = { Text("This will delete your entire conversation. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    chatHistoryManager.clearHistory()
                    messages = listOf(ChatMessage(text = welcomeMsg, isUser = false))
                    showClearDialog = false
                }) { Text("Clear", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Chat bubble ───────────────────────────────────────────────────────────────
@Composable
fun ChatBubble(message: ChatMessage) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    if (message.isUser) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 270.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
                        .background(AppPrimary)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(message.text, fontSize = 14.sp, color = Color.Black, lineHeight = 20.sp)
                }
                Spacer(Modifier.height(2.dp))
                Text(timeFormat.format(Date(message.timestamp)), fontSize = 10.sp, color = AppTextSecondary)
            }
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.Bottom) {
            // AI avatar
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(AppPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text("AI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.widthIn(max = 280.dp)) {
                // Message text
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                        .background(AppSurface)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(message.text, fontSize = 14.sp, color = Color.White, lineHeight = 20.sp)
                }
                Spacer(Modifier.height(2.dp))
                Text(timeFormat.format(Date(message.timestamp)), fontSize = 10.sp, color = AppTextSecondary)
            }
        }
    }
}

// ── Typing indicator ──────────────────────────────────────────────────────────
@Composable
fun TypingIndicator(elapsedSeconds: Int = 0) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.Bottom) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(AppPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text("AI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(AppSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = AppPrimary)
                val label = if (elapsedSeconds > 0) "Thinking… ${elapsedSeconds}s" else "Thinking…"
                Text(label, fontSize = 13.sp, color = AppTextSecondary)
            }
        }
    }
}

// ── Regex patterns and support functions (unchanged) ─────────────────────────

/** Regex patterns to extract height/weight from natural language */
private val HEIGHT_CM_REGEX  = Regex("""(\d{2,3})\s*(?:cm|centimeter|centimetre)""", RegexOption.IGNORE_CASE)
private val HEIGHT_FT_REGEX  = Regex("""(\d)\s*(?:ft|feet|foot|')\s*(\d{1,2})?""", RegexOption.IGNORE_CASE)
private val WEIGHT_KG_REGEX  = Regex("""(\d{2,3}(?:\.\d)?)\s*(?:kg|kgs|kilogram)""", RegexOption.IGNORE_CASE)
private val WEIGHT_LB_REGEX  = Regex("""(\d{2,3}(?:\.\d)?)\s*(?:lb|lbs|pound)""", RegexOption.IGNORE_CASE)

/**
 * Try to parse height/weight from the user's message.
 * Saves values to UserPreferences and recalculates BMI+goals.
 * Returns a confirmation string if values were found, null otherwise.
 */
fun parseAndSaveBodyMetrics(input: String, userPrefs: UserPreferences): String? {
    var heightCm: Float? = null
    var weightKg: Float? = null

    HEIGHT_CM_REGEX.find(input)?.let { heightCm = it.groupValues[1].toFloatOrNull() }
    if (heightCm == null) {
        HEIGHT_FT_REGEX.find(input)?.let { m ->
            val ft = m.groupValues[1].toFloatOrNull() ?: return@let
            val inch = m.groupValues[2].toFloatOrNull() ?: 0f
            heightCm = ft * 30.48f + inch * 2.54f
        }
    }
    WEIGHT_KG_REGEX.find(input)?.let { weightKg = it.groupValues[1].toFloatOrNull() }
    if (weightKg == null) {
        WEIGHT_LB_REGEX.find(input)?.let { m ->
            weightKg = (m.groupValues[1].toFloatOrNull() ?: return@let) * 0.4536f
        }
    }

    if (heightCm == null && weightKg == null) return null

    heightCm?.let { userPrefs.height = it }
    weightKg?.let { userPrefs.weight = it }
    userPrefs.calculateAndSaveGoals()

    return buildString {
        append("Got it! I've updated your profile:\n")
        heightCm?.let { append("📏 Height: ${it.roundToInt()} cm\n") }
        weightKg?.let { append("⚖️ Weight: ${String.format("%.1f", it)} kg\n") }
        if (userPrefs.bmi > 0f) {
            val cat = when {
                userPrefs.bmi < 18.5f -> "Underweight"
                userPrefs.bmi < 25f   -> "Healthy weight"
                userPrefs.bmi < 30f   -> "Overweight"
                else                  -> "Obese"
            }
            append("📊 BMI: ${String.format("%.1f", userPrefs.bmi)} ($cat)\n")
            append("🎯 Daily targets recalculated:\n")
            append("   Calories: ${userPrefs.calorieGoal} kcal\n")
            append("   Protein:  ${userPrefs.proteinGoal} g\n")
            append("   Water:    ${userPrefs.waterGoal} ml\n")
        }
    }
}

suspend fun getAIResponse(
    userInput: String,
    context: android.content.Context,
    db: AppDatabase,
    userPrefs: UserPreferences,
    aiAdvisor: AIHealthAdvisor,
    selectedProvider: AIProvider,
    recentMessages: List<ChatMessage> = emptyList()
): String {
    val lowerInput = userInput.lowercase().trim()

    // ── 0. Simple / short messages → use a minimal prompt (fast!) ───────
    val isSimpleMessage = lowerInput.length < 20 && hasAnyOf(lowerInput,
        "hi", "hello", "hey", "hii", "hiii", "yo", "sup", "thanks", "thank you",
        "ok", "okay", "bye", "good morning", "good night", "good evening",
        "good afternoon", "how are you", "what can you do", "help")
    if (isSimpleMessage) {
        // Return an instant local greeting – never touch the on-device model
        // for trivial inputs.  This avoids the native SIGABRT that can occur
        // when the MediaPipe tokenizer hits edge-case token IDs.
        val name = userPrefs.nickname.ifBlank { "there" }
        return generateSimpleGreeting(userInput, name)
    }

    // ── 1. Detect & save height / weight from the message ──────────────────
    val metricsConfirmation = parseAndSaveBodyMetrics(userInput, userPrefs)

    // ── 1b. Direct profile questions should never hit LLM ──────────────────
    val asksHeight = hasAnyOf(lowerInput, "my height", "tell me my height", "what is my height", "height?")
    val asksWeight = hasAnyOf(lowerInput, "my weight", "tell me my weight", "what is my weight", "weight?")
    val asksBmiDirect = hasAnyOf(lowerInput, "my bmi", "what is my bmi", "tell me my bmi")
    if (asksHeight || asksWeight || asksBmiDirect) {
        val h = userPrefs.height
        val w = userPrefs.weight
        val b = userPrefs.bmi
        return when {
            asksHeight && h > 0f -> "Your height is ${h.roundToInt()} cm."
            asksWeight && w > 0f -> "Your weight is ${String.format("%.1f", w)} kg."
            asksBmiDirect && b > 0f -> "Your BMI is ${String.format("%.1f", b)}."
            else -> "I don't have that value yet. Update your profile first."
        }
    }

    // ── 2. Classify intent (broadened to catch expanded option prompts) ────
    val isFoodLogRequest  = hasAnyOf(lowerInput, "what i ate", "what did i eat", "what i had", "what did i have", "my meals", "my food", "my breakfast", "my lunch", "my dinner", "my snack", "food log", "food today", "food i ate", "food i had", "today food", "today's food", "what have i eaten", "show my meal", "show my food", "ate today", "eaten today", "ate in breakfast", "ate in lunch", "ate in dinner", "ate for breakfast", "ate for lunch", "ate for dinner", "had for breakfast", "had for lunch", "had for dinner")
    val isRecipeRequest   = !isFoodLogRequest && hasAnyOf(lowerInput, "recipe", "cook", "make", "ingredient", "meal plan", "food plan", "what to eat", "what should i eat", "breakfast", "lunch", "dinner", "snack", "suggest", "meal for", "high protein", "high-protein", "keto", "paleo", "vegan", "vegetarian", "full day", "healthy swap", "free recipes", "healthy snack", "calorie budget", "light meal")
    val isExerciseRequest = hasAnyOf(lowerInput, "exercise", "workout", "gym", "fitness", "training", "cardio", "strength", "beginner workout", "stretching", "hiit", "split", "progressive overload", "low-impact", "fat loss", "muscle building", "muscle plan", "sets", "reps", "routine")
    val isWaterRequest    = hasAnyOf(lowerInput, "water", "hydrat", "drink", "fluid", "hydration schedule", "ml left", "ml to go", "catch up on water")
    val isStepsRequest    = hasAnyOf(lowerInput, "steps", "step goal", "daily step", "10000", "pedometer", "walking goal", "step target")
    val isBmiRequest      = hasAnyOf(lowerInput, "bmi", "body mass", "overweight", "underweight", "obese", "ideal weight", "weight loss", "weight gain", "lose weight", "gain weight", "body recomposition", "calorie deficit", "maintain weight")
    val isProgressRequest = hasAnyOf(lowerInput, "progress", "tracking", "how am i", "on track", "detailed progress", "report for today")

    // ── 3. Fetch today's log ───────────────────────────────────────────────
    val today    = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val userId   = userPrefs.userId
    val todaysLog = db.dailyNutritionLogDao().getByUserAndDate(userId, today)

    // ── 3a. Fetch individual food entries for today (what user actually ate) ─
    val todaysEntries = try { db.entryDao().getByDateAndUser(today, userId) } catch (_: Exception) { emptyList() }
    val todaysMeals = buildString {
        if (todaysEntries.isNotEmpty()) {
            val byMeal = todaysEntries.groupBy { it.mealType.lowercase() }
            for (meal in listOf("breakfast", "lunch", "dinner", "snack")) {
                val items = byMeal[meal] ?: continue
                append("${meal.replaceFirstChar { it.uppercase() }}:\n")
                for (entry in items) {
                    val food = try { db.foodDao().getById(entry.foodId) } catch (_: Exception) { null }
                    val name = food?.name ?: "Unknown food"
                    val qty = entry.quantityG
                    val cal = food?.calories?.let { it * qty / 100.0 }?.toInt() ?: 0
                    val prot = food?.proteinG?.let { it * qty / 100.0 }?.toInt() ?: 0
                    append("  - $name (${qty.toInt()}g, ${cal} cal, ${prot}g protein)\n")
                }
            }
        } else {
            append("No meals logged yet today.")
        }
    }

    // ── 3b. Fetch user extended profile (medical, target weight) ───────────
    val userProfile = try { db.profileDao().getByUserId(userId) } catch (_: Exception) { null }
    val medicalHistory = userProfile?.medicalHistory?.takeIf { it.isNotBlank() } ?: ""
    val targetWeight = userProfile?.targetWeightKg?.takeIf { it > 0.0 } ?: 0.0

    // ── 3c. Direct food-log question handler (no LLM needed) ──────────────
    if (isFoodLogRequest) {
        return buildString {
            if (todaysEntries.isEmpty()) {
                append("You haven't logged any food today yet. Use the + button to add meals!")
            } else {
                // Check if asking about specific meal
                val specificMeal = when {
                    hasAnyOf(lowerInput, "breakfast", "morning") -> "breakfast"
                    hasAnyOf(lowerInput, "lunch", "afternoon") -> "lunch"
                    hasAnyOf(lowerInput, "dinner", "evening", "night") -> "dinner"
                    hasAnyOf(lowerInput, "snack") -> "snack"
                    else -> null
                }
                if (specificMeal != null) {
                    val items = todaysEntries.filter { it.mealType.lowercase() == specificMeal }
                    if (items.isEmpty()) {
                        append("You haven't logged any $specificMeal today.")
                    } else {
                        append("Your $specificMeal today:\n\n")
                        var totalCal = 0; var totalProt = 0
                        for (entry in items) {
                            val food = try { db.foodDao().getById(entry.foodId) } catch (_: Exception) { null }
                            val name = food?.name ?: "Unknown"
                            val qty = entry.quantityG
                            val cal = food?.calories?.let { it * qty / 100.0 }?.toInt() ?: 0
                            val prot = food?.proteinG?.let { it * qty / 100.0 }?.toInt() ?: 0
                            totalCal += cal; totalProt += prot
                            append("\u2022 $name — ${qty.toInt()}g ($cal cal, ${prot}g protein)\n")
                        }
                        append("\nTotal: $totalCal cal, ${totalProt}g protein")
                    }
                } else {
                    append("Here's everything you ate today:\n\n")
                    var grandCal = 0; var grandProt = 0
                    val byMeal = todaysEntries.groupBy { it.mealType.lowercase() }
                    for (meal in listOf("breakfast", "lunch", "dinner", "snack")) {
                        val items = byMeal[meal] ?: continue
                        append("${meal.replaceFirstChar { it.uppercase() }}:\n")
                        for (entry in items) {
                            val food = try { db.foodDao().getById(entry.foodId) } catch (_: Exception) { null }
                            val name = food?.name ?: "Unknown"
                            val qty = entry.quantityG
                            val cal = food?.calories?.let { it * qty / 100.0 }?.toInt() ?: 0
                            val prot = food?.proteinG?.let { it * qty / 100.0 }?.toInt() ?: 0
                            grandCal += cal; grandProt += prot
                            append("  \u2022 $name — ${qty.toInt()}g ($cal cal, ${prot}g protein)\n")
                        }
                        append("\n")
                    }
                    append("Total: $grandCal cal, ${grandProt}g protein")
                }
            }
        }
    }

    // ── 3d. Fetch weekly trends ────────────────────────────────────────────
    val weeklyLogs = db.dailyNutritionLogDao().getRecentLogs(userId, 7)

    // ── 4. Collect user profile values ────────────────────────────────────
    val weight        = userPrefs.weight
    val height        = userPrefs.height
    val age           = userPrefs.age
    val gender        = userPrefs.gender
    val activityLevel = userPrefs.activityLevel
    val fitnessGoal   = userPrefs.fitnessGoal
    val dietType      = userPrefs.dietType.ifBlank { "omnivore" }.let { if (it == "none") "omnivore" else it }
    val bmi           = userPrefs.bmi
    val bmiLabel      = when {
        bmi <= 0f   -> "unknown"
        bmi < 18.5f -> "Underweight"
        bmi < 25f   -> "Healthy"
        bmi < 30f   -> "Overweight"
        else        -> "Obese"
    }

    // ── 5. Health checks ──────────────────────────────────────────────────
    val healthCheck = HealthRulesEngine.runHealthChecks(userPrefs = userPrefs, dailyLog = todaysLog)

    // ── 6. Recipes ── query matching recipes from DB for context ───────────
    val mealType = currentMealType()
    val calRemaining = todaysLog?.let { it.caloriesGoal - it.caloriesConsumed } ?: userPrefs.calorieGoal
    val suitableRecipes: List<com.example.foodtracker.data.db.Recipe> = try {
        val byMeal = db.recipeDao().findSuitableRecipes(
            mealType    = mealType,
            dietType    = dietType.lowercase(),
            minCalories = 50,
            maxCalories = maxOf(calRemaining, 300),
            limit       = 5
        )
        byMeal.ifEmpty {
            db.recipeDao().getByDietType(dietType.lowercase()).take(5)
        }
    } catch (_: Exception) {
        db.recipeDao().getRandomRecipes(limit = 5)
    }

    // Also search the food database for items the user mentions
    val mentionedFoods: List<com.example.foodtracker.data.db.Food> = try {
        val words = lowerInput.split(" ").filter { it.length > 3 }
        words.flatMap { db.foodDao().searchLike("%$it%") }.distinctBy { it.id }.take(5)
    } catch (_: Exception) { emptyList() }

    // ── 7. Assemble context strings ───────────────────────────────────────
    val profileSummary = buildString {
        append("Name: ${userPrefs.nickname.ifBlank { "User" }}\n")
        if (age > 0)      append("Age: $age  Gender: $gender\n")
        if (weight > 0f)  append("Weight: ${String.format("%.1f", weight)} kg\n")
        if (height > 0f)  append("Height: ${height.roundToInt()} cm\n")
        if (bmi > 0f)     append("BMI: ${String.format("%.1f", bmi)} ($bmiLabel)\n")
        append("Activity level: $activityLevel\n")
        append("Fitness goal: $fitnessGoal\n")
        append("Diet type: $dietType\n")
        append("Daily calorie goal: ${userPrefs.calorieGoal} kcal\n")
        append("Daily protein goal: ${userPrefs.proteinGoal} g\n")
        append("Daily water goal:   ${userPrefs.waterGoal} ml\n")
        val allergies = userPrefs.allergies
        if (allergies.isNotBlank()) append("Allergies: $allergies\n")
        val foodPrefs = userPrefs.foodPreferences
        if (foodPrefs.isNotBlank()) append("Food preferences: $foodPrefs\n")
        if (userPrefs.bmr > 0) append("BMR: ${userPrefs.bmr} kcal | TDEE: ${userPrefs.tdee} kcal\n")
        if (medicalHistory.isNotBlank()) append("Medical history: $medicalHistory\n")
        if (targetWeight > 0.0) append("Target weight: ${String.format("%.1f", targetWeight)} kg\n")
    }

    val nutritionSummary = buildString {
        if (todaysLog != null) {
            append("Calories: ${todaysLog.caloriesConsumed}/${todaysLog.caloriesGoal} kcal\n")
            append("Protein:  ${todaysLog.proteinConsumed}/${todaysLog.proteinGoal} g\n")
            append("Carbs:    ${todaysLog.carbsConsumed}/${todaysLog.carbsGoal} g\n")
            append("Fat:      ${todaysLog.fatConsumed}/${todaysLog.fatGoal} g\n")
            append("Water:    ${todaysLog.waterConsumedMl}/${todaysLog.waterGoalMl} ml\n")
            append("Meals logged: ${todaysLog.mealCount}\n")
        } else {
            append("No nutrition logged yet today.\n")
        }
        append("\nTODAY'S FOOD LOG (individual items):\n")
        append(todaysMeals)
    }

    val weeklyTrends = if (weeklyLogs.isNotEmpty()) {
        val avgCal     = weeklyLogs.map { it.caloriesConsumed }.average().toInt()
        val avgProtein = weeklyLogs.map { it.proteinConsumed }.average().toInt()
        val avgWater   = weeklyLogs.map { it.waterConsumedMl }.average().toInt()
        val calGoal    = weeklyLogs.firstOrNull()?.caloriesGoal ?: userPrefs.calorieGoal
        val protGoal   = weeklyLogs.firstOrNull()?.proteinGoal ?: userPrefs.proteinGoal
        val waterGoal  = weeklyLogs.firstOrNull()?.waterGoalMl ?: userPrefs.waterGoal
        buildString {
            append("Last ${weeklyLogs.size} days logged:\n")
            append("Avg calories: $avgCal/$calGoal kcal (${if (calGoal > 0) avgCal * 100 / calGoal else 0}%)\n")
            append("Avg protein:  $avgProtein/$protGoal g (${if (protGoal > 0) avgProtein * 100 / protGoal else 0}%)\n")
            append("Avg water:    $avgWater/$waterGoal ml (${if (waterGoal > 0) avgWater * 100 / waterGoal else 0}%)\n")
        }
    } else "No weekly data yet."

    val healthWarnings = buildString {
        if (healthCheck.warnings.isNotEmpty())   append(healthCheck.warnings.joinToString("\n") + "\n")
        if (healthCheck.suggestions.isNotEmpty()) append(healthCheck.suggestions.take(2).joinToString("\n"))
        if (isEmpty()) append("No warnings – all metrics look good!")
    }

    // ── 8. Build the LLM prompt ────────────────────────────────────────────
    // SmolLM-135M cannot handle the full buildChatPrompt output (too long,
    // too complex). Use a compact prompt for on-device inference.
    val activeProvider = selectedProvider
    val prompt = if (activeProvider == AIProvider.ON_DEVICE) {
        buildOnDevicePrompt(
            userInput         = if (metricsConfirmation != null) "$userInput\n[Profile updated]" else userInput,
            weight            = weight,
            height            = height,
            bmi               = bmi,
            bmiLabel          = bmiLabel,
            age               = age,
            gender            = gender,
            activityLevel     = activityLevel,
            fitnessGoal       = fitnessGoal,
            dietType          = dietType,
            todaysLog         = todaysLog,
            todaysMeals       = todaysMeals,
            isBmiRequest      = isBmiRequest,
            isExerciseRequest = isExerciseRequest,
            isWaterRequest    = isWaterRequest,
            isStepsRequest    = isStepsRequest
        )
    } else {
        buildChatPrompt(
            userInput        = if (metricsConfirmation != null) "$userInput\n[Profile updated: $metricsConfirmation]" else userInput,
            userProfile      = profileSummary,
            todaysNutrition  = nutritionSummary,
            weeklyTrends     = weeklyTrends,
            healthWarnings   = healthWarnings,
            availableRecipes = suitableRecipes,
            mentionedFoods   = mentionedFoods,
            chatHistory      = recentMessages,
            isRecipeRequest   = isRecipeRequest,
            isExerciseRequest = isExerciseRequest,
            isWaterRequest    = isWaterRequest,
            isStepsRequest    = isStepsRequest,
            isBmiRequest      = isBmiRequest,
            isProgressRequest = isProgressRequest
        )
    }

    // ── 9. Call LLM, prepend metrics confirmation if any ──────────────────
    val llmReply = try {
        val reply = aiAdvisor.askQuestion(prompt, activeProvider)
        // If any template tokens leaked through (on-device model garbage), fall back locally
        if (reply.contains("<|im_start|>") || reply.contains("<|im_end|>")) {
            android.util.Log.w("AITrainer", "Template token leak – using local fallback")
            generateLocalResponse(
                userInput, todaysLog, suitableRecipes, mentionedFoods, userPrefs,
                isRecipeRequest, isExerciseRequest, isWaterRequest, isStepsRequest,
                isBmiRequest, isProgressRequest, weeklyLogs
            )
        } else reply
    } catch (e: Exception) {
        android.util.Log.w("AITrainer", "LLM call failed (${e.javaClass.simpleName}): ${e.message}")
        generateLocalResponse(
            userInput, todaysLog, suitableRecipes, mentionedFoods, userPrefs,
            isRecipeRequest, isExerciseRequest, isWaterRequest, isStepsRequest,
            isBmiRequest, isProgressRequest, weeklyLogs
        )
    }

    return if (metricsConfirmation != null) "$metricsConfirmation\n$llmReply" else llmReply
}

fun buildChatPrompt(
    userInput: String,
    userProfile: String,
    todaysNutrition: String,
    weeklyTrends: String = "",
    healthWarnings: String,
    availableRecipes: List<com.example.foodtracker.data.db.Recipe>,
    mentionedFoods: List<com.example.foodtracker.data.db.Food> = emptyList(),
    chatHistory: List<ChatMessage> = emptyList(),
    isRecipeRequest: Boolean,
    isExerciseRequest: Boolean = false,
    isWaterRequest: Boolean = false,
    isStepsRequest: Boolean = false,
    isBmiRequest: Boolean = false,
    isProgressRequest: Boolean = false
): String {
    // Always include recipes when we have them (useful for meal/nutrition context)
    val recipeBlock = if (availableRecipes.isNotEmpty()) """
AVAILABLE RECIPES FROM OUR DATABASE (recommend these — they match the user's diet):
${availableRecipes.take(3).joinToString("\n\n") { r ->
    """  • ${r.name} (${r.caloriesPerServing} cal, ${r.proteinPerServing}g protein, ${r.carbsPerServing}g carbs, ${r.fatPerServing}g fat)
    Diet: ${r.dietType} | Meal: ${r.mealType} | Prep: ${r.prepTimeMinutes}min + Cook: ${r.cookTimeMinutes}min
    Ingredients: ${r.ingredients.replace("\n", ", ").take(200)}
    Steps: ${r.instructions.replace("\n", " ").take(300)}"""
}}
""" else ""

    val foodDbBlock = if (mentionedFoods.isNotEmpty()) """
MATCHING FOODS FROM OUR NUTRITION DATABASE (use exact values when suggesting these):
${mentionedFoods.joinToString("\n") { f ->
    "  • ${f.name}: ${f.calories ?: 0} cal, ${f.proteinG ?: 0}g protein, ${f.carbsG ?: 0}g carbs, ${f.fatG ?: 0}g fat per 100g"
}}
""" else ""

    val historyBlock = if (chatHistory.isNotEmpty()) {
        val recent = chatHistory.takeLast(4)
        buildString {
            append("── CONVERSATION HISTORY (recent) ──────────────\n")
            recent.forEach { msg ->
                val role = if (msg.isUser) "User" else "Assistant"
                val text = msg.text.take(120)
                append("$role: $text\n")
            }
        }
    } else ""

    val taskBlock = when {
        isProgressRequest -> """
TASK – Daily Progress Report:
Give the user a detailed progress report based on their TODAY'S NUTRITION and WEEKLY TRENDS data above.
1. How they're tracking on calories, protein, and water vs their goals (use exact numbers)
2. What's going well and what needs attention
3. Specific food suggestions for the rest of the day to hit their remaining targets
4. One motivational observation about their weekly trend
Be specific — cite the actual numbers from their data, not generic advice."""

        isExerciseRequest -> """
TASK – Exercise Recommendation:
Based on the user's BMI, weight, height, activity level, and weekly trends above, provide:
1. Weekly workout plan (days + type: cardio / strength / flexibility)
2. Recommended daily step count with a reason
3. One beginner-friendly exercise they can start today with sets/reps
4. One tip to stay consistent
Be specific with duration (e.g., 30 min) and intensity (light/moderate/hard).
Reference the user's actual weekly nutrition adherence when relevant."""

        isWaterRequest || isStepsRequest -> """
TASK – Water & Daily Steps:
Based on the user's weight, BMI, and activity level, calculate and explain:
1. Exact daily water intake recommendation in ml AND cups/glasses, with formula used
2. Daily step target (min / optimal / stretch goal) suited to their fitness level
3. Three practical tips on hitting both targets
Show the maths briefly so the user understands the personalisation."""

        isBmiRequest -> """
TASK – BMI Analysis:
1. Interpret the user's BMI with its health implications
2. Ideal weight range for their height (using healthy BMI 18.5–24.9)
3. Personalised food strategy to reach/maintain healthy weight
4. Exercise strategy that matches their current BMI category
5. Weekly water intake and step target
Be encouraging and practical, not clinical."""

        isRecipeRequest -> """
TASK – Meal / Recipe Suggestion:
Consider the user's remaining calories, diet type, allergies, preferences, and any ingredients they mentioned.
NEVER suggest foods containing the user's listed allergens.
IMPORTANT: PREFER suggesting recipes from AVAILABLE RECIPES FROM OUR DATABASE listed above — these are verified and we have exact nutrition data.
If user mentions specific foods, use the MATCHING FOODS FROM OUR NUTRITION DATABASE values for accurate macro calculations.
For each meal suggestion:
- Name & brief description
- Full ingredient list with quantities
- Step-by-step cooking instructions
- Exact nutrition (calories, protein, carbs, fat)
Then suggest what to eat for the rest of the day to meet their goals."""

        else -> """
TASK – General Health Q&A:
Answer the user's question using their actual profile data and weekly trends when relevant.
Reference specific numbers (e.g. "You've averaged X cal this week vs your Y goal").
Be specific and actionable. Keep under 200 words.
If they haven't provided height/weight yet, gently ask so you can personalise better."""
    }

    return """
You are a health assistant. Answer ONLY the user's question directly.
Do not add unrelated context, examples, or extra sections.
Keep reply under 120 words.

USER: ${userProfile.take(1200)}
NUTRITION: ${todaysNutrition.take(600)}
$recipeBlock
$foodDbBlock
$historyBlock
USER MESSAGE: $userInput

$taskBlock

Be brief and direct. Use plain text unless the user asked for a plan.
    """.trimIndent()
}

/**
 * Builds a compact prompt for Gemma3-1B-IT on-device model.
 *
 * Gemma3-1B can process ~1200 chars of context and produce coherent,
 * personalised responses — far more capable than the previous SmolLM-135M.
 *
 * [OnDeviceLlmEngine.generate] will additionally wrap this with the
 * Gemma chat template (<start_of_turn>user … <end_of_turn> <start_of_turn>model),
 * so this function should return just the conversational content.
 */
fun buildOnDevicePrompt(
    userInput: String,
    weight: Float,
    height: Float,
    bmi: Float,
    bmiLabel: String,
    age: Int,
    gender: String,
    activityLevel: String,
    fitnessGoal: String,
    dietType: String,
    todaysLog: com.example.foodtracker.data.db.DailyNutritionLog?,
    todaysMeals: String = "",
    isBmiRequest: Boolean,
    isExerciseRequest: Boolean,
    isWaterRequest: Boolean,
    isStepsRequest: Boolean
): String {
    val profile = buildString {
        if (weight > 0f) append("Weight: ${weight.roundToInt()}kg, ")
        if (height > 0f) append("Height: ${height.roundToInt()}cm, ")
        if (bmi > 0f)    append("BMI: ${String.format("%.1f", bmi)} ($bmiLabel), ")
        if (age > 0)     append("Age: $age, Gender: $gender, ")
        append("Activity: $activityLevel, Diet: $dietType, Goal: $fitnessGoal")
    }
    val nutrition = todaysLog?.let {
        buildString {
            append("Calories: ${it.caloriesConsumed}/${it.caloriesGoal} kcal")
            append(", Protein: ${it.proteinConsumed}/${it.proteinGoal}g")
            append(", Carbs: ${it.carbsConsumed}/${it.carbsGoal}g")
            append(", Fat: ${it.fatConsumed}/${it.fatGoal}g")
            append(", Water: ${it.waterConsumedMl}/${it.waterGoalMl}ml")
            append(", Meals logged: ${it.mealCount}")
        }
    } ?: "No food logged yet today. Daily goals: calories and protein targets not yet tracked."
    val mealsBlock = if (todaysMeals.isNotBlank() && !todaysMeals.contains("No meals logged")) {
        "\nMeals eaten today:\n${todaysMeals.take(400)}"
    } else ""
    val taskHint = when {
        isBmiRequest      -> "\nTASK: Interpret the user's BMI, explain health implications, give ideal weight range, and 3 specific actionable tips."
        isExerciseRequest -> "\nTASK: Give a personalised weekly workout plan with specific exercises, sets, reps, and duration suited to their BMI and activity level."
        isWaterRequest || isStepsRequest -> "\nTASK: Calculate exact daily water intake (weight × 35ml/kg), show the math, and recommend step targets for their activity level."
        else              -> ""
    }
    return "Use the user's ACTUAL data below to answer. Always cite specific numbers from their data.\n\n" +
           "USER PROFILE: $profile\n" +
           "TODAY'S NUTRITION: $nutrition$mealsBlock\n\n" +
           "QUESTION: $userInput$taskHint"
}

fun generateLocalResponse(
    userInput: String,
    todaysLog: com.example.foodtracker.data.db.DailyNutritionLog?,
    recipes: List<com.example.foodtracker.data.db.Recipe>,
    mentionedFoods: List<com.example.foodtracker.data.db.Food> = emptyList(),
    userPrefs: UserPreferences,
    isRecipeRequest: Boolean = false,
    isExerciseRequest: Boolean = false,
    isWaterRequest: Boolean = false,
    isStepsRequest: Boolean = false,
    isBmiRequest: Boolean = false,
    isProgressRequest: Boolean = false,
    weeklyLogs: List<com.example.foodtracker.data.db.DailyNutritionLog> = emptyList()
): String {
    val name   = userPrefs.nickname.ifBlank { "there" }
    val weight = userPrefs.weight
    val height = userPrefs.height
    val bmi    = userPrefs.bmi
    val diet   = userPrefs.dietType.ifBlank { "balanced" }
    val calRemaining = todaysLog?.let { it.caloriesGoal - it.caloriesConsumed } ?: userPrefs.calorieGoal
    val protRemaining = todaysLog?.let { it.proteinGoal - it.proteinConsumed } ?: userPrefs.proteinGoal
    val lowerInput = userInput.lowercase()

    // ── 1. Progress report ────────────────────────────────────────────────
    if (isProgressRequest || hasAnyOf(lowerInput, "progress", "doing", "how am i", "my diet", "record", "last", "track", "today")) {
        return if (todaysLog != null && todaysLog.mealCount > 0) {
            buildString {
                append("📊 Today's Progress for $name\n\n")
                append("Calories: ${todaysLog.caloriesConsumed}/${todaysLog.caloriesGoal} kcal")
                val calLeft = todaysLog.caloriesGoal - todaysLog.caloriesConsumed
                if (calLeft > 0) append(" ($calLeft remaining)")
                append("\n")
                append("Protein:  ${todaysLog.proteinConsumed}/${todaysLog.proteinGoal}g")
                val protLeft = todaysLog.proteinGoal - todaysLog.proteinConsumed
                if (protLeft > 0) append(" (${protLeft}g remaining)")
                append("\n")
                append("Carbs:    ${todaysLog.carbsConsumed}/${todaysLog.carbsGoal}g\n")
                append("Fat:      ${todaysLog.fatConsumed}/${todaysLog.fatGoal}g\n")
                append("Water:    ${todaysLog.waterConsumedMl}/${todaysLog.waterGoalMl}ml\n")
                append("Meals logged: ${todaysLog.mealCount}\n\n")
                val calPct = if (todaysLog.caloriesGoal > 0) todaysLog.caloriesConsumed * 100 / todaysLog.caloriesGoal else 0
                append(when {
                    calPct >= 90 -> "✅ Great job! You're almost at your calorie goal.\n"
                    calPct >= 50 -> "💪 Solid progress — ${calPct}% of your calorie target done.\n"
                    else         -> "⚠️ Only ${calPct}% of your calorie target so far — make sure you eat enough!\n"
                })
                if (calLeft > 200 && recipes.isNotEmpty()) {
                    val rec = recipes.first()
                    append("\n💡 Suggestion: ${rec.name} (${rec.caloriesPerServing} cal, ${rec.proteinPerServing}g protein)")
                }
                if (weeklyLogs.size >= 2) {
                    val avgCal = weeklyLogs.map { it.caloriesConsumed }.average().toInt()
                    val adherence = if (todaysLog.caloriesGoal > 0) avgCal * 100 / todaysLog.caloriesGoal else 0
                    append("\n\n📈 ${weeklyLogs.size}-day trend: Avg ${avgCal} cal/day (${adherence}% of goal)")
                }
            }
        } else {
            buildString {
                append("📊 $name, you haven't logged any meals today yet.\n\n")
                append("Your daily targets:\n")
                append("• Calories: ${userPrefs.calorieGoal} kcal\n")
                append("• Protein: ${userPrefs.proteinGoal}g\n")
                append("• Water: ${userPrefs.waterGoal}ml\n\n")
                append("Start logging your meals through the \"+\" button to see your daily progress here!")
                if (weeklyLogs.isNotEmpty()) {
                    val avgCal = weeklyLogs.map { it.caloriesConsumed }.average().toInt()
                    append("\n\n📈 Last ${weeklyLogs.size} days avg: $avgCal cal/day")
                }
            }
        }
    }

    // ── 2. Ingredient-based meal suggestions ("I have rice, chicken, eggs") ─
    if (hasAnyOf(lowerInput, "i have", "what can i make", "can i make", "ingredients") || mentionedFoods.size >= 2) {
        return buildString {
            append("🍳 Here's what I can suggest, $name:\n\n")
            if (mentionedFoods.isNotEmpty()) {
                append("📝 Nutrition info from our database:\n")
                mentionedFoods.forEach { f ->
                    append("• ${f.name}: ${f.calories?.roundToInt() ?: "?"}cal, ${f.proteinG?.roundToInt() ?: "?"}g protein per 100g\n")
                }
                append("\n")
            }
            val foodNames = mentionedFoods.map { it.name.lowercase() }
            val matchingRecipes = if (foodNames.isNotEmpty()) {
                recipes.filter { recipe ->
                    val ingr = recipe.ingredients.lowercase()
                    foodNames.any { ingr.contains(it) }
                }
            } else emptyList()

            if (matchingRecipes.isNotEmpty()) {
                append("🍽️ Recipes you can make with your ingredients:\n\n")
                matchingRecipes.take(2).forEach { r ->
                    append("⭐ ${r.name}\n")
                    append("   ${r.caloriesPerServing} cal | ${r.proteinPerServing}g protein | ${r.carbsPerServing}g carbs | ${r.fatPerServing}g fat\n")
                    append("   Prep: ${r.prepTimeMinutes}min + Cook: ${r.cookTimeMinutes}min\n")
                    append("   Ingredients: ${r.ingredients.replace("\n", ", ").trim()}\n")
                    append("   Steps: ${r.instructions.replace("\n", " ").trim()}\n\n")
                }
                if (calRemaining > 0) {
                    append("You still need ~$calRemaining cal today. ")
                    val rec = matchingRecipes.first()
                    val servings = calRemaining.toFloat() / maxOf(rec.caloriesPerServing, 1)
                    val servTxt = if (servings < 0.5f) "a half serving" else if (servings < 1.5f) "one serving" else "${servings.roundToInt()} servings"
                    append("About $servTxt of ${rec.name} would fit.\n")
                }
            } else if (recipes.isNotEmpty()) {
                append("I don't have an exact recipe match for those ingredients, but here are good options for your $diet diet:\n\n")
                recipes.take(2).forEach { r ->
                    append("⭐ ${r.name}\n")
                    append("   ${r.caloriesPerServing} cal | ${r.proteinPerServing}g protein\n")
                    append("   Ingredients: ${r.ingredients.replace("\n", ", ").trim()}\n")
                    append("   Steps: ${r.instructions.replace("\n", " ").trim()}\n\n")
                }
            } else {
                append("I don't have matching recipes loaded yet. Try asking about your progress, BMI analysis, or exercise plans — I have all your health data ready!")
            }
        }
    }

    // ── 3. General recipe/meal suggestions ─────────────────────────────────
    if (isRecipeRequest || hasAnyOf(lowerInput, "meal", "suggest", "food", "eat", "recipe", "cook", "breakfast", "lunch", "dinner", "snack")) {
        return if (recipes.isNotEmpty()) {
            val rec = recipes.random()
            buildString {
                append("🍽️ Here's a recipe for you, $name:\n\n")
                append("⭐ ${rec.name}\n")
                append("${rec.caloriesPerServing} cal | ${rec.proteinPerServing}g protein | ${rec.carbsPerServing}g carbs | ${rec.fatPerServing}g fat\n")
                append("Diet: ${rec.dietType} | Prep: ${rec.prepTimeMinutes}min + Cook: ${rec.cookTimeMinutes}min\n\n")
                append("📝 Ingredients:\n${rec.ingredients}\n\n")
                append("👨‍🍳 Steps:\n${rec.instructions}\n\n")
                if (calRemaining > 0) {
                    append("This covers ${rec.caloriesPerServing} of your $calRemaining remaining calories.")
                    val left = calRemaining - rec.caloriesPerServing
                    if (left > 200) append(" You'd still have ~$left cal for another meal/snack.")
                }
            }
        } else {
            "I don't have recipes loaded for your $diet diet right now. Try asking about your progress, BMI, exercise plans, or water goals — I have all your health data ready!"
        }
    }

    // ── 4. BMI analysis ───────────────────────────────────────────────────
    if (isBmiRequest && bmi > 0f) {
        val cat = when {
            bmi < 18.5f -> "Underweight"
            bmi < 25f   -> "Healthy weight"
            bmi < 30f   -> "Overweight"
            else        -> "Obese"
        }
        val idealMin = (18.5f * (height / 100f) * (height / 100f)).roundToInt()
        val idealMax = (24.9f * (height / 100f) * (height / 100f)).roundToInt()
        return buildString {
            append("📊 BMI Analysis for $name\n\n")
            append("Your BMI is ${String.format("%.1f", bmi)} — $cat.\n")
            if (height > 0f) append("Healthy weight range for ${height.roundToInt()} cm: ${idealMin}–${idealMax} kg.\n\n")
            when {
                bmi < 18.5f -> {
                    append("🍽️ Focus on calorie-dense foods: nuts, avocado, whole grains, legumes, dairy.\n")
                    append("Eat 5–6 smaller meals. Target ${userPrefs.proteinGoal}g protein/day.\n\n")
                    append("🏃 Light strength training 3×/week to build muscle.\n")
                    append("💧 Water: ${(weight * 35).roundToInt()} ml/day.\n")
                    append("👣 Steps: 7,000–8,000/day.")
                }
                bmi < 25f -> {
                    append("🍽️ Keep your balanced diet. Protein target: ${userPrefs.proteinGoal}g.\n\n")
                    append("🏃 150 min moderate cardio + 2× strength/week.\n")
                    append("💧 Water: ${(weight * 35).roundToInt()} ml/day.\n")
                    append("👣 Steps: 8,000–10,000/day.")
                }
                bmi < 30f -> {
                    append("🍽️ Aim for 300–500 cal deficit. Cut refined carbs and sugars. Half your plate = veggies.\n\n")
                    append("🏃 30–45 min brisk walking daily + 2× strength sessions/week.\n")
                    append("💧 Water: ${(weight * 35).roundToInt()} ml/day.\n")
                    append("👣 Steps: 10,000/day minimum.")
                }
                else -> {
                    append("🍽️ 500 cal deficit. Prioritise protein (${userPrefs.proteinGoal}g). Avoid sugary drinks/fried food.\n\n")
                    append("🏃 Start with 20–30 min low-impact cardio daily. Add weights 2×/week.\n")
                    append("💧 Water: ${(weight * 40).roundToInt()} ml/day.\n")
                    append("👣 Steps: Build to 8,000–10,000/day gradually.")
                }
            }
        }
    }

    // ── 5. Exercise plan ──────────────────────────────────────────────────
    if (isExerciseRequest) {
        val actLevel = userPrefs.activityLevel.lowercase()
        return buildString {
            append("🏃 Exercise Plan for $name\n\n")
            when {
                actLevel.contains("sedentary") || actLevel.contains("low") -> {
                    append("Beginner-friendly week:\n")
                    append("• Mon/Wed/Fri: 20–30 min brisk walk\n")
                    append("• Tue/Thu: 10 min stretching or yoga\n")
                    append("• Sat: Light bodyweight circuit (squats, push-ups, lunges × 2 sets)\n")
                    append("• Sun: Rest\n\n")
                    append("👣 Step target: 6,000 → build to 8,000.\n")
                }
                actLevel.contains("moderate") -> {
                    append("Intermediate weekly plan:\n")
                    append("• Mon/Wed: 30–40 min cardio\n")
                    append("• Tue/Thu: Strength training — 3 sets × 10–12 reps\n")
                    append("• Fri: 20 min HIIT\n")
                    append("• Sat: Active rest (walk or yoga)\n\n")
                    append("👣 Step target: 8,000–10,000.\n")
                }
                else -> {
                    append("Advanced weekly split:\n")
                    append("• Mon/Tue: Push + Pull\n")
                    append("• Wed: 45 min zone-2 cardio\n")
                    append("• Thu: Legs + Core\n")
                    append("• Fri: HIIT 25–30 min\n")
                    append("• Sat: Active hobby • Sun: Recovery\n\n")
                    append("👣 Step target: 10,000–12,000.\n")
                }
            }
            if (weight > 0f && bmi > 0f) {
                append("\n💡 At ${String.format("%.1f", weight)}kg (BMI ${String.format("%.1f", bmi)}), ")
                append(if (bmi < 25f) "strength training will maintain your healthy composition." else "cardio + strength gives best fat-loss results.")
            }
        }
    }

    // ── 6. Water & steps ──────────────────────────────────────────────────
    if (isWaterRequest || isStepsRequest) {
        val w = if (weight > 0f) weight else 70f
        val waterMl = (w * 35).roundToInt()
        val waterGlasses = (waterMl / 250f).roundToInt()
        val steps = when {
            userPrefs.activityLevel.lowercase().let { it.contains("low") || it.contains("sedentary") } -> "6,000–8,000"
            userPrefs.activityLevel.lowercase().contains("moderate") -> "8,000–10,000"
            else -> "10,000–12,000"
        }
        return buildString {
            append("💧 Water & 👣 Steps for $name\n\n")
            append("Water: ~$waterMl ml/day ($waterGlasses glasses)\n")
            append("Formula: ${if (weight > 0f) "${weight.roundToInt()}kg" else "~70kg"} × 35ml = $waterMl ml\n")
            append("(Add 350–500ml per 30min of exercise)\n\n")
            append("Step goal: $steps steps/day\n\n")
            if (todaysLog != null) {
                val waterLeft = todaysLog.waterGoalMl - todaysLog.waterConsumedMl
                if (waterLeft > 0) append("⚠️ You still need ${waterLeft}ml more water today!\n")
                else append("✅ You've hit your water goal today!\n")
            }
            append("\nTips:\n")
            append("1. Keep a water bottle at your desk\n")
            append("2. Drink 1 glass before each meal\n")
            append("3. 10-min walk after meals = ~1,000 steps")
        }
    }

    // ── 7. Protein guide ──────────────────────────────────────────────────
    if (lowerInput.contains("protein")) {
        return buildString {
            append("🥩 Protein Guide for $name\n\n")
            append("Your target: ${userPrefs.proteinGoal}g/day")
            if (weight > 0f) append(" (${String.format("%.1f", userPrefs.proteinGoal.toFloat() / weight)}g per kg)")
            append("\n\n")
            if (todaysLog != null) {
                append("Today: ${todaysLog.proteinConsumed}/${todaysLog.proteinGoal}g (${protRemaining}g remaining)\n\n")
            }
            append("High-protein foods:\n")
            val proteinFoods = mentionedFoods.filter { (it.proteinG ?: 0.0) > 10.0 }
            if (proteinFoods.isNotEmpty()) {
                proteinFoods.forEach { f ->
                    append("• ${f.name}: ${f.proteinG?.roundToInt()}g protein per 100g\n")
                }
            } else {
                append("• Chicken breast: ~31g/100g\n")
                append("• Eggs: ~13g/100g\n")
                append("• Greek yogurt: ~10g/100g\n")
                append("• Lentils (dal): ~9g/100g\n")
                append("• Paneer: ~18g/100g\n")
            }
        }
    }

    // ── 8. Health tips ────────────────────────────────────────────────────
    if (hasAnyOf(lowerInput, "tip", "advice", "health")) {
        val tips = listOf(
            "Drink a glass of water before each meal — helps digestion and portion control.",
            "Aim for 30g of protein at breakfast to reduce cravings.",
            "A 10-minute walk after meals improves digestion and lowers blood sugar.",
            "Prep meals on Sunday so healthy choices are always ready.",
            "Sleep 7–8 hours — as crucial as nutrition for fat loss and muscle gain.",
            "Eat slowly; your brain takes 20 min to register fullness."
        )
        return "💡 $name, here's a tip: ${tips.random()}"
    }

    // ── 9. Smart fallback (replaces generic snapshot) ─────────────────────
    return buildString {
        append("Hey $name! I understood your message but I'm running in offline mode right now, so my answers are limited.\n\n")
        if (todaysLog != null && todaysLog.mealCount > 0) {
            append("📊 Today so far: ${todaysLog.caloriesConsumed}/${todaysLog.caloriesGoal} cal, ${todaysLog.proteinConsumed}/${todaysLog.proteinGoal}g protein, ${todaysLog.waterConsumedMl}/${todaysLog.waterGoalMl}ml water\n\n")
        }
        append("Here's what I can help you with using your data:\n")
        append("• \"How is my diet today?\" — full nutrition breakdown\n")
        append("• \"Suggest a meal\" — recipe from our ${recipes.size}+ recipes\n")
        append("• \"I have chicken and rice\" — matching recipes\n")
        append("• \"My BMI\" — BMI analysis + personalised plan\n")
        append("• \"Exercise plan\" — workout for your level\n")
        append("• \"Water goal\" — hydration + step targets\n")
        if (weight <= 0f || height <= 0f) {
            append("\n💡 Tip: Update your profile with height and weight for personalised advice!")
        }
    }
}

/** Returns true when [str] contains any of the given [terms] (case already lowercased by caller). */
private fun hasAnyOf(str: String, vararg terms: String) = terms.any { str.contains(it) }

/** Fast local greeting for simple messages — no LLM needed. */
private fun generateSimpleGreeting(input: String, name: String): String {
    val lower = input.lowercase().trim()
    return when {
        hasAnyOf(lower, "bye", "good night") -> "Goodbye, $name! Take care and stay healthy! 😊"
        hasAnyOf(lower, "thanks", "thank you") -> "You're welcome, $name! Happy to help! 💪"
        hasAnyOf(lower, "help", "what can you do") -> "Hey $name! I can help with:\n• Meal & recipe suggestions\n• Calorie & nutrition tracking\n• Exercise recommendations\n• BMI analysis\n• Water & step goals\n\nJust ask me anything!"
        else -> "Hey $name! 👋 How can I help you today? Ask me about meals, workouts, nutrition, or anything health-related!"
    }
}
