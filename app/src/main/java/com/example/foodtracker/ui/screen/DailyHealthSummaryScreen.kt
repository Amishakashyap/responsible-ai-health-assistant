package com.example.foodtracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.data.db.DailyHealthSummary
import com.example.foodtracker.data.db.DailyNutritionLog
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.domain.ai.AIConfig
import com.example.foodtracker.domain.health.HealthAdvisorService
import com.example.foodtracker.domain.health.HealthRulesEngine
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import com.example.foodtracker.ui.theme.AppTextSecondary
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyHealthSummaryScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val healthAdvisorService = remember { HealthAdvisorService(context) }
    val aiConfig = remember { AIConfig(context) }
    val scope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(true) }
    var isGenerating by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<DailyHealthSummary?>(null) }
    var todaysLog by remember { mutableStateOf<DailyNutritionLog?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    
    val currentDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    
    // Load today's summary and log
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            summary = healthAdvisorService.getTodaysSummary()
            todaysLog = healthAdvisorService.getTodaysLog()
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Health Insights") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackground)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Card
                HeaderCard(userPrefs.nickname, currentDate)

                Spacer(modifier = Modifier.height(16.dp))

                // Offline mode banner (no external AI provider)
                if (aiConfig.isEnabled && !aiConfig.isExternalProviderAvailable()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Offline mode — using local rules engine. Configure an AI provider in AI Settings for personalized insights.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // AI disabled banner
                if (!aiConfig.isEnabled) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "AI is disabled",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Enable AI in AI Settings to generate health insights.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Generate Summary Button (if no summary exists)
                if (summary == null && !isGenerating) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppSurface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Generate Your Daily Health Insights",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Get personalized recommendations, meal suggestions, and health tips based on your progress",
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = AppTextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        isGenerating = true
                                        errorMessage = ""
                                        val result = healthAdvisorService.generateDailySummary()
                                        if (result.success) {
                                            summary = result.summary
                                            todaysLog = healthAdvisorService.getTodaysLog()
                                        } else {
                                            errorMessage = result.error ?: "Failed to generate summary"
                                        }
                                        isGenerating = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Insights")
                            }
                        }
                    }
                }
                
                // Loading indicator while generating
                if (isGenerating) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppSurface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Analyzing your health data...",
                                fontSize = 16.sp,
                                color = AppTextSecondary
                            )
                        }
                    }
                }
                
                // Error message
                if (errorMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                errorMessage,
                                fontSize = 14.sp,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Display summary if available
                summary?.let { summaryData ->
                    
                    // Today's Progress Card
                    todaysLog?.let { log ->
                        TodaysProgressCard(log)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Daily Summary Card
                    InsightCard(
                        icon = Icons.Default.Star,
                        title = "Daily Summary",
                        content = summaryData.summaryText,
                        color = Color(0xFF4CAF50)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Meal Suggestion Card
                    InsightCard(
                        icon = Icons.Default.Home,
                        title = "Next Meal Suggestion",
                        content = summaryData.mealSuggestion,
                        color = Color(0xFFFF9800)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Recipe Recommendation (if available)
                    if (summaryData.recipeId != null) {
                        // TODO: Load and display recipe details
                    }
                    
                    // Habit Tip Card
                    InsightCard(
                        icon = Icons.Default.Info,
                        title = "Habit Tip",
                        content = summaryData.habitTip,
                        color = Color(0xFF9C27B0)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Health Warnings (if any)
                    val warnings = parseWarnings(summaryData.warnings)
                    if (warnings.isNotEmpty()) {
                        WarningsCard(warnings)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Motivational Message Card
                    MotivationalCard(summaryData.motivationalMessage)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Regenerate Button
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppSurface
                        )
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isGenerating = true
                                    val result = healthAdvisorService.generateDailySummary()
                                    if (result.success) {
                                        summary = result.summary
                                    }
                                    isGenerating = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Regenerate Insights")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HeaderCard(nickname: String, date: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            AppSurface,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "Hello, $nickname! 👋",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    date,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun TodaysProgressCard(log: DailyNutritionLog) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppSurface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Today's Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            ProgressRow(
                label = "Calories",
                consumed = log.caloriesConsumed,
                goal = log.caloriesGoal,
                unit = "cal",
                color = Color(0xFFFF5722)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            ProgressRow(
                label = "Protein",
                consumed = log.proteinConsumed,
                goal = log.proteinGoal,
                unit = "g",
                color = Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            ProgressRow(
                label = "Water",
                consumed = log.waterConsumedMl,
                goal = log.waterGoalMl,
                unit = "ml",
                color = Color(0xFF2196F3)
            )
        }
    }
}

@Composable
private fun ProgressRow(
    label: String,
    consumed: Int,
    goal: Int,
    unit: String,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "$consumed / $goal $unit",
                fontSize = 14.sp,
                color = AppTextSecondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = if (goal > 0) (consumed.toFloat() / goal).coerceIn(0f, 1f) else 0f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun InsightCard(
    icon: ImageVector,
    title: String,
    content: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppSurface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = AppTextSecondary
            )
        }
    }
}

@Composable
private fun WarningsCard(warnings: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Health Alerts",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            warnings.forEach { warning ->
                Text(
                    "• $warning",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFFD32F2F)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun MotivationalCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "✨",
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                message,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp
            )
        }
    }
}

private fun parseWarnings(warningsJson: String): List<String> {
    return try {
        if (warningsJson.isEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        Gson().fromJson(warningsJson, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}
