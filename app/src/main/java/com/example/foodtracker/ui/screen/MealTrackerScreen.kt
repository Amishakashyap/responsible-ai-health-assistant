package com.example.foodtracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodtracker.data.db.AppDatabase
import com.example.foodtracker.data.db.Entry
import com.example.foodtracker.data.db.Food
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class MealEntry(
    val entry: Entry,
    val food: Food,
    val calories: Double
)

@Composable
fun MealTrackerScreen(padding: PaddingValues = PaddingValues(0.dp)) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    
    var breakfastMeals by remember { mutableStateOf<List<MealEntry>>(emptyList()) }
    var lunchMeals by remember { mutableStateOf<List<MealEntry>>(emptyList()) }
    var dinnerMeals by remember { mutableStateOf<List<MealEntry>>(emptyList()) }
    var snackMeals by remember { mutableStateOf<List<MealEntry>>(emptyList()) }
    
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    LaunchedEffect(refreshTrigger) {
        scope.launch {
            try {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val entries = db.entryDao().getByDateAndUser(today, userPrefs.userId)
                
                val breakfast = mutableListOf<MealEntry>()
                val lunch = mutableListOf<MealEntry>()
                val dinner = mutableListOf<MealEntry>()
                val snack = mutableListOf<MealEntry>()
                
                entries.forEach { entry ->
                    val food = db.foodDao().getById(entry.foodId)
                    if (food != null) {
                        val factor = entry.quantityG / 100.0
                        val calories = (food.calories ?: 0.0) * factor
                        val mealEntry = MealEntry(entry, food, calories)
                        
                        when (entry.mealType.lowercase()) {
                            "breakfast" -> breakfast.add(mealEntry)
                            "lunch" -> lunch.add(mealEntry)
                            "dinner" -> dinner.add(mealEntry)
                            "snack" -> snack.add(mealEntry)
                        }
                    }
                }
                
                breakfastMeals = breakfast
                lunchMeals = lunch
                dinnerMeals = dinner
                snackMeals = snack
                
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }
    
    if (isLoading) {
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    
    val totalBreakfastCalories = breakfastMeals.sumOf { it.calories }
    val totalLunchCalories = lunchMeals.sumOf { it.calories }
    val totalDinnerCalories = dinnerMeals.sumOf { it.calories }
    val totalSnackCalories = snackMeals.sumOf { it.calories }
    val grandTotal = totalBreakfastCalories + totalLunchCalories + totalDinnerCalories + totalSnackCalories

    Column(
        Modifier
            .padding(padding)
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Meal Tracker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Track what you ate today",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        
        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppSurface
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Today's Total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "%.0f kcal".format(grandTotal),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Breakfast Section
        MealSection(
            title = "🌅 Breakfast",
            meals = breakfastMeals,
            totalCalories = totalBreakfastCalories,
            onDeleteMeal = { entryId ->
                scope.launch {
                    db.entryDao().deleteById(entryId)
                    refreshTrigger++
                }
            }
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Lunch Section
        MealSection(
            title = "☀️ Lunch",
            meals = lunchMeals,
            totalCalories = totalLunchCalories,
            onDeleteMeal = { entryId ->
                scope.launch {
                    db.entryDao().deleteById(entryId)
                    refreshTrigger++
                }
            }
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Dinner Section
        MealSection(
            title = "🌙 Dinner",
            meals = dinnerMeals,
            totalCalories = totalDinnerCalories,
            onDeleteMeal = { entryId ->
                scope.launch {
                    db.entryDao().deleteById(entryId)
                    refreshTrigger++
                }
            }
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Snacks Section
        MealSection(
            title = "🍿 Snacks",
            meals = snackMeals,
            totalCalories = totalSnackCalories,
            onDeleteMeal = { entryId ->
                scope.launch {
                    db.entryDao().deleteById(entryId)
                    refreshTrigger++
                }
            }
        )
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun MealSection(
    title: String,
    meals: List<MealEntry>,
    totalCalories: Double,
    onDeleteMeal: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "%.0f kcal".format(totalCalories),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            if (meals.isEmpty()) {
                Text(
                    "No items logged yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                meals.forEach { mealEntry ->
                    MealItem(
                        mealEntry = mealEntry,
                        onDelete = { onDeleteMeal(mealEntry.entry.id) }
                    )
                    if (mealEntry != meals.last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MealItem(
    mealEntry: MealEntry,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                mealEntry.food.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                "%.0f g".format(mealEntry.entry.quantityG),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "%.0f kcal".format(mealEntry.calories),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
