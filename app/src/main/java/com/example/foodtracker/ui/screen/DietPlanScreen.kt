package com.example.foodtracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.data.diet.DietPlanManager
import com.example.foodtracker.data.diet.PlannedMeal
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface

@Composable
fun DietPlanScreen(padding: PaddingValues = PaddingValues(0.dp)) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val dietPlanManager = remember { DietPlanManager(context) }

    var meals by remember {
        mutableStateOf<List<PlannedMeal>>(emptyList())
    }
    var planSource by remember { mutableStateOf("default") }

    // Load plan on enter
    LaunchedEffect(Unit) {
        meals = if (dietPlanManager.hasPlan()) {
            planSource = dietPlanManager.getPlanSource()
            dietPlanManager.loadPlan()
        } else {
            planSource = "default"
            dietPlanManager.buildDefaultPlan(userPrefs.fitnessGoal, userPrefs.calorieGoal)
        }
    }

    Column(
        Modifier
            .padding(padding)
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Diet Plan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = {
                // Reset to default plan
                dietPlanManager.clearPlan()
                meals = dietPlanManager.buildDefaultPlan(userPrefs.fitnessGoal, userPrefs.calorieGoal)
                planSource = "default"
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset to default plan")
            }
        }

        // Source badge
        val sourceBadgeText = when (planSource) {
            "ai" -> "✨ Set from AI Trainer"
            "default" -> "📋 Default plan based on your goals"
            else -> "📋 Your saved plan"
        }
        Text(
            sourceBadgeText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Goal: ${userPrefs.fitnessGoal.replaceFirstChar { it.uppercase() }} · ${userPrefs.calorieGoal} cal/day",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        if (meals.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppSurface)
            ) {
                Text(
                    "No plan yet. Use the AI Trainer to get a personalized meal plan, then tap 'Apply to Diet Plan'.",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            meals.forEach { meal ->
                MealCard(meal)
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Tip: Chat with the AI Trainer and ask for a meal plan. Then tap \"Apply to Diet Plan\" to save it here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MealCard(meal: PlannedMeal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(meal.mealType, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (meal.time.isNotEmpty()) {
                    Text(meal.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (meal.calories > 0) {
                Text("~${meal.calories} cal · ${meal.protein}g protein", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            meal.items.lines().forEach { item ->
                if (item.isNotBlank()) {
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text("• ", color = MaterialTheme.colorScheme.primary)
                        Text(item, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
