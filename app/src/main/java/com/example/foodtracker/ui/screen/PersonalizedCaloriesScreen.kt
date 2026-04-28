package com.example.foodtracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.data.db.AppDatabase
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import com.example.foodtracker.utils.BMICalculator
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizedCaloriesScreen(
    padding: PaddingValues = PaddingValues(0.dp),
    onNavigateToAddFood: () -> Unit = {}
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    
    // User data
    val nickname = userPrefs.nickname
    val bmi = userPrefs.bmi
    val calorieGoal = userPrefs.calorieGoal
    val proteinGoal = userPrefs.proteinGoal
    val carbsGoal = userPrefs.carbsGoal
    val fatGoal = userPrefs.fatGoal
    
    // Real consumed data from database
    var caloriesConsumed by remember { mutableStateOf(0) }
    var proteinConsumed by remember { mutableStateOf(0) }
    var carbsConsumed by remember { mutableStateOf(0) }
    var fatConsumed by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Load consumed data from database
    LaunchedEffect(refreshTrigger) {
        scope.launch {
            try {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val entries = db.entryDao().getByDateAndUser(today, userPrefs.userId)
                
                var totalCal = 0.0
                var totalProt = 0.0
                var totalCarbs = 0.0
                var totalFat = 0.0
                
                entries.forEach { entry ->
                    val food = db.foodDao().getById(entry.foodId)
                    if (food != null) {
                        val factor = entry.quantityG / 100.0
                        totalCal += (food.calories ?: 0.0) * factor
                        totalProt += (food.proteinG ?: 0.0) * factor
                        totalCarbs += (food.carbsG ?: 0.0) * factor
                        totalFat += (food.fatG ?: 0.0) * factor
                    }
                }
                
                caloriesConsumed = totalCal.toInt()
                proteinConsumed = totalProt.toInt()
                carbsConsumed = totalCarbs.toInt()
                fatConsumed = totalFat.toInt()
            } catch (e: Exception) {
                android.util.Log.e("PersonalizedCalories", "Error loading nutrition data", e)
            } finally {
                isLoading = false
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(padding)
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Personalized Greeting
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "$nickname's Daily Nutrition 🍽️",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "BMI: ",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        "%.1f".format(bmi),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        BMICalculator.getBMIEmoji(bmi),
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        BMICalculator.getBMICategoryDescription(bmi),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Calories Card
        NutritionCard(
            title = "Calories",
            emoji = "🔥",
            consumed = caloriesConsumed,
            goal = calorieGoal,
            unit = "cal",
            color = Color(0xFFFF6B35),
            nickname = nickname
        )
        
        // Protein Card
        NutritionCard(
            title = "Protein",
            emoji = "💪",
            consumed = proteinConsumed,
            goal = proteinGoal,
            unit = "g",
            color = Color(0xFF4CAF50),
            nickname = nickname
        )
        
        // Carbs Card
        NutritionCard(
            title = "Carbohydrates",
            emoji = "🍞",
            consumed = carbsConsumed,
            goal = carbsGoal,
            unit = "g",
            color = Color(0xFF2196F3),
            nickname = nickname
        )
        
        // Fat Card
        NutritionCard(
            title = "Fat",
            emoji = "🥑",
            consumed = fatConsumed,
            goal = fatGoal,
            unit = "g",
            color = Color(0xFFFFC107),
            nickname = nickname
        )
        Spacer(Modifier.height(80.dp))
    }
    FloatingActionButton(
        onClick = { onNavigateToAddFood() },
        containerColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Food"
            )
            Spacer(Modifier.width(8.dp))
            Text("Add Food")
        }
    }
    }
}

@Composable
fun NutritionCard(
    title: String,
    emoji: String,
    consumed: Int,
    goal: Int,
    unit: String,
    color: Color,
    nickname: String
) {
    val remaining = goal - consumed
    val progress = if (goal > 0) (consumed.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "$consumed / $goal $unit",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = color
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.LightGray.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(color.copy(alpha = 0.7f), color)
                            )
                        )
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Remaining text
            Text(
                when {
                    remaining > 0 -> "$nickname, ${remaining}${unit} remaining"
                    remaining == 0 -> "Perfect! Goal achieved, $nickname! ✅"
                    else -> "Goal exceeded by ${-remaining}${unit}"
                },
                fontSize = 14.sp,
                color = when {
                    remaining > 0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    remaining == 0 -> Color(0xFF4CAF50)
                    else -> Color(0xFFFF9800)
                },
                fontWeight = if (remaining <= 0) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}
