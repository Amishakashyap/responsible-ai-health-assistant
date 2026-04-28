package com.example.foodtracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.foodtracker.data.db.AppDatabase
import com.example.foodtracker.data.db.DailyNutritionLog
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WaterTrackingScreen(padding: PaddingValues = PaddingValues(0.dp)) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()

    val today = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    val glassSize = 250 // ml per glass

    var totalMl by remember { mutableStateOf(0) }
    var customMl by remember { mutableStateOf("") }
    var targetMl by remember { mutableStateOf(userPrefs.waterGoal.coerceAtLeast(500)) }
    var recentLogs by remember { mutableStateOf<List<DailyNutritionLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load today's water data from DB on enter
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                targetMl = userPrefs.waterGoal.coerceAtLeast(500)
                val log = db.dailyNutritionLogDao().getByUserAndDate(userPrefs.userId, today)
                if (log != null) {
                    totalMl = log.waterConsumedMl
                    if (log.waterGoalMl > 0) targetMl = log.waterGoalMl
                }
                recentLogs = db.dailyNutritionLogDao().getRecentLogs(userPrefs.userId, 7)
            } catch (e: Exception) {
                android.util.Log.e("WaterTracking", "Error loading data", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Persist water amount to DailyNutritionLog
    fun saveWater(newTotal: Int) {
        scope.launch {
            try {
                val existing = db.dailyNutritionLogDao().getByUserAndDate(userPrefs.userId, today)
                val log = if (existing != null) {
                    existing.copy(
                        waterConsumedMl = newTotal,
                        waterGoalMl = targetMl,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    DailyNutritionLog(
                        userId = userPrefs.userId,
                        date = today,
                        waterConsumedMl = newTotal,
                        waterGoalMl = targetMl,
                        caloriesGoal = userPrefs.calorieGoal,
                        proteinGoal = userPrefs.proteinGoal,
                        carbsGoal = userPrefs.carbsGoal,
                        fatGoal = userPrefs.fatGoal
                    )
                }
                db.dailyNutritionLogDao().upsert(log)
            } catch (e: Exception) {
                android.util.Log.e("WaterTracking", "Error saving water data", e)
            }
        }
    }

    val glasses = totalMl / glassSize

    if (isLoading) {
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        Modifier
            .padding(padding)
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Water Tracking", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        // Today's progress card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface)
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Daily Water Intake", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Text(
                    "$totalMl / $targetMl ml",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$glasses glasses",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (totalMl.toFloat() / targetMl).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                )
                if (totalMl >= targetMl) {
                    Spacer(Modifier.height(8.dp))
                    Text("Goal reached! Great job! 🎉", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Glass counter
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Add by Glass ($glassSize ml)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            if (totalMl >= glassSize) {
                                val newTotal = totalMl - glassSize
                                totalMl = newTotal
                                saveWater(newTotal)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = totalMl >= glassSize
                    ) {
                        Text("- 1 Glass")
                    }
                    Button(
                        onClick = {
                            val newTotal = totalMl + glassSize
                            totalMl = newTotal
                            saveWater(newTotal)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ 1 Glass")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Custom ml input
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Add Custom Amount (ml)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customMl,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*$"))) customMl = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter ml") },
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val ml = customMl.toIntOrNull() ?: 0
                            if (ml > 0) {
                                val newTotal = totalMl + ml
                                totalMl = newTotal
                                saveWater(newTotal)
                                customMl = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                totalMl = 0
                saveWater(0)
                customMl = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset Today")
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Target: $targetMl ml/day",
            style = MaterialTheme.typography.bodyMedium
        )

        // 7-day history
        if (recentLogs.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("Last 7 Days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            recentLogs.forEach { log ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(containerColor = AppSurface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(log.date, style = MaterialTheme.typography.bodyMedium)
                        val pct = if (log.waterGoalMl > 0) (log.waterConsumedMl * 100 / log.waterGoalMl) else 0
                        Text(
                            "${log.waterConsumedMl} ml ($pct%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (pct >= 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
