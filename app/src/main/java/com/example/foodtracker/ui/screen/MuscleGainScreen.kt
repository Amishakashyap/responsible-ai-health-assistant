package com.example.foodtracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodtracker.data.db.AppDatabase
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppPrimary
import com.example.foodtracker.ui.theme.AppSurface
import com.example.foodtracker.ui.theme.AppTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MuscleGainScreen(padding: PaddingValues = PaddingValues(0.dp)) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()

    var todayProtein by remember { mutableStateOf(0) }
    var todayCalories by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    val weight = userPrefs.weight
    val height = userPrefs.height
    val proteinGoal = userPrefs.proteinGoal
    val calorieGoal = userPrefs.calorieGoal
    val tdee = userPrefs.tdee
    val fitnessGoal = userPrefs.fitnessGoal.lowercase()

    // Personalized protein recommendation (2.0–2.2g per kg for muscle gain)
    val recommendedProteinLow = if (weight > 0) (weight * 2.0f).roundToInt() else proteinGoal
    val recommendedProteinHigh = if (weight > 0) (weight * 2.2f).roundToInt() else (proteinGoal * 1.1f).roundToInt()

    // Calorie surplus for muscle gain (+250–500 above TDEE)
    val surplusCalories = if (tdee > 0) calorieGoal - tdee else 0

    LaunchedEffect(Unit) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val log = db.dailyNutritionLogDao().getByUserAndDate(userPrefs.userId, today)
                todayProtein = log?.proteinConsumed ?: 0
                todayCalories = log?.caloriesConsumed ?: 0
            }
            isLoading = false
        }
    }

    val proteinProgress = if (proteinGoal > 0) (todayProtein.toFloat() / proteinGoal).coerceIn(0f, 1f) else 0f
    val calorieProgress = if (calorieGoal > 0) (todayCalories.toFloat() / calorieGoal).coerceIn(0f, 1f) else 0f

    Column(
        Modifier
            .padding(padding)
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Muscle Gain", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        if (weight > 0f) {
            Text(
                "Personalized for ${weight.roundToInt()}kg · ${fitnessGoal.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextSecondary
            )
        }
        Spacer(Modifier.height(16.dp))

        // Today's protein progress
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Today's Protein", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(
                        "${todayProtein}g / ${proteinGoal}g",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { proteinProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = AppPrimary,
                    trackColor = AppBackground
                )
                Spacer(Modifier.height(8.dp))
                val remaining = (proteinGoal - todayProtein).coerceAtLeast(0)
                Text(
                    if (remaining == 0) "Goal reached!" else "$remaining g more to hit your daily goal",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (remaining == 0) AppPrimary else AppTextSecondary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Today's calorie progress
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Today's Calories", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(
                        "$todayCalories / $calorieGoal kcal",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { calorieProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = AppPrimary,
                    trackColor = AppBackground
                )
                if (tdee > 0) {
                    Spacer(Modifier.height(8.dp))
                    val surplusText = when {
                        surplusCalories > 0 -> "+$surplusCalories kcal surplus above your TDEE ($tdee kcal)"
                        surplusCalories < 0 -> "${surplusCalories} kcal below your TDEE — consider eating more"
                        else -> "At maintenance calories (TDEE: $tdee kcal)"
                    }
                    Text(surplusText, style = MaterialTheme.typography.bodySmall, color = AppTextSecondary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Protein recommendation
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Your Protein Target", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (weight > 0)
                        "Based on your weight (${weight.roundToInt()}kg): $recommendedProteinLow–${recommendedProteinHigh}g/day"
                    else
                        "Set your weight in Profile to get a personalized protein target",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTextSecondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your current goal: ${proteinGoal}g/day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (weight > 0 && proteinGoal < recommendedProteinLow) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tip: For muscle gain, consider updating your goal to $recommendedProteinLow–${recommendedProteinHigh}g in Profile Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Tips based on fitness goal
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Muscle Gain Tips", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(8.dp))
                val tips = when {
                    fitnessGoal.contains("bulk") || fitnessGoal.contains("muscle") || fitnessGoal.contains("gain") -> listOf(
                        "Eat $recommendedProteinLow–${recommendedProteinHigh}g protein daily based on your ${weight.roundToInt()}kg",
                        "Maintain a 250–500 kcal surplus above your TDEE ($tdee kcal)",
                        "Prioritize compound lifts: squat, deadlift, bench, overhead press",
                        "Sleep 7–9 hours for optimal muscle recovery and growth hormone",
                        "Track progressive overload — add weight or reps weekly"
                    )
                    fitnessGoal.contains("recomp") || fitnessGoal.contains("lean") -> listOf(
                        "Eat at maintenance ($tdee kcal) with high protein ($recommendedProteinLow–${recommendedProteinHigh}g)",
                        "Recomposition is slower — be patient with weekly progress",
                        "Lift heavy 3–4x per week to preserve and build muscle",
                        "High protein preserves muscle while losing fat",
                        "Track measurements, not just weight on the scale"
                    )
                    else -> listOf(
                        "Aim for $recommendedProteinLow–${recommendedProteinHigh}g protein per day for muscle growth",
                        "Eat 250–500 kcal above your TDEE for muscle gain",
                        "Resistance train at least 3 times per week",
                        "Prioritize sleep: muscle is built during recovery",
                        "Stay consistent — muscle gain takes months, not weeks"
                    )
                }
                tips.forEach { tip ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("• ", color = AppPrimary)
                        Text(tip, style = MaterialTheme.typography.bodyMedium, color = AppTextSecondary)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // High-protein food suggestions
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("High-Protein Foods", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(8.dp))
                val foods = listOf(
                    "Chicken breast — 31g protein per 100g",
                    "Paneer — 18g protein per 100g",
                    "Eggs — 13g protein per 100g",
                    "Greek yoghurt — 10g protein per 100g",
                    "Lentils (dal) — 9g protein per 100g",
                    "Chickpeas — 9g protein per 100g"
                )
                foods.forEach { food ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("• ", color = AppPrimary)
                        Text(food, style = MaterialTheme.typography.bodySmall, color = AppTextSecondary)
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}
