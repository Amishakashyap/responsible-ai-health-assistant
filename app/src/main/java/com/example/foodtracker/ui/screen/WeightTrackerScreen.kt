package com.example.foodtracker.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.data.db.AppDatabase
import com.example.foodtracker.data.db.WeightEntry
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WeightTrackerScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()

    var input by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<WeightEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val zone = ZoneId.systemDefault()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                entries = db.weightEntryDao().getByUser(userPrefs.userId)
            } catch (e: Exception) {
                android.util.Log.e("WeightTracker", "Error loading entries", e)
            } finally {
                isLoading = false
            }
        }
    }

    val latestEntry = entries.firstOrNull()
    val userHeight = userPrefs.height
    val bmi = if (latestEntry != null && userHeight > 0f) {
        latestEntry.weightKg / ((userHeight / 100f) * (userHeight / 100f))
    } else null

    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(padding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Weight Tracker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // BMI display from latest weight
            if (bmi != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(containerColor = AppSurface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Current BMI", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "%.1f".format(bmi),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    bmi < 18.5f -> androidx.compose.ui.graphics.Color(0xFF2196F3)
                                    bmi < 25f -> MaterialTheme.colorScheme.primary
                                    bmi < 30f -> androidx.compose.ui.graphics.Color(0xFFFFA726)
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    bmi < 18.5f -> "Underweight"
                                    bmi < 25f -> "Normal"
                                    bmi < 30f -> "Overweight"
                                    else -> "Obese"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "Based on ${latestEntry!!.weightKg} kg, ${userHeight} cm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Input row
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Weight (kg)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val w = input.toFloatOrNull()
                    if (w == null || w <= 0f) {
                        scope.launch { snackbarHostState.showSnackbar("Enter a valid weight") }
                    } else {
                        scope.launch {
                            try {
                                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                db.weightEntryDao().insert(WeightEntry(userId = userPrefs.userId, date = today, weightKg = w))
                                userPrefs.weight = w
                                entries = db.weightEntryDao().getByUser(userPrefs.userId)
                                input = ""
                                snackbarHostState.showSnackbar("Weight saved: ${w} kg")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error saving weight")
                            }
                        }
                    }
                }) {
                    Text("Save")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Trend chart (shown when >= 2 entries)
            if (!isLoading && entries.size >= 2) {
                Text("Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(containerColor = AppSurface)
                ) {
                    val chartEntries = entries.reversed().takeLast(10)
                    val minW = chartEntries.minOf { it.weightKg }
                    val maxW = chartEntries.maxOf { it.weightKg }
                    val range = (maxW - minW).coerceAtLeast(0.5f)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val n = chartEntries.size
                        val xStep = if (n > 1) w / (n - 1).toFloat() else w
                        val pad = 8.dp.toPx()

                        // Grid lines
                        for (i in 0..2) {
                            val y = h * i / 2f
                            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                        }

                        // Trend line
                        for (i in 0 until n - 1) {
                            val x1 = i * xStep
                            val x2 = (i + 1) * xStep
                            val y1 = h - pad - ((chartEntries[i].weightKg - minW) / range) * (h - 2 * pad)
                            val y2 = h - pad - ((chartEntries[i + 1].weightKg - minW) / range) * (h - 2 * pad)
                            drawLine(primaryColor, Offset(x1, y1), Offset(x2, y2), strokeWidth = 2.dp.toPx())
                        }

                        // Data points
                        for (i in chartEntries.indices) {
                            val x = i * xStep
                            val y = h - pad - ((chartEntries[i].weightKg - minW) / range) * (h - 2 * pad)
                            drawCircle(primaryColor, radius = 4.dp.toPx(), center = Offset(x, y))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Text("Entries", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (entries.isEmpty()) {
                Text("No entries yet. Add your weight for today.")
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(entries, key = { it.id }) { entry ->
                        Card(
                            elevation = CardDefaults.cardElevation(0.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AppSurface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val displayTs = try {
                                    LocalDateTime.ofInstant(Instant.ofEpochMilli(entry.recordedAt), zone)
                                        .format(displayFormatter)
                                } catch (t: Throwable) { entry.date }
                                Text(displayTs, style = MaterialTheme.typography.bodyMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${entry.weightKg} kg", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    db.weightEntryDao().deleteById(entry.id)
                                                    entries = db.weightEntryDao().getByUser(userPrefs.userId)
                                                } catch (e: Exception) {
                                                    snackbarHostState.showSnackbar("Error deleting entry")
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Delete", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

